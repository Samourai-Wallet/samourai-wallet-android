package com.samourai.wallet.bip47;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.R;
import com.squareup.picasso.Picasso;

import org.bitcoinj.core.AddressFormatException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BIP47ShowQR extends Activity {

    private static Display display = null;
    private static int imgWidth = 0;

    private ImageView ivQR = null;
    private TextView tvAddress = null;
    private LinearLayout addressLayout = null;

    private String addr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bip47_show_qr);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("label") && extras.containsKey("pcode"))	{
            setTitle(extras.getString("label"));
            addr = extras.getString("pcode");
        }
        else    {
            setTitle(getText(R.string.bip47_setup1_title));

            try {
                addr = BIP47Util.getInstance(BIP47ShowQR.this).getPaymentCode().toString();
            }
            catch(AddressFormatException afe) {
                ;
            }
        }

        display = (BIP47ShowQR.this).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        imgWidth = Math.max(size.x - 360, 150);

        addressLayout = (LinearLayout)findViewById(R.id.receive_address_layout);
        addressLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                new AlertDialog.Builder(BIP47ShowQR.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addr);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(BIP47ShowQR.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

                return false;
            }
        });

        tvAddress = (TextView)findViewById(R.id.show_text);

        ivQR = (ImageView)findViewById(R.id.qr);
        ivQR.setMaxWidth(imgWidth);

        displayQRCode();

        doPayNymTask();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bip47_menu_show_qr, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
        }
        else if(id == R.id.action_share_receive) {

            new AlertDialog.Builder(BIP47ShowQR.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.receive_address_to_share)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {

                            String strFileName = AppUtil.getInstance(BIP47ShowQR.this).getReceiveQRFilename();
                            File file = new File(strFileName);
                            if(!file.exists()) {
                                try {
                                    file.createNewFile();
                                }
                                catch(Exception e) {
                                    Toast.makeText(BIP47ShowQR.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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

                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getSystemService(android.content.Context.CLIPBOARD_SERVICE);
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
                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                    //From API 24 sending FIle on intent ,require custom file provider
                                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                            BIP47ShowQR.this,
                                            getApplicationContext()
                                                    .getPackageName() + ".provider", file));
                                } else {
                                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                }
                                 startActivity(Intent.createChooser(intent, BIP47ShowQR.this.getText(R.string.send_payment_code)));
                            }

                        }

                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    ;
                }
            }).show();

        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayQRCode() {

        ivQR.setImageBitmap(generateQRCode(addr));

        tvAddress.setText(addr);
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

    private void doPayNymTask() {

        new Thread(new Runnable() {

            private Handler handler = new Handler();

            @Override
            public void run() {

                Looper.prepare();

                final String strPaymentCode = addr;

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("nym", strPaymentCode);
                    String res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(BIP47ShowQR.this).postURL("application/json", null, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + "api/v1/nym", obj.toString());
                    Log.d("BIP47Activity", res);

                    JSONObject responseObj = new JSONObject(res);
                    if(responseObj.has("codes"))    {
                        JSONArray array = responseObj.getJSONArray("codes");
                        if(array.getJSONObject(0).has("claimed") && array.getJSONObject(0).getBoolean("claimed") == true)    {
                            final String strNymName = responseObj.getString("nymName");
                            handler.post(new Runnable() {
                                public void run() {
                                    Log.d("BIP47Activity", strNymName);

                                    final ImageView ivAvatar = (ImageView) findViewById(R.id.avatar);
                                    Picasso.with(BIP47ShowQR.this).load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + strPaymentCode + "/avatar").into(ivAvatar);

                                    ((TextView)findViewById(R.id.nymName)).setText(strNymName);

                                }
                            });
                        }
                        else    {
                            handler.post(new Runnable() {
                                public void run() {
                                    ((TextView)findViewById(R.id.nymName)).setVisibility(View.GONE);
                                    ((ImageView)findViewById(R.id.avatar)).setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                    else    {
                        handler.post(new Runnable() {
                            public void run() {
                                ((TextView)findViewById(R.id.nymName)).setVisibility(View.GONE);
                                ((ImageView)findViewById(R.id.avatar)).setVisibility(View.GONE);
                            }
                        });
                    }

                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                Looper.loop();

            }
        }).start();
    }

}
