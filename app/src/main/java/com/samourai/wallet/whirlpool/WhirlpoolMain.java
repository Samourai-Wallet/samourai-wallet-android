package com.samourai.wallet.whirlpool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.service.JobRefreshService;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.utxos.PreSelectUtil;
import com.samourai.wallet.utxos.UTXOSActivity;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.whirlpool.fragments.WhirlPoolLoaderDialog;
import com.samourai.wallet.whirlpool.models.Cycle;
import com.samourai.wallet.whirlpool.newPool.DepositOrChooseUtxoDialog;
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity;
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import org.bitcoinj.core.Coin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class WhirlpoolMain extends AppCompatActivity {

    public static int NEWPOOL_REQ_CODE = 6102;
    private ArrayList<Cycle> cycles = new ArrayList<>();
    private static final String TAG = "WhirlpoolMain";
    private RecyclerView premixList;
    private TextView whirlpoolBalance;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private TextView amountSubText;
    private MixAdapter adapter;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private List<String> seenNonTx0Hashes = new ArrayList<>();
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
                        loadPremixes();
                    }

                });
        compositeDisposable.add(disposable);

        IntentFilter broadcastIntent = new IntentFilter(DISPLAY_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, broadcastIntent);
        collapsingToolbarLayout.setOnClickListener(view -> switchBalance());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            LogUtil.info(TAG, "onCreate: ".concat(String.valueOf(seenNonTx0Hashes.size())));
            Intent intent = new Intent(this, JobRefreshService.class);
            intent.putExtra("notifTx", false);
            intent.putExtra("dragged", true);
            intent.putExtra("launch", false);
            JobRefreshService.enqueueWork(getApplicationContext(), intent);
            swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.VISIBLE);
        });

    }

    private void loadPremixes() {
        progressBar.setVisibility(View.VISIBLE);
        WhirlpoolWallet wallet = AndroidWhirlpoolWalletService.getInstance().getWallet();
        HashMap<String, List<Tx>> premix_xpub = APIFactory.getInstance(getApplicationContext()).getPremixXpubTxs();
        List<Cycle> txes = new ArrayList<>();
        for (String key : premix_xpub.keySet()) {
            for (Tx tx : premix_xpub.get(key)) {
                //allow only received tx's
                if (tx.getAmount() > 0) {
                    Cycle cycle = new Cycle(tx.toJSON());
                    try {
                        for (WhirlpoolUtxo utxo : wallet.getUtxosPremix()) {
                            if (utxo.getUtxo().tx_hash.equals(tx.getHash())) {
                                if (utxo.getUtxoState() != null && utxo.getUtxoState().getMixableStatus() != null) {
                                    cycle.addWhirlpoolUTXO(utxo);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    txes.add(cycle);
                }
            }
        }
        Disposable disposable = filter(txes)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cycles1 -> {
                    cycles.clear();
                    cycles.addAll(cycles1);
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.INVISIBLE);
                    balanceIndex = balanceIndex-1;
                    switchBalance();
                }, er -> {
                    er.printStackTrace();
                    progressBar.setVisibility(View.INVISIBLE);
                });

        compositeDisposable.add(disposable);
    }

    private Observable<List<Cycle>> filter(List<Cycle> cycles) {
        return Observable.fromCallable(() -> {
            List<Cycle> txes = new ArrayList<>();
            for (Cycle cycle : cycles) {
                if (Tx0DisplayUtil.getInstance().contains(cycle.getHash())) {
                    txes.add(cycle);
                } else {
                    if (!seenNonTx0Hashes.contains(cycle.getHash())) {
                        JSONObject object = APIFactory.getInstance(getApplicationContext()).getTxInfo(cycle.getHash());
                        if (object.has("outputs")) {
                            JSONArray array = object.getJSONArray("outputs");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject outpoint = array.getJSONObject(i);
                                if (outpoint.has("type") && outpoint.getString("type").equals("nulldata")) {
                                    if (!Tx0DisplayUtil.getInstance().contains(cycle.getHash()))
                                        Tx0DisplayUtil.getInstance().add(cycle.getHash());
                                    txes.add(cycle);
                                }else {
                                    seenNonTx0Hashes.add(cycle.getHash());
                                }
                            }
                        }
                    }

                }

            }
            return txes;
        });

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
                WhirlpoolTx0 tx0 = new WhirlpoolTx0(1000000L, 10L, 0, coins);
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


    private void listenPoolState() {
        //TODO
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whirlpool_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == NEWPOOL_REQ_CODE) {
            Intent intent = new Intent(this, JobRefreshService.class);
            intent.putExtra("notifTx", false);
            intent.putExtra("dragged", true);
            intent.putExtra("launch", false);
            JobRefreshService.enqueueWork(getApplicationContext(), intent);
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
                loadPremixes();
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
        } else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MixAdapter extends RecyclerView.Adapter<MixAdapter.ViewHolder> {


        MixAdapter(List<Cycle> items) {

        }

        @Override
        public MixAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cycle_item, parent, false);
            return new MixAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MixAdapter.ViewHolder holder, int position) {

            Cycle cycleTX = cycles.get(position);
            holder.mixingAmount.setText(Coin.valueOf((long) cycleTX.getAmount()).toPlainString().concat(" BTC"));

            try {
                if (cycleTX.getCurrentRunningMix() != null){
                    holder.mixingProgress.setText(cycleTX.getCurrentRunningMix().getUtxoState().getMixProgress().getMixStep().getMessage());
                }
                else{
                    holder.mixingProgress.setText("Queue");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Date date = new Date();
            date.setTime(cycleTX.getTS() * 1000);
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            fmt.setTimeZone(TimeZone.getDefault());
            holder.mixingTime.setText(fmt.format(date));
            holder.itemView.setOnClickListener(view -> {
                Intent intent = new Intent(getApplicationContext(), CycleDetail.class);
                intent.putExtra("hash", cycleTX.getHash());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return cycles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            TextView mixingProgress, mixingTime, mixingAmount;
            ImageView progressStatus;

            ViewHolder(View view) {
                super(view);
                mView = view;
                mixingAmount = view.findViewById(R.id.whirlpool_cycle_item_mixing_amount);
                mixingProgress = view.findViewById(R.id.whirlpool_cycle_item_mixing_text);
                progressStatus = view.findViewById(R.id.whirlpool_cycle_item_mixing_status_icon);
                mixingTime = view.findViewById(R.id.whirlpool_cycle_item_time);
            }

        }
    }


}
