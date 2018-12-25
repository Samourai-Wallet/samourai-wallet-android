package com.samourai.wallet.whirlpool;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.views.CycleDetailHeader;


public class CycleDetail extends AppCompatActivity {

    private CycleDetailHeader cycleDetailHeader;
    private Boolean showMenuItems = false;
    private TextView CycleStatus,TransactionStatus,TransactionId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle);
        cycleDetailHeader = findViewById(R.id.cycleProgressHeader);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        CycleStatus = findViewById(R.id.whirlpool_status);
        TransactionStatus = findViewById(R.id.transaction_status);
        TransactionId = findViewById(R.id.transaction_id);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cycleDetailHeader.setProgress(60, 600);
            }
        }, 1500);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cycleDetailHeader.setProgress(100, 800);
            }
        }, 2500);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cycleDetailHeader.switchToBroadCastedSection();
                setBroadCastedState();
            }
        }, 5000);


    }

    private void setBroadCastedState() {
        showMenuItems = true;
        invalidateOptionsMenu();
        CycleStatus.setText(R.string.transaction_broadcast);
        CycleStatus.setTextColor(Color.parseColor("#e0ce5e"));
        TransactionStatus.setText("");
        TransactionId.setVisibility(View.VISIBLE);
        TransactionId.setText("36ede7de4834dcbf83d0afd5f5209cd7afcb64b6eed4a0bfaea3b3dcc9b84313");

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whirlpool_cycle_detail_menu, menu);
        menu.findItem(R.id.whirpool_explore_menu).setVisible(showMenuItems);
        menu.findItem(R.id.whirpool_chart_menu).setVisible(showMenuItems);
        return super.onCreateOptionsMenu(menu);
    }
}
