package com.samourai.wallet.language;

import android.content.Context;
import android.support.v4.app.FragmentActivity;

public abstract class BaseFragmentActivity extends FragmentActivity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }
}
