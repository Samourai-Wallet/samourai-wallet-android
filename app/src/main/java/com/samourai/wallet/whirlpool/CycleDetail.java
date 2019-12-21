package com.samourai.wallet.whirlpool;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class CycleDetail extends AppCompatActivity {

    private Boolean showMenuItems = true;
    private TextView cycleStatus, transactionStatus, transactionId;
    private RecyclerView cycledTxsRecyclerView;
    private TxCyclesAdapter txCyclesAdapter;
    private ArrayList<UTXOCoin> mixedUTXOs = new ArrayList<>();
    private ProgressBar cycleProgress;
    private TextView registeringInputs, cyclingTx, waitingForConfirmation, cycledTxesListHeader, cycleTotalFee;
    private ImageView registeringCheck, cyclingCheck, confirmCheck;
    private List<WhirlpoolUtxo> whirlpoolUtxos = new ArrayList<>();
    private List<WhirlpoolUtxo> whirlpoolUtxosMixDone = new ArrayList<>();
    private static final String TAG = "CycleDetail";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String hash;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle);
        setSupportActionBar(findViewById(R.id.toolbar));

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        transactionId = findViewById(R.id.whirlpool_header_tx_hash);
        cycleProgress = findViewById(R.id.pool_cycle_progress);
        cycledTxsRecyclerView = findViewById(R.id.whirlpool_cycled_tx_rv);
        cycledTxsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        txCyclesAdapter = new TxCyclesAdapter();
        cycledTxsRecyclerView.setAdapter(txCyclesAdapter);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        cycledTxsRecyclerView.addItemDecoration(new ItemDividerDecorator(drawable));
        cyclingTx = findViewById(R.id.cycling_tx_txt);
        registeringInputs = findViewById(R.id.registering_inputs_txt);
        waitingForConfirmation = findViewById(R.id.cyle_waiting_txt);
        cycledTxesListHeader = findViewById(R.id.cycled_transaction_list_header);
        registeringCheck = findViewById(R.id.register_input_check);
        cyclingCheck = findViewById(R.id.cycling_check);
        confirmCheck = findViewById(R.id.blockchain_confirm_check);
        cycleProgress.setMax(100);
        cycleProgress.setProgress(0);

        hash = getIntent().getExtras().getString("hash");
        if (hash == null) {
            finish();
            return;
        }
        transactionId.setText(hash);

        listenUTXO();
        setMixStatus();
    }

    private void setMixStatus() {
        WhirlpoolWallet wallet = AndroidWhirlpoolWalletService.getInstance().getWallet();
        try {
            for (WhirlpoolUtxo utxo : wallet.getUtxosPremix()) {
                if (utxo.getUtxo().tx_hash.equals(hash)) {
                    whirlpoolUtxos.add(utxo);
                    getSupportActionBar().setTitle(utxo.getUtxoConfig().getPoolId());

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            int cycled = 0;
            for (WhirlpoolUtxo utxo : whirlpoolUtxos) {
                if ( utxo.getUtxoState().getMixProgress() != null && utxo.getUtxoState().getMixProgress().getMixStep() == MixStep.SUCCESS) {
                    cycled = cycled + 1;
                    whirlpoolUtxosMixDone.add(utxo);
                    txCyclesAdapter.notifyDataSetChanged();
                }
            }
            cycledTxesListHeader.setText("Cycled (".concat(String.valueOf(cycled).concat("/")).concat(String.valueOf(whirlpoolUtxos.size())).concat(" )"));
        } catch (Exception e) {
//            cycledTxesListHeader.setText("");
            e.printStackTrace();
        }
    }

    public WhirlpoolUtxo getCurrentRunningMix() {
        for (WhirlpoolUtxo utxo : whirlpoolUtxos) {
            if (utxo.getUtxoState() != null && utxo.getUtxoState().getMixableStatus() != null) {
                return utxo;
            }
        }
        return null;
    }


    private void listenUTXO() {
        if (getCurrentRunningMix() == null) {
            return;
        }
        updateState(getCurrentRunningMix().getUtxoState());
        Disposable disposable = getCurrentRunningMix().getUtxoState()
                .getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateState, er -> {
                    Log.e(TAG, "listenUTXO: ".concat(er.getMessage()));
                    er.printStackTrace();
                });
        compositeDisposable.add(disposable);
    }

    private void updateState(WhirlpoolUtxoState whirlpoolUtxoState) {
        cycleProgress.setProgress(whirlpoolUtxoState.getMixProgress().getProgressPercent());
        MixStep step = whirlpoolUtxoState.getMixProgress().getMixStep();
        if (step == MixStep.CONFIRMED_INPUT || whirlpoolUtxoState.getStatus() == WhirlpoolUtxoStatus.MIX_QUEUE) {
            enableCheck(confirmCheck);
        }
        if (step == MixStep.REGISTERED_INPUT || step == MixStep.CONFIRMED_INPUT) {
            enableCheck(confirmCheck);
            enableCheck(registeringCheck);
        }
        if (step == MixStep.SUCCESS) {
            enableCheck(confirmCheck);
            enableCheck(registeringCheck);
            enableCheck(cyclingCheck);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whirlpool_cycle_detail_menu, menu);
        menu.findItem(R.id.whirlpool_explore_menu).setVisible(showMenuItems);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.whirlpool_explore_menu: {
                openExplorer(hash);
                break;
            }
            case android.R.id.home: {
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void openExplorer(String hash) {

        String blockExplorer = "https://m.oxt.me/transaction/";
        if (SamouraiWallet.getInstance().isTestNet()) {
            blockExplorer = "https://blockstream.info/testnet/";
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + hash));
        startActivity(browserIntent);

    }

    private void enableCheck(ImageView imageView) {
        imageView.setAlpha(1f);
        imageView.setImageResource(R.drawable.ic_check_circle_24dp);
        imageView.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green_ui_2));
    }

    public class TxCyclesAdapter extends RecyclerView.Adapter<TxCyclesAdapter.ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cycle_tx, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            WhirlpoolUtxo utxo = whirlpoolUtxos.get(position);
            holder.amount.setText(Coin.valueOf(utxo.getUtxo().value).toPlainString());
            holder.utxoHash.setText(utxo.getUtxo().tx_hash);
            holder.layout.setOnClickListener(view -> {
                openExplorer(utxo.getUtxo().tx_hash);
            });
        }

        @Override
        public int getItemCount() {
            return whirlpoolUtxosMixDone.size();
        }


        class ViewHolder extends RecyclerView.ViewHolder {

            TextView utxoHash, amount;
            View layout;


            ViewHolder(View itemView) {
                super(itemView);
                amount = itemView.findViewById(R.id.whirlpool_cycle_list_item_utxo_amount);
                utxoHash = itemView.findViewById(R.id.whirlpool_cycle_list_item_utxo_hash);
                layout = itemView;
            }
        }


    }

}
