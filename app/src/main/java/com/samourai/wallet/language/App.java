package com.samourai.wallet.language;

import android.content.Context;
import android.content.res.Configuration;
import android.support.multidex.MultiDexApplication;



// This class is used as Application class for Samourai.
// It extends MultiDexApplication to support MultiDex and sets the chosen language on start and onConfigurationChanged.

public class App extends MultiDexApplication {


    public static LocaleManager localeManager;

    @Override
    protected void attachBaseContext(Context base) {
        localeManager = new LocaleManager(base);
        super.attachBaseContext(localeManager.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        localeManager.setLocale(this);
    }
}