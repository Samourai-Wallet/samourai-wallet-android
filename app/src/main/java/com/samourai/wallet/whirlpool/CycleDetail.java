package com.samourai.wallet.whirlpool;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.views.CycleProgressHeader;


public class CycleDetail extends AppCompatActivity {

    private CycleProgressHeader cycleProgressHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle);
        cycleProgressHeader = findViewById(R.id.cycleProgressHeader);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cycleProgressHeader.setProgress(60,600);
            }
        }, 1500);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cycleProgressHeader.setProgress(100,800);
            }
        }, 2500);



    }

}
