package com.samourai.wallet;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.samourai.wallet.widgets.ArcProgress;

public class TestUI2Activity extends AppCompatActivity {

    private ArcProgress progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ui2);
        progress = findViewById(R.id.progress);
        progress.start(2);
    }
}
