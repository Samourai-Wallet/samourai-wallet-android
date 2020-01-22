package com.samourai.wallet.whirlpool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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

import com.samourai.wallet.R;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.service.JobRefreshService;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.utxos.PreSelectUtil;
import com.samourai.wallet.utxos.UTXOSActivity;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.whirlpool.fragments.WhirlPoolLoaderDialog;
import com.samourai.wallet.whirlpool.models.WhirlpoolUtxoViewModel;
import com.samourai.wallet.whirlpool.newPool.DepositOrChooseUtxoDialog;
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity;
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;

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
        findViewById(R.id.spend_from_postmix).setOnClickListener(view -> {
            Intent intent = new Intent(WhirlpoolMain.this, SendActivity.class);
            intent.putExtra("_account", WhirlpoolMeta.getInstance(WhirlpoolMain.this).getWhirlpoolPostmix());
            startActivity(intent);
        });

        premixList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MixAdapter(new ArrayList<>());

        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        premixList.addItemDecoration(new ItemDividerDecorator(drawable));
        premixList.setAdapter(adapter);

        long postMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPostMixBalance();
        long preMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPreMixBalance();

        whirlpoolBalance.setText(Coin.valueOf(postMixBalance + preMixBalance).toPlainString().concat(" BTC"));
        startWhirlpool();

        Disposable disposable = AndroidWhirlpoolWalletService.getInstance().listenConnectionStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectionStates -> {
                    if (connectionStates == AndroidWhirlpoolWalletService.ConnectionStates.CONNECTED) {
                        listenPoolState();
                        loadPremixes(false);
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
            loadPremixes(true);
        });

    }

    private void loadPremixes(boolean loadSilently) {
        Optional<WhirlpoolWallet> whirlpoolWalletOpt = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWallet();
        if (!whirlpoolWalletOpt.isPresent()) {
            return;
        }

        if (loadSilently)
            progressBar.setVisibility(View.VISIBLE);
        WhirlpoolWallet wallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWallet().get();
        try {

            Disposable disposable = filter(wallet.getUtxosPremix(), new ArrayList<>(wallet.getMixingState().getUtxosMixing()))
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

    private Observable<List<WhirlpoolUtxoViewModel>> filter(Collection<WhirlpoolUtxo> premix, List<WhirlpoolUtxo> mixingUtxos) {
        return Observable.fromCallable(() -> {
            List<WhirlpoolUtxoViewModel> list = new ArrayList<>();

            List<WhirlpoolUtxo> utxoList = new ArrayList<>(premix);

            WhirlpoolUtxoViewModel section = WhirlpoolUtxoViewModel.section("Mixing");
            list.add(section);
            for (WhirlpoolUtxo utxo : utxoList) {
                WhirlpoolUtxoViewModel whirlpoolUtxoViewModel = null;
                for (WhirlpoolUtxo mixUtxo : mixingUtxos) {
                    if (mixUtxo.getUtxo().tx_hash.equals(utxo.getUtxo().tx_hash) && mixUtxo.getUtxo().tx_output_n == utxo.getUtxo().tx_output_n) {
                        whirlpoolUtxoViewModel = WhirlpoolUtxoViewModel.copy(mixingUtxos.get(mixingUtxos.indexOf(utxo)));
                    }
                }
                if (whirlpoolUtxoViewModel == null) {
                    whirlpoolUtxoViewModel = WhirlpoolUtxoViewModel.copy(utxo);
                }

                if (utxo.getUtxoState() != null) {
                    if (utxo.getUtxoState().getStatus() != WhirlpoolUtxoStatus.MIX_QUEUE &&
                            utxo.getUtxoState().getStatus() != WhirlpoolUtxoStatus.TX0_FAILED) {

                        list.add(whirlpoolUtxoViewModel);
                    }
                }
            }
            if (list.size() == 1) {
                list.remove(section);
            }
            WhirlpoolUtxoViewModel queueSection = WhirlpoolUtxoViewModel.section("UnMixed");
            list.add(queueSection);

            for (WhirlpoolUtxo utxo : utxoList) {
                WhirlpoolUtxoViewModel whirlpoolUtxoViewModel = null;
                for (WhirlpoolUtxo mixUtxo : mixingUtxos) {
                    if (mixUtxo.getUtxo().tx_hash.equals(utxo.getUtxo().tx_hash) && mixUtxo.getUtxo().tx_output_n == utxo.getUtxo().tx_output_n)
                        whirlpoolUtxoViewModel = WhirlpoolUtxoViewModel.copy(mixingUtxos.get(mixingUtxos.indexOf(utxo)));
                }
                if (whirlpoolUtxoViewModel == null) {
                    whirlpoolUtxoViewModel = WhirlpoolUtxoViewModel.copy(utxo);
                }
                if (whirlpoolUtxoViewModel.getUtxoState().getStatus() == WhirlpoolUtxoStatus.MIX_QUEUE) {
                    list.add(whirlpoolUtxoViewModel);
                }

            }
            if (list.get(list.size() - 1).isSection()) {
                list.remove(queueSection);
            }
            for (WhirlpoolUtxoViewModel utxo : list) {
                list.get(list.indexOf(utxo)).setId(list.indexOf(utxo));

            }
            listenPoolState(utxoList);
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
                    loadPremixes(true);
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
                WhirlpoolTx0 tx0 = new WhirlpoolTx0(1000000L, mediumFee, 0, coins);
                try {
                    tx0.make();
                } catch (Exception ex) {
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Tx0 is not possible with selected utxo.")
                            .setCancelable(true);
                    builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                    return;
                }
                if (tx0.getTx0() == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        DepositOrChooseUtxoDialog depositOrChooseUtxoDialog = new DepositOrChooseUtxoDialog();
        depositOrChooseUtxoDialog.show(getSupportFragmentManager(), depositOrChooseUtxoDialog.getTag());
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
                whirlpoolBalance.setText(Coin.valueOf(postMixBalance + preMixBalance).toPlainString().concat(" BTC"));
                break;
            }
            case 1: {
                amountSubText.setText(R.string.total_pre_mix_balance);
                whirlpoolBalance.setText(Coin.valueOf(preMixBalance).toPlainString().concat(" BTC"));
                break;
            }
            case 2: {
                amountSubText.setText(R.string.total_post_mix_balance);
                whirlpoolBalance.setText(Coin.valueOf(postMixBalance).toPlainString().concat(" BTC"));
                break;
            }
        }

    }


    private void listenPoolState() throws Exception {
        Optional<WhirlpoolWallet> whirlpoolWalletOpt = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWallet();
        if (!whirlpoolWalletOpt.isPresent()) {
            return;
        }
        WhirlpoolWallet whirlpoolWallet = whirlpoolWalletOpt.get();
        Disposable mixStateDisposable = whirlpoolWallet.getMixingState().getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mixingState -> {
                    loadPremixes(true);
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

        if (requestCode == NEWPOOL_REQ_CODE) {
            Optional<WhirlpoolWallet> whirlpoolWalletOpt = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWallet();
            if (!whirlpoolWalletOpt.isPresent()) {
                return;
            }
            WhirlpoolWallet whirlpoolWallet = whirlpoolWalletOpt.get();
            try {
                whirlpoolWallet.getUtxosPremix(true);
                whirlpoolWallet.getUtxosPostmix(true);
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
                loadPremixes(false);
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
        } else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doSCODE() {

        final EditText scode = new EditText(WhirlpoolMain.this);

        String strCurrentCode = WhirlpoolMeta.getInstance(WhirlpoolMain.this).getSCODE();
        if (strCurrentCode != null && strCurrentCode.length() > 0) {
            scode.setText(strCurrentCode);
        }

        new AlertDialog.Builder(WhirlpoolMain.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.enter_scode)
                .setView(scode)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String strSCODE = scode.getText().toString().trim();
                        if (scode != null) {
                            WhirlpoolMeta.getInstance(WhirlpoolMain.this).setSCODE(strSCODE);
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        }).show();

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
            holder.mixingAmount.setText(Coin.valueOf(whirlpoolUtxoModel.getUtxo().value).toPlainString().concat(" BTC"));
            try {
                if (whirlpoolUtxoModel.getUtxoState() != null) {
                    String progress = "";
                    if (whirlpoolUtxoModel.getUtxoState().getMessage() != null) {
                        progress = whirlpoolUtxoModel.getUtxoState().getMessage();
                    }
                    try {
                        int mixTarget = whirlpoolUtxoModel.getUtxoConfig().getMixsTarget();
                        int mixDone = whirlpoolUtxoModel.getUtxoConfig().getMixsDone();
                        progress = progress.concat(" ").concat(String.valueOf(mixDone)).concat("/").concat(String.valueOf(mixTarget));
                    } catch (Exception ex) {
//                        ex.printStackTrace();
                    }
                    holder.mixingProgress.setText(progress);
                    try {
                        switch (whirlpoolUtxoModel.getUtxoState().getStatus()) {
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
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            holder.mixingTime.setText("Premix");
            holder.itemView.setOnClickListener(view -> {
                Intent intent = new Intent(getApplicationContext(), CycleDetail.class);
                intent.putExtra("hash", whirlpoolUtxoModel.getUtxo().toString());
                startActivity(intent);
            });
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
            TextView mixingProgress, mixingTime, mixingAmount, section;
            ImageView progressStatus;

            ViewHolder(View view, int type) {
                super(view);
                mView = view;
                if (type == UTXO) {

                    mixingAmount = view.findViewById(R.id.whirlpool_cycle_item_mixing_amount);
                    mixingProgress = view.findViewById(R.id.whirlpool_cycle_item_mixing_text);
                    progressStatus = view.findViewById(R.id.whirlpool_cycle_item_mixing_status_icon);
                    mixingTime = view.findViewById(R.id.whirlpool_cycle_item_time);
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
