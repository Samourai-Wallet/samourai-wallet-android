package com.samourai.wallet.util;

import com.samourai.wallet.BuildConfig;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

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

    public static void setLoggersDebug() {
        // skip noisy logs
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.ERROR);

        // enable debug logs for external Samourai libraries...
        ((Logger) LoggerFactory.getLogger("com.samourai")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.wallet")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.soroban")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.whirlpool")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.xmanager")).setLevel(Level.DEBUG);

        // skip noisy logs
        ((Logger) LoggerFactory.getLogger("com.samourai.wallet.staging")).setLevel(Level.ERROR);
    }

}
