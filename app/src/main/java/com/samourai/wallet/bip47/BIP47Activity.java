package com.samourai.wallet.bip47;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import net.sourceforge.zbar.Symbol;

import org.apache.commons.lang3.StringEscapeUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.base.Splitter;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.SecretPoint;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.R;

import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.util.WebUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class BIP47Activity extends Activity {

    private static final int EDIT_PCODE = 2000;
    private static final int RECOMMENDED_PCODE = 2001;
    private static final int SCAN_PCODE = 2077;

    private SwipeMenuListView listView = null;
    BIP47EntryAdapter adapter = null;
    private String[] pcodes = null;
    private static HashMap<String,String> meta = null;
    private static HashMap<String,Bitmap> bitmaps = null;

    private FloatingActionsMenu ibBIP47Menu = null;
    private FloatingActionButton actionAdd = null;
    private FloatingActionButton actionPartners = null;
    private FloatingActionButton actionSign = null;

    private Timer timer = null;
    private Handler handler = null;

    private ProgressDialog progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bip47_list);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        ibBIP47Menu = (FloatingActionsMenu)findViewById(R.id.bip47_menu);

        actionAdd = (FloatingActionButton)findViewById(R.id.bip47_add);
        actionAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                doAdd();
            }
        });

        actionPartners = (FloatingActionButton)findViewById(R.id.bip47_partners);
        actionPartners.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                /*
                AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                        .setTitle(R.string.app_name)
                        .setMessage("Want to see your payment code suggested here? Contact us at wallet@samouraiwallet.com.")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }

                        });

                dlg.show();
                */

                Intent intent = new Intent(BIP47Activity.this, BIP47Recommended.class);
                startActivityForResult(intent, RECOMMENDED_PCODE);

            }
        });

        actionSign = (FloatingActionButton)findViewById(R.id.bip47_sign);
        actionSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                doSign();
            }
        });

        if(meta == null)    {
            meta = new HashMap<String,String>();
        }
        if(bitmaps == null)    {
            bitmaps = new HashMap<String,Bitmap>();
        }

        listView = (SwipeMenuListView) findViewById(R.id.list);

        handler = new Handler();
        refreshList();

        adapter = new BIP47EntryAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final String itemValue = (String) listView.getItemAtPosition(position);
                String msg = "";
                if (BIP47Meta.getInstance().getLabel(itemValue) != null && BIP47Meta.getInstance().getLabel(itemValue).length() > 0) {
                    msg = BIP47Meta.getInstance().getLabel(itemValue) + ":";
                }
//                Toast.makeText(getApplicationContext(), msg + "Outgoing status:" + BIP47Meta.getInstance().getOutgoingStatus(itemValue), Toast.LENGTH_LONG).show();

                if (BIP47Meta.getInstance().getOutgoingStatus(itemValue) == BIP47Meta.STATUS_NOT_SENT) {

                    doNotifTx(itemValue);

                }
                else if (BIP47Meta.getInstance().getOutgoingStatus(itemValue) == BIP47Meta.STATUS_SENT_NO_CFM) {

//                    Toast.makeText(BIP47Activity.this, R.string.bip47_wait_for_confirmation, Toast.LENGTH_SHORT).show();

                    AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.bip47_wait_for_confirmation_or_retry)
                            .setCancelable(false)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    doNotifTx(itemValue);

                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            });
                    if(!isFinishing())    {
                        dlg.show();
                    }

                }
                else {

                    AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.bip47_spend_to_pcode)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    AppUtil.getInstance(BIP47Activity.this).restartApp("pcode", itemValue);

                                }

                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            });

                    dlg.show();

                }

            }

        });

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {

                switch (index) {

                    case 0:

                    {
                        Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
                        intent.putExtra("label", BIP47Meta.getInstance().getLabel(pcodes[position]));
                        intent.putExtra("pcode", pcodes[position]);
                        startActivityForResult(intent, EDIT_PCODE);
                    }

                        break;

                    case 1:

                        doSync(pcodes[position]);

                        break;

                    case 2:

                    {
                        Intent intent = new Intent(BIP47Activity.this, BIP47ShowQR.class);
                        intent.putExtra("label", BIP47Meta.getInstance().getLabel(pcodes[position]));
                        intent.putExtra("pcode", pcodes[position]);
                        startActivity(intent);
                    }

                        break;

                    case 3:

                        // archive
                        BIP47Meta.getInstance().setArchived(pcodes[position], true);
                        refreshList();
                        adapter.notifyDataSetChanged();

                        break;

                }

                return false;
            }
        });

        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {

                int outgoing = BIP47Meta.getInstance().getOutgoingIdx(pcodes[position]);
                int incoming = BIP47Meta.getInstance().getIncomingIdx(pcodes[position]);

//                Toast.makeText(BIP47Activity.this, pcodes[position], Toast.LENGTH_SHORT).show();
                Toast.makeText(BIP47Activity.this, "Incoming index:" + incoming + ", Outgoing index:" + outgoing, Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                // create "edit" item
                SwipeMenuItem openItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                openItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                openItem.setWidth(180);
                // set a icon
                openItem.setIcon(R.drawable.ic_edit_white_24dp);
                // add to menu
                menu.addMenuItem(openItem);

                // create "sync" item
                SwipeMenuItem syncItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                syncItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                syncItem.setWidth(180);
                // set a icon
                syncItem.setIcon(android.R.drawable.ic_popup_sync);
                // add to menu
                menu.addMenuItem(syncItem);

                // create "qr" item
                SwipeMenuItem qrItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                qrItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                qrItem.setWidth(180);
                // set a icon
                qrItem.setIcon(R.drawable.ic_receive_qr);
                // add to menu
                menu.addMenuItem(qrItem);

                // create "qr" item
                SwipeMenuItem archiveItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                archiveItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                archiveItem.setWidth(180);
                // set a icon
                archiveItem.setIcon(android.R.drawable.ic_media_pause);
                // add to menu
                menu.addMenuItem(archiveItem);

            }
        };

        listView.setMenuCreator(creator);
        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

        doTimer();

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("pcode"))	{
            String pcode = extras.getString("pcode");
            String meta = null;
            if(extras.containsKey("meta"))    {
                meta = extras.getString("meta");
            }

            String _meta = null;
            try {
                Map<String, String> map = new HashMap<String,String>();
                if(meta != null && meta.length() > 0)    {
                    _meta = URLDecoder.decode(meta, "UTF-8");
                    map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(_meta);
                }

                Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
                intent.putExtra("pcode", pcode);
                intent.putExtra("label", map.containsKey("title") ? map.get("title") : "");
                startActivityForResult(intent, EDIT_PCODE);
            }
            catch(UnsupportedEncodingException uee) {
                ;
            }
            catch(Exception e) {
                ;
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshList();

    }

    @Override
    protected void onDestroy() {

        killTimer();

        try {
            PayloadUtil.getInstance(BIP47Activity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BIP47Activity.this).getGUID() + AccessFactory.getInstance(BIP47Activity.this).getPIN()));
        }
        catch(Exception e) {
            ;
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_PCODE) {
            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {
                String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
                processScan(strResult);
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_PCODE) {
            ;
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_PCODE) {

            if(data.hasExtra("pcode"))    {

                String pcode = data.getStringExtra("pcode");

                if(BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT)    {

                    doNotifTx(pcode);

                }

            }

        }
        else if (resultCode == Activity.RESULT_CANCELED && requestCode == EDIT_PCODE) {
            ;
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == RECOMMENDED_PCODE) {

            if(data.hasExtra("pcode") && data.hasExtra("label"))    {

                String pcode = data.getStringExtra("pcode");
                String label = data.getStringExtra("label");

                BIP47Meta.getInstance().setLabel(pcode, label);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();

                        try {
                            PayloadUtil.getInstance(BIP47Activity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BIP47Activity.this).getGUID() + AccessFactory.getInstance().getPIN()));
                        }
                        catch(MnemonicException.MnemonicLengthException mle) {
                            mle.printStackTrace();
                            Toast.makeText(BIP47Activity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                        }
                        catch(DecoderException de) {
                            de.printStackTrace();
                            Toast.makeText(BIP47Activity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                        }
                        catch(JSONException je) {
                            je.printStackTrace();
                            Toast.makeText(BIP47Activity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                        }
                        catch(IOException ioe) {
                            ioe.printStackTrace();
                            Toast.makeText(BIP47Activity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                        }
                        catch(java.lang.NullPointerException npe) {
                            npe.printStackTrace();
                            Toast.makeText(BIP47Activity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                        }
                        catch(DecryptionException de) {
                            de.printStackTrace();
                            Toast.makeText(BIP47Activity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                        }
                        finally {
                            ;
                        }

                        Looper.loop();

                    }
                }).start();

                if(BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT)    {

                    doNotifTx(pcode);

                }

            }

        }
        else if (resultCode == Activity.RESULT_CANCELED && requestCode == RECOMMENDED_PCODE) {
            ;
        }
        else {
            ;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bip47_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
        }
        else if(id == R.id.action_show_qr) {
            Intent intent = new Intent(BIP47Activity.this, BIP47ShowQR.class);
            startActivity(intent);
        }
        else if(id == R.id.action_unarchive) {
            doUnArchive();
        }
        else if(id == R.id.action_sync_all) {
            doSyncAll();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doScan() {
        Intent intent = new Intent(BIP47Activity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
        startActivityForResult(intent, SCAN_PCODE);
    }

    private void processScan(String data) {

        if(data.startsWith("bitcoin:") && data.length() > 8)    {
            data = data.substring(8);
        }

        if(FormatsUtil.getInstance().isValidPaymentCode(data)) {

            try {
                if(data.equals(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString())) {
                    Toast.makeText(BIP47Activity.this, R.string.bip47_cannot_scan_own_pcode, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (AddressFormatException afe) {
                ;
            }

            Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
            intent.putExtra("pcode", data);
            startActivityForResult(intent, EDIT_PCODE);

        } else {
            Toast.makeText(BIP47Activity.this, R.string.scan_error, Toast.LENGTH_SHORT).show();
        }

    }

    private void doAdd() {

        AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                .setTitle(R.string.bip47_add1_title)
                .setMessage(R.string.bip47_add1_text)
                .setPositiveButton(R.string.paste, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
                        startActivityForResult(intent, EDIT_PCODE);

                    }

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        doScan();
                    }
                });

        dlg.show();

    }

    private void doSign() {

        String strDate = new Date(System.currentTimeMillis()).toLocaleString();
        String message = BIP47Activity.this.getString(R.string.bip47_sign_text2) + " " + strDate;

        final EditText etMessage = new EditText(BIP47Activity.this);
        etMessage.setHint(message);

        AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                .setTitle(R.string.bip47_sign)
                .setMessage(R.string.bip47_sign_text1)
                .setView(etMessage)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        String strSignedMessage = null;
                        String result = etMessage.getText().toString();
                        if(result == null || result.length() == 0)    {
                            String strDate = new Date(System.currentTimeMillis()).toLocaleString();
                            String message = BIP47Activity.this.getString(R.string.bip47_sign_text2) + " " + strDate;
                            strSignedMessage = MessageSignUtil.getInstance().signMessageArmored(BIP47Util.getInstance(BIP47Activity.this).getNotificationAddress().getECKey(), message);
                        }
                        else    {
                            strSignedMessage = MessageSignUtil.getInstance().signMessageArmored(BIP47Util.getInstance(BIP47Activity.this).getNotificationAddress().getECKey(), result);
                        }

                        TextView showText = new TextView(BIP47Activity.this);
                        showText.setText(strSignedMessage);
                        showText.setTextIsSelectable(true);
                        showText.setPadding(40, 10, 40, 10);
                        showText.setTextSize(18.0f);
                        new AlertDialog.Builder(BIP47Activity.this)
                                .setTitle(R.string.app_name)
                                .setView(showText)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                    }
                                }).show();

                    }

                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

        dlg.show();

    }

    private void doUnArchive()  {

        Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels(true);

        //
        // check for own payment code
        //
        try {
            if (_pcodes.contains(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString())) {
                _pcodes.remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
                BIP47Meta.getInstance().remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
            }
        } catch (AddressFormatException afe) {
            ;
        }

        for (String pcode : _pcodes) {
            BIP47Meta.getInstance().setArchived(pcode, false);
        }

        pcodes = new String[_pcodes.size()];
        int i = 0;
        for (String pcode : _pcodes) {
            pcodes[i] = pcode;
            ++i;
        }

        adapter.notifyDataSetChanged();

    }

    private void doSyncAll()  {

        Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels(false);

        //
        // check for own payment code
        //
        try {
            if (_pcodes.contains(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString())) {
                _pcodes.remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
                BIP47Meta.getInstance().remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
            }
        } catch (AddressFormatException afe) {
            ;
        }

        for (String pcode : _pcodes) {
            doSync(pcode);
        }

        adapter.notifyDataSetChanged();

    }

    private void refreshList()  {

        Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels(false);

        //
        // check for own payment code
        //
        try {
            if (_pcodes.contains(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString())) {
                _pcodes.remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
                BIP47Meta.getInstance().remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
            }
        } catch (AddressFormatException afe) {
            ;
        }

        pcodes = new String[_pcodes.size()];
        int i = 0;
        for (String pcode : _pcodes) {
            pcodes[i] = pcode;
            ++i;
        }

        setDisplay();

        adapter = new BIP47EntryAdapter();
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        new PaymentCodeMetaTask().execute("");

    }

    private void setDisplay()   {

        if(pcodes.length > 0)    {
            listView.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text1)).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.text2)).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.text3)).setVisibility(View.GONE);
        }
        else    {
            listView.setVisibility(View.GONE);
            ((TextView)findViewById(R.id.text1)).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text2)).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text3)).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text3)).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    doHelp();
                    return false;
                }
            });
        }

    }

    private void doHelp()  {

        new AlertDialog.Builder(BIP47Activity.this)
                .setTitle(R.string.bip47_setup1_title)
                .setMessage(R.string.bip47_setup1_text)
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        new AlertDialog.Builder(BIP47Activity.this)
                                .setTitle(R.string.bip47_setup2_title)
                                .setMessage(R.string.bip47_setup2_text)
                                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        new AlertDialog.Builder(BIP47Activity.this)
                                                .setTitle(R.string.bip47_setup2_title)
                                                .setMessage(R.string.bip47_setup3_text)
                                                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
                                                        intent.putExtra("label", "Samourai Donations");
                                                        intent.putExtra("pcode", "PM8TJVzLGqWR3dtxZYaTWn3xJUop3QP3itR4eYzX7XvV5uAfctEEuHhKNo3zCcqfAbneMhyfKkCthGv5werVbwLruhZyYNTxqbCrZkNNd2pPJA2e2iAh");
                                                        startActivityForResult(intent, EDIT_PCODE);

                                                    }

                                                }).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                ;

                                            }
                                        }).show();

                                    }

                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                ;

                            }
                        }).show();

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        ;

                    }
        }).show();

    }

    private void doNotifTx(final String pcode)  {

        //
        // get wallet balance
        //
        long balance = 0L;
        try    {
            balance = APIFactory.getInstance(BIP47Activity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BIP47Activity.this).get().getAccount(0).xpubstr());
        }
        catch(IOException ioe)    {
            balance = 0L;
        }
        catch(MnemonicException.MnemonicLengthException mle)    {
            balance = 0L;
        }
        catch(java.lang.NullPointerException npe)    {
            balance = 0L;
        }

        //
        // get unspents
        //
        List<UTXO> utxos = APIFactory.getInstance(BIP47Activity.this).getUtxos();
        final List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalValueSelected = 0L;
//        long change = 0L;
        BigInteger fee = null;
        //
        // spend dust threshold amount to notification address
        //
        long amount = SendNotifTxFactory._bNotifTxValue.longValue();
        //
        // add Samourai Wallet fee to total amount
        //
        amount += SendNotifTxFactory._bSWFee.longValue();

        // sort in ascending order by value
        final List<UTXO> _utxos = utxos;
        Collections.sort(_utxos, new UTXO.UTXOComparator());
        Collections.reverse(_utxos);

        //
        // get smallest 1 UTXO > than spend + fee + sw fee + dust
        //
        for(UTXO u : _utxos)   {
            if(u.getValue() >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(1, 4).longValue()))    {
                selectedUTXO.add(u);
                totalValueSelected += u.getValue();
                Log.d("BIP47Activity", "single output");
                Log.d("BIP47Activity", "value selected:" + u.getValue());
                Log.d("BIP47Activity", "total value selected:" + totalValueSelected);
                Log.d("BIP47Activity", "nb inputs:" + u.getOutpoints().size());
                break;
            }
        }

        //
        // use high fee settings
        //
        SuggestedFee suggestedFee = FeeUtil.getInstance().getSuggestedFee();
        FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());

        if(selectedUTXO.size() == 0)    {
            // sort in descending order by value
            Collections.sort(_utxos, new UTXO.UTXOComparator());
            int selected = 0;

            // get largest UTXOs > than spend + fee + dust
            for(UTXO u : _utxos)   {

                selectedUTXO.add(u);
                totalValueSelected += u.getValue();
                selected += u.getOutpoints().size();

                if(totalValueSelected >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(selected, 4).longValue()))    {
                    Log.d("BIP47Activity", "multiple outputs");
                    Log.d("BIP47Activity", "total value selected:" + totalValueSelected);
                    Log.d("BIP47Activity", "nb inputs:" + u.getOutpoints().size());
                    break;
                }
            }

            fee = FeeUtil.getInstance().estimatedFee(selected, 4);

        }
        else    {
            fee = FeeUtil.getInstance().estimatedFee(1, 4);
        }

        //
        // reset fee to previous setting
        //
        FeeUtil.getInstance().setSuggestedFee(suggestedFee);

        //
        // total amount to spend including fee
        //
        if((amount + fee.longValue()) >= balance)    {
            Toast.makeText(BIP47Activity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
        }

        //
        // payment code to be notified
        //
        PaymentCode payment_code;
        try {
            payment_code = new PaymentCode(pcode);
        }
        catch (AddressFormatException afe) {
            payment_code = null;
        }

        if(payment_code == null)    {

        }

        //
        // create outpoints for spend later
        //
        final List<MyTransactionOutPoint> outpoints = new ArrayList<MyTransactionOutPoint>();
        for(UTXO u : selectedUTXO)   {
            outpoints.addAll(u.getOutpoints());
        }
        //
        // create inputs from outpoints
        //
        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();
        for(MyTransactionOutPoint o  : outpoints) {
            Script script = new Script(o.getScriptBytes());

            if(script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(MainNetParams.get(), null, new byte[0], o, o.getTxHash().toString(), o.getTxOutputN());
            inputs.add(input);
        }
        //
        // sort inputs
        //
        Collections.sort(inputs, new SendFactory.BIP69InputComparator());
        //
        // find outpoint that corresponds to 0th input
        //
        MyTransactionOutPoint outPoint = null;
        for(MyTransactionOutPoint o  : outpoints) {
            if(o.getTxHash().toString().equals(inputs.get(0).getTxHash()) && o.getTxOutputN() == inputs.get(0).getTxPos())    {
                outPoint = o;
                break;
            }
        }

        if(outPoint == null)    {
            Toast.makeText(BIP47Activity.this, R.string.bip47_cannot_identify_outpoint, Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] op_return = null;
        //
        // get private key corresponding to outpoint
        //
        try {
            Script inputScript = new Script(outPoint.getConnectedPubKeyScript());
            String address = inputScript.getToAddress(MainNetParams.get()).toString();
            ECKey ecKey = null;
            String privStr = null;
            String path = APIFactory.getInstance(BIP47Activity.this).getUnspentPaths().get(address);
            if(path == null)    {
                String _pcode = BIP47Meta.getInstance().getPCode4Addr(address);
                int idx = BIP47Meta.getInstance().getIdx4Addr(address);
                PaymentAddress addr = BIP47Util.getInstance(BIP47Activity.this).getReceiveAddress(new PaymentCode(_pcode), idx);
                ecKey = addr.getReceiveECKey();
            }
            else    {
                String[] s = path.split("/");
                HD_Address hd_address = AddressFactory.getInstance(BIP47Activity.this).get(0, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                privStr = hd_address.getPrivateKeyString();
                DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), privStr);
                ecKey = pk.getKey();
            }

            //
            // use outpoint for payload masking
            //
            byte[] privkey = ecKey.getPrivKeyBytes();
            byte[] pubkey = payment_code.notificationAddress().getPubKey();
            byte[] outpoint = outPoint.bitcoinSerialize();
//                Log.i("BIP47Activity", "outpoint:" + Hex.toHexString(outpoint));
//                Log.i("BIP47Activity", "payer shared secret:" + Hex.toHexString(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes()));
            byte[] mask = PaymentCode.getMask(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes(), outpoint);
//                Log.i("BIP47Activity", "mask:" + Hex.toHexString(mask));
//                Log.i("BIP47Activity", "mask length:" + mask.length);
//                Log.i("BIP47Activity", "payload0:" + Hex.toHexString(BIP47Util.getInstance(context).getPaymentCode().getPayload()));
            op_return = PaymentCode.blind(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().getPayload(), mask);
//                Log.i("BIP47Activity", "payload1:" + Hex.toHexString(op_return));
        }
        catch(NotSecp256k1Exception ns) {
            Toast.makeText(BIP47Activity.this, ns.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        catch(InvalidKeyException ike) {
            Toast.makeText(BIP47Activity.this, ike.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        catch(InvalidKeySpecException ikse) {
            Toast.makeText(BIP47Activity.this, ikse.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        catch(NoSuchAlgorithmException nsae) {
            Toast.makeText(BIP47Activity.this, nsae.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        catch(NoSuchProviderException nspe) {
            Toast.makeText(BIP47Activity.this, nspe.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put(Hex.toHexString(op_return), BigInteger.ZERO);
        receivers.put(payment_code.notificationAddress().getAddressString(), SendNotifTxFactory._bNotifTxValue);
        receivers.put(SendNotifTxFactory.SAMOURAI_NOTIF_TX_FEE_ADDRESS, SendNotifTxFactory._bSWFee);

        final long change = totalValueSelected - (amount + fee.longValue());
        if(change > 0L)  {
            try {
                String change_address = HD_WalletFactory.getInstance(BIP47Activity.this).get().getAccount(0).getChange().getAddressAt(HD_WalletFactory.getInstance(BIP47Activity.this).get().getAccount(0).getChange().getAddrIdx()).getAddressString();
                receivers.put(change_address, BigInteger.valueOf(change));
            }
            catch(IOException ioe) {
                Toast.makeText(BIP47Activity.this, ioe.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            catch(MnemonicException.MnemonicLengthException mle) {
                Toast.makeText(BIP47Activity.this, mle.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Log.d("BIP47Activity", "outpoints:" + outpoints.size());
        Log.d("BIP47Activity", "totalValueSelected:" + BigInteger.valueOf(totalValueSelected).toString());
        Log.d("BIP47Activity", "amount:" + BigInteger.valueOf(amount).toString());
        Log.d("BIP47Activity", "change:" + BigInteger.valueOf(change).toString());
        Log.d("BIP47Activity", "fee:" + fee.toString());

        if(change < 0L)    {
            Toast.makeText(BIP47Activity.this, R.string.bip47_cannot_compose_notif_tx, Toast.LENGTH_SHORT).show();
            return;
        }

        final MyTransactionOutPoint _outPoint = outPoint;

        String strNotifTxMsg = getText(R.string.bip47_setup4_text1) + " ";
        long notifAmount = amount;
        String strAmount = MonetaryUtil.getInstance().getBTCFormat().format(((double) notifAmount) / 1e8) + " BTC ";
        strNotifTxMsg += strAmount + getText(R.string.bip47_setup4_text2);
        strNotifTxMsg += "\n";
        strNotifTxMsg += "(" + MonetaryUtil.getInstance().getBTCFormat().format(((double) fee.longValue()) / 1e8) + " BTC miner's fee).";

        AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                .setTitle(R.string.bip47_setup4_title)
                .setMessage(strNotifTxMsg)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                Looper.prepare();

                                Transaction tx = SendFactory.getInstance(BIP47Activity.this).makeTransaction(0, outpoints, receivers);
                                if(tx != null)    {

                                    String input0hash = tx.getInput(0L).getOutpoint().getHash().toString();
                                    Log.d("BIP47Activity", "input0 hash:" + input0hash);
                                    Log.d("BIP47Activity", "_outPoint hash:" + _outPoint.getTxHash().toString());
                                    int input0index = (int)tx.getInput(0L).getOutpoint().getIndex();
                                    Log.d("BIP47Activity", "input0 index:" + input0index);
                                    Log.d("BIP47Activity", "_outPoint index:" + _outPoint.getTxOutputN());
                                    if(!input0hash.equals(_outPoint.getTxHash().toString()) || input0index != _outPoint.getTxOutputN())    {
                                        Toast.makeText(BIP47Activity.this, R.string.bip47_cannot_compose_notif_tx, Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    tx = SendFactory.getInstance(BIP47Activity.this).signTransaction(tx);
                                    final String hexTx = new String(org.spongycastle.util.encoders.Hex.encode(tx.bitcoinSerialize()));
                                    Log.d("SendActivity", tx.getHashAsString());
                                    Log.d("SendActivity", hexTx);

                                    boolean isOK = false;
                                    String response = null;
                                    try {
                                        response = PushTx.getInstance(BIP47Activity.this).samourai(hexTx);
                                        Log.d("SendActivity", "pushTx:" + response);

                                        if(response != null)    {
                                            org.json.JSONObject jsonObject = new org.json.JSONObject(response);
                                            if(jsonObject.has("status"))    {
                                                if(jsonObject.getString("status").equals("ok"))    {
                                                    isOK = true;
                                                }
                                            }
                                        }
                                        else    {
                                            Toast.makeText(BIP47Activity.this, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        if(isOK)    {
                                            Toast.makeText(BIP47Activity.this, R.string.payment_channel_init, Toast.LENGTH_SHORT).show();
                                            //
                                            // set outgoing index for payment code to 0
                                            //
                                            BIP47Meta.getInstance().setOutgoingIdx(pcode, 0);
//                        Log.i("SendNotifTxFactory", "tx hash:" + tx.getHashAsString());
                                            //
                                            // status to NO_CFM
                                            //
                                            BIP47Meta.getInstance().setOutgoingStatus(pcode, tx.getHashAsString(), BIP47Meta.STATUS_SENT_NO_CFM);

                                            //
                                            // increment change index
                                            //
                                            if(change > 0L)    {
                                                try {
                                                    HD_WalletFactory.getInstance(BIP47Activity.this).get().getAccount(0).getChange().incAddrIdx();
                                                }
                                                catch(IOException ioe) {
                                                    ;
                                                }
                                                catch(MnemonicException.MnemonicLengthException mle) {
                                                    ;
                                                }
                                            }

                                            PayloadUtil.getInstance(BIP47Activity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BIP47Activity.this).getGUID() + AccessFactory.getInstance(BIP47Activity.this).getPIN()));

                                        }
                                        else    {
                                            Toast.makeText(BIP47Activity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                    catch(JSONException je) {
                                        Toast.makeText(BIP47Activity.this, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    catch(MnemonicException.MnemonicLengthException mle) {
                                        Toast.makeText(BIP47Activity.this, "pushTx:" + mle.getMessage(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    catch(DecoderException de) {
                                        Toast.makeText(BIP47Activity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    catch(IOException ioe) {
                                        Toast.makeText(BIP47Activity.this, "pushTx:" + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    catch(DecryptionException de) {
                                        Toast.makeText(BIP47Activity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                }

                                Looper.loop();

                            }
                        }).start();

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                });

        dlg.show();

    }

    private class BIP47EntryAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;

        public BIP47EntryAdapter() {
            inflater = (LayoutInflater)BIP47Activity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return pcodes.length;
        }

        @Override
        public Object getItem(int position) {
            return pcodes[position];
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)BIP47Activity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.bip47_entry, null);
            }
            else    {
                view = convertView;
            }

            String strLabel = BIP47Meta.getInstance().getDisplayLabel(pcodes[position]);

            final TextView tvInitial = (TextView)view.findViewById(R.id.Initial);
            tvInitial.setText(strLabel.substring(0, 1).toUpperCase());
            if(position % 3 == 0)    {
                tvInitial.setBackgroundResource(R.drawable.ripple_initial_red);
            }
            else if(position % 2 == 1)    {
                tvInitial.setBackgroundResource(R.drawable.ripple_initial_green);
            }
            else {
                tvInitial.setBackgroundResource(R.drawable.ripple_initial_blue);
            }

            final TextView tvLabel = (TextView)view.findViewById(R.id.Label);
            tvLabel.setText(strLabel);

            final ImageView ivAvatar = (ImageView)view.findViewById(R.id.Avatar);
            ivAvatar.setVisibility(View.GONE);

            if(meta.containsKey(pcodes[position]))    {
                try {

                    JSONObject obj = new JSONObject(meta.get(pcodes[position]));

                    if(obj.has("user-avatar"))    {

                        String avatarUrl = obj.getString("user-avatar");

                        if(bitmaps.containsKey(pcodes[position]))    {
                            ivAvatar.setImageBitmap(bitmaps.get(pcodes[position]));
                        }
                        else    {
                            Picasso.with(BIP47Activity.this)
                                    .load(avatarUrl)
                                    .into(new Target() {
                                        @Override
                                        public void onBitmapLoaded (final Bitmap bitmap, Picasso.LoadedFrom from){
                                            ivAvatar.setImageBitmap(bitmap);
                                            bitmaps.put(pcodes[position], bitmap);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {}

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {}
                                    });
                        }

                        tvInitial.setVisibility(View.GONE);
                        ivAvatar.setVisibility(View.VISIBLE);

                    }

                    if(obj.has("title"))    {

                        String label = StringEscapeUtils.unescapeHtml4(obj.getString("title"));

                        if((BIP47Meta.getInstance().getLabel(pcodes[position]) == null ||
                                BIP47Meta.getInstance().getLabel(pcodes[position]).length() == 0 ||
                                FormatsUtil.getInstance().isValidPaymentCode(BIP47Meta.getInstance().getLabel(pcodes[position]))
                                &&
                                (label != null && label.length() > 0)))    {
                            strLabel = label;
                            BIP47Meta.getInstance().setLabel(pcodes[position], strLabel);
                            tvLabel.setText(strLabel);
                        }

                    }

                }
                catch(JSONException je) {
                    ;
                }
            }

            TextView tvLatest = (TextView)view.findViewById(R.id.Latest);
            String strLatest = "";
            if(BIP47Meta.getInstance().getOutgoingStatus(pcodes[position]) == BIP47Meta.STATUS_NOT_SENT) {
                if(BIP47Meta.getInstance().incomingExists(pcodes[position]))    {
                    strLatest = BIP47Activity.this.getText(R.string.bip47_status_incoming) + "\n";
                }
                strLatest += BIP47Activity.this.getText(R.string.bip47_status_tbe);
            }
            else if (BIP47Meta.getInstance().getOutgoingStatus(pcodes[position]) == BIP47Meta.STATUS_SENT_NO_CFM) {
                if(BIP47Meta.getInstance().incomingExists(pcodes[position]))    {
                    strLatest = BIP47Activity.this.getText(R.string.bip47_status_incoming) + "\n";
                }
                strLatest += BIP47Activity.this.getText(R.string.bip47_status_pending);
            }
            else if (BIP47Meta.getInstance().getOutgoingStatus(pcodes[position]) == BIP47Meta.STATUS_SENT_CFM) {
                if(BIP47Meta.getInstance().incomingExists(pcodes[position]))    {
                    strLatest = BIP47Activity.this.getText(R.string.bip47_status_incoming) + "\n";
                }
                strLatest += BIP47Activity.this.getText(R.string.bip47_status_active);
            }
            else {
                ;
            }

            if(BIP47Meta.getInstance().getLatestEvent(pcodes[position]) != null && BIP47Meta.getInstance().getLatestEvent(pcodes[position]).length() > 0)    {
                strLatest += "\n" + BIP47Meta.getInstance().getLatestEvent(pcodes[position]);
            }

            tvLatest.setText(strLatest);

            return view;
        }
    }

    private void killTimer()    {

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

    }

    private void doTimer() {

        if(timer == null) {
            timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    if(refreshDisplay())    {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                refreshList();
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }

                                }
                            }).start();

                        }
                    });
                }
            }, 5000, 30000);

        }

    }

    public boolean refreshDisplay()   {

        boolean changed = false;

        //
        // check for incoming payment code notification tx
        //
        int before = BIP47Meta.getInstance().getLabels().size();
        try {
            PaymentCode pcode = BIP47Util.getInstance(BIP47Activity.this).getPaymentCode();
            APIFactory.getInstance(BIP47Activity.this).getNotifAddress(pcode.notificationAddress().getAddressString());
        }
        catch (AddressFormatException afe) {
            afe.printStackTrace();
            Toast.makeText(BIP47Activity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        int after = BIP47Meta.getInstance().getLabels().size();

        if(before != after)    {
            changed = true;
        }

        //
        // check on outgoing payment code notification tx
        //
        List<org.apache.commons.lang3.tuple.Pair<String, String>> outgoingUnconfirmed = BIP47Meta.getInstance().getOutgoingUnconfirmed();
        for(org.apache.commons.lang3.tuple.Pair<String,String> pair : outgoingUnconfirmed)   {
            //Log.i("BalanceFragment", "outgoing payment code:" + pair.getLeft());
            //Log.i("BalanceFragment", "outgoing payment code tx:" + pair.getRight());
            int confirmations = APIFactory.getInstance(BIP47Activity.this).getNotifTxConfirmations(pair.getRight());
            if(confirmations > 0)    {
                BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_SENT_CFM);
                changed = true;
            }
            if(confirmations == -1)    {
                BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_NOT_SENT);
            }
        }

        return changed;
    }

    private void doSync(final String pcode)    {

        progress = new ProgressDialog(BIP47Activity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    PaymentCode payment_code = new PaymentCode(pcode);

                    int idx = 0;
                    boolean loop = true;
                    ArrayList<String> addrs = new ArrayList<String>();
                    while(loop) {
                        addrs.clear();
                        for(int i = idx; i < (idx + 20); i++)   {
                            PaymentAddress receiveAddress = BIP47Util.getInstance(BIP47Activity.this).getReceiveAddress(payment_code, i);
//                            Log.i("BIP47Activity", "sync receive from " + i + ":" + receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                            BIP47Meta.getInstance().setIncomingIdx(payment_code.toString(), i, receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                            BIP47Meta.getInstance().getIdx4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString(), i);
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString(), payment_code.toString());
                            addrs.add(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                        }
                        String[] s = addrs.toArray(new String[addrs.size()]);
                        int nb = APIFactory.getInstance(BIP47Activity.this).syncBIP47Incoming(s);
//                        Log.i("BIP47Activity", "sync receive idx:" + idx + ", nb == " + nb);
                        if(nb == 0)    {
                            loop = false;
                        }
                        idx += 20;
                    }

                    idx = 0;
                    loop = true;
                    BIP47Meta.getInstance().setOutgoingIdx(pcode, 0);
                    while(loop) {
                        addrs.clear();
                        for(int i = idx; i < (idx + 20); i++)   {
                            PaymentAddress sendAddress = BIP47Util.getInstance(BIP47Activity.this).getSendAddress(payment_code, i);
//                            Log.i("BIP47Activity", "sync send to " + i + ":" + sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString());
//                            BIP47Meta.getInstance().setOutgoingIdx(payment_code.toString(), i);
                            BIP47Meta.getInstance().getIdx4AddrLookup().put(sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString(), i);
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString(), payment_code.toString());
                            addrs.add(sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString());
                        }
                        String[] s = addrs.toArray(new String[addrs.size()]);
                        int nb = APIFactory.getInstance(BIP47Activity.this).syncBIP47Outgoing(s);
//                        Log.i("BIP47Activity", "sync send idx:" + idx + ", nb == " + nb);
                        if(nb == 0)    {
                            loop = false;
                        }
                        idx += 20;
                    }

                    BIP47Meta.getInstance().pruneIncoming();

                    PayloadUtil.getInstance(BIP47Activity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BIP47Activity.this).getGUID() + AccessFactory.getInstance(BIP47Activity.this).getPIN()));

                    Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                    LocalBroadcastManager.getInstance(BIP47Activity.this).sendBroadcast(intent);

                }
                catch(IOException ioe) {
                    ;
                }
                catch(JSONException je) {
                    ;
                }
                catch(DecryptionException de) {
                    ;
                }
                catch(NotSecp256k1Exception nse) {
                    ;
                }
                catch(InvalidKeySpecException ikse) {
                    ;
                }
                catch(InvalidKeyException ike) {
                    ;
                }
                catch(NoSuchAlgorithmException nsae) {
                    ;
                }
                catch(NoSuchProviderException nspe) {
                    ;
                }
                catch(MnemonicException.MnemonicLengthException mle) {
                    ;
                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                if (refreshDisplay()) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            refreshList();
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                }

                            }
                        }).start();

                    }
                });

            }
        }).start();

    }

    private class PaymentCodeMetaTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            if(meta == null)    {
                meta = new HashMap<String,String>();
            }
            if(bitmaps == null)    {
                bitmaps = new HashMap<String,Bitmap>();
            }
        }

        @Override
        protected String doInBackground(String... s) {

            for(int i = 0; i < pcodes.length; i++)  {
                String result = null;
                String url = WebUtil.PAYMENTCODE_IO_SEARCH + pcodes[i];
                try {
                    result = WebUtil.getInstance(BIP47Activity.this).getURL(url);

                    JSONObject obj = new JSONObject(result);
                    if(!meta.containsKey(pcodes[i]))    {
                        meta.put(pcodes[i], obj.toString());
                    }

                }
                catch(Exception e) {
                    ;
                }
            }

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            adapter.notifyDataSetChanged();
        }

        protected void onProgressUpdate(String... progress) {

            refreshList();

        }

    }

}
