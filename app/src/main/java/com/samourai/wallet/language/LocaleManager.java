package com.samourai.wallet.language;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.res.Configuration;

import com.samourai.wallet.SettingsActivity2;
import com.samourai.wallet.util.PrefsUtil;

import java.util.Locale;


public class LocaleManager {

    private final SharedPreferences prefs;

    public LocaleManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Context setLocale(Context c) {
        return updateResources(c, getLanguage(c));
    }


    public static String getLanguage(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        if (prefs.getBoolean("useSystemLanguage", true)) {
            return Resources.getSystem().getConfiguration().locale.getLanguage();
        }
        else    {
            return "en";
        }
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        return context;
    }

}
