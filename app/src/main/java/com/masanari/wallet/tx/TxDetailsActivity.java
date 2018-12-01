package com.masanari.wallet.tx;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.masanari.wallet.R;
import com.masanari.wallet.widgets.CircleImageView;

import java.util.Objects;


public class TxDetailsActivity extends AppCompatActivity {

    private CircleImageView payNymAvatar;
    private TextView payNymUsername, btcUnit, btcValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        payNymUsername = findViewById(R.id.tx_paynym_username);
        btcUnit = findViewById(R.id.tx_unit);
        btcValue = findViewById(R.id.tx_btc_value);
        payNymAvatar = findViewById(R.id.img_paynym_avatar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btcValue.setOnClickListener(new View.OnClickListener() {
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

    }


    private void toggleUnits() {
        TransitionManager.beginDelayedTransition((ViewGroup) btcUnit.getRootView().getRootView(), new AutoTransition());

        if (btcUnit.getText().equals("BTC")) {
            btcUnit.setText("sat");
            btcValue.setText("3,433.000");
        } else {
            btcUnit.setText("BTC");
            btcValue.setText("0.00003433");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showPaynym: {
                showPaynym();
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
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
        payNymUsername.setText("+blackflower");
        payNymAvatar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.paynym));
    }


}

