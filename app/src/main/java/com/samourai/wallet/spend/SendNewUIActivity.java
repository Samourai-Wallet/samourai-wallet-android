package com.samourai.wallet.spend;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.samourai.wallet.R;
import com.samourai.wallet.spend.widgets.EntropyBar;
import com.samourai.wallet.spend.widgets.SendTransactionDetailsView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;


public class SendNewUIActivity extends AppCompatActivity {

    private SendTransactionDetailsView sendTransactionDetailsView;
    private ViewSwitcher amountViewSwitcher;
    private EditText toEditText, btcEditText, satEditText;
    private Button btnReview, btnSend;
    private Switch ricochetHopsSwitch;
    private Boolean isOnReviewPage = false;
    private EntropyBar entropyBar;
    private static final String TAG = "SendNewUIActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_new_ui);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_send));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("");

        //CustomView for showing and hiding body of th UI
        sendTransactionDetailsView = findViewById(R.id.sendTransactionDetailsView);

        //ViewSwitcher Element for toolbar section of the UI.
        //we can switch between Form and review screen with this element
        amountViewSwitcher = findViewById(R.id.toolbar_view_switcher);

        //Input elements from toolbar section of the UI
        toEditText = findViewById(R.id.edt_send_to);
        btcEditText = findViewById(R.id.amountBTC);
        satEditText = findViewById(R.id.amountSat);
        btcEditText.addTextChangedListener(BTCWatcher);
        satEditText.addTextChangedListener(satWatcher);

        entropyBar = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.entropyBar);
        //view elements from review segment and transaction segment can be access through respective
        //methods which returns root viewGroup
        btnReview = sendTransactionDetailsView.getTransactionView().findViewById(R.id.review_button);
        ricochetHopsSwitch = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_hops_switch);

        btnSend = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.send_btn);


        btnReview.setOnClickListener(v -> review());


        btnSend.setOnClickListener(v -> Toast.makeText(SendNewUIActivity.this, "Send Clicked", Toast.LENGTH_SHORT).show());


    }

    private TextWatcher BTCWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            satEditText.removeTextChangedListener(satWatcher);
            btcEditText.removeTextChangedListener(this);

            try {
                String cleared_comma = editable.toString().replace(",", "");
                Float btc = Float.parseFloat(cleared_comma);
                Double sats = getSatValue(btc);
                BigDecimal numberBigDecimal = new BigDecimal(btc);
//                Log.i(TAG, "afterTextChanged: ".concat(String.vabtc.longValue()));
                DecimalFormat df = new DecimalFormat("#,###,###,###");
                satEditText.setText(df.format(sats));
//                btcEditText.setText(String.valueOf(numberBigDecimal));
//                btcEditText.setSelection(String.valueOf(numberBigDecimal).length());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            satEditText.addTextChangedListener(satWatcher);
            btcEditText.addTextChangedListener(this);


        }
    };

    private TextWatcher satWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            satEditText.removeTextChangedListener(this);
            btcEditText.removeTextChangedListener(BTCWatcher);

            try {
                String cleared_comma = editable.toString().replace(",", "");
                Double sats = Double.parseDouble(cleared_comma);
                Float btc = getBtcValue(sats);
//                if(sats)
                BigDecimal numberBigDecimal = new BigDecimal(btc);

//                Log.i(TAG, "afterTextChanged: ".concat(String.vabtc.longValue()));
                DecimalFormat df = new DecimalFormat("#,###,###,###");
                satEditText.setText(df.format(sats));
                satEditText.setSelection(df.format(sats).length());
                btcEditText.setText(String.format(Locale.ENGLISH, "%f", numberBigDecimal.floatValue()));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            satEditText.addTextChangedListener(this);
            btcEditText.addTextChangedListener(BTCWatcher);

        }
    };


    private Float getBtcValue(Double sats) {
        return (float) (sats / 100000000);
    }

    private Double getSatValue(Float btc) {
        return Double.valueOf(btc * 100000000);
    }

    private void review() {
        amountViewSwitcher.showNext();
        sendTransactionDetailsView.showReview(ricochetHopsSwitch.isChecked());
        isOnReviewPage = true;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                entropyBar.disable();
            }
        }, 4000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                entropyBar.setRange(3);
            }
        }, 7000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                entropyBar.setRange(2);
            }
        }, 9000);
    }

    private void backToTransactionView() {
        amountViewSwitcher.showPrevious();
        sendTransactionDetailsView.showTransaction();
        isOnReviewPage = false;
    }

    @Override
    public void onBackPressed() {
        if (isOnReviewPage) {
            backToTransactionView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


}
