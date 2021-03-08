package com.samourai.wallet.whirlpool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.psbt.PSBTUtil;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.network.NetworkDashboard;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.cahoots.ManualCahootsActivity;
import com.samourai.wallet.service.JobRefreshService;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.LinearLayoutManagerWrapper;
import com.samourai.wallet.utxos.PreSelectUtil;
import com.samourai.wallet.utxos.UTXOSActivity;
import com.samourai.wallet.utxos.UTXOUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.whirlpool.fragments.WhirlPoolLoaderDialog;
import com.samourai.wallet.whirlpool.models.WhirlpoolUtxoViewModel;
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity;
import com.samourai.wallet.whirlpool.newPool.WhirlpoolDialog;
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.util.FormatsUtil.getBTCDecimalFormat;

public class WhirlpoolMain extends AppCompatActivity {

    public static int NEWPOOL_REQ_CODE = 6102;
    private List<WhirlpoolUtxoViewModel> whirlpoolUtxoViewModels = new ArrayList<>();
    private static final String TAG = "WhirlpoolMain";
    private RecyclerView premixList;
    private TextView whirlpoolBalance;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private TextView amountSubText;
    private MixAdapter adapter;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ProgressBar progressBar;
    private int balanceIndex = 0;
    private SwipeRefreshLayout swipeRefreshLayout;
    public static final String DISPLAY_INTENT = "com.samourai.wallet.BalanceFragment.DISPLAY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whirlpool_main);
        Toolbar toolbar = findViewById(R.id.toolbar_whirlpool);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        whirlpoolBalance = findViewById(R.id.whirlpool_balance_textview);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_whirlpool_main);
        amountSubText = findViewById(R.id.toolbar_subtext);
        progressBar = findViewById(R.id.whirlpool_main_progressBar);
        premixList = findViewById(R.id.rv_whirlpool_dashboard);
        swipeRefreshLayout = findViewById(R.id.tx_swipe_container_whirlpool);

        findViewById(R.id.whirlpool_fab).setOnClickListener(view -> showBottomSheetDialog());


        premixList.setLayoutManager(new LinearLayoutManagerWrapper(this));

        adapter = new MixAdapter(new ArrayList<>());

        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        premixList.addItemDecoration(new ItemDividerDecorator(drawable));
        premixList.setAdapter(adapter);

        long postMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPostMixBalance();
        long preMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPreMixBalance();

        whirlpoolBalance.setText(getBTCDecimalFormat(postMixBalance + preMixBalance).concat(" BTC"));
        startWhirlpool();

        Disposable disposable = AndroidWhirlpoolWalletService.getInstance().listenConnectionStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectionStates -> {
                    if (connectionStates == AndroidWhirlpoolWalletService.ConnectionStates.CONNECTED) {
                        listenPoolState();
                        loadMixes(false);
                    }
                });
        compositeDisposable.add(disposable);


        IntentFilter broadcastIntent = new IntentFilter(DISPLAY_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, broadcastIntent);
        collapsingToolbarLayout.setOnClickListener(view -> switchBalance());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Intent intent = new Intent(this, JobRefreshService.class);
            intent.putExtra("notifTx", false);
            intent.putExtra("dragged", true);
            intent.putExtra("launch", false);
            JobRefreshService.enqueueWork(getApplicationContext(), intent);
            swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.VISIBLE);
            loadMixes(true);
        });

    }

    private void loadMixes(boolean loadSilently) {
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
        if (whirlpoolWallet == null) {
            return;
        }

        if (loadSilently)
            progressBar.setVisibility(View.VISIBLE);
        try {

            Disposable disposable = filter(whirlpoolWallet, new ArrayList<>(whirlpoolWallet.getMixingState().getUtxosMixing()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(whirlpoolUtxoViewModelList -> {
                        adapter.updateList(whirlpoolUtxoViewModelList);
                        progressBar.setVisibility(View.INVISIBLE);
                        balanceIndex = balanceIndex - 1;
                        switchBalance();
                    }, er -> {
                        er.printStackTrace();
                        progressBar.setVisibility(View.INVISIBLE);
                    });

            compositeDisposable.add(disposable);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Observable<List<WhirlpoolUtxoViewModel>> filter(WhirlpoolWallet wallet, List<WhirlpoolUtxo> mixingUtxos) {
        return Observable.fromCallable(() -> {
            List<WhirlpoolUtxoViewModel> list = new ArrayList<>();

            List<WhirlpoolUtxo> utxoPremix = new ArrayList<>(wallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.PREMIX));
            WhirlpoolUtxoViewModel section = WhirlpoolUtxoViewModel.section("Mixing");
            list.add(section);


            for (WhirlpoolUtxo mixUtxo : mixingUtxos) {
                WhirlpoolUtxoViewModel whirlpoolUtxoViewModel = WhirlpoolUtxoViewModel.copy(mixingUtxos.get(mixingUtxos.indexOf(mixUtxo)));
                list.add(whirlpoolUtxoViewModel);
            }

            if (list.size() == 1) {
                list.remove(section);
            }

            WhirlpoolUtxoViewModel queueSection = WhirlpoolUtxoViewModel.section("Unmixed");
            list.add(queueSection);

            for (WhirlpoolUtxo utxo : utxoPremix) {
                WhirlpoolUtxoViewModel whirlpoolUtxoViewModel = null;
                for (WhirlpoolUtxo mixUtxo : mixingUtxos) {
                    if (mixUtxo.getUtxo().tx_hash.equals(utxo.getUtxo().tx_hash) && mixUtxo.getUtxo().tx_output_n == utxo.getUtxo().tx_output_n)
                        whirlpoolUtxoViewModel = WhirlpoolUtxoViewModel.copy(mixingUtxos.get(mixingUtxos.indexOf(utxo)));
                }
                if (whirlpoolUtxoViewModel == null) {
                    whirlpoolUtxoViewModel = WhirlpoolUtxoViewModel.copy(utxo);
                }
                if (whirlpoolUtxoViewModel.getWhirlpoolUtxo().getUtxoState().getStatus() == WhirlpoolUtxoStatus.MIX_QUEUE) {
                    list.add(whirlpoolUtxoViewModel);
                }

            }
            if (list.get(list.size() - 1).isSection()) {
                list.remove(queueSection);
            }
            for (WhirlpoolUtxoViewModel utxo : list) {
                list.get(list.indexOf(utxo)).setId(list.indexOf(utxo));

            }
            listenPoolState(mixingUtxos);
            return list;
        });
    }

    private void listenPoolState(List<WhirlpoolUtxo> list) {
        List<Observable<WhirlpoolUtxoState>> state = new ArrayList<>();
        for (WhirlpoolUtxo utxo : list) {
            state.add(utxo.getUtxoState().getObservable());
        }

        Disposable disposable = Observable.merge(state)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(whirlpoolUtxoState -> {
                    loadMixes(true);
                });
        compositeDisposable.add(disposable);
    }

    private void startWhirlpool() {
        if (AndroidWhirlpoolWalletService.getInstance().listenConnectionStatus().getValue()
                == AndroidWhirlpoolWalletService.ConnectionStates.CONNECTED && WhirlpoolNotificationService.isRunning(getApplicationContext())) {
            validateIntentAndStartNewPool();
        } else {
            WhirlPoolLoaderDialog whirlPoolLoaderDialog = new WhirlPoolLoaderDialog();
            //After successful whirlpool connection  @validateIntentAndStartNewPool method will invoked to validate preselected utxo's
            whirlPoolLoaderDialog.setOnInitComplete(this::validateIntentAndStartNewPool);
            whirlPoolLoaderDialog.show(getSupportFragmentManager(), whirlPoolLoaderDialog.getTag());
        }

    }

    private void validateIntentAndStartNewPool() {

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("preselected")) {
            Intent intent = new Intent(getApplicationContext(), NewPoolActivity.class);
            int account = getIntent().getExtras().getInt("_account");
            intent.putExtra("_account", getIntent().getExtras().getInt("_account"));
            intent.putExtra("preselected", getIntent().getExtras().getString("preselected"));
            if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix()) {
                List<UTXOCoin> coins = PreSelectUtil.getInstance().getPreSelected(getIntent().getExtras().getString("preselected"));
                long mediumFee = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
                WhirlpoolTx0 tx0 = new WhirlpoolTx0(WhirlpoolMeta.getInstance(WhirlpoolMain.this).getMinimumPoolDenomination(), mediumFee, 1, coins);
                try {
                    tx0.make();
                } catch (Exception ex) {
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                    builder.setMessage("Tx0 is not possible with selected utxo.")
                            .setCancelable(true);
                    builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                    return;
                }
                if (tx0.getTx0() == null) {
                    MaterialAlertDialogBuilder builder= new MaterialAlertDialogBuilder(this);
                    builder.setMessage("Tx0 is not possible with selected utxo.")
                            .setCancelable(true);
                    builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();

                } else {
                    startActivityForResult(intent, NEWPOOL_REQ_CODE);
                }
            } else {
                startActivityForResult(intent, NEWPOOL_REQ_CODE);

            }
        }

    }

    private void showBottomSheetDialog() {
        WhirlpoolDialog whirlpoolDialog = new WhirlpoolDialog();
        whirlpoolDialog.show(getSupportFragmentManager(), whirlpoolDialog.getTag());
    }

    private void switchBalance() {
        if (balanceIndex == 2) {
            balanceIndex = 0;
        } else {
            balanceIndex = balanceIndex + 1;
        }
        long postMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPostMixBalance();
        long preMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPreMixBalance();
        switch (balanceIndex) {
            case 0: {
                amountSubText.setText(R.string.total_whirlpool_balance);
                whirlpoolBalance.setText(getBTCDecimalFormat(postMixBalance + preMixBalance).concat(" BTC"));
                break;
            }
            case 1: {
                amountSubText.setText(R.string.total_pre_mix_balance);
                whirlpoolBalance.setText(getBTCDecimalFormat(preMixBalance).concat(" BTC"));
                break;
            }
            case 2: {
                amountSubText.setText(R.string.total_post_mix_balance);
                whirlpoolBalance.setText(getBTCDecimalFormat(postMixBalance).concat(" BTC"));
                break;
            }
        }

    }


    private void listenPoolState() throws Exception {
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
        if (whirlpoolWallet == null) {
            return;
        }
        Disposable mixStateDisposable = whirlpoolWallet.getMixingState().getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mixingState -> {
                    loadMixes(true);
                }, Throwable::printStackTrace);

        compositeDisposable.add(mixStateDisposable);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whirlpool_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEWPOOL_REQ_CODE && resultCode == Activity.RESULT_OK) {
            WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
            if (whirlpoolWallet == null) {
                return;
            }
            try {
                whirlpoolWallet.getUtxoSupplier().expire();
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, JobRefreshService.class);
                intent.putExtra("notifTx", false);
                intent.putExtra("dragged", true);
                intent.putExtra("launch", false);
                JobRefreshService.enqueueWork(getApplicationContext(), intent);
            }, 800);
            progressBar.setVisibility(View.VISIBLE);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(WhirlpoolMain.this).checkTimeOut();
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (DISPLAY_INTENT.equals(intent.getAction())) {
                loadMixes(false);
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == android.R.id.home)
            finish();
        if (id == R.id.action_utxo) {
            Intent intent = new Intent(WhirlpoolMain.this, UTXOSActivity.class);
            intent.putExtra("_account", WhirlpoolMeta.getInstance(WhirlpoolMain.this).getWhirlpoolPostmix());
            startActivity(intent);
        } else if (id == R.id.action_menu_view_post_mix) {
            Intent intent = new Intent(WhirlpoolMain.this, BalanceActivity.class);
            intent.putExtra("_account", WhirlpoolMeta.getInstance(WhirlpoolMain.this).getWhirlpoolPostmix());
            startActivity(intent);
        } else if (id == R.id.action_scode) {
            doSCODE();
        } else if (id == R.id.action_scan_qr) {
            CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
            cameraFragmentBottomSheet.show(getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());

            cameraFragmentBottomSheet.setQrCodeScanListener(code -> {
                cameraFragmentBottomSheet.dismissAllowingStateLoss();
                try {
                    if (Cahoots.isCahoots(code.trim())) {
                        Intent cahootIntent = ManualCahootsActivity.createIntentResume(this, WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix(), code.trim());
                        startActivity(cahootIntent);
                    } else if (FormatsUtil.getInstance().isPSBT(code.trim())) {
                        PSBTUtil.getInstance(getApplication()).doPSBT(code.trim());
                    } else if (DojoUtil.getInstance(getApplication()).isValidPairingPayload(code.trim())) {
                        Intent intent = new Intent(getApplication(), NetworkDashboard.class);
                        intent.putExtra("params", code.trim());
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplication(), SendActivity.class);
                        intent.putExtra("uri", code.trim());
                        intent.putExtra("_account", WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix());
                        startActivity(intent);
                    }
                } catch (Exception e) {
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveState(){
        new Thread(() -> {
            try {
                PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance(getApplicationContext()).getPIN()));
            } catch (Exception e) {
                ;
            }

        }).start();
    }

    private void doSCODE() {

        final EditText scode = new EditText(WhirlpoolMain.this);

        String strCurrentCode = WhirlpoolMeta.getInstance(WhirlpoolMain.this).getSCODE();
        if (strCurrentCode != null && strCurrentCode.length() > 0) {
            scode.setText(strCurrentCode);
        }

        new MaterialAlertDialogBuilder(WhirlpoolMain.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.enter_scode)
                .setView(scode)
                .setNeutralButton("Remove SCODE", (dialog, which) -> {
                    WhirlpoolMeta.getInstance(WhirlpoolMain.this).setSCODE("");
                    WhirlpoolNotificationService.stopService(getApplicationContext());
                    saveState();
                    new Handler().postDelayed(() -> {
                        Intent _intent = new Intent(WhirlpoolMain.this, BalanceActivity.class);
                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(_intent);
                    }, 1000L);
                })
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    String strSCODE = scode.getText().toString().trim();
                    if (scode != null) {
                        WhirlpoolMeta.getInstance(WhirlpoolMain.this).setSCODE(strSCODE);
                        WhirlpoolNotificationService.stopService(getApplicationContext());
                        saveState();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent _intent = new Intent(WhirlpoolMain.this, BalanceActivity.class);
                                _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(_intent);
                            }

                        }, 1000L);

                    }
                    dialog.dismiss();
                }).setNegativeButton(R.string.cancel, (dialog, whichButton) -> dialog.dismiss()).show();

    }

    private class MixAdapter extends RecyclerView.Adapter<MixAdapter.ViewHolder> {
        int SECTION = 0, UTXO = 1;

        MixAdapter(List<WhirlpoolUtxoViewModel> items) {
            this.setHasStableIds(true);
        }

        @Override
        public MixAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == UTXO) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cycle_item, parent, false);

                return new MixAdapter.ViewHolder(view, UTXO);

            } else {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.tx_item_section_layout, parent, false);
                return new ViewHolder(view, SECTION);
            }
        }

        @Override
        public void onBindViewHolder(final MixAdapter.ViewHolder holder, int position) {

            WhirlpoolUtxoViewModel whirlpoolUtxoModel = whirlpoolUtxoViewModels.get(position);

            if (whirlpoolUtxoModel.isSection()) {
                holder.section.setText(whirlpoolUtxoModel.getSection());
                return;
            }
            WhirlpoolUtxo whirlpoolUtxo = whirlpoolUtxoModel.getWhirlpoolUtxo();
            holder.mixingAmount.setText(getBTCDecimalFormat(whirlpoolUtxo.getUtxo().value).concat(" BTC"));
            try {
                if (whirlpoolUtxo.getUtxoState() != null) {
                    String progress = "";
                    if (whirlpoolUtxo.getUtxoState().getMessage() != null) {
                        progress = whirlpoolUtxo.getUtxoState().getMessage();
                    }
                    holder.mixingProgress.setText(progress);
                    if (UTXOUtil.getInstance().getNote(whirlpoolUtxo.getUtxo().tx_hash) != null) {
                        holder.txNoteGroup.setVisibility(View.VISIBLE);
                        holder.tvNoteView.setText(UTXOUtil.getInstance().getNote(whirlpoolUtxo.getUtxo().tx_hash));
                    } else {
                        holder.txNoteGroup.setVisibility(View.GONE);
                    }
                    try {
                        holder.mixingUtxoIndicator.setImageDrawable(getResources().getDrawable(R.drawable.incoming_tx_green));
                        holder.utxoType.setText("Premix");
                        switch (whirlpoolUtxo.getUtxoState().getStatus()) {
                            case READY: {
                                if (progress.length() == 0) {
                                    holder.mixingProgress.setText("Ready");
                                }
                                holder.progressStatus.setColorFilter(getResources().getColor(R.color.whirlpoolBlue));
                                break;
                            }
                            case STOP: {
                                if (progress.length() == 0) {
                                    holder.mixingProgress.setText("Stop");
                                }
                                holder.progressStatus.setColorFilter(getResources().getColor(R.color.disabled_white));
                                break;
                            }
                            case MIX_STARTED: {
                                if (progress.length() == 0) {
                                    holder.mixingProgress.setText("Joined a mix");
                                }
                                holder.mixingUtxoIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_repeat_24dp));
                                holder.utxoType.setText("Mixing");
                                break;
                            }
                            case MIX_QUEUE: {
                                if (progress.length() == 0) {
                                    holder.mixingProgress.setText("Queued");
                                }
                                holder.progressStatus.setColorFilter(getResources().getColor(R.color.warning_yellow));
                                break;
                            }
                            case MIX_SUCCESS: {
                                if (progress.length() == 0) {
                                    holder.mixingProgress.setText("Mix success");
                                }
                                holder.mixingUtxoIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_repeat_24dp));
                                holder.utxoType.setText("Postmix");
                                holder.progressStatus.setColorFilter(getResources().getColor(R.color.green_ui_2));
                                break;
                            }
                            case MIX_FAILED: {
                                if (progress.length() == 0) {
                                    holder.mixingProgress.setText("Mix failed");
                                }
                                holder.progressStatus.setColorFilter(getResources().getColor(R.color.red));
                                break;
                            }
                        }
                    } catch (Exception er) {
                        er.printStackTrace();
                    }

                } else {
                    holder.mixingProgress.setText("Queue");
                    holder.progressStatus.setColorFilter(getResources().getColor(R.color.disabled_white));
                }
                if (whirlpoolUtxo.getAccount() == WhirlpoolAccount.POSTMIX) {
                    holder.mixingProgress.setText(holder.mixingProgress.getText().toString());
                    holder.utxoType.setText("postmix");
                    holder.mixingUtxoIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_repeat_24dp));
                }

                // prefix with "Mix x/y - "
                try {
                    int currentMix = whirlpoolUtxo.getMixsDone() + 1;
                    String mixInfo = "Mix " + currentMix + " - ";
                    holder.mixingProgress.setText(mixInfo + holder.mixingProgress.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
//            holder.itemView.setOnClickListener(view -> {
//                Intent intent = new Intent(getApplicationContext(), CycleDetail.class);
//                intent.putExtra("hash", whirlpoolUtxoModel.getUtxo().toString());
//                startActivity(intent);
//            });
        }

        @Override
        public int getItemCount() {
            return whirlpoolUtxoViewModels.size();
        }


        @Override
        public long getItemId(int position) {
            return whirlpoolUtxoViewModels.get(position).getId();
        }

        @Override
        public int getItemViewType(int position) {
            if (whirlpoolUtxoViewModels.get(position).isSection()) {
                return SECTION;
            } else {
                return UTXO;
            }
        }

        public void updateList(List<WhirlpoolUtxoViewModel> newList) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new WhirlpoolUtxoDiff(whirlpoolUtxoViewModels, newList));
            whirlpoolUtxoViewModels = new ArrayList<>();
            whirlpoolUtxoViewModels.addAll(newList);
            diffResult.dispatchUpdatesTo(this);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            TextView mixingProgress, utxoType, mixingAmount, section, tvNoteView;
            ImageView progressStatus, mixingUtxoIndicator;
            Group txNoteGroup;

            ViewHolder(View view, int type) {
                super(view);
                mView = view;
                if (type == UTXO) {
                    mixingAmount = view.findViewById(R.id.whirlpool_cycle_item_mixing_amount);
                    mixingProgress = view.findViewById(R.id.whirlpool_cycle_item_mixing_text);
                    progressStatus = view.findViewById(R.id.whirlpool_cycle_item_mixing_status_icon);
                    utxoType = view.findViewById(R.id.whirlpool_cycle_item_time);
                    mixingUtxoIndicator = view.findViewById(R.id.utxo_mixing_indicator_icon);
                    tvNoteView = view.findViewById(R.id.tx_note_view);
                    txNoteGroup = itemView.findViewById(R.id.whirlpool_main_note_group);
                } else {
                    section = itemView.findViewById(R.id.section_title);
                }
            }

        }
    }

    public class WhirlpoolUtxoDiff extends DiffUtil.Callback {

        List<WhirlpoolUtxoViewModel> oldList;
        List<WhirlpoolUtxoViewModel> newList;

        WhirlpoolUtxoDiff(List<WhirlpoolUtxoViewModel> newPersons, List<WhirlpoolUtxoViewModel> oldPersons) {
            this.newList = newPersons;
            this.oldList = oldPersons;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == (newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}
