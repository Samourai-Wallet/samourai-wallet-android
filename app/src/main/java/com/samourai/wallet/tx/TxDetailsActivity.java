package com.samourai.wallet.tx;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samourai.wallet.BalanceActivity;
import com.samourai.wallet.R;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.DateUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.widgets.CircleImageView;

import org.bitcoinj.core.Coin;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Objects;


public class TxDetailsActivity extends AppCompatActivity {

    private CircleImageView payNymAvatar;
    private TextView payNymUsername, btcUnit, amount, txStatus, txId, txDate, bottomButton;
    private Tx tx;
    private static final String TAG = "TxDetailsActivity";
    private String BTCDisplayAmount, SatDisplayAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

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

    }

    private void setTx() {

        calculateBTCDisplayAmount((long) tx.getAmount());
        calculateSatoshiDisplayAmount((long) tx.getAmount());
        amount.setText(BTCDisplayAmount);
        if (tx.getConfirmations() < 3) {
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
            showPaynym();
        }

    }

    private void calculateBTCDisplayAmount(long value) {
        BTCDisplayAmount = Coin.valueOf(value).toPlainString();
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

        //Set Paynym details here
        payNymUsername.setText(BIP47Meta.getInstance().getDisplayLabel(tx.getPaymentCode()));
        payNymAvatar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.paynym));
    }


}
