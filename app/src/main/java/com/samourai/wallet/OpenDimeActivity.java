package com.samourai.wallet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.github.magnusja.libaums.javafs.JavaFsFileSystemCreator;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemFactory;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.send.SweepUtil;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.PrivKeyReader;

import org.bitcoinj.core.Coin;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenDimeActivity extends Activity {

    static {
        FileSystemFactory.registerFileSystem(new JavaFsFileSystemCreator());
    }

    private static final String ACTION_USB_PERMISSION = "com.samourai.wallet.USB_PERMISSION";
    private static final String TAG = OpenDimeActivity.class.getSimpleName();

    private List<UsbFile> files = new ArrayList<UsbFile>();
    private UsbFile currentDir = null;

    private TextView tvAddress = null;
    private TextView tvBalance = null;
    private TextView tvKey = null;

    private ImageView ivQR = null;

    private Button btTopUp = null;
    private Button btView = null;
    private Button btSweep = null;

    private String strAddress = null;
    private CharSequenceX strPrivKey = null;
    private long balance = 0L;

    private static Display display = null;
    private static int imgWidth = 0;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {

                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                    if (device != null) {
//                        setupDevice();
                        readOpenDimeUSB();
                    }
                }

            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Log.d(TAG, "USB device attached");

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    readOpenDimeUSB();
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Log.d(TAG, "USB device detached");

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    if (OpenDimeActivity.this.currentDevice != -1) {
                        OpenDimeActivity.this.massStorageDevices[currentDevice].close();
                    }
                    // check if there are other devices or set action bar title
                    // to no device if not
//                    readOpenDimeUSB();
                }

            }

        }
    };

    private Deque<UsbFile> dirs = new ArrayDeque<UsbFile>();
    private FileSystem currentFs = null;

    UsbMassStorageDevice[] massStorageDevices;
    private int currentDevice = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_opendime);

        setTitle(R.string.samourai_opendime);

        if(!AccessFactory.getInstance(OpenDimeActivity.this).isLoggedIn())    {
            Intent _intent = new Intent(OpenDimeActivity.this, PinEntryActivity.class);
            _intent.putExtra("opendime", true);
            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(_intent);
        }

        display = (OpenDimeActivity.this).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        imgWidth = Math.max(size.x - 300, 150);

        tvAddress = (TextView)findViewById(R.id.address);
        tvBalance = (TextView)findViewById(R.id.balanceAmount);
        tvKey = (TextView)findViewById(R.id.keyStatus);
        ivQR = (ImageView)findViewById(R.id.qr);
        btTopUp = (Button)findViewById(R.id.topup);
        btView = (Button)findViewById(R.id.view);
        btSweep = (Button)findViewById(R.id.sweep);

        btTopUp.setVisibility(View.GONE);
/*
        btTopUp = (Button)findViewById(R.id.topup);
        btTopUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


            }
        });
*/
        btView = (Button)findViewById(R.id.view);
        btView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(strAddress != null)    {
                    String blockExplorer = "https://m.oxt.me/transaction/";
                    if (SamouraiWallet.getInstance().isTestNet()) {
                        blockExplorer = "https://blockstream.info/testnet/";
                    }

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + strAddress));
                    startActivity(browserIntent);
                }

            }
        });

        btSweep = (Button)findViewById(R.id.sweep);
        btSweep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(strPrivKey != null)    {
                    SweepUtil.getInstance(OpenDimeActivity.this).sweep(new PrivKeyReader(strPrivKey), SweepUtil.TYPE_P2PKH);
                }

            }
        });

        ivQR.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(strAddress != null)    {

                    new AlertDialog.Builder(OpenDimeActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.receive_address_to_clipboard)
                            .setCancelable(false)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)OpenDimeActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                    android.content.ClipData clip = null;
                                    clip = android.content.ClipData.newPlainText("Receive address", strAddress);
                                    clipboard.setPrimaryClip(clip);
                                    Toast.makeText(OpenDimeActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                }

                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            ;
                        }
                    }).show();

                }

                return false;
            }
        });

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);

        readOpenDimeUSB();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.opendime_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_refresh) {

            readOpenDimeUSB();

        }
        else if (id == R.id.action_share_receive) {

            if(strAddress != null)    {

                new AlertDialog.Builder(OpenDimeActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_share)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                String strFileName = AppUtil.getInstance(OpenDimeActivity.this).getReceiveQRFilename();
                                File file = new File(strFileName);
                                if(!file.exists()) {
                                    try {
                                        file.createNewFile();
                                    }
                                    catch(Exception e) {
                                        Toast.makeText(OpenDimeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                file.setReadable(true, false);

                                FileOutputStream fos = null;
                                try {
                                    fos = new FileOutputStream(file);
                                }
                                catch(FileNotFoundException fnfe) {
                                    ;
                                }

                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)OpenDimeActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", strAddress);
                                clipboard.setPrimaryClip(clip);

                                if(file != null && fos != null) {
                                    Bitmap bitmap = ((BitmapDrawable)ivQR.getDrawable()).getBitmap();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                    try {
                                        fos.close();
                                    }
                                    catch(IOException ioe) {
                                        ;
                                    }

                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_SEND);
                                    intent.setType("image/png");
                                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                                        //From API 24 sending FIle on intent ,require custom file provider
                                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                OpenDimeActivity.this,
                                                getApplicationContext()
                                                        .getPackageName() + ".provider", file));
                                    } else {
                                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                    }                                    startActivity(Intent.createChooser(intent, OpenDimeActivity.this.getText(R.string.select_app)));
                                }

                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

            }

        }
        else    {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {

            AppUtil.getInstance(OpenDimeActivity.this).restartApp();

            return true;
        }
        else	{
            ;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        try {
            UsbFile dir = dirs.pop();
        }
        catch (NoSuchElementException e) {
            ;
        }

        AppUtil.getInstance(OpenDimeActivity.this).restartApp();

        super.onBackPressed();
    }

    private boolean hasPublicAddress()  {

        for(UsbFile usbFile : files)   {

            String s = readUsbFile(usbFile);

            if(usbFile.getName().equals("address.txt") && FormatsUtil.getInstance().isValidBitcoinAddress(s.trim()))	{
                strAddress = s.trim();
                return true;
            }

        }

        return false;
    }

    private boolean hasPrivkey()  {

        for(UsbFile usbFile : files)   {

            if(usbFile.getName().equals("private-key.txt"))	{
                return true;
            }

        }

        return false;
    }

    private boolean hasExposedPrivkey()  {

        for(UsbFile usbFile : files)   {

            if(usbFile.getName().equals("private-key.txt"))	{

                String s = readUsbFile(usbFile);

                if(s != null && s.contains("SEALED") && s.contains("README.txt"))    {
                    return false;
                }
                else if(s != null)    {
                    strPrivKey = new CharSequenceX(s.trim());
                    PrivKeyReader privkeyReader = new PrivKeyReader(strPrivKey);
                    try {
                        if(privkeyReader.getFormat() != null)   {
                            return true;
                        }
                        else    {
                            strPrivKey = null;
                            return false;
                        }
                    }
                    catch(Exception e) {
                        strPrivKey = null;
                        return false;
                    }
                }
                else    {
                    ;
                }
            }

        }

        return false;
    }

    private boolean hasValidatedSignedMessage()  {

        for(UsbFile usbFile : files)   {

            if(usbFile.getName().equals("verify2.txt"))	{

                String s = readUsbFile(usbFile);

                return verifySignedMessage(s.trim()) ? true : false;
            }

        }

        return false;
    }

    private String readUsbFile(UsbFile usbFile)    {

        String ret = null;

        try {
            UsbFileInputStream inputStream = new UsbFileInputStream(usbFile);
            byte[] buf = new byte[(int)usbFile.getLength()];
            inputStream.read(buf);
            ret = new String(buf, "UTF-8");
        }
        catch(IOException ioe) {
            ;
        }

        return ret;
    }

    private boolean verifySignedMessage(String strText)	{

        boolean ret = false;
        String[] s = strText.split("[\\r\\n]+");

        try	{
            ret = MessageSignUtil.getInstance(OpenDimeActivity.this).verifySignedMessage(s[1].trim(), s[0].trim(), s[2].trim());
        }
        catch(SignatureException se)	{
            ;
        }

        return ret;

    }

    private void readOpenDimeUSB()  {

        strAddress = null;
        strPrivKey = null;
        balance = 0L;

        OpenDimeActivity.this.runOnUiThread(new Runnable() {

            private Handler handler = new Handler();

            public void run() {

                UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                massStorageDevices = UsbMassStorageDevice.getMassStorageDevices(OpenDimeActivity.this);

                if (massStorageDevices.length == 0) {
                    Log.w(TAG, "no device found!");
                    return;
                }

                currentDevice = 0;

                UsbDevice usbDevice = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if(usbDevice == null)    {
                    HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
                    Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                    while(deviceIterator.hasNext()){
                        UsbDevice device = deviceIterator.next();
                        if(device != null)  {
                            usbDevice = device;
                            break;
                        }
                    }
                }

                if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
                    Log.d(TAG, "received usb device via intent");

                    // requesting permission is not needed in this case
                    try {
                        massStorageDevices[currentDevice].init();

                        // we always use the first partition of the device
                        currentFs = massStorageDevices[currentDevice].getPartitions().get(0).getFileSystem();
                        Log.d(TAG, "Capacity: " + currentFs.getCapacity());
                        Log.d(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
                        Log.d(TAG, "Free Space: " + currentFs.getFreeSpace());
                        Log.d(TAG, "Chunk size: " + currentFs.getChunkSize());
                        currentDir = currentFs.getRootDirectory();

                        files.clear();

                        UsbFile[] ufiles = currentDir.listFiles();
                        for(int i = 0; i < ufiles.length; i++)	{
                            Log.d("UsbFileListAdapter", "found root level file:" + ufiles[i].getName());
                            if(ufiles[i].getName().equals("address.txt") || ufiles[i].getName().equals("private-key.txt"))	{
                                files.add(ufiles[i]);
                            }
                            if(ufiles[i].isDirectory())	{

                                Log.d("UsbFileListAdapter", "is directory:" + ufiles[i].getName());

                                UsbFile advancedDir = ufiles[i];
                                UsbFile[] afiles = advancedDir.listFiles();
                                for(int j = 0; j < afiles.length; j++)	{
                                    Log.d("UsbFileListAdapter", "found inner dir file:" + afiles[j].getName());
                                    if(afiles[j].getName().equals("verify2.txt"))	{
                                        files.add(afiles[j]);
                                    }

                                }

                            }

                        }

                        if(hasPrivkey() && hasExposedPrivkey() && hasPublicAddress())    {
                            handler.post(new Runnable() {
                                public void run() {
//                                    Toast.makeText(OpenDimeActivity.this, "spendable|consultable", Toast.LENGTH_LONG).show();
                                    tvAddress.setText(strAddress);
                                    btSweep.setVisibility(View.VISIBLE);
//                                    btTopUp.setVisibility(View.VISIBLE);
                                    btView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        else if(hasPrivkey() && !hasExposedPrivkey() && hasPublicAddress())   {
                            handler.post(new Runnable() {
                                public void run() {
//                                    Toast.makeText(OpenDimeActivity.this, "not spendable|consultable", Toast.LENGTH_LONG).show();
                                    tvAddress.setText(strAddress);
                                    btSweep.setVisibility(View.GONE);
//                                    btTopUp.setVisibility(View.VISIBLE);
                                    btView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        else if(hasPublicAddress())   {
                            handler.post(new Runnable() {
                                public void run() {
//                                    Toast.makeText(OpenDimeActivity.this, "consultable", Toast.LENGTH_LONG).show();
                                    tvAddress.setText(strAddress);
                                    btSweep.setVisibility(View.GONE);
//                                    btTopUp.setVisibility(View.VISIBLE);
                                    btView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        else    {
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(OpenDimeActivity.this, "not initialised", Toast.LENGTH_LONG).show();
                                    btSweep.setVisibility(View.GONE);
//                                    btTopUp.setVisibility(View.GONE);
                                    btView.setVisibility(View.GONE);
                                }
                            });
                        }

                        if(hasValidatedSignedMessage() && hasPublicAddress())    {
                            handler.post(new Runnable() {
                                public void run() {
//                                    Toast.makeText(OpenDimeActivity.this, "validated", Toast.LENGTH_LONG).show();
                                    Spannable spannable = new SpannableString(OpenDimeActivity.this.getText(R.string.opendime_verified) + " - " + OpenDimeActivity.this.getText(R.string.opendime_valid_key));
                                    spannable.setSpan(new ForegroundColorSpan(Color.GREEN), 0, OpenDimeActivity.this.getText(R.string.opendime_verified).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    tvKey.setText(spannable);
                                }
                            });
                        }
                        else if(!hasValidatedSignedMessage() && hasPublicAddress())    {
                            handler.post(new Runnable() {
                                public void run() {
//                                    Toast.makeText(OpenDimeActivity.this, "invalidated", Toast.LENGTH_LONG).show();
                                    Spannable spannable = new SpannableString(OpenDimeActivity.this.getText(R.string.opendime_not_verified) + " - " + OpenDimeActivity.this.getText(R.string.opendime_no_valid_key));
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, OpenDimeActivity.this.getText(R.string.opendime_not_verified).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    tvKey.setText(spannable);
                                    btSweep.setVisibility(View.GONE);
//                                    btTopUp.setVisibility(View.GONE);
                                    btView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        else    {
                            ;
                        }

                        if(strAddress != null)    {
                            ivQR.setImageBitmap(generateQRCode(strAddress));

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Looper.prepare();

                                    final JSONObject obj = APIFactory.getInstance(OpenDimeActivity.this).getAddressInfo(strAddress);
                                    JSONObject walletObj = null;

                                    try {

                                        if(obj != null && obj.has("wallet"))    {
                                            walletObj = obj.getJSONObject("wallet");
                                            if(walletObj.has("final_balance"))    {
                                                balance = walletObj.getLong("final_balance");
                                            }
                                        }

                                    }
                                    catch(JSONException je) {
                                        ;
                                    }

                                    if(walletObj != null && walletObj.has("final_balance"))    {
                                        handler.post(new Runnable() {
                                            public void run() {
                                                String strBalance = "" + Coin.valueOf(balance).toPlainString() + " BTC";
                                                if(balance > 0L && strPrivKey != null && strPrivKey.length() > 0)    {
                                                    btSweep.setVisibility(View.VISIBLE);
                                                }
                                                else    {
                                                    btSweep.setVisibility(View.GONE);
                                                }
                                                tvBalance.setText(strBalance);
                                            }
                                        });
                                    }

                                    Looper.loop();

                                }
                            }).start();

                        }

                    }
                    catch (IOException e) {
                        Log.e(TAG, "error setting up device", e);
                    }

                }
                else {
                    // first request permission from user to communicate with the
                    // underlying
                    // UsbDevice
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(OpenDimeActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(massStorageDevices[currentDevice].getUsbDevice(), permissionIntent);
                }

            }

        });

    }

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

}
