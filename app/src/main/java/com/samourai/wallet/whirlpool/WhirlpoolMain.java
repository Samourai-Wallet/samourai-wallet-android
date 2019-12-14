package com.samourai.wallet.whirlpool;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.utxos.UTXOSActivity;
import com.samourai.wallet.whirlpool.fragments.WhirlPoolLoaderDialog;
import com.samourai.wallet.whirlpool.models.Cycle;
import com.samourai.wallet.whirlpool.newPool.DepositOrChooseUtxoDialog;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class WhirlpoolMain extends AppCompatActivity {

    private ArrayList<Cycle> cycles = new ArrayList<>();
    private static final String TAG = "WhirlpoolMain";
    private String tabTitle[] = {"Dashboard", "In Progress", "Completed"};
    private RecyclerView mixList;
    private TextView totalAmountToDisplay;
    private TextView amountSubText;
    private MixAdapter adapter;
    WhirlpoolWallet wallet;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whirlpool_main);
        Toolbar toolbar = findViewById(R.id.toolbar_whirlpool);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        totalAmountToDisplay = findViewById(R.id.whirlpool_total_amount_to_display);
        amountSubText = findViewById(R.id.toolbar_subtext);
        mixList = findViewById(R.id.rv_whirlpool_dashboard);
        findViewById(R.id.whirlpool_fab).setOnClickListener(view -> {
            showBottomSheetDialog();
        });
        mixList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MixAdapter(new ArrayList<>());

        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        mixList.addItemDecoration(new ItemDividerDecorator(drawable));
        mixList.setAdapter(adapter);

        long postMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPostMixBalance();
        long preMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPreMixBalance();

        totalAmountToDisplay.setText(Coin.valueOf(postMixBalance + preMixBalance).toPlainString().concat(" BTC"));
        startWhirlpool();

        Disposable disposable = AndroidWhirlpoolWalletService.getInstance().listenConnectionStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectionStates -> {
                    if (connectionStates == AndroidWhirlpoolWalletService.ConnectionStates.CONNECTED) {
                        listenPoolState();
                        new Handler().postDelayed(this::listenPoolState, 3000);
                    }

                });
        compositeDisposable.add(disposable);
    }

    private void startWhirlpool() {
        if (AndroidWhirlpoolWalletService.getInstance().listenConnectionStatus().getValue()
                == AndroidWhirlpoolWalletService.ConnectionStates.CONNECTED) {
            listenPoolState();
        } else {
            WhirlPoolLoaderDialog whirlPoolLoaderDialog = new WhirlPoolLoaderDialog();
            whirlPoolLoaderDialog.show(getSupportFragmentManager(), whirlPoolLoaderDialog.getTag());
        }

    }

    private void showBottomSheetDialog() {
        DepositOrChooseUtxoDialog depositOrChooseUtxoDialog = new DepositOrChooseUtxoDialog();
        depositOrChooseUtxoDialog.show(getSupportFragmentManager(), depositOrChooseUtxoDialog.getTag());
    }


    private void listenPoolState() {
        wallet = AndroidWhirlpoolWalletService.getInstance().getWallet();
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        MixingState mixingState = wallet.getMixingState();
        updateCycles(mixingState.getUtxosMixing());
        Disposable disposable = mixingState.getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    cycles.clear();
                    if (!state.getUtxosMixing().isEmpty()) {
                        updateCycles(state.getUtxosMixing());
                        adapter.notifyDataSetChanged();
                    }
                }, er -> {
                    er.printStackTrace();
                });
        compositeDisposable.add(disposable);

    }

    private void updateCycles(Collection<WhirlpoolUtxo> utxosMixing) {
        for (WhirlpoolUtxo whirlpoolUtxo : utxosMixing) {
            Cycle cycle = new Cycle();
            cycle.setAmount(whirlpoolUtxo.getUtxo().value);
            cycle.setProgress(whirlpoolUtxo.getUtxoState().getMixProgress().getProgressPercent());
            cycle.setMixStep(whirlpoolUtxo.getUtxoState().getMixProgress().getMixStep());
            cycle.setPoolId(whirlpoolUtxo.getUtxoConfig().getPoolId());
            cycle.setTxHash(whirlpoolUtxo.getUtxo().tx_hash);
            cycles.add(cycle);
            Disposable disposable = whirlpoolUtxo.getUtxoState().getObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(whirlpoolUtxoState -> {
                        Cycle _copy_cycle = cycles.get(cycles.indexOf(cycle));
                        _copy_cycle.setMixStep(whirlpoolUtxo.getUtxoState().getMixProgress().getMixStep());
                        adapter.notifyDataSetChanged();
                    });
            compositeDisposable.add(disposable);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whirlpool_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(WhirlpoolMain.this).checkTimeOut();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_postmix) {
            Intent intent = new Intent(WhirlpoolMain.this, SendActivity.class);
            intent.putExtra("_account", WhirlpoolMeta.getInstance(WhirlpoolMain.this).getWhirlpoolPostmix());
            startActivity(intent);
        } else if (id == R.id.action_utxo) {
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

            Cycle utxo = cycles.get(position);
            //TODO: bind views with WaaS mix states
            holder.mixingAmount.setText(utxo.getPoolId());
            Intent intent = new Intent(getApplicationContext(), CycleDetail.class);
            intent.putExtra("hash", utxo.getTxHash());
            holder.mixingProgress.setText(utxo.getMixStep().getMessage());
            holder.itemView.setOnClickListener(view -> startActivity(intent));

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
