package com.masanari.wallet.spend;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.masanari.wallet.R;
import com.masanari.wallet.spend.widgets.EntropyBar;
import com.masanari.wallet.spend.widgets.SendTransactionDetailsView;

import java.util.Objects;


public class SendNewUIActivity extends AppCompatActivity {

    private SendTransactionDetailsView sendTransactionDetailsView;
    private ViewSwitcher amountViewSwitcher;
    private EditText toEditText, btcEditText, fiatEditText;
    private Button btnReview, btnSend;
    private Switch ricochetHopsSwitch;
    private Boolean isOnReviewPage = false;
    private EntropyBar entropyBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_new_ui);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_send));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle("");

        //CustomView for showing and hiding body of th UI
        sendTransactionDetailsView = findViewById(R.id.sendTransactionDetailsView);

        //ViewSwitcher Element for toolbar section of the UI.
        //we can switch between Form and review screen with this element
        amountViewSwitcher = findViewById(R.id.toolbar_view_switcher);

        //Input elements from toolbar section of the UI
        toEditText = findViewById(R.id.edt_send_to);
        btcEditText = findViewById(R.id.amountBTC);
        fiatEditText = findViewById(R.id.amountFiat);

        entropyBar = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.entropyBar);
        //view elements from review segment and transaction segment can be access through respective
        //methods which returns root viewGroup
        btnReview = sendTransactionDetailsView.getTransactionView().findViewById(R.id.review_button);
        ricochetHopsSwitch = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_hops_switch);

        btnSend = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.send_btn);


        btnReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                review();
            }
        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SendNewUIActivity.this, "Send Clicked", Toast.LENGTH_SHORT).show();
            }
        });


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
        },4000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                entropyBar.setRange(3);
            }
        },7000);
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

