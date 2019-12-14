package com.samourai.wallet.whirlpool;

import android.graphics.drawable.Drawable;
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
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoState;

import java.util.ArrayList;

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
    private TextView registeringInputs, cyclingTx, cycledTxesListHeader;
    private ImageView registeringCheck, cyclingCheck, confirmCheck;
    private WhirlpoolUtxo whirlpoolUtxo;
    private static final String TAG = "CycleDetail";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

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
        cycledTxesListHeader = findViewById(R.id.cycled_transaction_list_header);
        registeringCheck = findViewById(R.id.register_input_check);
        cyclingCheck = findViewById(R.id.cycling_check);
        confirmCheck = findViewById(R.id.blockchain_confirm_check);

        disableProgressSection(registeringInputs, registeringCheck);
        disableProgressSection(cyclingTx, cyclingCheck);

        cycleProgress.setMax(100);
        cycleProgress.setProgress(0);



        String hash = getIntent().getExtras().getString("hash");
        if (hash == null) {
            finish();
            return;
        }
        transactionId.setText(hash);

        WhirlpoolWallet wallet = AndroidWhirlpoolWalletService.getInstance().getWallet();
        MixingState mixingState = wallet.getMixingState();

        for (WhirlpoolUtxo utxo : mixingState.getUtxosMixing()) {
            if (utxo.getUtxo().tx_hash.equals(hash)) {
                whirlpoolUtxo = utxo;
                getSupportActionBar().setTitle(whirlpoolUtxo.getUtxoConfig().getPoolId());
                listenUTXO();
            }
        }
    }

    private void listenUTXO() {
        updateState(whirlpoolUtxo.getUtxoState());
        Disposable disposable = whirlpoolUtxo.getUtxoState()
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

        if (whirlpoolUtxoState.getMixProgress().getProgressPercent() >= 10) {
            enableCheck(confirmCheck);
        }
        if (whirlpoolUtxoState.getMixProgress().getProgressPercent() <= 30) {
            enableCheck(confirmCheck);
            enableCheck(registeringCheck);
        }
        if (whirlpoolUtxoState.getMixProgress().getProgressPercent() >= 50) {
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
                Toast.makeText(getApplicationContext(), "Open in browser", Toast.LENGTH_SHORT).show();
                break;
            }
            case android.R.id.home: {
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableCheck(ImageView imageView) {
        imageView.setAlpha(1f);
        imageView.setImageResource(R.drawable.ic_check_circle_24dp);
        imageView.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green_ui_2));
    }

    private void disableProgressSection(TextView textView, ImageView imageView) {
        textView.setAlpha(0.6f);
        imageView.setAlpha(0.6f);
        imageView.setImageResource(R.drawable.circle_dot_white);
        imageView.clearColorFilter();
    }

    private void enableSection(TextView textView, ImageView imageView) {
        textView.setAlpha(1f);
        imageView.setAlpha(1f);
        imageView.setImageResource(R.drawable.circle_dot_white);
        imageView.clearColorFilter();
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
            //TODO - Bind views
        }

        @Override
        public int getItemCount() {

            return mixedUTXOs.size();
        }


        class ViewHolder extends RecyclerView.ViewHolder {

            TextView utxoHash, amount;


            ViewHolder(View itemView) {
                super(itemView);
                amount = itemView.findViewById(R.id.whirlpool_cycle_list_item_utxo_amount);
                utxoHash = itemView.findViewById(R.id.whirlpool_cycle_list_item_utxo_hash);
            }
        }


    }

}
