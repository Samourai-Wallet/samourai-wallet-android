package com.samourai.wallet.language;

import android.content.Context;
import android.preference.PreferenceActivity;

public abstract class BasePreferenceActivity extends PreferenceActivity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }
}
