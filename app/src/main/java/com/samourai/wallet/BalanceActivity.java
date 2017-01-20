package com.samourai.wallet;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.MnemonicException;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.*;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.DateUtil;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.PrivKeyReader;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.util.TorUtil;
import com.samourai.wallet.util.TypefaceUtil;
import com.samourai.wallet.util.WebUtil;

import org.bitcoinj.core.Coin;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.spongycastle.util.encoders.Hex;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import net.sourceforge.zbar.Symbol;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class BalanceActivity extends Activity {

    private final static int SCAN_COLD_STORAGE = 2011;
    private final static int SCAN_QR = 2012;

    private LinearLayout tvBalanceBar = null;
    private TextView tvBalanceAmount = null;
    private TextView tvBalanceUnits = null;

    private ListView txList = null;
    private List<Tx> txs = null;
    private HashMap<String, Boolean> txStates = null;
    private TransactionAdapter txAdapter = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;

    private FloatingActionsMenu ibQuickSend = null;
    private FloatingActionButton actionReceive = null;
    private FloatingActionButton actionSend = null;
    private FloatingActionButton actionBIP47 = null;

    private boolean isBTC = true;

    public static final String ACTION_INTENT = "com.samourai.wallet.BalanceFragment.REFRESH";
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if(ACTION_INTENT.equals(intent.getAction())) {

                final boolean notifTx = intent.getBooleanExtra("notifTx", false);
                final boolean fetch = intent.getBooleanExtra("fetch", false);

                final String rbfHash;
                if(intent.hasExtra("rbf"))    {
                    rbfHash = intent.getStringExtra("rbf");
                }
                else    {
                    rbfHash = null;
                }

                BalanceActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvBalanceAmount.setText("");
                        tvBalanceUnits.setText("");
                        refreshTx(notifTx, fetch, false);

                        if(BalanceActivity.this != null)    {

                            try {
                                PayloadUtil.getInstance(BalanceActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BalanceActivity.this).getGUID() + AccessFactory.getInstance(BalanceActivity.this).getPIN()));
                            }
                            catch(MnemonicException.MnemonicLengthException mle) {
                                ;
                            }
                            catch(JSONException je) {
                                ;
                            }
                            catch(IOException ioe) {
                                ;
                            }
                            catch(DecryptionException de) {
                                ;
                            }

                            if(rbfHash != null)    {
                                new AlertDialog.Builder(BalanceActivity.this)
                                        .setTitle(R.string.app_name)
                                        .setMessage(rbfHash + "\n\n" + getString(R.string.rbf_incoming))
                                        .setCancelable(true)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                doExplorerView(rbfHash);

                                            }
                                        })
                                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                ;
                                            }
                                        }).show();

                            }

                        }

                    }
                });

            }

        }
    };

    protected BroadcastReceiver torStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i("BalanceActivity", "torStatusReceiver onReceive()");

            if (OrbotHelper.ACTION_STATUS.equals(intent.getAction())) {

                boolean enabled = (intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_ON));
                Log.i("BalanceActivity", "status:" + enabled);

                TorUtil.getInstance(BalanceActivity.this).setStatusFromBroadcast(enabled);

            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LayoutInflater inflator = BalanceActivity.this.getLayoutInflater();
        tvBalanceBar = (LinearLayout)inflator.inflate(R.layout.balance_layout, null);
        tvBalanceBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(isBTC)    {
                    isBTC = false;
                }
                else    {
                    isBTC = true;
                }
                displayBalance();
                txAdapter.notifyDataSetChanged();
                return false;
            }
        });
        tvBalanceAmount = (TextView)tvBalanceBar.findViewById(R.id.BalanceAmount);
        tvBalanceUnits = (TextView)tvBalanceBar.findViewById(R.id.BalanceUnits);

        ibQuickSend = (FloatingActionsMenu)findViewById(R.id.wallet_menu);
        actionSend = (FloatingActionButton)findViewById(R.id.send);
        actionSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
                intent.putExtra("via_menu", true);
                startActivity(intent);

            }
        });

        actionReceive = (FloatingActionButton)findViewById(R.id.receive);
        actionReceive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                if(SamouraiWallet.getInstance().getCurrentSelectedAccount() == 2 ||
                        (SamouraiWallet.getInstance().getCurrentSelectedAccount() == 0 && SamouraiWallet.getInstance().getShowTotalBalance())
                        )    {

                    new AlertDialog.Builder(BalanceActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.receive2Samourai)
                            .setCancelable(false)
                            .setPositiveButton(R.string.generate_receive_yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent intent = new Intent(BalanceActivity.this, ReceiveActivity.class);
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            }).show();

                }
                else    {
                    Intent intent = new Intent(BalanceActivity.this, ReceiveActivity.class);
                    startActivity(intent);
                }

            }
        });

        actionBIP47 = (FloatingActionButton)findViewById(R.id.bip47);
        actionBIP47.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(BalanceActivity.this, com.samourai.wallet.bip47.BIP47Activity.class);
                startActivity(intent);
            }
        });

        txs = new ArrayList<Tx>();
        txStates = new HashMap<String, Boolean>();
        txList = (ListView)findViewById(R.id.txList);
        txAdapter = new TransactionAdapter();
        txList.setAdapter(txAdapter);
        txList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

                if(position == 0) {
                    return;
                }

                long viewId = view.getId();
                View v = (View)view.getParent();
                Tx tx = txs.get(position - 1);
                ImageView ivTxStatus = (ImageView)v.findViewById(R.id.TransactionStatus);
                TextView tvConfirmationCount = (TextView)v.findViewById(R.id.ConfirmationCount);

                if(viewId == R.id.ConfirmationCount || viewId == R.id.TransactionStatus) {

                    if(txStates.containsKey(tx.getHash()) && txStates.get(tx.getHash()) == true) {
                        txStates.put(tx.getHash(), false);
                        displayTxStatus(false, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                    }
                    else {
                        txStates.put(tx.getHash(), true);
                        displayTxStatus(true, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                    }

                }
                else {

                    doExplorerView(tx.getHash());

                }

            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        refreshTx(false, true, true);
                    }
                });

            }
        });
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiver, filter);

        TorUtil.getInstance(BalanceActivity.this).setStatusFromBroadcast(false);
        registerReceiver(torStatusReceiver, new IntentFilter(OrbotHelper.ACTION_STATUS));

        refreshTx(false, true, false);

    }

    @Override
    public void onResume() {
        super.onResume();

//        IntentFilter filter = new IntentFilter(ACTION_INTENT);
//        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiver, filter);

        AppUtil.getInstance(BalanceActivity.this).checkTimeOut();

        if(!AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            startService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
        }

    }

    @Override
    public void onPause() {
        super.onPause();

//        LocalBroadcastManager.getInstance(BalanceActivity.this).unregisterReceiver(receiver);

        ibQuickSend.collapse();

    }

    @Override
    public void onDestroy() {

        LocalBroadcastManager.getInstance(BalanceActivity.this).unregisterReceiver(receiver);

        unregisterReceiver(torStatusReceiver);

        if(AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            stopService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if(!OrbotHelper.isOrbotInstalled(BalanceActivity.this))    {
            menu.findItem(R.id.action_tor).setVisible(false);
        }
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_share_receive).setVisible(false);
        menu.findItem(R.id.action_ricochet).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            doSettings();
        }
        else if (id == R.id.action_sweep) {
            doSweep();
        }
        else if (id == R.id.action_tor) {

            if(!OrbotHelper.isOrbotInstalled(BalanceActivity.this))    {
                ;
            }
            else if(TorUtil.getInstance(BalanceActivity.this).statusFromBroadcast())    {
                item.setIcon(R.drawable.tor_off);
                TorUtil.getInstance(BalanceActivity.this).setStatusFromBroadcast(false);
            }
            else    {
                OrbotHelper.requestStartTor(BalanceActivity.this);
                item.setIcon(R.drawable.tor_on);
                TorUtil.getInstance(BalanceActivity.this).setStatusFromBroadcast(true);
            }

            return true;

        }
        else if (id == R.id.action_backup) {

            if(SamouraiWallet.getInstance().hasPassphrase(BalanceActivity.this))    {
                try {
                    if(HD_WalletFactory.getInstance(BalanceActivity.this).get() != null && SamouraiWallet.getInstance().hasPassphrase(BalanceActivity.this))    {
                        doBackup();
                    }
                    else    {

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.passphrase_needed_for_backup).setCancelable(false);
                        AlertDialog alert = builder.create();

                        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }});

                        if(!isFinishing())    {
                            alert.show();
                        }

                    }
                }
                catch(MnemonicException.MnemonicLengthException mle) {
                    ;
                }
                catch(IOException ioe) {
                    ;
                }
            }
            else    {
                Toast.makeText(BalanceActivity.this, R.string.passphrase_required, Toast.LENGTH_SHORT).show();
            }

        }
        else if (id == R.id.action_scan_qr) {
            doScan();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_COLD_STORAGE)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                PrivKeyReader privKeyReader = null;

                String format = null;
                try	{
                    privKeyReader = new PrivKeyReader(new CharSequenceX(strResult), null);
                    format = privKeyReader.getFormat();
                }
                catch(Exception e)	{
                    Toast.makeText(BalanceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(format != null)	{

                    if(format.equals(PrivKeyReader.BIP38))	{

                        final PrivKeyReader pvr = privKeyReader;

                        final EditText password38 = new EditText(BalanceActivity.this);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.bip38_pw)
                                .setView(password38)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        String password = password38.getText().toString();

                                        ProgressDialog progress = new ProgressDialog(BalanceActivity.this);
                                        progress.setCancelable(false);
                                        progress.setTitle(R.string.app_name);
                                        progress.setMessage(getString(R.string.decrypting_bip38));
                                        progress.show();

                                        boolean keyDecoded = false;

                                        try {
                                            BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), strResult);
                                            final ECKey ecKey = bip38.decrypt(password);
                                            if(ecKey != null && ecKey.hasPrivKey()) {

                                                if(progress != null && progress.isShowing())    {
                                                    progress.cancel();
                                                }

                                                pvr.setPassword(new CharSequenceX(password));
                                                keyDecoded = true;

                                                Toast.makeText(BalanceActivity.this, pvr.getFormat(), Toast.LENGTH_SHORT).show();
                                                Toast.makeText(BalanceActivity.this, pvr.getKey().toAddress(MainNetParams.get()).toString(), Toast.LENGTH_SHORT).show();

                                            }
                                        }
                                        catch(Exception e) {
                                            e.printStackTrace();
                                            Toast.makeText(BalanceActivity.this, R.string.bip38_pw_error, Toast.LENGTH_SHORT).show();
                                        }

                                        if(progress != null && progress.isShowing())    {
                                            progress.cancel();
                                        }

                                        if(keyDecoded)    {
                                            doSweep(pvr);
                                        }

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        Toast.makeText(BalanceActivity.this, R.string.bip38_pw_error, Toast.LENGTH_SHORT).show();

                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
                        }

                    }
                    else if(privKeyReader != null)	{
                        doSweep(privKeyReader);
                    }
                    else    {
                        ;
                    }

                }

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_COLD_STORAGE)	{
            ;
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == SCAN_QR)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
                intent.putExtra("uri", strResult);
                startActivity(intent);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_QR)	{
            ;
        }
        else {
            ;
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.ask_you_sure_exit).setCancelable(false);
            AlertDialog alert = builder.create();

            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    try {
                        PayloadUtil.getInstance(BalanceActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BalanceActivity.this).getGUID() + AccessFactory.getInstance(BalanceActivity.this).getPIN()));
                    }
                    catch(MnemonicException.MnemonicLengthException mle) {
                        ;
                    }
                    catch(JSONException je) {
                        ;
                    }
                    catch(IOException ioe) {
                        ;
                    }
                    catch(DecryptionException de) {
                        ;
                    }

                    Intent intent = new Intent(BalanceActivity.this, ExodusActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    BalanceActivity.this.startActivity(intent);

                }});

            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

            if(!isFinishing())    {
                alert.show();
            }

            return true;
        }
        else	{
            ;
        }

        return false;
    }

    private void doScan() {
        Intent intent = new Intent(BalanceActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_QR);
    }

    private void doSettings()	{
        TimeOutUtil.getInstance().updatePin();
        Intent intent = new Intent(BalanceActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void doSweep()	{
        Intent intent = new Intent(BalanceActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_COLD_STORAGE);
    }

    private void doSweep(final PrivKeyReader privKeyReader)  {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                try {

                    if(privKeyReader == null || privKeyReader.getKey() == null || !privKeyReader.getKey().hasPrivKey())    {
                        Toast.makeText(BalanceActivity.this, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String address = privKeyReader.getKey().toAddress(MainNetParams.get()).toString();
                    UTXO utxo = APIFactory.getInstance(BalanceActivity.this).getUnspentOutputsForSweep(address);
                    if(utxo != null)    {

                        long total_value = 0L;
                        final List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                        for(MyTransactionOutPoint outpoint : outpoints)   {
                            total_value += outpoint.getValue().longValue();
                        }

                        final BigInteger fee = FeeUtil.getInstance().estimatedFee(outpoints.size(), 1);

                        final long amount = total_value - fee.longValue();
//                        Log.d("BalanceActivity", "Total value:" + total_value);
//                        Log.d("BalanceActivity", "Amount:" + amount);
//                        Log.d("BalanceActivity", "Fee:" + fee.toString());

                        String message = "Sweep " + Coin.valueOf(amount).toPlainString() + " from " + address + " (fee:" + Coin.valueOf(fee.longValue()).toPlainString() + ")?";

                        AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(message);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {

                                final ProgressDialog progress = new ProgressDialog(BalanceActivity.this);
                                progress.setCancelable(false);
                                progress.setTitle(R.string.app_name);
                                progress.setMessage(getString(R.string.please_wait_sending));
                                progress.show();

                                String receive_address = AddressFactory.getInstance(BalanceActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                                final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                                receivers.put(receive_address, BigInteger.valueOf(amount));
                                org.bitcoinj.core.Transaction tx = SendFactory.getInstance(BalanceActivity.this).makeTransaction(0, outpoints, receivers);

                                tx = SendFactory.getInstance(BalanceActivity.this).signTransactionForSweep(tx, privKeyReader);
                                final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
//                                Log.d("BalanceActivity", hexTx);

                                try {
                                    String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "pushtx", "tx=" + hexTx);
                                    Log.d("BalanceActivity", "pushTx:" + response);
                                    if(response.contains("Transaction Submitted"))    {
                                        Toast.makeText(BalanceActivity.this, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                                    }
                                    else    {
                                        Toast.makeText(BalanceActivity.this, R.string.cannot_sweep_privkey, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                catch(Exception e) {
                                    Toast.makeText(BalanceActivity.this, R.string.cannot_sweep_privkey, Toast.LENGTH_SHORT).show();
                                }

                                if(progress != null && progress.isShowing())    {
                                    progress.dismiss();
                                }

                            }
                        });
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                ;
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();

                    }
                    else    {
                        Toast.makeText(BalanceActivity.this, R.string.cannot_find_unspents, Toast.LENGTH_SHORT).show();
                    }

                }
                catch(Exception e) {
                    Toast.makeText(BalanceActivity.this, R.string.cannot_sweep_privkey, Toast.LENGTH_SHORT).show();
                }

                Looper.loop();

            }
        }).start();

    }

    private void doBackup() {

        try {
            final String passphrase = HD_WalletFactory.getInstance(BalanceActivity.this).get().getPassphrase();

            final String[] export_methods = new String[2];
            export_methods[0] = getString(R.string.export_to_clipboard);
            export_methods[1] = getString(R.string.export_to_email);

            new AlertDialog.Builder(BalanceActivity.this)
                    .setTitle(R.string.options_export)
                    .setSingleChoiceItems(export_methods, 0, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    try {
                                        PayloadUtil.getInstance(BalanceActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BalanceActivity.this).getGUID() + AccessFactory.getInstance(BalanceActivity.this).getPIN()));
                                    }
                                    catch (IOException ioe) {
                                        ;
                                    }
                                    catch (JSONException je) {
                                        ;
                                    }
                                    catch (DecryptionException de) {
                                        ;
                                    }
                                    catch (MnemonicException.MnemonicLengthException mle) {
                                        ;
                                    }

                                    String encrypted = null;
                                    try {
                                        encrypted = AESUtil.encrypt(PayloadUtil.getInstance(BalanceActivity.this).getPayload().toString(), new CharSequenceX(passphrase), AESUtil.DefaultPBKDF2Iterations);
                                    } catch (Exception e) {
                                        Toast.makeText(BalanceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    } finally {
                                        if (encrypted == null) {
                                            Toast.makeText(BalanceActivity.this, R.string.encryption_error, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }

                                    if (which == 0) {
                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = null;
                                        clip = android.content.ClipData.newPlainText("Wallet backup", encrypted);
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(BalanceActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Intent email = new Intent(Intent.ACTION_SEND);
                                        email.putExtra(Intent.EXTRA_SUBJECT, "Samourai Wallet backup");
                                        email.putExtra(Intent.EXTRA_TEXT, encrypted);
                                        email.setType("message/rfc822");
                                        startActivity(Intent.createChooser(email, BalanceActivity.this.getText(R.string.choose_email_client)));
                                    }

                                    dialog.dismiss();
                                }
                            }
                    ).show();

        }
        catch(IOException ioe) {
            ioe.printStackTrace();
            Toast.makeText(BalanceActivity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            Toast.makeText(BalanceActivity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

    }

    private class TransactionAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_BALANCE = 1;

        TransactionAdapter() {
            inflater = (LayoutInflater)BalanceActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if(txs == null) {
                txs = new ArrayList<Tx>();
                txStates = new HashMap<String, Boolean>();
            }
            return txs.size() + 1;
        }

        @Override
        public String getItem(int position) {
            if(txs == null) {
                txs = new ArrayList<Tx>();
                txStates = new HashMap<String, Boolean>();
            }
            if(position == 0) {
                return "";
            }
            return txs.get(position - 1).toString();
        }

        @Override
        public long getItemId(int position) {
            return position - 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_BALANCE : TYPE_ITEM;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            int type = getItemViewType(position);
            if(convertView == null) {
                if(type == TYPE_BALANCE) {
                    view = tvBalanceBar;
                }
                else {
                    view = inflater.inflate(R.layout.tx_layout_simple, parent, false);
                }
            }
            else {
                view = convertView;
            }

            if(type == TYPE_BALANCE) {
                ;
            }
            else {
                view.findViewById(R.id.TransactionStatus).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView)parent).performItemClick(v, position, 0);
                    }
                });

                view.findViewById(R.id.ConfirmationCount).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView)parent).performItemClick(v, position, 0);
                    }
                });

                Tx tx = txs.get(position - 1);

                TextView tvTodayLabel = (TextView)view.findViewById(R.id.TodayLabel);
                String strDateGroup = DateUtil.getInstance(BalanceActivity.this).group(tx.getTS());
                if(position == 1) {
                    tvTodayLabel.setText(strDateGroup);
                    tvTodayLabel.setVisibility(View.VISIBLE);
                }
                else {
                    Tx prevTx = txs.get(position - 2);
                    String strPrevDateGroup = DateUtil.getInstance(BalanceActivity.this).group(prevTx.getTS());

                    if(strPrevDateGroup.equals(strDateGroup)) {
                        tvTodayLabel.setVisibility(View.GONE);
                    }
                    else {
                        tvTodayLabel.setText(strDateGroup);
                        tvTodayLabel.setVisibility(View.VISIBLE);
                    }
                }

                String strDetails = null;
                String strTS = DateUtil.getInstance(BalanceActivity.this).formatted(tx.getTS());
                long _amount = 0L;
                if(tx.getAmount() < 0.0) {
                    _amount = Math.abs((long)tx.getAmount());
                    strDetails = BalanceActivity.this.getString(R.string.you_sent);
                }
                else {
                    _amount = (long)tx.getAmount();
                    strDetails = BalanceActivity.this.getString(R.string.you_received);
                }
                String strAmount = null;
                String strUnits = null;
                if(isBTC)    {
                    strAmount = getBTCDisplayAmount(_amount);
                    strUnits = getBTCDisplayUnits();
                }
                else    {
                    strAmount = getFiatDisplayAmount(_amount);
                    strUnits = getFiatDisplayUnits();
                }

                TextView tvDirection = (TextView)view.findViewById(R.id.TransactionDirection);
                TextView tvDirection2 = (TextView)view.findViewById(R.id.TransactionDirection2);
                TextView tvDetails = (TextView)view.findViewById(R.id.TransactionDetails);
                ImageView ivTxStatus = (ImageView)view.findViewById(R.id.TransactionStatus);
                TextView tvConfirmationCount = (TextView)view.findViewById(R.id.ConfirmationCount);

                tvDirection.setTypeface(TypefaceUtil.getInstance(BalanceActivity.this).getAwesomeTypeface());
                if(tx.getAmount() < 0.0) {
                    tvDirection.setTextColor(Color.RED);
                    tvDirection.setText(Character.toString((char) TypefaceUtil.awesome_arrow_up));
                }
                else {
                    tvDirection.setTextColor(Color.GREEN);
                    tvDirection.setText(Character.toString((char) TypefaceUtil.awesome_arrow_down));
                }

                if(txStates.containsKey(tx.getHash()) && txStates.get(tx.getHash()) == false) {
                    txStates.put(tx.getHash(), false);
                    displayTxStatus(false, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                }
                else {
                    txStates.put(tx.getHash(), true);
                    displayTxStatus(true, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                }

                tvDirection2.setText(strDetails + " " + strAmount + " " + strUnits);
                if(tx.getPaymentCode() != null)    {
                    String strTaggedTS = strTS + " ";
                    String strSubText = " " + BIP47Meta.getInstance().getDisplayLabel(tx.getPaymentCode()) + " ";
                    strTaggedTS += strSubText;
                    tvDetails.setText(strTaggedTS);
                } else {
                    tvDetails.setText(strTS);
                }
            }

            return view;
        }

    }

    private void refreshTx(final boolean notifTx, final boolean fetch, final boolean dragged) {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                //
                // TBD: check on lookahead/lookbehind for all incoming payment codes
                //
                if(fetch || txs.size() == 0)    {
                    APIFactory.getInstance(BalanceActivity.this).initWallet();
                }

                try {
                    int acc = 0;
                    if(SamouraiWallet.getInstance().getShowTotalBalance())    {
                        if(SamouraiWallet.getInstance().getCurrentSelectedAccount() == 0)    {
                            txs = APIFactory.getInstance(BalanceActivity.this).getAllXpubTxs();
                        }
                        else    {
                            acc = SamouraiWallet.getInstance().getCurrentSelectedAccount() - 1;
                            txs = APIFactory.getInstance(BalanceActivity.this).getXpubTxs().get(HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(acc).xpubstr());
                        }
                    }
                    else    {
                        txs = APIFactory.getInstance(BalanceActivity.this).getXpubTxs().get(HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(acc).xpubstr());
                    }
                    if(txs != null)    {
                        Collections.sort(txs, new APIFactory.TxMostRecentDateComparator());
                    }

                    if(AddressFactory.getInstance().getHighestTxReceiveIdx(acc) > HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(acc).getReceive().getAddrIdx()) {
                        HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(acc).getReceive().setAddrIdx(AddressFactory.getInstance().getHighestTxReceiveIdx(acc));
                    }
                    if(AddressFactory.getInstance().getHighestTxChangeIdx(acc) > HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(acc).getChange().getAddrIdx()) {
                        HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(acc).getChange().setAddrIdx(AddressFactory.getInstance().getHighestTxChangeIdx(acc));
                    }
                }
                catch(IOException ioe) {
                    ioe.printStackTrace();
                }
                catch(MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                }
                finally {
                    ;
                }

                handler.post(new Runnable() {
                    public void run() {
                        if(dragged)    {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        tvBalanceAmount.setText("");
                        tvBalanceUnits.setText("");
                        displayBalance();
                        txAdapter.notifyDataSetChanged();
                    }
                });

                PrefsUtil.getInstance(BalanceActivity.this).setValue(PrefsUtil.FIRST_RUN, false);

                if(notifTx)    {
                    //
                    // check for incoming payment code notification tx
                    //
                    try {
                        PaymentCode pcode = BIP47Util.getInstance(BalanceActivity.this).getPaymentCode();
                        APIFactory.getInstance(BalanceActivity.this).getNotifAddress(pcode.notificationAddress().getAddressString());
//                    Log.i("BalanceFragment", "payment code:" + pcode.toString());
//                    Log.i("BalanceFragment", "notification address:" + pcode.notificationAddress().getAddressString());
                    }
                    catch (AddressFormatException afe) {
                        afe.printStackTrace();
                        Toast.makeText(BalanceActivity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
                    }

                    //
                    // check on outgoing payment code notification tx
                    //
                    List<Pair<String,String>> outgoingUnconfirmed = BIP47Meta.getInstance().getOutgoingUnconfirmed();
//                Log.i("BalanceFragment", "outgoingUnconfirmed:" + outgoingUnconfirmed.size());
                    for(Pair<String,String> pair : outgoingUnconfirmed)   {
//                    Log.i("BalanceFragment", "outgoing payment code:" + pair.getLeft());
//                    Log.i("BalanceFragment", "outgoing payment code tx:" + pair.getRight());
                        int confirmations = APIFactory.getInstance(BalanceActivity.this).getNotifTxConfirmations(pair.getRight());
                        if(confirmations > 0)    {
                            BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_SENT_CFM);
                        }
                        if(confirmations == -1)    {
                            BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_NOT_SENT);
                        }
                    }

                    Intent intent = new Intent("com.samourai.wallet.BalanceActivity.RESTART_SERVICE");
                    LocalBroadcastManager.getInstance(BalanceActivity.this).sendBroadcast(intent);
                }

                Looper.loop();

            }
        }).start();

    }

    private void displayBalance() {
        String strFiat = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        double btc_fx = ExchangeRateFactory.getInstance(BalanceActivity.this).getAvgPrice(strFiat);

        long balance = 0L;
        if(SamouraiWallet.getInstance().getShowTotalBalance())    {
            if(SamouraiWallet.getInstance().getCurrentSelectedAccount() == 0)    {
                balance = APIFactory.getInstance(BalanceActivity.this).getXpubBalance();
            }
            else    {
                if(APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().size() > 0)    {
                    try    {
                        if(APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(SamouraiWallet.getInstance().getCurrentSelectedAccount() - 1).xpubstr()) != null)    {
                            balance = APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(SamouraiWallet.getInstance().getCurrentSelectedAccount() - 1).xpubstr());
                        }
                    }
                    catch(IOException ioe)    {
                        ;
                    }
                    catch(MnemonicException.MnemonicLengthException mle)    {
                        ;
                    }
                }
            }
        }
        else    {
            if(APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().size() > 0)    {
                try    {
                    if(APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(SamouraiWallet.getInstance().getCurrentSelectedAccount()).xpubstr()) != null)    {
                        balance = APIFactory.getInstance(BalanceActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(SamouraiWallet.SAMOURAI_ACCOUNT).xpubstr());
                    }
                }
                catch(IOException ioe)    {
                    ;
                }
                catch(MnemonicException.MnemonicLengthException mle)    {
                    ;
                }
            }
        }
        double btc_balance = (((double)balance) / 1e8);
        double fiat_balance = btc_fx * btc_balance;

        if(isBTC) {
            tvBalanceAmount.setText(getBTCDisplayAmount(balance));
            tvBalanceUnits.setText(getBTCDisplayUnits());
        }
        else {
            tvBalanceAmount.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance));
            tvBalanceUnits.setText(strFiat);
        }

    }

    private String getBTCDisplayAmount(long value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format(((double)(value * 1000000L)) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format(((double)(value * 1000L)) / 1e8);
                break;
            default:
                strAmount = Coin.valueOf(value).toPlainString();
                break;
        }

        return strAmount;
    }

    private String getBTCDisplayUnits() {

        return (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

    private String getFiatDisplayAmount(long value) {

        String strFiat = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        double btc_fx = ExchangeRateFactory.getInstance(BalanceActivity.this).getAvgPrice(strFiat);
        String strAmount = MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (((double)value) / 1e8));

        return strAmount;
    }

    private String getFiatDisplayUnits() {

        return PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");

    }

    private void displayTxStatus(boolean heads, long confirmations, TextView tvConfirmationCount, ImageView ivTxStatus)	{

        if(heads)	{
            if(confirmations == 0) {
                rotateTxStatus(tvConfirmationCount, true);
                ivTxStatus.setVisibility(View.VISIBLE);
                ivTxStatus.setImageResource(R.drawable.ic_query_builder_white);
                tvConfirmationCount.setVisibility(View.GONE);
            }
            else if(confirmations > 3) {
                rotateTxStatus(tvConfirmationCount, true);
                ivTxStatus.setVisibility(View.VISIBLE);
                ivTxStatus.setImageResource(R.drawable.ic_done_white);
                tvConfirmationCount.setVisibility(View.GONE);
            }
            else {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText(Long.toString(confirmations));
                ivTxStatus.setVisibility(View.GONE);
            }
        }
        else	{
            if(confirmations < 100) {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText(Long.toString(confirmations));
                ivTxStatus.setVisibility(View.GONE);
            }
            else    {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText("\u221e");
                ivTxStatus.setVisibility(View.GONE);
            }
        }

    }

    private void rotateTxStatus(View view, boolean clockwise)	{

        float degrees = 360f;
        if(!clockwise)	{
            degrees = -360f;
        }

        ObjectAnimator animation = ObjectAnimator.ofFloat(view, "rotationY", 0.0f, degrees);
        animation.setDuration(1000);
        animation.setRepeatCount(0);
        animation.setInterpolator(new AnticipateInterpolator());
        animation.start();
    }

    private void doExplorerView(String strHash)   {

        if(strHash != null) {
            int sel = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
            CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerUrls()[sel];

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + strHash));
            startActivity(browserIntent);
        }

    }

}
