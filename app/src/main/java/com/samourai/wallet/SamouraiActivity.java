package com.samourai.wallet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.samourai.wallet.whirlpool.WhirlpoolMeta;


@SuppressLint("Registered")
public class SamouraiActivity extends AppCompatActivity {

    protected int account = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("_account")) {
            if (getIntent().getExtras().getInt("_account") == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                account = WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix();
            }
        }
        setUpTheme();
    }

    private void setUpTheme() {
        if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix()) {
            setTheme(R.style.SamouraiAppWhirlpoolTheme);
        }
    }
}
