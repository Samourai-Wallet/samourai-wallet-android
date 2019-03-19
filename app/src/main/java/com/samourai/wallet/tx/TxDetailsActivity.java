package com.samourai.wallet.tx;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.send.boost.RBFTask;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.boost.CPFPTask;
import com.samourai.wallet.SendActivity;
import com.samourai.wallet.util.DateUtil;
import com.samourai.wallet.widgets.CircleImageView;
import com.squareup.picasso.Picasso;

import org.bitcoinj.core.Coin;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class TxDetailsActivity extends AppCompatActivity {

    private CircleImageView payNymAvatar;
    private TextView payNymUsername, btcUnit, amount, txStatus, txId, txDate, bottomButton, minerFee, minerFeeRate;
    private Tx tx;
    private static final String TAG = "TxDetailsActivity";
    private String BTCDisplayAmount, SatDisplayAmount, paynymDisplayName;
    private RBFTask rbfTask = null;
    private CPFPTask cpfpTask = null;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx);
        setSupportActionBar(findViewById(R.id.toolbar));

        if (getIntent().hasExtra("TX")) {
            try {
                JSONObject TxJsonObject = new JSONObject(getIntent().getStringExtra("TX"));
                tx = new Tx(TxJsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        payNymUsername = findViewById(R.id.tx_paynym_username);
        btcUnit = findViewById(R.id.tx_unit);
        amount = findViewById(R.id.tx_amount);
        payNymAvatar = findViewById(R.id.img_paynym_avatar);
        txId = findViewById(R.id.transaction_id);
        txStatus = findViewById(R.id.tx_status);
        txDate = findViewById(R.id.tx_date);
        bottomButton = findViewById(R.id.btn_bottom_button);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        progressBar = findViewById(R.id.progressBar);
        minerFee = findViewById(R.id.tx_miner_fee_paid);
        minerFeeRate = findViewById(R.id.tx_miner_fee_rate);
        amount.setOnClickListener(view -> toggleUnits());
        btcUnit.setOnClickListener(view -> toggleUnits());

        setTx();

        bottomButton.setOnClickListener(view -> {
            if (this.isBoostingAvailable()) {
                this.doBoosting();
            } else {
                refundOrPayAgain();
            }
        });

        txId.setOnClickListener(view -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.txid_to_clipboard)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) TxDetailsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip;
                        clip = android.content.ClipData.newPlainText("tx id", ((TextView) view).getText());
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                        }
                        Toast.makeText(TxDetailsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
            }).show();
        });

    }

    private void refundOrPayAgain() {
        Intent intent = new Intent(this, SendActivity.class);
        intent.putExtra("pcode", tx.getPaymentCode());
        if (!isSpend()) {
            intent.putExtra("amount", tx.getAmount());
        }
        startActivity(intent);
    }


    private void setTx() {

        calculateBTCDisplayAmount((long) tx.getAmount());
        calculateSatoshiDisplayAmount((long) tx.getAmount());
        amount.setText(BTCDisplayAmount);
        bottomButton.setVisibility(View.GONE);

        if (tx.getConfirmations() <= 3) {
            txStatus.setTextColor(ContextCompat.getColor(this, R.color.tx_broadcast_offline_bg));
            String txConfirmation = getString(R.string.unconfirmed) +
                    " (" +
                    tx.getConfirmations() +
                    "/3)";
            txStatus.setText(txConfirmation);
        }

        if (tx.getConfirmations() > 3) {
            String txConfirmation = String.valueOf(tx.getConfirmations()) +
                    " " +
                    getString(R.string.confirmation);

            txStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            txStatus.setText(txConfirmation);
            bottomButton.setVisibility(View.GONE);
        }
        txId.setText(tx.getHash());
        txDate.setText(DateUtil.getInstance(this).formatted(tx.getTS()));

        if (tx.getPaymentCode() != null) {
            bottomButton.setVisibility(View.VISIBLE);
            paynymDisplayName = BIP47Meta.getInstance().getDisplayLabel(tx.getPaymentCode());
            showPaynym();
            if (isSpend()) {
                bottomButton.setText(R.string.pay_again);
            } else {
                bottomButton.setText(R.string.refund);
            }
        }
        if (this.isBoostingAvailable()) {
            bottomButton.setVisibility(View.VISIBLE);
            bottomButton.setText(R.string.boost_transaction_fee);
        }


        fetchTxDetails();
    }

    private void doBoosting() {
        String message = getString(R.string.options_unconfirmed_tx);

        if (this.isRBFPossible()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name);
            builder.setMessage(message);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.options_bump_fee, (dialog, whichButton) -> this.RBFBoost());
            builder.setNegativeButton(R.string.cancel, (dialog, whichButton) -> dialog.dismiss());

            AlertDialog alert = builder.create();
            alert.show();
            return;
        } else {
            if (this.isCPFPPossible()) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(TxDetailsActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage(message);
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.options_bump_fee, (dialog, whichButton) -> this.CPFBoost());
                builder.setNegativeButton(R.string.cancel, (dialog, whichButton) -> dialog.dismiss());
                android.app.AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

    private void CPFBoost() {
        if (cpfpTask == null || cpfpTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            cpfpTask = new CPFPTask(TxDetailsActivity.this);
            cpfpTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tx.getHash());
        }
    }

    private void RBFBoost() {


        if (rbfTask == null || rbfTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            rbfTask = new RBFTask(this);
            rbfTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tx.getHash());
        }

    }

    private boolean isBoostingAvailable() {
        return tx.getConfirmations() < 1;
    }

    private boolean isSpend() {
        return tx.getAmount() < 0;
    }

    private void fetchTxDetails() {
        toggleProgress(View.VISIBLE);
        makeTxNetworkRequest()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(JSONObject jsonObject) {
                        toggleProgress(View.INVISIBLE);

                        try {
                            setFeeInfo(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        toggleProgress(View.INVISIBLE);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    /**
     * @param jsonObject
     * @throws JSONException
     */
    private void setFeeInfo(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("fees")) {
            minerFee.setText(jsonObject.getString("fees").concat(" sats"));
        }
        if (jsonObject.has("feerate")) {
            minerFeeRate.setText(jsonObject.getString("feerate").concat(" sats"));
        }
    }

    private Observable<JSONObject> makeTxNetworkRequest() {
        return Observable.create(emitter -> emitter.onNext(APIFactory.getInstance(TxDetailsActivity.this).getTxInfo(tx.getHash())));
    }


    private void calculateBTCDisplayAmount(long value) {
        BTCDisplayAmount = Coin.valueOf(value).toPlainString();
    }

    private void toggleProgress(int Visibility) {
        progressBar.setVisibility(Visibility);
    }

    private void toggleUnits() {
        TransitionManager.beginDelayedTransition((ViewGroup) btcUnit.getRootView().getRootView(), new AutoTransition());

        if (btcUnit.getText().equals("BTC")) {
            btcUnit.setText("sat");
            amount.setText(SatDisplayAmount);

        } else {
            btcUnit.setText("BTC");
            amount.setText(BTCDisplayAmount);
        }
    }

    private void calculateSatoshiDisplayAmount(long value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#", symbols);
        df.setMinimumIntegerDigits(1);
        df.setMaximumIntegerDigits(16);
        df.setGroupingUsed(true);
        df.setGroupingSize(3);
        SatDisplayAmount = df.format(value);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_block_explore: {
                doExplorerView();
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Opens external BlockExplorer
     */
    private void doExplorerView() {

        String blockExplorer = "https://m.oxt.me/transaction/";
        if (SamouraiWallet.getInstance().isTestNet()) {
            blockExplorer = "https://blockstream.info/testnet/";
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + tx.getHash()));
        startActivity(browserIntent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tx_details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void showPaynym() {
        TransitionManager.beginDelayedTransition((ViewGroup) payNymAvatar.getRootView().getRootView(), new AutoTransition());
        payNymUsername.setVisibility(View.VISIBLE);
        payNymAvatar.setVisibility(View.VISIBLE);
        payNymUsername.setText(paynymDisplayName);
        Picasso.with(this)
                .load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + tx.getPaymentCode() + "/avatar")
                .into(payNymAvatar);

    }


    /***
     * checks tx can be boosted using
     * Replace-by-fee method
     * @return boolean
     */
    private boolean isRBFPossible() {
        return tx.getConfirmations() < 1 && tx.getAmount() < 0.0 && RBFUtil.getInstance().contains(tx.getHash());
    }

    /***
     * checks tx can be boosted using
     * child pays for parent method
     * @return boolean
     */
    private boolean isCPFPPossible() {
        boolean a = tx.getConfirmations() < 1 && tx.getAmount() >= 0.0;
        boolean b = tx.getConfirmations() < 1 && tx.getAmount() < 0.0;
        return (a || b);
    }


}
