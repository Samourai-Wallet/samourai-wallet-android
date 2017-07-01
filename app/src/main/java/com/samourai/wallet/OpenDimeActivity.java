package com.samourai.wallet;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import com.github.magnusja.libaums.javafs.JavaFsFileSystemCreator;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemFactory;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.PrivKeyReader;

public class OpenDimeActivity extends Activity {

    static {
        FileSystemFactory.registerFileSystem(new JavaFsFileSystemCreator());
    }

    private static final String ACTION_USB_PERMISSION = "com.samourai.wallet.USB_PERMISSION";
    private static final String TAG = MainActivity.class.getSimpleName();

    private List<UsbFile> files = new ArrayList<UsbFile>();
    private UsbFile currentDir = null;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {

                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                    if (device != null) {
                        setupDevice();
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Log.d(TAG, "USB device attached");

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    discoverDevice();
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Log.d(TAG, "USB device detached");

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    if (OpenDimeActivity.this.currentDevice != -1) {
                        OpenDimeActivity.this.massStorageDevices[currentDevice].close();
                    }
                    // check if there are other devices or set action bar title
                    // to no device if not
                    discoverDevice();
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

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);

        discoverDevice();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void selectDevice(int position) {
        currentDevice = position;
        setupDevice();
    }

    private void discoverDevice() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        massStorageDevices = UsbMassStorageDevice.getMassStorageDevices(this);

        if (massStorageDevices.length == 0) {
            Log.w(TAG, "no device found!");
            return;
        }

        currentDevice = 0;

        UsbDevice usbDevice = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
            Log.d(TAG, "received usb device via intent");
            // requesting permission is not needed in this case
            setupDevice();
        } else {
            // first request permission from user to communicate with the
            // underlying
            // UsbDevice
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(massStorageDevices[currentDevice].getUsbDevice(), permissionIntent);
        }
    }

    private void setupDevice() {
        try {
            massStorageDevices[currentDevice].init();

            // we always use the first partition of the device
            currentFs = massStorageDevices[currentDevice].getPartitions().get(0).getFileSystem();
            Log.d(TAG, "Capacity: " + currentFs.getCapacity());
            Log.d(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
            Log.d(TAG, "Free Space: " + currentFs.getFreeSpace());
            Log.d(TAG, "Chunk size: " + currentFs.getChunkSize());
            currentDir = currentFs.getRootDirectory();

            refresh();

        } catch (IOException e) {
            Log.e(TAG, "error setting up device", e);
        }

    }
/*
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {

        UsbFile entry = adapter.getItem(position);

        try {
            UsbFileInputStream inputStream = new UsbFileInputStream(entry);
            byte[] buf = new byte[(int)entry.getLength()];
            inputStream.read(buf);
            String s = new String(buf, "UTF-8");

            if(entry.getName().equals("private-key.txt") && s.contains("SEALED") && s.contains("README.txt"))	{
                Toast.makeText(OpenDimeActivity.this, "Private key sealed", Toast.LENGTH_LONG).show();
            }
            else if(entry.getName().equals("private-key.txt"))	{
                Toast.makeText(OpenDimeActivity.this, s.trim(), Toast.LENGTH_LONG).show();
            }
            else if(entry.getName().equals("address.txt"))	{
                Toast.makeText(OpenDimeActivity.this, s.trim(), Toast.LENGTH_LONG).show();
            }
            else if(entry.getName().equals("verify2.txt"))	{
                Toast.makeText(OpenDimeActivity.this, "Verify signed message:" + verifySignedMessage(s), Toast.LENGTH_LONG).show();
            }
            else	{
                ;
            }

        }
        catch(IOException ioe) {
            ;
        }

    }
*/
    @Override
    public void onBackPressed() {
        try {
            UsbFile dir = dirs.pop();
//            listView.setAdapter(adapter = new UsbFileListAdapter(this, dir));
        }
        catch (NoSuchElementException e) {
            super.onBackPressed();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);

    }

    public void refresh() throws IOException {

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

        Toast.makeText(OpenDimeActivity.this, "Files found:" + files.size(), Toast.LENGTH_LONG).show();

        if(hasPrivkey() && hasExposedPrivkey() && hasPublicAddress())    {
            Toast.makeText(OpenDimeActivity.this, "spendable|consultable", Toast.LENGTH_LONG).show();
        }
        else if(hasPrivkey() && !hasExposedPrivkey() && hasPublicAddress())   {
            Toast.makeText(OpenDimeActivity.this, "not spendable|consultable", Toast.LENGTH_LONG).show();
        }
        else if(hasPublicAddress())   {
            Toast.makeText(OpenDimeActivity.this, "consultable", Toast.LENGTH_LONG).show();
        }
        else    {
            Toast.makeText(OpenDimeActivity.this, "not initialised", Toast.LENGTH_LONG).show();
        }

        if(hasValidatedSignedMessage() && hasPublicAddress())    {
            Toast.makeText(OpenDimeActivity.this, "validated", Toast.LENGTH_LONG).show();
        }
        else if(!hasValidatedSignedMessage() && hasPublicAddress())    {
            Toast.makeText(OpenDimeActivity.this, "invalidated", Toast.LENGTH_LONG).show();
        }
        else    {
            ;
        }

    }

    private boolean hasPublicAddress()  {

        for(UsbFile usbFile : files)   {

            String s = readUsbFile(usbFile);

            if(usbFile.getName().equals("address.txt") && FormatsUtil.getInstance().isValidBitcoinAddress(s.trim()))	{
//                    Toast.makeText(OpenDimeActivity.this, s.trim(), Toast.LENGTH_LONG).show();
                return true;
            }

        }

        return false;
    }

    private boolean hasPrivkey()  {

        for(UsbFile usbFile : files)   {

            if(usbFile.getName().equals("private-key.txt"))	{
//                    Toast.makeText(OpenDimeActivity.this, s.trim(), Toast.LENGTH_LONG).show();
                return true;
            }

        }

        return false;
    }

    private boolean hasExposedPrivkey()  {

        for(UsbFile usbFile : files)   {

            if(usbFile.getName().equals("private-key.txt"))	{
//                    Toast.makeText(OpenDimeActivity.this, s.trim(), Toast.LENGTH_LONG).show();

                String s = readUsbFile(usbFile);

                if(s.contains("SEALED") && s.contains("README.txt"))    {
                    return false;
                }
                else    {
                    PrivKeyReader privkeyReader = new PrivKeyReader(new CharSequenceX(s.trim()));
                    try {
                        if(privkeyReader.getFormat() != null)   {
                            return true;
                        }
                    }
                    catch(Exception e) {
                        return false;
                    }
                }
            }

        }

        return false;
    }

    private boolean hasValidatedSignedMessage()  {

        for(UsbFile usbFile : files)   {

            if(usbFile.getName().equals("verify2.txt"))	{

                String s = readUsbFile(usbFile);

//                    Toast.makeText(OpenDimeActivity.this, s.trim(), Toast.LENGTH_LONG).show();
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
            ret = MessageSignUtil.getInstance(OpenDimeActivity.this).verifyMessage(s[1].trim(), s[0].trim(), s[2].trim());
        }
        catch(SignatureException se)	{
            ;
        }

        return ret;

    }

}
