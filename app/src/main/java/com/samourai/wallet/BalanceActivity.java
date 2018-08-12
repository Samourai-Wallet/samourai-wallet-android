package com.samourai.wallet;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.InputType;
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
import android.util.Log;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.MnemonicException;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.wallet.JSONRPC.JSONRPC;
import com.samourai.wallet.JSONRPC.PoW;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.paynym.ClaimPayNymActivity;
import com.samourai.wallet.bip47.rpc.*;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.permissions.PermissionsUtil;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.SweepUtil;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.service.RefreshService;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.DateUtil;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.PrivKeyReader;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.util.TorUtil;
import com.samourai.wallet.util.TypefaceUtil;

import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.encoders.DecoderException;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import com.yanzhenjie.zbar.Symbol;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class BalanceActivity extends Activity {

    private final static int SCAN_COLD_STORAGE = 2011;
    private final static int SCAN_QR = 2012;

    private LinearLayout layoutAlert = null;

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

    private PoWTask powTask = null;
    private RBFTask rbfTask = null;
    private CPFPTask cpfpTask = null;

    private ProgressDialog progress = null;

    public static final String ACTION_INTENT = "com.samourai.wallet.BalanceFragment.REFRESH";
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if(ACTION_INTENT.equals(intent.getAction())) {

                final boolean notifTx = intent.getBooleanExtra("notifTx", false);
                final boolean fetch = intent.getBooleanExtra("fetch", false);

                final String rbfHash;
                final String blkHash;
                if(intent.hasExtra("rbf"))    {
                    rbfHash = intent.getStringExtra("rbf");
                }
                else    {
                    rbfHash = null;
                }
                if(intent.hasExtra("hash"))    {
                    blkHash = intent.getStringExtra("hash");
                }
                else    {
                    blkHash = null;
                }

                Handler handler = new Handler();
                handler.post(new Runnable() {
                    public void run() {
                        refreshTx(notifTx, false, false);

                        if(BalanceActivity.this != null)    {

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
                /*
                BalanceActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        refreshTx(notifTx, false, false);

                        if(BalanceActivity.this != null)    {

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
                */

                if(BalanceActivity.this != null && blkHash != null && PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true && TrustedNodeUtil.getInstance().isSet())    {

//                    BalanceActivity.this.runOnUiThread(new Runnable() {
//                    @Override
                    handler.post(new Runnable() {
                        public void run() {
                            if(powTask == null || powTask.getStatus().equals(AsyncTask.Status.FINISHED))    {
                                powTask = new PoWTask();
                                powTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, blkHash);
                            }
                        }

                    });

                }

            }

        }
    };

    public static final String DISPLAY_INTENT = "com.samourai.wallet.BalanceFragment.DISPLAY";
    protected BroadcastReceiver receiverDisplay = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if(DISPLAY_INTENT.equals(intent.getAction())) {

                updateDisplay();

                List<UTXO> utxos = APIFactory.getInstance(BalanceActivity.this).getUtxos(false);
                for(UTXO utxo : utxos)   {
                    List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                    for(MyTransactionOutPoint out : outpoints)   {

                        byte[] scriptBytes = out.getScriptBytes();
                        String address = null;
                        try {
                            if(Bech32Util.getInstance().isBech32Script(Hex.toHexString(scriptBytes)))    {
                                address = Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(scriptBytes));
                            }
                            else    {
                                address = new Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                            }
                        }
                        catch(Exception e) {
                            ;
                        }
                        String path = APIFactory.getInstance(BalanceActivity.this).getUnspentPaths().get(address);
                        if(path != null && path.startsWith("M/1/"))    {
                            continue;
                        }

                        final String hash = out.getHash().toString();
                        final int idx = out.getTxOutputN();
                        final long amount = out.getValue().longValue();

                        if(amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD &&
                                !BlockedUTXO.getInstance().contains(hash, idx) &&
                                !BlockedUTXO.getInstance().containsNotDusted(hash, idx))    {

//                            BalanceActivity.this.runOnUiThread(new Runnable() {
//                            @Override
                            Handler handler = new Handler();
                            handler.post(new Runnable() {
                                public void run() {

                                    String message = BalanceActivity.this.getString(R.string.dusting_attempt);
                                    message += "\n\n";
                                    message += BalanceActivity.this.getString(R.string.dusting_attempt_amount);
                                    message += " ";
                                    message += Coin.valueOf(amount).toPlainString();
                                    message += " BTC\n";
                                    message += BalanceActivity.this.getString(R.string.dusting_attempt_id);
                                    message += " ";
                                    message += hash + "-" + idx;

                                    AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                                            .setTitle(R.string.dusting_tx)
                                            .setMessage(message)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.dusting_attempt_mark_unspendable, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {

                                                    BlockedUTXO.getInstance().add(hash, idx, amount);

                                                }
                                            }).setNegativeButton(R.string.dusting_attempt_ignore, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {

                                                    BlockedUTXO.getInstance().addNotDusted(hash, idx);

                                                }
                                            });
                                    if(!isFinishing())    {
                                        dlg.show();
                                    }

                                }
                            });

                        }

                    }

                }

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

        if(SamouraiWallet.getInstance().isTestNet())    {
            setTitle(getText(R.string.app_name) + ":" + "TestNet");
        }

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

                try {
                    HD_Wallet hdw = HD_WalletFactory.getInstance(BalanceActivity.this).get();

                    if(hdw != null)    {
                        Intent intent = new Intent(BalanceActivity.this, ReceiveActivity.class);
                        startActivity(intent);
                    }

                }
                catch(IOException | MnemonicException.MnemonicLengthException e) {
                    ;
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
                final Tx tx = txs.get(position - 1);
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

                    String message = getString(R.string.options_unconfirmed_tx);

                    // RBF
                    if(tx.getConfirmations() < 1 && tx.getAmount() < 0.0 && RBFUtil.getInstance().contains(tx.getHash()))    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(message);
                        builder.setCancelable(true);
                        builder.setPositiveButton(R.string.options_bump_fee, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {

                                if(rbfTask == null || rbfTask.getStatus().equals(AsyncTask.Status.FINISHED))    {
                                    rbfTask = new RBFTask();
                                    rbfTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tx.getHash());
                                }

                            }
                        });
                        builder.setNegativeButton(R.string.options_block_explorer, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                doExplorerView(tx.getHash());
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                    // CPFP receive
                    else if(tx.getConfirmations() < 1 && tx.getAmount() >= 0.0)   {
                        AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(message);
                        builder.setCancelable(true);
                        builder.setPositiveButton(R.string.options_bump_fee, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {

                                if(cpfpTask == null || cpfpTask.getStatus().equals(AsyncTask.Status.FINISHED))    {
                                    cpfpTask = new CPFPTask();
                                    cpfpTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tx.getHash());
                                }

                            }
                        });
                        builder.setNegativeButton(R.string.options_block_explorer, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                doExplorerView(tx.getHash());
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                    // CPFP spend
                    else if(tx.getConfirmations() < 1 && tx.getAmount() < 0.0)   {
                        AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(message);
                        builder.setCancelable(true);
                        builder.setPositiveButton(R.string.options_bump_fee, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {

                                if(cpfpTask == null || cpfpTask.getStatus().equals(AsyncTask.Status.FINISHED))    {
                                    cpfpTask = new CPFPTask();
                                    cpfpTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tx.getHash());
                                }

                            }
                        });
                        builder.setNegativeButton(R.string.options_block_explorer, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                doExplorerView(tx.getHash());
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                    else    {
                        doExplorerView(tx.getHash());
                        return;
                    }

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
                        refreshTx(false, true, false);
                        swipeRefreshLayout.setRefreshing(false);
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
        IntentFilter filterDisplay = new IntentFilter(DISPLAY_INTENT);
        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiverDisplay, filterDisplay);

//        TorUtil.getInstance(BalanceActivity.this).setStatusFromBroadcast(false);
        registerReceiver(torStatusReceiver, new IntentFilter(OrbotHelper.ACTION_STATUS));

        if(!PermissionsUtil.getInstance(BalanceActivity.this).hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || !PermissionsUtil.getInstance(BalanceActivity.this).hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionsUtil.getInstance(BalanceActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.READ_WRITE_EXTERNAL_PERMISSION_CODE);
        }
        if(!PermissionsUtil.getInstance(BalanceActivity.this).hasPermission(Manifest.permission.CAMERA)) {
            PermissionsUtil.getInstance(BalanceActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.CAMERA_PERMISSION_CODE);
        }

        if(PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_CLAIMED, false) == true && PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, false) == false)    {
            doFeaturePayNymUpdate();
        }
        else if(PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_CLAIMED, false) == false &&
                PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_REFUSED, false) == false)    {
            doClaimPayNym();
        }
        else    {
            ;
        }

        if(!AppUtil.getInstance(BalanceActivity.this).isClipboardSeen())    {
            doClipboardCheck();
        }

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {

                progress = new ProgressDialog(BalanceActivity.this);
                progress.setCancelable(true);
                progress.setTitle(R.string.app_name);
                progress.setMessage(getText(R.string.refresh_tx_pre));
                progress.show();
            }
        });

        final Handler delayedHandler = new Handler();
        delayedHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                boolean notifTx = false;
                Bundle extras = getIntent().getExtras();
                if(extras != null && extras.containsKey("notifTx"))	{
                    notifTx = extras.getBoolean("notifTx");
                }

                refreshTx(notifTx,false, true);

//                updateDisplay();
            }
        }, 100L);

        if(!AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            startService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
        }

    }

    @Override
    public void onResume() {
        super.onResume();

//        IntentFilter filter = new IntentFilter(ACTION_INTENT);
//        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiver, filter);

        if(TorUtil.getInstance(BalanceActivity.this).statusFromBroadcast())    {
            OrbotHelper.requestStartTor(BalanceActivity.this);
        }

        AppUtil.getInstance(BalanceActivity.this).checkTimeOut();

        Intent intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
        LocalBroadcastManager.getInstance(BalanceActivity.this).sendBroadcast(intent);

    }

    @Override
    public void onPause() {
        super.onPause();

        ibQuickSend.collapse();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            stopService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
        }

    }

    @Override
    public void onDestroy() {

        LocalBroadcastManager.getInstance(BalanceActivity.this).unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(BalanceActivity.this).unregisterReceiver(receiverDisplay);

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
        else if(TorUtil.getInstance(BalanceActivity.this).statusFromBroadcast())   {
            OrbotHelper.requestStartTor(BalanceActivity.this);
            menu.findItem(R.id.action_tor).setIcon(R.drawable.tor_on);
        }
        else    {
            menu.findItem(R.id.action_tor).setIcon(R.drawable.tor_off);
        }
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_share_receive).setVisible(false);
        menu.findItem(R.id.action_ricochet).setVisible(false);
        menu.findItem(R.id.action_empty_ricochet).setVisible(false);
        menu.findItem(R.id.action_sign).setVisible(false);
        menu.findItem(R.id.action_fees).setVisible(false);
        menu.findItem(R.id.action_batch).setVisible(false);

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
        else if (id == R.id.action_support) {
            doSupport();
        }
        else if (id == R.id.action_sweep) {
            if(!AppUtil.getInstance(BalanceActivity.this).isOfflineMode())    {
                doSweep();
            }
            else    {
                Toast.makeText(BalanceActivity.this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.action_utxo) {
            doUTXO();
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

                doPrivKey(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_COLD_STORAGE)	{
            ;
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == SCAN_QR)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                PrivKeyReader privKeyReader = new PrivKeyReader(new CharSequenceX(strResult));
                try {
                    if(privKeyReader.getFormat() != null)    {
                        doPrivKey(strResult);
                    }
                    else    {
                        Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
                        intent.putExtra("uri", strResult);
                        startActivity(intent);
                    }
                }
                catch(Exception e) {
                    ;
                }

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

    private void updateDisplay()    {
        txs = APIFactory.getInstance(BalanceActivity.this).getAllXpubTxs();
        if(txs != null)    {
            Collections.sort(txs, new APIFactory.TxMostRecentDateComparator());
        }
        tvBalanceAmount.setText("");
        tvBalanceUnits.setText("");
        displayBalance();
        txAdapter.notifyDataSetChanged();

        if(progress != null && progress.isShowing())    {
            progress.dismiss();
            progress.cancel();
            progress = null;
        }

    }

    private void doClaimPayNym() {
        Intent intent = new Intent(BalanceActivity.this, ClaimPayNymActivity.class);
        startActivity(intent);
    }

    private void doSettings()	{
        TimeOutUtil.getInstance().updatePin();
        Intent intent = new Intent(BalanceActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void doSupport()	{
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://samourai.kayako.com/"));
        startActivity(intent);
    }

    private void doUTXO()	{
        Intent intent = new Intent(BalanceActivity.this, UTXOActivity.class);
        startActivity(intent);
    }

    private void doScan() {
        Intent intent = new Intent(BalanceActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_QR);
    }

    private void doSweepViaScan()	{
        Intent intent = new Intent(BalanceActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_COLD_STORAGE);
    }

    private void doSweep()   {

        AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.action_sweep)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_privkey, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText privkey = new EditText(BalanceActivity.this);
                        privkey.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.enter_privkey)
                                .setView(privkey)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final String strPrivKey = privkey.getText().toString();

                                        if(strPrivKey != null && strPrivKey.length() > 0)    {
                                            doPrivKey(strPrivKey);
                                        }

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
                        }

                    }

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        doSweepViaScan();

                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doPrivKey(final String data) {

        PrivKeyReader privKeyReader = null;

        String format = null;
        try	{
            privKeyReader = new PrivKeyReader(new CharSequenceX(data), null);
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
                                    BIP38PrivateKey bip38 = new BIP38PrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), data);
                                    final ECKey ecKey = bip38.decrypt(password);
                                    if(ecKey != null && ecKey.hasPrivKey()) {

                                        if(progress != null && progress.isShowing())    {
                                            progress.cancel();
                                        }

                                        pvr.setPassword(new CharSequenceX(password));
                                        keyDecoded = true;

                                        Toast.makeText(BalanceActivity.this, pvr.getFormat(), Toast.LENGTH_SHORT).show();
                                        Toast.makeText(BalanceActivity.this, pvr.getKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), Toast.LENGTH_SHORT).show();

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
                                    SweepUtil.getInstance(BalanceActivity.this).sweep(pvr, SweepUtil.TYPE_P2PKH);
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
                SweepUtil.getInstance(BalanceActivity.this).sweep(privKeyReader, SweepUtil.TYPE_P2PKH);
            }
            else    {
                ;
            }

        }
        else    {
            Toast.makeText(BalanceActivity.this, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
        }

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

                                    JSONObject obj = PayloadUtil.getInstance(BalanceActivity.this).putPayload(encrypted, true);

                                    if (which == 0) {
                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = null;
                                        clip = android.content.ClipData.newPlainText("Wallet backup", obj.toString());
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(BalanceActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Intent email = new Intent(Intent.ACTION_SEND);
                                        email.putExtra(Intent.EXTRA_SUBJECT, "Samourai Wallet backup");
                                        email.putExtra(Intent.EXTRA_TEXT, obj.toString());
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

    private void doClipboardCheck()  {

        final android.content.ClipboardManager clipboard = (android.content.ClipboardManager)BalanceActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if(clipboard.hasPrimaryClip())    {
            final ClipData clip = clipboard.getPrimaryClip();
            ClipData.Item item = clip.getItemAt(0);
            if(item.getText() != null)    {
                String text = item.getText().toString();
                String[] s = text.split("\\s+");

                try {
                    for(int i = 0; i < s.length; i++)   {
                        PrivKeyReader privKeyReader = new PrivKeyReader(new CharSequenceX(s[i]));
                        if(privKeyReader.getFormat() != null &&
                                (privKeyReader.getFormat().equals(PrivKeyReader.WIF_COMPRESSED) ||
                                        privKeyReader.getFormat().equals(PrivKeyReader.WIF_UNCOMPRESSED) ||
                                        privKeyReader.getFormat().equals(PrivKeyReader.BIP38) ||
                                        FormatsUtil.getInstance().isValidXprv(s[i])
                                )
                                )    {

                            new AlertDialog.Builder(BalanceActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.privkey_clipboard)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            clipboard.setPrimaryClip(ClipData.newPlainText("", ""));

                                        }

                                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            }).show();

                        }
                    }
                }
                catch(Exception e) {
                    ;
                }
            }
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

    private void refreshTx(final boolean notifTx, final boolean dragged, final boolean launch) {

        if(AppUtil.getInstance(BalanceActivity.this).isOfflineMode())    {
            Toast.makeText(BalanceActivity.this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show();
            /*
            CoordinatorLayout coordinatorLayout = new CoordinatorLayout(BalanceActivity.this);
            Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.in_offline_mode, Snackbar.LENGTH_LONG);
            snackbar.show();
            */
        }

        Intent intent = new Intent(BalanceActivity.this, RefreshService.class);
        intent.putExtra("notifTx", notifTx);
        intent.putExtra("dragged", dragged);
        intent.putExtra("launch", launch);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
        else    {
            startService(intent);
        }

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
                    catch(NullPointerException npe)    {
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
                catch(NullPointerException npe)    {
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

        strAmount = Coin.valueOf(value).toPlainString();

        return strAmount;
    }

    private String getBTCDisplayUnits() {

        return MonetaryUtil.getInstance().getBTCUnits();

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
            if(sel >= BlockExplorerUtil.getInstance().getBlockExplorerTxUrls().length)    {
                sel = 0;
            }
            CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerTxUrls()[sel];

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + strHash));
            startActivity(browserIntent);
        }

    }

    private class PoWTask extends AsyncTask<String, Void, String> {

        private boolean isOK = true;
        private String strBlockHash = null;

        @Override
        protected String doInBackground(String... params) {

            strBlockHash = params[0];

            JSONRPC jsonrpc = new JSONRPC(TrustedNodeUtil.getInstance().getUser(), TrustedNodeUtil.getInstance().getPassword(), TrustedNodeUtil.getInstance().getNode(), TrustedNodeUtil.getInstance().getPort());
            JSONObject nodeObj = jsonrpc.getBlockHeader(strBlockHash);
            if(nodeObj != null && nodeObj.has("hash"))    {
                PoW pow = new PoW(strBlockHash);
                String hash = pow.calcHash(nodeObj);
                if(hash != null && hash.toLowerCase().equals(strBlockHash.toLowerCase()))    {

                    JSONObject headerObj = APIFactory.getInstance(BalanceActivity.this).getBlockHeader(strBlockHash);
                    if(headerObj != null && headerObj.has(""))    {
                        if(!pow.check(headerObj, nodeObj, hash))    {
                            isOK = false;
                        }
                    }

                }
                else    {
                    isOK = false;
                }
            }

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {

            if(!isOK)    {

                new AlertDialog.Builder(BalanceActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.trusted_node_pow_failed) + "\n" + "Block hash:" + strBlockHash)
                        .setCancelable(false)
                        .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();

                            }
                        }).show();

            }

        }

        @Override
        protected void onPreExecute() {
            ;
        }

    }

    private class CPFPTask extends AsyncTask<String, Void, String> {

        private List<UTXO> utxos = null;
        private Handler handler = null;

        @Override
        protected void onPreExecute() {
            handler = new Handler();
            utxos = APIFactory.getInstance(BalanceActivity.this).getUtxos(true);
        }

        @Override
        protected String doInBackground(String... params) {

            Looper.prepare();

            Log.d("BalanceActivity", "hash:" + params[0]);

            JSONObject txObj = APIFactory.getInstance(BalanceActivity.this).getTxInfo(params[0]);
            if(txObj.has("inputs") && txObj.has("outputs"))    {

                final SuggestedFee suggestedFee = FeeUtil.getInstance().getSuggestedFee();

                try {
                    JSONArray inputs = txObj.getJSONArray("inputs");
                    JSONArray outputs = txObj.getJSONArray("outputs");

                    int p2pkh = 0;
                    int p2sh_p2wpkh = 0;
                    int p2wpkh = 0;

                    for(int i = 0; i < inputs.length(); i++)   {
                        if(inputs.getJSONObject(i).has("outpoint") && inputs.getJSONObject(i).getJSONObject("outpoint").has("scriptpubkey"))    {
                            String scriptpubkey = inputs.getJSONObject(i).getJSONObject("outpoint").getString("scriptpubkey");
                            Script script = new Script(Hex.decode(scriptpubkey));
                            String address = null;
                            if(Bech32Util.getInstance().isBech32Script(scriptpubkey))    {
                                try {
                                    address = Bech32Util.getInstance().getAddressFromScript(scriptpubkey);
                                }
                                catch(Exception e) {
                                    ;
                                }
                            }
                            else    {
                                address = script.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                            }
                            if(FormatsUtil.getInstance().isValidBech32(address))    {
                                p2wpkh++;
                            }
                            else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                                p2sh_p2wpkh++;
                            }
                            else    {
                                p2pkh++;
                            }
                        }
                    }

                    FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                    BigInteger estimatedFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length());

                    long total_inputs = 0L;
                    long total_outputs = 0L;
                    long fee = 0L;

                    UTXO utxo = null;

                    for(int i = 0; i < inputs.length(); i++)   {
                        JSONObject obj = inputs.getJSONObject(i);
                        if(obj.has("outpoint"))    {
                            JSONObject objPrev = obj.getJSONObject("outpoint");
                            if(objPrev.has("value"))    {
                                total_inputs += objPrev.getLong("value");
                            }
                        }
                    }

                    for(int i = 0; i < outputs.length(); i++)   {
                        JSONObject obj = outputs.getJSONObject(i);
                        if(obj.has("value"))    {
                            total_outputs += obj.getLong("value");

                            String addr = obj.getString("address");
                            Log.d("BalanceActivity", "checking address:" + addr);
                            if(utxo == null)    {
                                utxo = getUTXO(addr);
                            }
                            else    {
                                break;
                            }
                        }
                    }

                    boolean feeWarning = false;
                    fee = total_inputs - total_outputs;
                    if(fee > estimatedFee.longValue())    {
                        feeWarning = true;
                    }

                    Log.d("BalanceActivity", "total inputs:" + total_inputs);
                    Log.d("BalanceActivity", "total outputs:" + total_outputs);
                    Log.d("BalanceActivity", "fee:" + fee);
                    Log.d("BalanceActivity", "estimated fee:" + estimatedFee.longValue());
                    Log.d("BalanceActivity", "fee warning:" + feeWarning);
                    if(utxo != null)    {
                        Log.d("BalanceActivity", "utxo found");

                        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
                        selectedUTXO.add(utxo);
                        int selected = utxo.getOutpoints().size();

                        long remainingFee = (estimatedFee.longValue() > fee) ? estimatedFee.longValue() - fee : 0L;
                        Log.d("BalanceActivity", "remaining fee:" + remainingFee);
                        int receiveIdx = AddressFactory.getInstance(BalanceActivity.this).getHighestTxReceiveIdx(0);
                        Log.d("BalanceActivity", "receive index:" + receiveIdx);
                        final String addr;
                        if(PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == true)    {
                            addr = utxo.getOutpoints().get(0).getAddress();
                        }
                        else    {
                            addr = outputs.getJSONObject(0).getString("address");
                        }
                        final String ownReceiveAddr;
                        if(FormatsUtil.getInstance().isValidBech32(addr))    {
                            ownReceiveAddr = AddressFactory.getInstance(BalanceActivity.this).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
                        }
                        else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress())    {
                            ownReceiveAddr = AddressFactory.getInstance(BalanceActivity.this).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
                        }
                        else    {
                            ownReceiveAddr = AddressFactory.getInstance(BalanceActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                        }
                        Log.d("BalanceActivity", "receive address:" + ownReceiveAddr);

                        long totalAmount = utxo.getValue();
                        Log.d("BalanceActivity", "amount before fee:" + totalAmount);
                        Triple<Integer,Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
                        BigInteger cpfpFee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), 1);
                        Log.d("BalanceActivity", "cpfp fee:" + cpfpFee.longValue());

                        p2pkh = outpointTypes.getLeft();
                        p2sh_p2wpkh = outpointTypes.getMiddle();
                        p2wpkh = outpointTypes.getRight();

                        if(totalAmount < (cpfpFee.longValue() + remainingFee)) {
                            Log.d("BalanceActivity", "selecting additional utxo");
                            Collections.sort(utxos, new UTXO.UTXOComparator());
                            for(UTXO _utxo : utxos)   {
                                totalAmount += _utxo.getValue();
                                selectedUTXO.add(_utxo);
                                selected += _utxo.getOutpoints().size();
                                outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
                                p2pkh += outpointTypes.getLeft();
                                p2sh_p2wpkh += outpointTypes.getMiddle();
                                p2wpkh += outpointTypes.getRight();
                                cpfpFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, 1);
                                if(totalAmount > (cpfpFee.longValue() + remainingFee + SamouraiWallet.bDust.longValue())) {
                                    break;
                                }
                            }
                            if(totalAmount < (cpfpFee.longValue() + remainingFee + SamouraiWallet.bDust.longValue())) {
                                handler.post(new Runnable() {
                                    public void run() {
                                        Toast.makeText(BalanceActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                FeeUtil.getInstance().setSuggestedFee(suggestedFee);
                                return "KO";
                            }
                        }

                        cpfpFee = cpfpFee.add(BigInteger.valueOf(remainingFee));
                        Log.d("BalanceActivity", "cpfp fee:" + cpfpFee.longValue());

                        final List<MyTransactionOutPoint> outPoints = new ArrayList<MyTransactionOutPoint>();
                        for(UTXO u : selectedUTXO)   {
                            outPoints.addAll(u.getOutpoints());
                        }

                        long _totalAmount = 0L;
                        for(MyTransactionOutPoint outpoint : outPoints)   {
                            _totalAmount += outpoint.getValue().longValue();
                        }
                        Log.d("BalanceActivity", "checked total amount:" + _totalAmount);
                        assert(_totalAmount == totalAmount);

                        long amount = totalAmount - cpfpFee.longValue();
                        Log.d("BalanceActivity", "amount after fee:" + amount);

                        if(amount < SamouraiWallet.bDust.longValue())    {
                            Log.d("BalanceActivity", "dust output");
                            Toast.makeText(BalanceActivity.this, R.string.cannot_output_dust, Toast.LENGTH_SHORT).show();
                        }

                        final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                        receivers.put(ownReceiveAddr, BigInteger.valueOf(amount));

                        String message = "";
                        if(feeWarning)  {
                            message += BalanceActivity.this.getString(R.string.fee_bump_not_necessary);
                            message += "\n\n";
                        }
                        message += BalanceActivity.this.getString(R.string.bump_fee) + " " + Coin.valueOf(remainingFee).toPlainString() + " BTC";

                        AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                if(AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                                                    stopService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
                                                }
                                                startService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));

                                                Transaction tx = SendFactory.getInstance(BalanceActivity.this).makeTransaction(0, outPoints, receivers);
                                                if(tx != null)    {
                                                    tx = SendFactory.getInstance(BalanceActivity.this).signTransaction(tx);
                                                    final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
                                                    Log.d("BalanceActivity", hexTx);

                                                    final String strTxHash = tx.getHashAsString();
                                                    Log.d("BalanceActivity", strTxHash);

                                                    boolean isOK = false;
                                                    try {

                                                        isOK = PushTx.getInstance(BalanceActivity.this).pushTx(hexTx);

                                                        if(isOK)    {

                                                            handler.post(new Runnable() {
                                                                public void run() {
                                                                    Toast.makeText(BalanceActivity.this, R.string.cpfp_spent, Toast.LENGTH_SHORT).show();

                                                                    FeeUtil.getInstance().setSuggestedFee(suggestedFee);

                                                                    Intent _intent = new Intent(BalanceActivity.this, MainActivity2.class);
                                                                    _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                                    startActivity(_intent);
                                                                }
                                                            });

                                                        }
                                                        else    {
                                                            handler.post(new Runnable() {
                                                                public void run() {
                                                                    Toast.makeText(BalanceActivity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                                                                }
                                                            });

                                                            // reset receive index upon tx fail
                                                            if(FormatsUtil.getInstance().isValidBech32(addr))    {
                                                                int prevIdx = BIP84Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                                                BIP84Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                                            }
                                                            else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress())    {
                                                                int prevIdx = BIP49Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                                                BIP49Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                                            }
                                                            else    {
                                                                int prevIdx = HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(0).getReceive().getAddrIdx() - 1;
                                                                HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                                            }

                                                        }
                                                    }
                                                    catch(MnemonicException.MnemonicLengthException | DecoderException | IOException e) {
                                                        handler.post(new Runnable() {
                                                            public void run() {
                                                                Toast.makeText(BalanceActivity.this, "pushTx:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                    finally {
                                                        ;
                                                    }

                                                }

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        try {
                                            if(Bech32Util.getInstance().isBech32Script(addr))    {
                                                int prevIdx = BIP84Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                                BIP84Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                            }
                                            else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress())    {
                                                int prevIdx = BIP49Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                                BIP49Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                            }
                                            else    {
                                                int prevIdx = HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(0).getReceive().getAddrIdx() - 1;
                                                HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                            }
                                        }
                                        catch(MnemonicException.MnemonicLengthException | DecoderException | IOException e) {
                                            handler.post(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(BalanceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        finally {
                                            dialog.dismiss();
                                        }

                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
                        }

                    }
                    else    {
                        handler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(BalanceActivity.this, R.string.cannot_create_cpfp, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
                catch(final JSONException je) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(BalanceActivity.this, "cpfp:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                FeeUtil.getInstance().setSuggestedFee(suggestedFee);

            }
            else    {
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(BalanceActivity.this, R.string.cpfp_cannot_retrieve_tx, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            Looper.loop();

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            ;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            ;
        }

        private UTXO getUTXO(String address)    {

            UTXO ret = null;
            int idx = -1;

            for(int i = 0; i < utxos.size(); i++)  {
                UTXO utxo = utxos.get(i);
                Log.d("BalanceActivity", "utxo address:" + utxo.getOutpoints().get(0).getAddress());
                if(utxo.getOutpoints().get(0).getAddress().equals(address))    {
                    ret = utxo;
                    idx = i;
                    break;
                }
            }

            if(ret != null)    {
                utxos.remove(idx);
                return ret;
            }

            return null;
        }

    }

    private class RBFTask extends AsyncTask<String, Void, String> {

        private List<UTXO> utxos = null;
        private Handler handler = null;
        private RBFSpend rbf = null;
        private HashMap<String,Long> input_values = null;

        @Override
        protected void onPreExecute() {
            handler = new Handler();
            utxos = APIFactory.getInstance(BalanceActivity.this).getUtxos(true);
            input_values = new HashMap<String,Long>();
        }

        @Override
        protected String doInBackground(final String... params) {

            Looper.prepare();

            Log.d("BalanceActivity", "hash:" + params[0]);

            rbf = RBFUtil.getInstance().get(params[0]);
            Log.d("BalanceActivity", "rbf:" + rbf.toJSON().toString());
            final Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), Hex.decode(rbf.getSerializedTx()));
            Log.d("BalanceActivity", "tx serialized:" + rbf.getSerializedTx());
            Log.d("BalanceActivity", "tx inputs:" + tx.getInputs().size());
            Log.d("BalanceActivity", "tx outputs:" + tx.getOutputs().size());
            JSONObject txObj = APIFactory.getInstance(BalanceActivity.this).getTxInfo(params[0]);
            if(tx != null && txObj.has("inputs") && txObj.has("outputs"))    {
                try {
                    JSONArray inputs = txObj.getJSONArray("inputs");
                    JSONArray outputs = txObj.getJSONArray("outputs");

                    int p2pkh = 0;
                    int p2sh_p2wpkh = 0;
                    int p2wpkh = 0;

                    for(int i = 0; i < inputs.length(); i++)   {
                        if(inputs.getJSONObject(i).has("outpoint") && inputs.getJSONObject(i).getJSONObject("outpoint").has("scriptpubkey"))    {
                            String scriptpubkey = inputs.getJSONObject(i).getJSONObject("outpoint").getString("scriptpubkey");
                            Script script = new Script(Hex.decode(scriptpubkey));
                            String address = null;
                            if(Bech32Util.getInstance().isBech32Script(scriptpubkey))    {
                                try {
                                    address = Bech32Util.getInstance().getAddressFromScript(scriptpubkey);
                                }
                                catch(Exception e) {
                                    ;
                                }
                            }
                            else    {
                                address = script.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                            }
                            if(FormatsUtil.getInstance().isValidBech32(address))    {
                                p2wpkh++;
                            }
                            else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                                p2sh_p2wpkh++;
                            }
                            else    {
                                p2pkh++;
                            }
                        }
                    }

                    SuggestedFee suggestedFee = FeeUtil.getInstance().getSuggestedFee();
                    FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                    BigInteger estimatedFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length());

                    long total_inputs = 0L;
                    long total_outputs = 0L;
                    long fee = 0L;
                    long total_change = 0L;
                    List<String> selfAddresses = new ArrayList<String>();

                    for(int i = 0; i < inputs.length(); i++)   {
                        JSONObject obj = inputs.getJSONObject(i);
                        if(obj.has("outpoint"))    {
                            JSONObject objPrev = obj.getJSONObject("outpoint");
                            if(objPrev.has("value"))    {
                                total_inputs += objPrev.getLong("value");
                                String key = objPrev.getString("txid") + ":" + objPrev.getLong("vout");
                                input_values.put(key, objPrev.getLong("value"));
                            }
                        }
                    }

                    for(int i = 0; i < outputs.length(); i++)   {
                        JSONObject obj = outputs.getJSONObject(i);
                        if(obj.has("value"))    {
                            total_outputs += obj.getLong("value");

                            String _addr = null;
                            if(obj.has("address"))    {
                                _addr = obj.getString("address");
                            }

                            selfAddresses.add(_addr);
                            if(_addr != null && rbf.getChangeAddrs().contains(_addr.toString()))    {
                                total_change += obj.getLong("value");
                            }
                        }
                    }

                    boolean feeWarning = false;
                    fee = total_inputs - total_outputs;
                    if(fee > estimatedFee.longValue())    {
                        feeWarning = true;
                    }

                    long remainingFee = (estimatedFee.longValue() > fee) ? estimatedFee.longValue() - fee : 0L;

                    Log.d("BalanceActivity", "total inputs:" + total_inputs);
                    Log.d("BalanceActivity", "total outputs:" + total_outputs);
                    Log.d("BalanceActivity", "total change:" + total_change);
                    Log.d("BalanceActivity", "fee:" + fee);
                    Log.d("BalanceActivity", "estimated fee:" + estimatedFee.longValue());
                    Log.d("BalanceActivity", "fee warning:" + feeWarning);
                    Log.d("BalanceActivity", "remaining fee:" + remainingFee);

                    List<TransactionOutput> txOutputs = new ArrayList<TransactionOutput>();
                    txOutputs.addAll(tx.getOutputs());

                    long remainder = remainingFee;
                    if(total_change > remainder)    {
                        for(TransactionOutput output : txOutputs)   {
                            Script script = output.getScriptPubKey();
                            String scriptPubKey = Hex.toHexString(script.getProgram());
                            Address _p2sh = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                            Address _p2pkh = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
                            try {
                                if((Bech32Util.getInstance().isBech32Script(scriptPubKey) && rbf.getChangeAddrs().contains(Bech32Util.getInstance().getAddressFromScript(scriptPubKey))) || (_p2sh != null && rbf.getChangeAddrs().contains(_p2sh.toString())) || (_p2pkh != null && rbf.getChangeAddrs().contains(_p2pkh.toString())))    {
                                    if(output.getValue().longValue() >= (remainder + SamouraiWallet.bDust.longValue()))    {
                                        output.setValue(Coin.valueOf(output.getValue().longValue() - remainder));
                                        remainder = 0L;
                                        break;
                                    }
                                    else    {
                                        remainder -= output.getValue().longValue();
                                        output.setValue(Coin.valueOf(0L));      // output will be discarded later
                                    }
                                }
                            }
                            catch(Exception e) {
                                ;
                            }

                        }

                    }

                    //
                    // original inputs are not modified
                    //
                    List<MyTransactionInput> _inputs = new ArrayList<MyTransactionInput>();
                    List<TransactionInput> txInputs = tx.getInputs();
                    for(TransactionInput input : txInputs) {
                        MyTransactionInput _input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], input.getOutpoint(), input.getOutpoint().getHash().toString(), (int)input.getOutpoint().getIndex());
                        _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_NO);
                        _inputs.add(_input);
                        Log.d("BalanceActivity", "add outpoint:" + _input.getOutpoint().toString());
                    }

                    Triple<Integer,Integer,Integer> outpointTypes = null;
                    if(remainder > 0L)    {
                        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
                        long selectedAmount = 0L;
                        int selected = 0;
                        long _remainingFee = remainder;
                        Collections.sort(utxos, new UTXO.UTXOComparator());
                        for(UTXO _utxo : utxos)   {

                            Log.d("BalanceActivity", "utxo value:" + _utxo.getValue());

                            //
                            // do not select utxo that are change outputs in current rbf tx
                            //
                            boolean isChange = false;
                            boolean isSelf = false;
                            for(MyTransactionOutPoint outpoint : _utxo.getOutpoints())  {
                                if(rbf.containsChangeAddr(outpoint.getAddress()))    {
                                    Log.d("BalanceActivity", "is change:" + outpoint.getAddress());
                                    Log.d("BalanceActivity", "is change:" + outpoint.getValue().longValue());
                                    isChange = true;
                                    break;
                                }
                                if(selfAddresses.contains(outpoint.getAddress()))    {
                                    Log.d("BalanceActivity", "is self:" + outpoint.getAddress());
                                    Log.d("BalanceActivity", "is self:" + outpoint.getValue().longValue());
                                    isSelf = true;
                                    break;
                                }
                            }
                            if(isChange || isSelf)    {
                                continue;
                            }

                            selectedUTXO.add(_utxo);
                            selected += _utxo.getOutpoints().size();
                            Log.d("BalanceActivity", "selected utxo:" + selected);
                            selectedAmount += _utxo.getValue();
                            Log.d("BalanceActivity", "selected utxo value:" + _utxo.getValue());
                            outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(_utxo.getOutpoints()));
                            p2pkh += outpointTypes.getLeft();
                            p2sh_p2wpkh += outpointTypes.getMiddle();
                            p2wpkh += outpointTypes.getRight();
                            _remainingFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length() == 1 ? 2 : outputs.length()).longValue();
                            Log.d("BalanceActivity", "_remaining fee:" + _remainingFee);
                            if(selectedAmount >= (_remainingFee + SamouraiWallet.bDust.longValue())) {
                                break;
                            }
                        }
                        long extraChange = 0L;
                        if(selectedAmount < (_remainingFee + SamouraiWallet.bDust.longValue())) {
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(BalanceActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                                }
                            });
                            return "KO";
                        }
                        else    {
                            extraChange = selectedAmount - _remainingFee;
                            Log.d("BalanceActivity", "extra change:" + extraChange);
                        }

                        boolean addedChangeOutput = false;
                        // parent tx didn't have change output
                        if(outputs.length() == 1 && extraChange > 0L)    {
                            try {
                                boolean isSegwitChange = (FormatsUtil.getInstance().isValidBech32(outputs.getJSONObject(0).getString("address")) ||  Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outputs.getJSONObject(0).getString("address")).isP2SHAddress()) || PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false;

                                String change_address = null;
                                if(isSegwitChange)    {
                                    int changeIdx = BIP49Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getChange().getAddrIdx();
                                    change_address = BIP49Util.getInstance(BalanceActivity.this).getAddressAt(AddressFactory.CHANGE_CHAIN, changeIdx).getAddressAsString();
                                }
                                else    {
                                    int changeIdx = HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(0).getChange().getAddrIdx();
                                    change_address = HD_WalletFactory.getInstance(BalanceActivity.this).get().getAccount(0).getChange().getAddressAt(changeIdx).getAddressString();
                                }

                                Script toOutputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), change_address));
                                TransactionOutput output = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(extraChange), toOutputScript.getProgram());
                                txOutputs.add(output);
                                addedChangeOutput = true;
                            }
                            catch(MnemonicException.MnemonicLengthException | IOException e) {
                                handler.post(new Runnable() {
                                    public void run() {
                                        Toast.makeText(BalanceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Toast.makeText(BalanceActivity.this, R.string.cannot_create_change_output, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return "KO";
                            }

                        }
                        // parent tx had change output
                        else    {
                            for(TransactionOutput output : txOutputs)   {
                                Script script = output.getScriptPubKey();
                                String scriptPubKey = Hex.toHexString(script.getProgram());
                                String _addr = null;
                                if(Bech32Util.getInstance().isBech32Script(scriptPubKey))    {
                                    try {
                                        _addr = Bech32Util.getInstance().getAddressFromScript(scriptPubKey);
                                    }
                                    catch(Exception e) {
                                        ;
                                    }
                                }
                                if(_addr == null)    {
                                    Address _address = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
                                    if(_address == null)    {
                                        _address = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                                    }
                                    _addr = _address.toString();
                                }
                                Log.d("BalanceActivity", "checking for change:" + _addr);
                                if(rbf.containsChangeAddr(_addr))    {
                                    Log.d("BalanceActivity", "before extra:" + output.getValue().longValue());
                                    output.setValue(Coin.valueOf(extraChange + output.getValue().longValue()));
                                    Log.d("BalanceActivity", "after extra:" + output.getValue().longValue());
                                    addedChangeOutput = true;
                                    break;
                                }
                            }
                        }

                        // sanity check
                        if(extraChange > 0L && !addedChangeOutput)    {
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(BalanceActivity.this, R.string.cannot_create_change_output, Toast.LENGTH_SHORT).show();
                                }
                            });
                            return "KO";
                        }

                        //
                        // update keyBag w/ any new paths
                        //
                        final HashMap<String,String> keyBag = rbf.getKeyBag();
                        for(UTXO _utxo : selectedUTXO)    {

                            for(MyTransactionOutPoint outpoint : _utxo.getOutpoints()) {

                                MyTransactionInput _input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], outpoint, outpoint.getTxHash().toString(), outpoint.getTxOutputN());
                                _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_NO);
                                _inputs.add(_input);
                                Log.d("BalanceActivity", "add selected outpoint:" + _input.getOutpoint().toString());

                                String path = APIFactory.getInstance(BalanceActivity.this).getUnspentPaths().get(outpoint.getAddress());
                                if(path != null)    {
                                    if(FormatsUtil.getInstance().isValidBech32(outpoint.getAddress()))    {
                                        rbf.addKey(outpoint.toString(), path + "/84");
                                    }
                                    else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outpoint.getAddress()) != null && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outpoint.getAddress()).isP2SHAddress())    {
                                        rbf.addKey(outpoint.toString(), path + "/49");
                                    }
                                    else    {
                                        rbf.addKey(outpoint.toString(), path);
                                    }
                                    Log.d("BalanceActivity", "outpoint address:" + outpoint.getAddress());
                                }
                                else    {
                                    String pcode = BIP47Meta.getInstance().getPCode4Addr(outpoint.getAddress());
                                    int idx = BIP47Meta.getInstance().getIdx4Addr(outpoint.getAddress());
                                    rbf.addKey(outpoint.toString(), pcode + "/" + idx);
                                }

                            }

                        }
                        rbf.setKeyBag(keyBag);

                    }

                    //
                    // BIP69 sort of outputs/inputs
                    //
                    final Transaction _tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams());
                    List<TransactionOutput> _txOutputs = new ArrayList<TransactionOutput>();
                    _txOutputs.addAll(txOutputs);
                    Collections.sort(_txOutputs, new SendFactory.BIP69OutputComparator());
                    for(TransactionOutput to : _txOutputs) {
                        // zero value outputs discarded here
                        if(to.getValue().longValue() > 0L)    {
                            _tx.addOutput(to);
                        }
                    }

                    List<MyTransactionInput> __inputs = new ArrayList<MyTransactionInput>();
                    __inputs.addAll(_inputs);
                    Collections.sort(__inputs, new SendFactory.BIP69InputComparator());
                    for(TransactionInput input : __inputs) {
                        _tx.addInput(input);
                    }

                    FeeUtil.getInstance().setSuggestedFee(suggestedFee);

                    String message = "";
                    if(feeWarning)  {
                        message += BalanceActivity.this.getString(R.string.fee_bump_not_necessary);
                        message += "\n\n";
                    }
                    message += BalanceActivity.this.getString(R.string.bump_fee) + " " + Coin.valueOf(remainingFee).toPlainString() + " BTC";

                    AlertDialog.Builder dlg = new AlertDialog.Builder(BalanceActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    Transaction __tx = signTx(_tx);
                                    final String hexTx = new String(Hex.encode(__tx.bitcoinSerialize()));
                                    Log.d("BalanceActivity", "hex tx:" + hexTx);

                                    final String strTxHash = __tx.getHashAsString();
                                    Log.d("BalanceActivity", "tx hash:" + strTxHash);

                                    if(__tx != null)    {

                                        boolean isOK = false;
                                        try {

                                            isOK = PushTx.getInstance(BalanceActivity.this).pushTx(hexTx);

                                            if(isOK)    {

                                                handler.post(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(BalanceActivity.this, R.string.rbf_spent, Toast.LENGTH_SHORT).show();

                                                        RBFSpend _rbf = rbf;    // includes updated 'keyBag'
                                                        _rbf.setSerializedTx(hexTx);
                                                        _rbf.setHash(strTxHash);
                                                        _rbf.setPrevHash(params[0]);
                                                        RBFUtil.getInstance().add(_rbf);

                                                        Intent _intent = new Intent(BalanceActivity.this, MainActivity2.class);
                                                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                        startActivity(_intent);
                                                    }
                                                });

                                            }
                                            else    {

                                                handler.post(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(BalanceActivity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                            }
                                        }
                                        catch(final DecoderException de) {
                                            handler.post(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(BalanceActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        finally {
                                            ;
                                        }

                                    }

                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    dialog.dismiss();

                                }
                            });
                    if(!isFinishing())    {
                        dlg.show();
                    }

                }
                catch(final JSONException je) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(BalanceActivity.this, "rbf:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
            else    {
                Toast.makeText(BalanceActivity.this, R.string.cpfp_cannot_retrieve_tx, Toast.LENGTH_SHORT).show();
            }

            Looper.loop();

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            ;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            ;
        }

        private Transaction signTx(Transaction tx)    {

            HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();
            HashMap<String,ECKey> keyBag49 = new HashMap<String,ECKey>();
            HashMap<String,ECKey> keyBag84 = new HashMap<String,ECKey>();

            HashMap<String,String> keys = rbf.getKeyBag();
            for(String outpoint : keys.keySet())   {

                ECKey ecKey = null;

                String[] s = keys.get(outpoint).split("/");
                Log.i("BalanceActivity", "path length:" + s.length);
                if(s.length == 4)    {
                    if(s[3].equals("84"))    {
                        HD_Address addr = BIP84Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                        ecKey = addr.getECKey();
                    }
                    else    {
                        HD_Address addr = BIP49Util.getInstance(BalanceActivity.this).getWallet().getAccount(0).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                        ecKey = addr.getECKey();
                    }
                }
                else if(s.length == 3)    {
                    HD_Address hd_address = AddressFactory.getInstance(BalanceActivity.this).get(0, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                    String strPrivKey = hd_address.getPrivateKeyString();
                    DumpedPrivateKey pk = new DumpedPrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), strPrivKey);
                    ecKey = pk.getKey();
                }
                else if(s.length == 2)    {
                    try {
                        PaymentAddress address = BIP47Util.getInstance(BalanceActivity.this).getReceiveAddress(new PaymentCode(s[0]), Integer.parseInt(s[1]));
                        ecKey = address.getReceiveECKey();
                    }
                    catch(Exception e) {
                        ;
                    }
                }
                else    {
                    ;
                }

                Log.i("BalanceActivity", "outpoint:" + outpoint);
                Log.i("BalanceActivity", "path:" + keys.get(outpoint));
//                Log.i("BalanceActivity", "ECKey address from ECKey:" + ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());

                if(ecKey != null) {
                    if(s.length == 4)    {
                        if(s[3].equals("84"))    {
                            keyBag84.put(outpoint, ecKey);
                        }
                        else    {
                            keyBag49.put(outpoint, ecKey);
                        }
                    }
                    else    {
                        keyBag.put(outpoint, ecKey);
                    }
                }
                else {
                    throw new RuntimeException("ECKey error: cannot process private key");
//                    Log.i("ECKey error", "cannot process private key");
                }

            }

            List<TransactionInput> inputs = tx.getInputs();
            for (int i = 0; i < inputs.size(); i++) {

                ECKey ecKey = null;
                String address = null;
                if(inputs.get(i).getValue() != null || keyBag49.containsKey(inputs.get(i).getOutpoint().toString()) || keyBag84.containsKey(inputs.get(i).getOutpoint().toString()))    {
                    if(keyBag84.containsKey(inputs.get(i).getOutpoint().toString()))    {
                        ecKey = keyBag84.get(inputs.get(i).getOutpoint().toString());
                        SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                        address = segwitAddress.getBech32AsString();
                    }
                    else    {
                        ecKey = keyBag49.get(inputs.get(i).getOutpoint().toString());
                        SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                        address = segwitAddress.getAddressAsString();
                    }
                }
                else    {
                    ecKey = keyBag.get(inputs.get(i).getOutpoint().toString());
                    address = ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                }
                Log.d("BalanceActivity", "pubKey:" + Hex.toHexString(ecKey.getPubKey()));
                Log.d("BalanceActivity", "address:" + address);

                if(inputs.get(i).getValue() != null || keyBag49.containsKey(inputs.get(i).getOutpoint().toString()) || keyBag84.containsKey(inputs.get(i).getOutpoint().toString()))    {

                    final SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    Script scriptPubKey = segwitAddress.segWitOutputScript();
                    final Script redeemScript = segwitAddress.segWitRedeemScript();
                    System.out.println("redeem script:" + Hex.toHexString(redeemScript.getProgram()));
                    final Script scriptCode = redeemScript.scriptCode();
                    System.out.println("script code:" + Hex.toHexString(scriptCode.getProgram()));

                    TransactionSignature sig = tx.calculateWitnessSignature(i, ecKey, scriptCode, Coin.valueOf(input_values.get(inputs.get(i).getOutpoint().toString())), Transaction.SigHash.ALL, false);
                    final TransactionWitness witness = new TransactionWitness(2);
                    witness.setPush(0, sig.encodeToBitcoin());
                    witness.setPush(1, ecKey.getPubKey());
                    tx.setWitness(i, witness);

                    if(!FormatsUtil.getInstance().isValidBech32(address) && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                        final ScriptBuilder sigScript = new ScriptBuilder();
                        sigScript.data(redeemScript.getProgram());
                        tx.getInput(i).setScriptSig(sigScript.build());
                        tx.getInput(i).getScriptSig().correctlySpends(tx, i, scriptPubKey, Coin.valueOf(input_values.get(inputs.get(i).getOutpoint().toString())), Script.ALL_VERIFY_FLAGS);
                    }

                }
                else    {
                    Log.i("BalanceActivity", "sign outpoint:" + inputs.get(i).getOutpoint().toString());
                    Log.i("BalanceActivity", "ECKey address from keyBag:" + ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());

                    Log.i("BalanceActivity", "script:" + ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())));
                    Log.i("BalanceActivity", "script:" + Hex.toHexString(ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())).getProgram()));
                    TransactionSignature sig = tx.calculateSignature(i, ecKey, ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())).getProgram(), Transaction.SigHash.ALL, false);
                    tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(sig, ecKey));
                }

            }

            return tx;
        }

    }

    private void doFeaturePayNymUpdate() {

        new Thread(new Runnable() {

            private Handler handler = new Handler();

            @Override
            public void run() {

                Looper.prepare();

                try {

                    JSONObject obj = new JSONObject();
                    obj.put("code", BIP47Util.getInstance(BalanceActivity.this).getPaymentCode().toString());
//                    Log.d("BalanceActivity", obj.toString());
                    String res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(BalanceActivity.this).postURL("application/json", null, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + "api/v1/token", obj.toString());
//                    Log.d("BalanceActivity", res);

                    JSONObject responseObj = new JSONObject(res);
                    if(responseObj.has("token"))    {
                        String token = responseObj.getString("token");

                        String sig = MessageSignUtil.getInstance(BalanceActivity.this).signMessage(BIP47Util.getInstance(BalanceActivity.this).getNotificationAddress().getECKey(), token);
//                        Log.d("BalanceActivity", sig);

                        obj = new JSONObject();
                        obj.put("nym", BIP47Util.getInstance(BalanceActivity.this).getPaymentCode().toString());
                        obj.put("code", BIP47Util.getInstance(BalanceActivity.this).getFeaturePaymentCode().toString());
                        obj.put("signature", sig);

//                        Log.d("BalanceActivity", "nym/add:" + obj.toString());
                        res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(BalanceActivity.this).postURL("application/json", token, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + "api/v1/nym/add", obj.toString());
//                        Log.d("BalanceActivity", res);

                        responseObj = new JSONObject(res);
                        if(responseObj.has("segwit") && responseObj.has("token"))    {
                            PrefsUtil.getInstance(BalanceActivity.this).setValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, true);
                        }
                        else if(responseObj.has("claimed") && responseObj.getBoolean("claimed") == true)    {
                            PrefsUtil.getInstance(BalanceActivity.this).setValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, true);
                        }
                        else    {
                            ;
                        }

                    }
                    else    {
                        ;
                    }

                }
                catch(JSONException je) {
                    je.printStackTrace();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                Looper.loop();

            }

        }).start();

    }

}
