package com.samourai.wallet;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.samourai.wallet.widgets.ArcProgress;
import com.samourai.wallet.widgets.TransactionProgressView;

public class TestUI2Activity extends AppCompatActivity {

    private ArcProgress progress;
    TransactionProgressView progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ui2);

        progressView = findViewById(R.id.transactionProgressView);

        progressView.getmArcProgress().start(800);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressView.offlineMode(800);
            }
        }, 2400);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressView.showCheck();
            }
        }, 3800);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressView.toggleOfflineButton();
            }
        }, 4800);

    }
}
