package com.samourai.wallet.home;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.ProgressIndicator;
import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.R;
import com.samourai.wallet.ReceiveActivity;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.paynym.fragments.PayNymOnBoardBottomSheet;
import com.samourai.wallet.send.soroban.meeting.SorobanMeetingListenActivity;
import com.samourai.wallet.settings.SettingsActivity;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.psbt.PSBTUtil;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.home.adapters.TxAdapter;
import com.samourai.wallet.network.NetworkDashboard;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.paynym.PayNymHome;
import com.samourai.wallet.permissions.PermissionsUtil;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SweepUtil;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.cahoots.ManualCahootsActivity;
import com.samourai.wallet.service.JobRefreshService;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.tx.TxDetailsActivity;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.PrivKeyReader;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.utxos.UTXOSActivity;
import com.samourai.wallet.whirlpool.WhirlpoolMain;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import io.matthewnelson.topl_service.TorServiceController;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BalanceActivity extends SamouraiActivity {

    private final static int SCAN_COLD_STORAGE = 2011;
    private final static int SCAN_QR = 2012;
    private final static int UTXO_REQUESTCODE = 2012;
    private static final String TAG = "BalanceActivity";


    private List<Tx> txs = null;
    private RecyclerView TxRecyclerView;
    private ProgressIndicator progressBar;
    private BalanceViewModel balanceViewModel;

    private RicochetQueueTask ricochetQueueTask = null;
    private com.github.clans.fab.FloatingActionMenu menuFab;
    private SwipeRefreshLayout txSwipeLayout;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Toolbar toolbar;
    private Menu menu;
    private ImageView menuTorIcon;
    private ProgressBar progressBarMenu;
    private View whirlpoolFab, sendFab, receiveFab, paynymFab;

    public static final String ACTION_INTENT = "com.samourai.wallet.BalanceFragment.REFRESH";
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {
                if (progressBar != null) {
                    showProgress();
                }
                final boolean notifTx = intent.getBooleanExtra("notifTx", false);
                final boolean fetch = intent.getBooleanExtra("fetch", false);

                final String rbfHash;
                final String blkHash;
                if (intent.hasExtra("rbf")) {
                    rbfHash = intent.getStringExtra("rbf");
                } else {
                    rbfHash = null;
                }
                if (intent.hasExtra("hash")) {
                    blkHash = intent.getStringExtra("hash");
                } else {
                    blkHash = null;
                }

                Handler handler = new Handler();
                handler.post(() -> {
                    refreshTx(notifTx, false, false);

                    if (BalanceActivity.this != null) {

                        if (rbfHash != null) {
                            new MaterialAlertDialogBuilder(BalanceActivity.this)
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
                });

            }

        }
    };

    public static final String DISPLAY_INTENT = "com.samourai.wallet.BalanceFragment.DISPLAY";
    protected BroadcastReceiver receiverDisplay = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if (DISPLAY_INTENT.equals(intent.getAction())) {

                updateDisplay(true);
                List<UTXO> utxos = APIFactory.getInstance(BalanceActivity.this).getUtxos(false);
                for (UTXO utxo : utxos) {
                    List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                    for (MyTransactionOutPoint out : outpoints) {

                        byte[] scriptBytes = out.getScriptBytes();
                        String address = null;
                        try {
                            if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(scriptBytes))) {
                                address = Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(scriptBytes));
                            } else {
                                address = new Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                            }
                        } catch (Exception e) {
                        }
                        String path = APIFactory.getInstance(BalanceActivity.this).getUnspentPaths().get(address);
                        if (path != null && path.startsWith("M/1/")) {
                            continue;
                        }

                        final String hash = out.getHash().toString();
                        final int idx = out.getTxOutputN();
                        final long amount = out.getValue().longValue();
                        boolean contains = ((BlockedUTXO.getInstance().contains(hash, idx) || BlockedUTXO.getInstance().containsNotDusted(hash, idx)));

                        boolean containsInPostMix = (BlockedUTXO.getInstance().containsPostMix(hash, idx) || BlockedUTXO.getInstance().containsNotDustedPostMix(hash, idx));


                        if (amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && (!contains && !containsInPostMix)) {

//                            BalanceActivity.this.runOnUiThread(new Runnable() {
//                            @Override
                            Handler handler = new Handler();
                            handler.post(() -> {

                                String message = BalanceActivity.this.getString(R.string.dusting_attempt);
                                message += "\n\n";
                                message += BalanceActivity.this.getString(R.string.dusting_attempt_amount);
                                message += " ";
                                message += FormatsUtil.formatBTC(amount);
                                message += BalanceActivity.this.getString(R.string.dusting_attempt_id);
                                message += " ";
                                message += hash + "-" + idx;

                                MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(BalanceActivity.this)
                                        .setTitle(R.string.dusting_tx)
                                        .setMessage(message)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.dusting_attempt_mark_unspendable, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                if (account == WhirlpoolMeta.getInstance(BalanceActivity.this).getWhirlpoolPostmix()) {
                                                    BlockedUTXO.getInstance().addPostMix(hash, idx, amount);
                                                } else {
                                                    BlockedUTXO.getInstance().add(hash, idx, amount);
                                                }
                                                saveState();
                                            }
                                        }).setNegativeButton(R.string.dusting_attempt_ignore, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                if (account == WhirlpoolMeta.getInstance(BalanceActivity.this).getWhirlpoolPostmix()) {
                                                    BlockedUTXO.getInstance().addNotDustedPostMix(hash, idx);
                                                } else {
                                                    BlockedUTXO.getInstance().addNotDusted(hash, idx);
                                                }
                                                saveState();
                                            }
                                        });
                                if (!isFinishing()) {
                                    dlg.show();
                                }

                            });

                        }

                    }

                }

            }

        }
    };

    protected void onCreate(Bundle savedInstanceState) {

        //Switch themes based on accounts (blue theme for whirlpool account)
        setSwitchThemes(true);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_balance);
        balanceViewModel = ViewModelProviders.of(this).get(BalanceViewModel.class);
        balanceViewModel.setAccount(account);


        makePaynymAvatarcache();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        TxRecyclerView = findViewById(R.id.rv_txes);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        mCollapsingToolbar = findViewById(R.id.toolbar_layout);
        txSwipeLayout = findViewById(R.id.tx_swipe_container);

        setSupportActionBar(toolbar);
        TxRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider);
        TxRecyclerView.addItemDecoration(new ItemDividerDecorator(drawable));
        menuFab = findViewById(R.id.fab_menu);
        txs = new ArrayList<>();
        whirlpoolFab = findViewById(R.id.whirlpool_fab);
        sendFab =  findViewById(R.id.send_fab);
        receiveFab =  findViewById(R.id.receive_fab);
        paynymFab =  findViewById(R.id.paynym_fab);

        boolean is_sat_prefs = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.IS_SAT, false);

        findViewById(R.id.whirlpool_fab).setOnClickListener(view -> {
            Intent intent = new Intent(BalanceActivity.this, WhirlpoolMain.class);
            startActivity(intent);
            menuFab.toggle(true);
        });

        sendFab.setOnClickListener(view -> {
            Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
            intent.putExtra("via_menu", true);
            intent.putExtra("_account", account);
            startActivity(intent);
            menuFab.toggle(true);
        });

        JSONObject payload = null;
        try {
            payload = PayloadUtil.getInstance(BalanceActivity.this).getPayload();
        } catch (Exception e) {
            AppUtil.getInstance(getApplicationContext()).restartApp();
            e.printStackTrace();
            return;
        }
        if(account == 0 && payload != null && payload.has("prev_balance"))    {
            try    {
                setBalance(payload.getLong("prev_balance"), is_sat_prefs);
            }
            catch(Exception e)    {
                setBalance(0L, is_sat_prefs);
            }
        }
        else    {
            setBalance(0L, is_sat_prefs);
        }

        receiveFab.setOnClickListener(view -> {
            menuFab.toggle(true);

            HD_Wallet hdw = HD_WalletFactory.getInstance(BalanceActivity.this).get();

            if (hdw != null) {
                Intent intent = new Intent(BalanceActivity.this, ReceiveActivity.class);
                startActivity(intent);
            }
        });
        paynymFab.setOnClickListener(view -> {
            menuFab.toggle(true);
            Intent intent = new Intent(BalanceActivity.this, PayNymHome.class);
            startActivity(intent);
        });
        txSwipeLayout.setOnRefreshListener(() -> {
            refreshTx(false, true, false);
            txSwipeLayout.setRefreshing(false);
            showProgress();
        });

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiver, filter);
        IntentFilter filterDisplay = new IntentFilter(DISPLAY_INTENT);
        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiverDisplay, filterDisplay);

        if (!PermissionsUtil.getInstance(BalanceActivity.this).hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || !PermissionsUtil.getInstance(BalanceActivity.this).hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionsUtil.getInstance(BalanceActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.READ_WRITE_EXTERNAL_PERMISSION_CODE);
        }

        if (PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_CLAIMED, false) && !PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, false)) {
            doFeaturePayNymUpdate();
        } else if (!PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_CLAIMED, false) &&
                 !PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_REFUSED, false)) {
             PayNymOnBoardBottomSheet payNymOnBoardBottomSheet = new PayNymOnBoardBottomSheet();
             payNymOnBoardBottomSheet.show(getSupportFragmentManager(),payNymOnBoardBottomSheet.getTag());
        }
        Log.i(TAG, "onCreate:PAYNYM_REFUSED ".concat(String.valueOf(PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.PAYNYM_REFUSED, false))));

        if (RicochetMeta.getInstance(BalanceActivity.this).getQueue().size() > 0) {
            if (ricochetQueueTask == null || ricochetQueueTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                ricochetQueueTask = new RicochetQueueTask();
                ricochetQueueTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        }

        if (!AppUtil.getInstance(BalanceActivity.this).isClipboardSeen()) {
            doClipboardCheck();
        }


        if (!AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            startService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
        }
        setUpTor();
        initViewModel();
        showProgress();

        if (account == 0) {
            final Handler delayedHandler = new Handler();
            delayedHandler.postDelayed(() -> {

                boolean notifTx = false;
                Bundle extras = getIntent().getExtras();
                if (extras != null && extras.containsKey("notifTx")) {
                    notifTx = extras.getBoolean("notifTx");
                }

                refreshTx(notifTx, false, true);

                updateDisplay(false);
            }, 100L);

            getSupportActionBar().setIcon(R.drawable.ic_samourai_logo);

        }
        else {
            getSupportActionBar().setIcon(R.drawable.ic_whirlpool);
            receiveFab.setVisibility(View.GONE);
            whirlpoolFab.setVisibility(View.GONE);
            paynymFab.setVisibility(View.GONE);
            new Handler().postDelayed(() -> updateDisplay(true), 600L);
        }
        balanceViewModel.loadOfflineData();

        boolean hadContentDescription = android.text.TextUtils.isEmpty(toolbar.getLogoDescription());
        String contentDescription = String.valueOf(!hadContentDescription ? toolbar.getLogoDescription() : "logoContentDescription");
        toolbar.setLogoDescription(contentDescription);
        ArrayList<View> potentialViews = new ArrayList<View>();
        toolbar.findViewsWithText(potentialViews,contentDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        View logoView = null;
        if(potentialViews.size() > 0){
            logoView = potentialViews.get(0);
            if (account == 0) {
                logoView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent _intent = new Intent(BalanceActivity.this, BalanceActivity.class);
                        _intent.putExtra("_account", WhirlpoolMeta.getInstance(BalanceActivity.this).getWhirlpoolPostmix());
                        startActivity(_intent);
                    } });
            } else {
                logoView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent _intent = new Intent(BalanceActivity.this, BalanceActivity.class);
                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(_intent);
                    } });
            }
        }

        updateDisplay(false);
        checkDeepLinks();
    }

    private void hideProgress() {
        progressBar.hide();
    }

    private void showProgress() {
        progressBar.setIndeterminate(true);
        progressBar.show();
    }

    private void checkDeepLinks() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return;
        }
        if (bundle.containsKey("pcode") || bundle.containsKey("uri") || bundle.containsKey("amount")) {
            if (balanceViewModel.getBalance().getValue() != null)
                bundle.putLong("balance", balanceViewModel.getBalance().getValue());
            Intent intent = new Intent(this, SendActivity.class);
            intent.putExtra("_account",account);
            intent.putExtras(bundle);
            startActivity(intent);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void initViewModel() {
        TxAdapter adapter = new TxAdapter(getApplicationContext(), new ArrayList<>(), account);
        adapter.setHasStableIds(true);
        adapter.setClickListener((position, tx) -> txDetails(tx));

        TxRecyclerView.setAdapter(adapter);

        boolean is_sat_prefs = PrefsUtil.getInstance(BalanceActivity.this).getValue(PrefsUtil.IS_SAT, false);

        balanceViewModel.getBalance().observe(this, balance -> {
            if (balance < 0) {
                return;
            }
            if (balanceViewModel.getSatState().getValue() != null) {
                setBalance(balance, is_sat_prefs);
            } else {
                setBalance(balance, is_sat_prefs);
            }
        });
        adapter.setTxes(balanceViewModel.getTxs().getValue());
        setBalance(balanceViewModel.getBalance().getValue(), is_sat_prefs);

        balanceViewModel.getSatState().observe(this, state -> {
            if (state == null) {
                state = false;
            }
            setBalance(balanceViewModel.getBalance().getValue(), state);
            adapter.notifyDataSetChanged();
        });
        balanceViewModel.getTxs().observe(this, new Observer<List<Tx>>() {
            @Override
            public void onChanged(@Nullable List<Tx> list) {
                adapter.setTxes(list);
            }
        });
        mCollapsingToolbar.setOnClickListener(v -> {
            boolean is_sat = balanceViewModel.toggleSat();
            PrefsUtil.getInstance(BalanceActivity.this).setValue(PrefsUtil.IS_SAT, is_sat);
        });

        mCollapsingToolbar.setOnLongClickListener(view -> {
            Intent intent = new Intent(BalanceActivity.this, UTXOSActivity.class);
            intent.putExtra("_account", account);
            startActivityForResult(intent,UTXO_REQUESTCODE);

            return false;
        });
    }

    private void setBalance(Long balance, boolean isSat) {
        if (balance == null) {
            return;
        }

        if (getSupportActionBar() != null) {
            TransitionManager.beginDelayedTransition(mCollapsingToolbar, new ChangeBounds());
            String displayAmount = isSat ? FormatsUtil.formatSats(balance) : FormatsUtil.formatBTC(balance);
            toolbar.setTitle(displayAmount);
            setTitle(displayAmount);
            mCollapsingToolbar.setTitle(displayAmount);
        }

        LogUtil.info(TAG, "setBalance: ".concat(FormatsUtil.formatSats(balance)));

    }

    @Override
    public void onResume() {
        super.onResume();

//        IntentFilter filter = new IntentFilter(ACTION_INTENT);
//        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiver, filter);

        AppUtil.getInstance(BalanceActivity.this).checkTimeOut();
//
//        Intent intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
//        LocalBroadcastManager.getInstance(BalanceActivity.this).sendBroadcast(intent);

    }

    public View createTag(String text){
        float scale = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView textView = new TextView(getApplicationContext());
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        textView.setLayoutParams(lparams);
        textView.setBackgroundResource(R.drawable.tag_round_shape);
        textView.setPadding((int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f), (int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f));
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        return textView;
    }

    @Override
    public void onPause() {
        super.onPause();

//        ibQuickSend.collapse();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            stopService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
        }

    }


    private void makePaynymAvatarcache() {
        try {

            ArrayList<String> paymentCodes = new ArrayList<>(BIP47Meta.getInstance().getSortedByLabels(false, true));
            for (String code : paymentCodes) {
                Picasso.get()
                        .load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + code + "/avatar").fetch(new Callback() {
                    @Override
                    public void onSuccess() {
                        /*NO OP*/
                    }

                    @Override
                    public void onError(Exception e) {
                        /*NO OP*/
                    }
                });
            }

        } catch (Exception ignored) {

        }
    }

    @Override
    public void onDestroy() {

        LocalBroadcastManager.getInstance(BalanceActivity.this).unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(BalanceActivity.this).unregisterReceiver(receiverDisplay);

        if(account == 0) {
            if (AppUtil.getInstance(BalanceActivity.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                stopService(new Intent(BalanceActivity.this.getApplicationContext(), WebSocketService.class));
            }
        }

        super.onDestroy();

        if(compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (BuildConfig.FLAVOR.equals("staging") )
            menu.findItem(R.id.action_mock_fees).setVisible(true);

        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_share_receive).setVisible(false);
        menu.findItem(R.id.action_ricochet).setVisible(false);
        menu.findItem(R.id.action_empty_ricochet).setVisible(false);
        menu.findItem(R.id.action_sign).setVisible(false);
        menu.findItem(R.id.action_fees).setVisible(false);
        menu.findItem(R.id.action_batch).setVisible(false);

        WhirlpoolMeta.getInstance(getApplicationContext());
        if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {

            menu.findItem(R.id.action_sweep).setVisible(false);
            menu.findItem(R.id.action_backup).setVisible(false);
            menu.findItem(R.id.action_postmix).setVisible(false);

            menu.findItem(R.id.action_network_dashboard).setVisible(false);
            MenuItem item = menu.findItem(R.id.action_menu_account);
            item.setActionView(createTag(" POST-MIX "));
            item.setVisible(true);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        }
        else {
            menu.findItem(R.id.action_soroban_collab).setVisible(false);
        }
        this.menu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == android.R.id.home){
            this.finish();
            return super.onOptionsItemSelected(item);
        }

        if(id == R.id.action_mock_fees){
            SamouraiWallet.MOCK_FEE =  ! SamouraiWallet.MOCK_FEE;
            refreshTx(false, true, false);
            txSwipeLayout.setRefreshing(false);
            showProgress();
            return super.onOptionsItemSelected(item);
        }

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_network_dashboard) {
            startActivity(new Intent(this, NetworkDashboard.class));
        }    // noinspection SimplifiableIfStatement
        if (id == R.id.action_copy_cahoots) {
            ClipboardManager clipboard = (ClipboardManager)  getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard.hasPrimaryClip())    {
                ClipData.Item clipItem = clipboard.getPrimaryClip().getItemAt(0);

                if(Cahoots.isCahoots(clipItem.getText().toString().trim())){
                    try {
                        Intent cahootIntent = ManualCahootsActivity.createIntentResume(this, account, clipItem.getText().toString().trim());
                        startActivity(cahootIntent);
                    }
                    catch (Exception e) {
                        Toast.makeText(this,R.string.cannot_process_cahoots,Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(this,R.string.cannot_process_cahoots,Toast.LENGTH_SHORT).show();
                }
            }
            else    {
                Toast.makeText(this,R.string.clipboard_empty,Toast.LENGTH_SHORT).show();
            }
        }
        if (id == R.id.action_settings) {
            doSettings();
        }
        else if (id == R.id.action_support) {
            doSupport();
        }
        else if (id == R.id.action_sweep) {
            if (!AppUtil.getInstance(BalanceActivity.this).isOfflineMode()) {
                doSweep();
            }
            else {
                Toast.makeText(BalanceActivity.this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.action_utxo) {
            doUTXO();
        }
        else if (id == R.id.action_backup) {

            if (SamouraiWallet.getInstance().hasPassphrase(BalanceActivity.this)) {
                if (HD_WalletFactory.getInstance(BalanceActivity.this).get() != null && SamouraiWallet.getInstance().hasPassphrase(BalanceActivity.this)) {
                    doBackup(HD_WalletFactory.getInstance(BalanceActivity.this).get().getPassphrase());
                }
            }
            else {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setTitle(R.string.enter_backup_password);
                View view = getLayoutInflater().inflate(R.layout.password_input_dialog_layout, null);
                EditText password = view.findViewById(R.id.restore_dialog_password_edittext);
                TextView message = view.findViewById(R.id.dialogMessage);
                message.setText(R.string.backup_password);
                builder.setPositiveButton(R.string.confirm,(dialog, which) -> {
                    String pw = password.getText().toString();
                    if (pw.length() >= AppUtil.MIN_BACKUP_PW_LENGTH && pw.length() <= AppUtil.MAX_BACKUP_PW_LENGTH) {
                        doBackup(pw);
                    }else{
                        Toast.makeText(getApplicationContext(), R.string.password_error, Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss() ;
                });
                builder.setNegativeButton(R.string.cancel,(dialog, which) -> dialog.dismiss());
                builder.setView(view);
                builder.show();
            }

        }
        else if (id == R.id.action_scan_qr) {
            doScan();
        }
        else if (id == R.id.action_postmix) {

            Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
            intent.putExtra("_account", WhirlpoolMeta.getInstance(BalanceActivity.this).getWhirlpoolPostmix());
            startActivity(intent);

        }
        else if(id == R.id.action_soroban_collab) {
            Intent intent = new Intent(this, SorobanMeetingListenActivity.class);
            intent.putExtra("_account", WhirlpoolMeta.getInstance(BalanceActivity.this).getWhirlpoolPostmix());
            startActivity(intent);
        }
        else {
            ;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setUpTor() {
        TorManager.INSTANCE.getTorStateLiveData().observe(this,torState -> {
            if (torState == TorManager.TorState.ON) {
                PrefsUtil.getInstance(this).setValue(PrefsUtil.ENABLE_TOR, true);
                if (this.progressBarMenu != null) {
                    this.progressBarMenu.setVisibility(View.INVISIBLE);
                    this.menuTorIcon.setImageResource(R.drawable.tor_on);
                }

            } else if (torState == TorManager.TorState.WAITING) {
                if (this.progressBarMenu != null) {
                    this.progressBarMenu.setVisibility(View.VISIBLE);
                    this.menuTorIcon.setImageResource(R.drawable.tor_on);
                }
            } else {
                if (this.progressBarMenu != null) {
                    this.progressBarMenu.setVisibility(View.INVISIBLE);
                    this.menuTorIcon.setImageResource(R.drawable.tor_off);
                }

            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_COLD_STORAGE) {

            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                doPrivKey(strResult);

            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_COLD_STORAGE) {
        } else if (resultCode == Activity.RESULT_OK && requestCode == SCAN_QR) {

            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                PrivKeyReader privKeyReader = new PrivKeyReader(new CharSequenceX(strResult.trim()));
                try {
                    if (privKeyReader.getFormat() != null) {
                        doPrivKey(strResult.trim());
                    } else if (Cahoots.isCahoots(strResult.trim())) {
                        Intent cahootIntent = ManualCahootsActivity.createIntentResume(this, account, strResult.trim());
                        startActivity(cahootIntent);
                    } else if (FormatsUtil.getInstance().isPSBT(strResult.trim())) {
                        PSBTUtil.getInstance(BalanceActivity.this).doPSBT(strResult.trim());
                    } else if (DojoUtil.getInstance(BalanceActivity.this).isValidPairingPayload(strResult.trim())) {

                        Intent intent = new Intent(BalanceActivity.this, NetworkDashboard.class);
                        intent.putExtra("params", strResult.trim());
                        startActivity(intent);

                    } else {
                        Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
                        intent.putExtra("uri", strResult.trim());
                        intent.putExtra("_account", account);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                }

            }
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UTXO_REQUESTCODE) {
            refreshTx(false, false, false);
            showProgress();
        } else {
            ;
        }

    }


    @Override
    public void onBackPressed() {


        if (account == 0) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setMessage(R.string.ask_you_sure_exit);
            AlertDialog alert = builder.create();

            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), (dialog, id) -> {

                try {
                    PayloadUtil.getInstance(BalanceActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BalanceActivity.this).getGUID() + AccessFactory.getInstance(BalanceActivity.this).getPIN()));
                } catch (MnemonicException.MnemonicLengthException mle) {
                } catch (JSONException je) {
                } catch (IOException ioe) {
                } catch (DecryptionException de) {
                }

                // disconnect Whirlpool on app back key exit
                if (WhirlpoolNotificationService.isRunning(getApplicationContext()))
                    WhirlpoolNotificationService.stopService(getApplicationContext());

                if (TorManager.INSTANCE.isConnected()) {
                    TorServiceController.stopTor();
                }
                TimeOutUtil.getInstance().reset();
                finishAffinity();
                finish();
                super.onBackPressed();
            });

            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), (dialog, id) -> dialog.dismiss());
            alert.show();

        } else {
            super.onBackPressed();
        }
    }

    private void updateDisplay(boolean fromRefreshService) {
        Disposable txDisposable = loadTxes(account)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((txes, throwable) -> {
                    if (throwable != null)
                        throwable.printStackTrace();


                    if (txes != null) {
                        if (txes.size() != 0) {
                            balanceViewModel.setTx(txes);
                        } else {
                            if (balanceViewModel.getTxs().getValue() != null && balanceViewModel.getTxs().getValue().size() == 0) {
                                balanceViewModel.setTx(txes);
                            }
                        }

                        Collections.sort(txes, new APIFactory.TxMostRecentDateComparator());
                        txs.clear();
                        txs.addAll(txes);
                    }

                    if (progressBar.getVisibility() == View.VISIBLE && fromRefreshService) {
                        hideProgress();
                    }
                });

        Disposable balanceDisposable = loadBalance(account)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((balance, throwable) -> {
                    if (throwable != null)
                        throwable.printStackTrace();

                    if (balanceViewModel.getBalance().getValue() != null) {
                        balanceViewModel.setBalance(balance);
                    } else {
                        balanceViewModel.setBalance(balance);
                    }
                });
        compositeDisposable.add(balanceDisposable);
        compositeDisposable.add(txDisposable);
//        displayBalance();
//        txAdapter.notifyDataSetChanged();


    }

    private Single<List<Tx>> loadTxes(int account) {
        return Single.fromCallable(() -> {
            List<Tx> loadedTxes = new ArrayList<>();
            if (account == 0) {
                loadedTxes = APIFactory.getInstance(BalanceActivity.this).getAllXpubTxs();
            } else if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                loadedTxes = APIFactory.getInstance(BalanceActivity.this).getAllPostMixTxs();
            }
            return loadedTxes;
        });
    }

    private Single<Long> loadBalance(int account) {
        return Single.fromCallable(() -> {
            long loadedBalance = 0L;
            if (account == 0) {
                loadedBalance = APIFactory.getInstance(BalanceActivity.this).getXpubBalance();
            } else if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                loadedBalance = APIFactory.getInstance(BalanceActivity.this).getXpubPostMixBalance();
            }
            return loadedBalance;
        });
    }


    private void doSettings() {
        TimeOutUtil.getInstance().updatePin();
        Intent intent = new Intent(BalanceActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void doSupport() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/"));
        startActivity(intent);
    }

    private void doUTXO() {
        Intent intent = new Intent(BalanceActivity.this, UTXOSActivity.class);
        intent.putExtra("_account", account);
        startActivityForResult(intent,UTXO_REQUESTCODE);
    }

    private void doScan() {

        CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
        cameraFragmentBottomSheet.show(getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());

        cameraFragmentBottomSheet.setQrCodeScanListener(code -> {
            cameraFragmentBottomSheet.dismissAllowingStateLoss();
            PrivKeyReader privKeyReader = new PrivKeyReader(new CharSequenceX(code.trim()));
            try {
                if (privKeyReader.getFormat() != null) {
                    doPrivKey(code.trim());
                } else if (Cahoots.isCahoots(code.trim())) {
                    Intent cahootIntent = ManualCahootsActivity.createIntentResume(this, account, code.trim());
                    startActivity(cahootIntent);

                } else if (FormatsUtil.getInstance().isPSBT(code.trim())) {
                    PSBTUtil.getInstance(BalanceActivity.this).doPSBT(code.trim());
                } else if (DojoUtil.getInstance(BalanceActivity.this).isValidPairingPayload(code.trim())) {
                    Intent intent = new Intent(BalanceActivity.this, NetworkDashboard.class);
                    intent.putExtra("params", code.trim());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
                    intent.putExtra("uri", code.trim());
                    intent.putExtra("_account", account);
                    startActivity(intent);
                }
            } catch (Exception e) {
            }
        });
    }

    private void doSweepViaScan() {

        CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
        cameraFragmentBottomSheet.show(getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
        cameraFragmentBottomSheet.setQrCodeScanListener(code -> {
            cameraFragmentBottomSheet.dismissAllowingStateLoss();
            PrivKeyReader privKeyReader = new PrivKeyReader(new CharSequenceX(code.trim()));
            try {
                if (privKeyReader.getFormat() != null) {
                    doPrivKey(code.trim());
                } else if (Cahoots.isCahoots(code.trim())) {
                    Intent cahootIntent = ManualCahootsActivity.createIntentResume(this, account, code.trim());
                    startActivity(cahootIntent);
                } else if (FormatsUtil.getInstance().isPSBT(code.trim())) {
                    PSBTUtil.getInstance(BalanceActivity.this).doPSBT(code.trim());
                } else if (DojoUtil.getInstance(BalanceActivity.this).isValidPairingPayload(code.trim())) {
                    Toast.makeText(BalanceActivity.this, "Samourai Dojo full node coming soon.", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
                    intent.putExtra("uri", code.trim());
                    intent.putExtra("_account", account);
                    startActivity(intent);
                }
            } catch (Exception e) {
            }
        });
    }

    private void doSweep() {

        MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(BalanceActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.action_sweep)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_privkey, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText privkey = new EditText(BalanceActivity.this);
                        privkey.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                        MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(BalanceActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.enter_privkey)
                                .setView(privkey)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final String strPrivKey = privkey.getText().toString();

                                        if (strPrivKey != null && strPrivKey.length() > 0) {
                                            doPrivKey(strPrivKey);
                                        }

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                    }
                                });
                        if (!isFinishing()) {
                            dlg.show();
                        }

                    }

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        doSweepViaScan();

                    }
                });
        if (!isFinishing()) {
            dlg.show();
        }

    }

    private void doPrivKey(final String data) {

        PrivKeyReader privKeyReader = null;

        String format = null;
        try {
            privKeyReader = new PrivKeyReader(new CharSequenceX(data), null);
            format = privKeyReader.getFormat();
        } catch (Exception e) {
            Toast.makeText(BalanceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (format != null) {

            if (format.equals(PrivKeyReader.BIP38)) {

                final PrivKeyReader pvr = privKeyReader;

                final EditText password38 = new EditText(BalanceActivity.this);
                password38.setSingleLine(true);
                password38.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(BalanceActivity.this)
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
                                    if (ecKey != null && ecKey.hasPrivKey()) {

                                        if (progress != null && progress.isShowing()) {
                                            progress.cancel();
                                        }

                                        pvr.setPassword(new CharSequenceX(password));
                                        keyDecoded = true;

                                        Toast.makeText(BalanceActivity.this, pvr.getFormat(), Toast.LENGTH_SHORT).show();
                                        Toast.makeText(BalanceActivity.this, pvr.getKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), Toast.LENGTH_SHORT).show();

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(BalanceActivity.this, R.string.bip38_pw_error, Toast.LENGTH_SHORT).show();
                                }

                                if (progress != null && progress.isShowing()) {
                                    progress.cancel();
                                }

                                if (keyDecoded) {
                                    SweepUtil.getInstance(BalanceActivity.this).sweep(pvr, SweepUtil.TYPE_P2PKH);
                                }

                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                Toast.makeText(BalanceActivity.this, R.string.bip38_pw_error, Toast.LENGTH_SHORT).show();

                            }
                        });
                if (!isFinishing()) {
                    dlg.show();
                }

            } else if (privKeyReader != null) {
                SweepUtil.getInstance(BalanceActivity.this).sweep(privKeyReader, SweepUtil.TYPE_P2PKH);
            } else {
                ;
            }

        } else {
            Toast.makeText(BalanceActivity.this, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
        }

    }

    private void doBackup(String passphrase) {


        final String[] export_methods = new String[2];
        export_methods[0] = getString(R.string.export_to_clipboard);
        export_methods[1] = getString(R.string.export_to_email);

        new MaterialAlertDialogBuilder(BalanceActivity.this)
                .setTitle(R.string.options_export)
                .setSingleChoiceItems(export_methods, 0, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                try {
                                    PayloadUtil.getInstance(BalanceActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BalanceActivity.this).getGUID() + AccessFactory.getInstance(BalanceActivity.this).getPIN()));
                                } catch (IOException ioe) {
                                    ;
                                } catch (JSONException je) {
                                    ;
                                } catch (DecryptionException de) {
                                    ;
                                } catch (MnemonicException.MnemonicLengthException mle) {
                                    ;
                                }

                                String encrypted = null;
                                try {
                                    encrypted = AESUtil.encryptSHA256(PayloadUtil.getInstance(BalanceActivity.this).getPayload().toString(), new CharSequenceX(passphrase));
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

    private void doClipboardCheck() {

        final android.content.ClipboardManager clipboard = (android.content.ClipboardManager) BalanceActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            final ClipData clip = clipboard.getPrimaryClip();
            ClipData.Item item = clip.getItemAt(0);
            if (item.getText() != null) {
                String text = item.getText().toString();
                String[] s = text.split("\\s+");

                try {
                    for (int i = 0; i < s.length; i++) {
                        PrivKeyReader privKeyReader = new PrivKeyReader(new CharSequenceX(s[i]));
                        if (privKeyReader.getFormat() != null &&
                                (privKeyReader.getFormat().equals(PrivKeyReader.WIF_COMPRESSED) ||
                                        privKeyReader.getFormat().equals(PrivKeyReader.WIF_UNCOMPRESSED) ||
                                        privKeyReader.getFormat().equals(PrivKeyReader.BIP38) ||
                                        FormatsUtil.getInstance().isValidXprv(s[i])
                                )
                        ) {

                            new MaterialAlertDialogBuilder(BalanceActivity.this)
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
                } catch (Exception e) {
                    ;
                }
            }
        }

    }

    private void refreshTx(final boolean notifTx, final boolean dragged, final boolean launch) {

        if (AppUtil.getInstance(BalanceActivity.this).isOfflineMode()) {
            Toast.makeText(BalanceActivity.this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show();
            /*
            CoordinatorLayout coordinatorLayout = new CoordinatorLayout(BalanceActivity.this);
            Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.in_offline_mode, Snackbar.LENGTH_LONG);
            snackbar.show();
            */
        }


        Intent intent = new Intent(this, JobRefreshService.class);
        intent.putExtra("notifTx", notifTx);
        intent.putExtra("dragged", dragged);
        intent.putExtra("launch", launch);
        JobRefreshService.enqueueWork(getApplicationContext(), intent);
//
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent);
//        } else {
//            startService(intent);
//        }

    }


    private void doExplorerView(String strHash) {

        if (strHash != null) {

            String blockExplorer = "https://m.oxt.me/transaction/";
            if (SamouraiWallet.getInstance().isTestNet()) {
                blockExplorer = "https://blockstream.info/testnet/";
            }
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + strHash));
            startActivity(browserIntent);
        }

    }

    private void txDetails(Tx tx) {
        if(account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix() && tx.getAmount() == 0){
            return;
        }
        Intent txIntent = new Intent(this, TxDetailsActivity.class);
        txIntent.putExtra("TX", tx.toJSON().toString());
        startActivity(txIntent);
    }

    private class RicochetQueueTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            if (RicochetMeta.getInstance(BalanceActivity.this).getQueue().size() > 0) {

                int count = 0;

                final Iterator<JSONObject> itr = RicochetMeta.getInstance(BalanceActivity.this).getIterator();

                while (itr.hasNext()) {

                    if (count == 3) {
                        break;
                    }

                    try {
                        JSONObject jObj = itr.next();
                        JSONArray jHops = jObj.getJSONArray("hops");
                        if (jHops.length() > 0) {

                            JSONObject jHop = jHops.getJSONObject(jHops.length() - 1);
                            String txHash = jHop.getString("hash");

                            JSONObject txObj = APIFactory.getInstance(BalanceActivity.this).getTxInfo(txHash);
                            if (txObj != null && txObj.has("block_height") && txObj.getInt("block_height") != -1) {
                                itr.remove();
                                count++;
                            }

                        }
                    } catch (JSONException je) {
                        ;
                    }
                }

            }

            if (RicochetMeta.getInstance(BalanceActivity.this).getStaggered().size() > 0) {

                int count = 0;

                List<JSONObject> staggered = RicochetMeta.getInstance(BalanceActivity.this).getStaggered();
                List<JSONObject> _staggered = new ArrayList<JSONObject>();

                for (JSONObject jObj : staggered) {

                    if (count == 3) {
                        break;
                    }

                    try {
                        JSONArray jHops = jObj.getJSONArray("script");
                        if (jHops.length() > 0) {

                            JSONObject jHop = jHops.getJSONObject(jHops.length() - 1);
                            String txHash = jHop.getString("tx");

                            JSONObject txObj = APIFactory.getInstance(BalanceActivity.this).getTxInfo(txHash);
                            if (txObj != null && txObj.has("block_height") && txObj.getInt("block_height") != -1) {
                                count++;
                            } else {
                                _staggered.add(jObj);
                            }

                        }
                    } catch (JSONException je) {
                        ;
                    } catch (ConcurrentModificationException cme) {
                        ;
                    }
                }

            }

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            ;
        }

        @Override
        protected void onPreExecute() {
            ;
        }

    }

    private void doFeaturePayNymUpdate() {

        Disposable disposable = Observable.fromCallable(() -> {
            JSONObject obj = new JSONObject();
            obj.put("code", BIP47Util.getInstance(BalanceActivity.this).getPaymentCode().toString());
//                    Log.d("BalanceActivity", obj.toString());
            String res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(BalanceActivity.this).postURL("application/json", null, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + "api/v1/token", obj.toString());
//                    Log.d("BalanceActivity", res);

            JSONObject responseObj = new JSONObject(res);
            if (responseObj.has("token")) {
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
                if (responseObj.has("segwit") && responseObj.has("token")) {
                    PrefsUtil.getInstance(BalanceActivity.this).setValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, true);
                } else if (responseObj.has("claimed") && responseObj.getBoolean("claimed") == true) {
                    PrefsUtil.getInstance(BalanceActivity.this).setValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, true);
                }

            }
            return true;
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    Log.i(TAG, "doFeaturePayNymUpdate: Feature update complete");
                }, error -> {
                    Log.i(TAG, "doFeaturePayNymUpdate: Feature update Fail");
                });
        compositeDisposable.add(disposable);


    }


}
