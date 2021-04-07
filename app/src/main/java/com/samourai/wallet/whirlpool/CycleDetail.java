package com.samourai.wallet.whirlpool;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixableStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;


public class CycleDetail extends AppCompatActivity {

    private Boolean showMenuItems = true;
    private TextView cycleStatus, transactionStatus, transactionId, minerFee;
    private RecyclerView cycledTxsRecyclerView;
    private ProgressBar cycleProgress;
    private TextView registeringInputs, cyclingTx, waitingForConfirmation, cycledTxesListHeader, cycleTotalFee;
    private ImageView registeringCheck, cyclingCheck, confirmCheck;
    private List<WhirlpoolUtxo> whirlpoolUtxos = new ArrayList<>();
    private WhirlpoolUtxo whirlpoolUtxo = null;
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
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        cycledTxsRecyclerView.addItemDecoration(new ItemDividerDecorator(drawable));
        cyclingTx = findViewById(R.id.cycling_tx_txt);
        registeringInputs = findViewById(R.id.registering_inputs_txt);
        waitingForConfirmation = findViewById(R.id.cyle_waiting_txt);
        cycledTxesListHeader = findViewById(R.id.cycled_transaction_list_header);
        registeringCheck = findViewById(R.id.register_input_check);
        cyclingCheck = findViewById(R.id.cycling_check);
        minerFee = findViewById(R.id.cycle_details_minerfee);
        confirmCheck = findViewById(R.id.blockchain_confirm_check);
        cycleProgress.setMax(100);
        cycleProgress.setProgress(0);

        hash = getIntent().getExtras().getString("hash");
        if (hash == null) {
            finish();
            return;
        }
        enableCycle(false);
        enableRegister(false);
        enableBlockChainConfirm(false);

        setMixStatus();
        listenUTXO();

    }

    private void setMixStatus() {
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
        if (whirlpoolWallet == null) {
            return;
        }
        try {
            for (WhirlpoolUtxo utxo : whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.PREMIX)) {
                if (utxo.getUtxo().toString().equals(hash)) {
                    getSupportActionBar().setTitle(utxo.getPoolId());
                    whirlpoolUtxo = utxo;
                    makeTxNetworkRequest();
                    transactionId.setText(utxo.getUtxo().tx_hash);
                    updateState(whirlpoolUtxo.getUtxoState());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void makeTxNetworkRequest() {
        Disposable disposable = Single.create(emitter -> emitter.onSuccess(APIFactory.getInstance(getApplicationContext()).getTxInfo(whirlpoolUtxo.getUtxo().tx_hash)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((jsonObject, throwable) -> {
                    JSONObject object = (JSONObject) jsonObject;
                    if (throwable == null) {
                        if (object.has("fees")) {
                            minerFee.setText(object.getString("fees").concat(" sats"));
                        }
                    } else {
                        throwable.printStackTrace();
                    }
                });
        compositeDisposable.add(disposable);
    }


    private void listenUTXO() {
        if (whirlpoolUtxo == null || whirlpoolUtxo.getUtxoState() == null) {
            return;
        }
        updateState(whirlpoolUtxo.getUtxoState());
        Disposable disposable = whirlpoolUtxo.getUtxoState()
                .getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateState, er -> {
                    er.printStackTrace();
                });
        compositeDisposable.add(disposable);
    }

    private void updateState(WhirlpoolUtxoState whirlpoolUtxoState) {
        try {
            if (whirlpoolUtxo.getUtxoState().getMixableStatus() != MixableStatus.UNCONFIRMED) {
                enableBlockChainConfirm(true);
                ((TextView) findViewById(R.id.cyle_waiting_txt)).setText("Tx confirmed");
            }
            if (whirlpoolUtxoState.getMixProgress() != null) {
                MixStep step = whirlpoolUtxoState.getMixProgress().getMixStep();

                cycleProgress.setProgress(whirlpoolUtxoState.getMixProgress().getProgressPercent());

                if (step == MixStep.CONFIRMED_INPUT || whirlpoolUtxoState.getStatus() == WhirlpoolUtxoStatus.MIX_QUEUE) {
                    enableBlockChainConfirm(true);
                }
                if (step == MixStep.REGISTERED_INPUT || step == MixStep.CONFIRMED_INPUT || step == MixStep.CONFIRMING_INPUT) {
                    enableBlockChainConfirm(true);
                    enableRegister(true);
                }
                if (step == MixStep.SUCCESS) {
                    enableBlockChainConfirm(true);
                    enableRegister(true);
                    enableCycle(true);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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
                if (whirlpoolUtxo != null)
                    openExplorer(whirlpoolUtxo.getUtxo().tx_hash);
                break;
            }
            case android.R.id.home: {
                finish();
            }
            case R.id.refresh_cycle_details: {
                setMixStatus();
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

    private void enableBlockChainConfirm(boolean enable) {
        if (enable) {
            enableSection(findViewById(R.id.cyle_waiting_txt), confirmCheck);
        } else {
            disableSection(findViewById(R.id.cyle_waiting_txt), confirmCheck);
        }

    }

    private void enableRegister(boolean enable) {
        if (enable) {
            enableSection(findViewById(R.id.registering_inputs_txt), registeringCheck);
        } else {
            disableSection(findViewById(R.id.registering_inputs_txt), registeringCheck);
        }

    }

    private void enableCycle(boolean enable) {
        if (enable) {
            enableSection(findViewById(R.id.cycling_tx_txt), cyclingCheck);
        } else {
            disableSection(findViewById(R.id.cycling_tx_txt), cyclingCheck);
        }

    }

    private void enableSection(TextView textView, ImageView imageView) {
        textView.setAlpha(1f);
        imageView.setAlpha(1f);
        imageView.setImageResource(R.drawable.ic_check_circle_24dp);
        imageView.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green_ui_2));
    }

    private void disableSection(TextView textView, ImageView imageView) {
        textView.setAlpha(0.6f);
        imageView.setAlpha(0.6f);
        imageView.setImageResource(R.drawable.circle_dot_white);
        imageView.clearColorFilter();
    }


}
