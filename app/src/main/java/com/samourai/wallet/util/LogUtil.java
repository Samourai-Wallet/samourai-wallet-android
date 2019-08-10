package com.samourai.wallet.util;

import com.samourai.wallet.BuildConfig;

public class LogUtil {

    public static void debug(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, message);
        }
    }

    public static void info(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, message);
        }
    }

    public static void error(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, message);
        }
    }

}
