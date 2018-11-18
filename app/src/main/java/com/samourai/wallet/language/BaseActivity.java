package com.samourai.wallet.language;

import android.app.Activity;
import android.content.Context;

public abstract class BaseActivity extends Activity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }
}
