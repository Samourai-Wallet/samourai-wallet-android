package com.samourai.wallet.tx;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.BalanceActivity;
import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.R;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.boost.RBFCallback;
import com.samourai.wallet.send.boost.RBFTask;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.DateUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.widgets.CircleImageView;
import com.squareup.picasso.Picasso;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Objects;

import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class TxDetailsActivity extends AppCompatActivity {

    private CircleImageView payNymAvatar;
    private TextView payNymUsername, btcUnit, amount, txStatus, txId, txDate, bottomButton;
    private Tx tx;
    private static final String TAG = "TxDetailsActivity";
    private String BTCDisplayAmount, SatDisplayAmount, paynymDisplayName;
    private RBFTask rbfTask = null;
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
        amount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleUnits();
            }
        });
        btcUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleUnits();
            }
        });

        setTx();

        bottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TxDetailsActivity.this.isBoostingAvailable()) {
                    TxDetailsActivity.this.doBoosting();
                } else {
                    TxDetailsActivity.this.payAgain();
                }
            }
        });

    }

    private void payAgain() {
    }

    private void setTx() {

        calculateBTCDisplayAmount((long) tx.getAmount());
        calculateSatoshiDisplayAmount((long) tx.getAmount());
        amount.setText(BTCDisplayAmount);
        if (this.isBoostingAvailable()) {

            txStatus.setTextColor(ContextCompat.getColor(this, R.color.tx_broadcast_offline_bg));
            String txConfirmation = getString(R.string.unconfirmed) +
                    " (" +
                    tx.getConfirmations() +
                    "/3)";
            txStatus.setText(txConfirmation);
            bottomButton.setText("Boost transaction fee");

        }

        if (tx.getConfirmations() > 3) {
            String txConfirmation = String.valueOf(tx.getConfirmations()) +
                    " " +
                    getString(R.string.confirmation);

            txStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            txStatus.setText(txConfirmation);
            bottomButton.setText("Pay again");
        }

        txId.setText(tx.getHash());
        txDate.setText(DateUtil.getInstance(this).formatted(tx.getTS()));
        if (tx.getPaymentCode() != null) {
            paynymDisplayName = BIP47Meta.getInstance().getDisplayLabel(tx.getPaymentCode());
            showPaynym();
        }


    }

    private void doBoosting() {
        if (this.isRBFPossible()) {

            String message = getString(R.string.options_unconfirmed_tx);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name);
            builder.setMessage(message);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.options_bump_fee, (dialog, whichButton) -> {
                TxDetailsActivity.this.RBFBoost();
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
    }

    private void RBFBoost() {
        rbfTask = new RBFTask(this, tx.getHash());

//        if (rbfTask == null || rbfTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
//            rbfTask = new RBFTask(TxDetailsActivity.this);
//            rbfTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tx.getHash());
//        }
        Log.i(TAG, "RBFBoost: CLICKe");
        toggleProgress(View.VISIBLE);
        rbfTask.prepareMessage()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i(TAG, "onNext: onSubscribe");

                    }

                    @Override
                    public void onNext(String message) {
                        toggleProgress(View.INVISIBLE);
                        android.app.AlertDialog.Builder dlg = new android.app.AlertDialog.Builder(TxDetailsActivity.this)
                                .setTitle(R.string.app_name)
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        toggleProgress(View.VISIBLE);
                                        rbfTask.pushRFB();
                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                });
                        dlg.show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.i(TAG, "onNext Error: ".concat(e.getMessage()));
                        toggleProgress(View.INVISIBLE);
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG, "onNext onComplete");
                    }
                });

        rbfTask.rbfCallback = message -> {
            toggleProgress(View.INVISIBLE);
//                Log.i(TAG, "onSuccess: ".concat(successMessage));
            Toast.makeText(TxDetailsActivity.this, message, Toast.LENGTH_LONG).show();
            Intent _intent = new Intent(this, MainActivity2.class);
            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(_intent);
        };

//

    }

    private boolean isBoostingAvailable() {
        return tx.getConfirmations() < 3;
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
            case R.id.showPaynym: {
                showPaynym();
                break;
            }
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

    private void doExplorerView() {
        if (tx.getHash() != null) {
            int sel = PrefsUtil.getInstance(this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
            if (sel >= BlockExplorerUtil.getInstance().getBlockExplorerTxUrls().length) {
                sel = 0;
            }
            CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerTxUrls()[sel];

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + tx.getHash()));
            startActivity(browserIntent);
        }

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


    private boolean isAwaitingForConfimation() {
        return tx.getConfirmations() < 3;
    }

    // CPFP
    private boolean isCPFPPossible() {
        return tx.getConfirmations() < 1 && tx.getAmount() < 0.0 || tx.getConfirmations() < 1 && tx.getAmount() >= 0.0;
    }

    // RBF
    private boolean isRBFPossible() {
        return tx.getConfirmations() < 1 && tx.getAmount() < 0.0 && RBFUtil.getInstance().contains(tx.getHash());
    }


}
