package com.samourai.wallet.language;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

public abstract class BaseAppCompatActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }
}
