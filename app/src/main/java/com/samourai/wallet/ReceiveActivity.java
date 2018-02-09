package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.service.WebSocketHandler;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.uri.BitcoinURI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

public class ReceiveActivity extends Activity {

    private static Display display = null;
    private static int imgWidth = 0;

    private ImageView ivQR = null;
    private TextView tvAddress = null;
    private LinearLayout addressLayout = null;

    private EditText edAmountBTC = null;
    private EditText edAmountFiat = null;
    private TextWatcher textWatcherBTC = null;
    private TextWatcher textWatcherFiat = null;
    private Switch swSegwit = null;

    private String defaultSeparator = null;

    private String strFiat = null;
    private double btc_fx = 286.0;
    private TextView tvFiatSymbol = null;

    private String addr = null;
    private String addr44 = null;
    private String addr49 = null;

    private ArrayList<String> addrList44 = null;
    private ArrayList<String> addrList49 = null;

    private boolean canRefresh44 = false;
    private boolean canRefresh49 = false;
    private Menu _menu = null;

    public static final String ACTION_INTENT = "com.samourai.wallet.ReceiveFragment.REFRESH";

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(ACTION_INTENT.equals(intent.getAction())) {

                Bundle extras = intent.getExtras();
                if(extras != null && extras.containsKey("received_on"))	{
                    String in_addr = extras.getString("received_on");

                    if(in_addr.equals(addr) || in_addr.equals(addr44) || in_addr.equals(addr49))    {
                        ReceiveActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                ReceiveActivity.this.finish();

                            }
                        });

                    }

                }

            }

        }
    };

    public ReceiveActivity() {
        ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ReceiveActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        display = (ReceiveActivity.this).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        imgWidth = Math.max(size.x - 460, 150);

        swSegwit = (Switch)findViewById(R.id.segwit);
        swSegwit.setChecked(PrefsUtil.getInstance(ReceiveActivity.this).getValue(PrefsUtil.USE_SEGWIT, true));
        swSegwit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked)    {
                    addr = addr49;
                }
                else    {
                    addr = addr44;
                }

                displayQRCode();

            }
        });

        addr49 = AddressFactory.getInstance(ReceiveActivity.this).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
        addr44 = AddressFactory.getInstance(ReceiveActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();

        addrList44 = new ArrayList<String>();
        addrList49 = new ArrayList<String>();

        new LoadAddressesTask().execute();


        if(PrefsUtil.getInstance(ReceiveActivity.this).getValue(PrefsUtil.USE_SEGWIT, true) == true)    {
            addr = addr49;
        }
        else    {
            addr = addr44;
        }

        addressLayout = (LinearLayout)findViewById(R.id.receive_address_layout);
        addressLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                new AlertDialog.Builder(ReceiveActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addr);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(ReceiveActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

                return false;
            }
        });

        tvAddress = (TextView)findViewById(R.id.receive_address);

        ivQR = (ImageView)findViewById(R.id.qr);
        ivQR.setMaxWidth(imgWidth);

        ivQR.setOnTouchListener(new OnSwipeTouchListener(ReceiveActivity.this) {
            @Override
            public void onSwipeLeft() {
                if(swSegwit.isChecked() && canRefresh49) {
                    addr49 = AddressFactory.getInstance(ReceiveActivity.this).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
                    addr = addr49;
                    canRefresh49 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                }
                else if(!swSegwit.isChecked() && canRefresh44)   {
                    addr44 = AddressFactory.getInstance(ReceiveActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                    addr = addr44;
                    canRefresh44 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                }
                else    {
                    ;
                }

            }
        });

        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
        defaultSeparator = Character.toString(symbols.getDecimalSeparator());

        strFiat = PrefsUtil.getInstance(ReceiveActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        btc_fx = ExchangeRateFactory.getInstance(ReceiveActivity.this).getAvgPrice(strFiat);
        tvFiatSymbol = (TextView)findViewById(R.id.fiatSymbol);
        tvFiatSymbol.setText(getDisplayUnits() + "-" + strFiat);

        edAmountBTC = (EditText)findViewById(R.id.amountBTC);
        edAmountFiat = (EditText)findViewById(R.id.amountFiat);

        textWatcherBTC = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                edAmountBTC.removeTextChangedListener(this);
                edAmountFiat.removeTextChangedListener(textWatcherFiat);

                int max_len = 8;
                NumberFormat btcFormat = NumberFormat.getInstance(Locale.US);
                btcFormat.setMaximumFractionDigits(max_len + 1);
                btcFormat.setMinimumFractionDigits(0);

                double d = 0.0;
                try {
                    d = NumberFormat.getInstance(Locale.US).parse(s.toString()).doubleValue();
                    String s1 = btcFormat.format(d);
                    if (s1.indexOf(defaultSeparator) != -1) {
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmountBTC.setText(s1.substring(0, s1.length() - 1));
                                edAmountBTC.setSelection(edAmountBTC.getText().length());
                                s = edAmountBTC.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }
                catch(ParseException pe) {
                    ;
                }

                if(d > 21000000.0)    {
                    edAmountFiat.setText("0.00");
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                    edAmountBTC.setText("0");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    Toast.makeText(ReceiveActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
                else    {
                    edAmountFiat.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(d * btc_fx));
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                }

                edAmountFiat.addTextChangedListener(textWatcherFiat);
                edAmountBTC.addTextChangedListener(this);

                displayQRCode();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edAmountBTC.addTextChangedListener(textWatcherBTC);

        textWatcherFiat = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                edAmountFiat.removeTextChangedListener(this);
                edAmountBTC.removeTextChangedListener(textWatcherBTC);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.US);
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                double d = 0.0;
                try	{
                    d = NumberFormat.getInstance(Locale.US).parse(s.toString()).doubleValue();
                    String s1 = fiatFormat.format(d);
                    if(s1.indexOf(defaultSeparator) != -1)	{
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
                        if(dec.length() > 0)	{
                            dec = dec.substring(1);
                            if(dec.length() > max_len)	{
                                edAmountFiat.setText(s1.substring(0, s1.length() - 1));
                                edAmountFiat.setSelection(edAmountFiat.getText().length());
                            }
                        }
                    }
                }
                catch(NumberFormatException nfe)	{
                    ;
                }
                catch(ParseException pe) {
                    ;
                }

                if((d / btc_fx) > 21000000.0)    {
                    edAmountFiat.setText("0.00");
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                    edAmountBTC.setText("0");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    Toast.makeText(ReceiveActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
                else    {
                    edAmountBTC.setText(MonetaryUtil.getInstance().getBTCFormat().format(d / btc_fx));
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                }

                edAmountBTC.addTextChangedListener(textWatcherBTC);
                edAmountFiat.addTextChangedListener(this);

                displayQRCode();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edAmountFiat.addTextChangedListener(textWatcherFiat);

        displayQRCode();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(ReceiveActivity.this).registerReceiver(receiver, filter);

        AppUtil.getInstance(ReceiveActivity.this).checkTimeOut();

    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(ReceiveActivity.this).unregisterReceiver(receiver);

    }

    @Override
    public void onDestroy() {
        ReceiveActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_sweep).setVisible(false);
        menu.findItem(R.id.action_backup).setVisible(false);
        menu.findItem(R.id.action_scan_qr).setVisible(false);
        menu.findItem(R.id.action_utxo).setVisible(false);
        menu.findItem(R.id.action_tor).setVisible(false);
        menu.findItem(R.id.action_ricochet).setVisible(false);
        menu.findItem(R.id.action_empty_ricochet).setVisible(false);
        menu.findItem(R.id.action_sign).setVisible(false);
        menu.findItem(R.id.action_fees).setVisible(false);
        menu.findItem(R.id.action_batch).setVisible(false);
        menu.findItem(R.id.action_show_addresses).setVisible(false);

        _menu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();



        if (id == R.id.action_show_addresses){
            if(swSegwit.isChecked()){
                new AlertDialog.Builder(ReceiveActivity.this)
                        .setTitle(R.string.app_name)
                        .setItems(addrList49.toArray(new CharSequence[addrList49.size()]), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addrList49.get(item));
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(ReceiveActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        }).show();
            }
            else{
                new AlertDialog.Builder(ReceiveActivity.this)
                        .setTitle(R.string.app_name)
                        .setItems(addrList44.toArray(new CharSequence[addrList44.size()]), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addrList44.get(item));
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(ReceiveActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        }).show();
            }
        }

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_share_receive) {

            new AlertDialog.Builder(ReceiveActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.receive_address_to_share)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {

                            String strFileName = AppUtil.getInstance(ReceiveActivity.this).getReceiveQRFilename();
                            File file = new File(strFileName);
                            if(!file.exists()) {
                                try {
                                    file.createNewFile();
                                }
                                catch(Exception e) {
                                    Toast.makeText(ReceiveActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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

                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = null;
                            clip = android.content.ClipData.newPlainText("Receive address", addr);
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
                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                startActivity(Intent.createChooser(intent, ReceiveActivity.this.getText(R.string.send_payment_code)));
                            }

                        }

                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    ;
                }
            }).show();

        }
        else if (id == R.id.action_refresh) {

            if(swSegwit.isChecked() && canRefresh49) {
                addr49 = AddressFactory.getInstance(ReceiveActivity.this).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
                addr = addr49;
                canRefresh49 = false;
                item.setVisible(false);
                displayQRCode();
            }
            else if(!swSegwit.isChecked() && canRefresh44)   {
                addr44 = AddressFactory.getInstance(ReceiveActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                addr = addr44;
                canRefresh44 = false;
                item.setVisible(false);
                displayQRCode();
            }
            else    {
                ;
            }

        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayQRCode() {

        try {
            double amount = NumberFormat.getInstance(Locale.US).parse(edAmountBTC.getText().toString()).doubleValue();

            long lamount = (long)(amount * 1e8);
            if(lamount != 0L) {
                ivQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr), Coin.valueOf(lamount), null, null)));
            }
            else {
                ivQR.setImageBitmap(generateQRCode(addr));
            }
        }
        catch(NumberFormatException nfe) {
            ivQR.setImageBitmap(generateQRCode(addr));
        }
        catch(ParseException pe) {
            ivQR.setImageBitmap(generateQRCode(addr));
        }

        tvAddress.setText(addr);
        checkPrevUse();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(AppUtil.getInstance(ReceiveActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                    stopService(new Intent(ReceiveActivity.this.getApplicationContext(), WebSocketService.class));
                }
                startService(new Intent(ReceiveActivity.this.getApplicationContext(), WebSocketService.class));
            }
        }).start();

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

    private void checkPrevUse() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject jsonObject = APIFactory.getInstance(ReceiveActivity.this).getAddressInfo(addr);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if(jsonObject != null && jsonObject.has("addresses") && jsonObject.getJSONArray("addresses").length() > 0) {
                                    JSONArray addrs = jsonObject.getJSONArray("addresses");
                                    JSONObject _addr = addrs.getJSONObject(0);
                                    if(_addr.has("n_tx") && _addr.getLong("n_tx") > 0L) {
                                        Toast.makeText(ReceiveActivity.this, R.string.address_used_previously, Toast.LENGTH_SHORT).show();
                                        if(swSegwit.isChecked())    {
                                            canRefresh49 = true;
                                        }
                                        else    {
                                            canRefresh44 = true;
                                        }
                                        if(_menu != null)    {
                                            _menu.findItem(R.id.action_refresh).setVisible(true);
                                        }
                                    }
                                    else {
                                        if(swSegwit.isChecked())    {
                                            canRefresh49 = false;
                                        }
                                        else    {
                                            canRefresh44 = false;
                                        }
                                        if(_menu != null)    {
                                            _menu.findItem(R.id.action_refresh).setVisible(false);
                                        }
                                    }
                                }

                            } catch (Exception e) {
                                if(swSegwit.isChecked())    {
                                    canRefresh49 = false;
                                }
                                else    {
                                    canRefresh44 = false;
                                }
                                if(_menu != null)    {
                                    _menu.findItem(R.id.action_refresh).setVisible(false);
                                }
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    if(swSegwit.isChecked())    {
                        canRefresh49 = false;
                    }
                    else    {
                        canRefresh44 = false;
                    }
                    if(_menu != null)    {
                        _menu.findItem(R.id.action_refresh).setVisible(false);
                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private class LoadAddressesTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            int addrID44 = 0;
            int addrID49 = 0;

            try{
                JSONObject jsonObject;

                while(AddressFactory.getInstance(ReceiveActivity.this).canIncBIP49ReceiveAddress(addrID49)){

                    jsonObject =  APIFactory.getInstance(ReceiveActivity.this).getAddressInfo(AddressFactory.getInstance(ReceiveActivity.this).getBIP49(SamouraiWallet.SAMOURAI_ACCOUNT, AddressFactory.RECEIVE_CHAIN, addrID49).getAddressAsString());
                    if(jsonObject != null && jsonObject.has("addresses") && jsonObject.getJSONArray("addresses").length() > 0) {
                        JSONArray addrs = jsonObject.getJSONArray("addresses");
                        JSONObject _addr = addrs.getJSONObject(0);
                        if (!(_addr.has("n_tx") && _addr.getLong("n_tx") > 0L)) {
                            addrList49.add(AddressFactory.getInstance(ReceiveActivity.this).getBIP49(SamouraiWallet.SAMOURAI_ACCOUNT, AddressFactory.RECEIVE_CHAIN, addrID49).getAddressAsString());
                        }
                    }
                    addrID49++;
                }

                while(AddressFactory.getInstance(ReceiveActivity.this).canIncReceiveAddress(SamouraiWallet.SAMOURAI_ACCOUNT, addrID44)){

                    jsonObject =  APIFactory.getInstance(ReceiveActivity.this).getAddressInfo(AddressFactory.getInstance(ReceiveActivity.this).get(SamouraiWallet.SAMOURAI_ACCOUNT, AddressFactory.RECEIVE_CHAIN, addrID44).getAddressString());
                    if(jsonObject != null && jsonObject.has("addresses") && jsonObject.getJSONArray("addresses").length() > 0) {
                        JSONArray addrs = jsonObject.getJSONArray("addresses");
                        JSONObject _addr = addrs.getJSONObject(0);
                        if (!(_addr.has("n_tx") && _addr.getLong("n_tx") > 0L)) {
                            addrList44.add(AddressFactory.getInstance(ReceiveActivity.this).get(SamouraiWallet.SAMOURAI_ACCOUNT, AddressFactory.RECEIVE_CHAIN, addrID44).getAddressString());
                        }
                    }
                    addrID44++;
                }

            }
            catch(Exception e){

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            _menu.findItem(R.id.action_show_addresses).setVisible(true);
        }

    }

    public String getDisplayUnits() {

        return MonetaryUtil.getInstance().getBTCUnits();

    }

}
