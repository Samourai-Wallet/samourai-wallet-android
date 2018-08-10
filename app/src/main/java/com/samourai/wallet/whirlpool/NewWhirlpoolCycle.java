package com.samourai.wallet.whirlpool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.samourai.wallet.R;

public class NewWhirlpoolCycle extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_whirlpool_cycle);
        Toolbar toolbar = findViewById(R.id.toolbar_new_whirlpool);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
