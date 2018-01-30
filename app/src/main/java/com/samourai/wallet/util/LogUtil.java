package com.samourai.wallet.util;

import com.samourai.wallet.BuildConfig;

public class LogUtil {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static String getTag() {
        String tag = "NO_TAG";
        String tagPrefix = "SW.";

        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (ste.getClassName().equals(LogUtil.class.getName())) {
                if (i+1 < stElements.length) {
                    StackTraceElement callerElement = stElements[i+1];
                    String[] classNames = callerElement.getClassName().split("\\.");
                    tag = classNames[classNames.length - 1];
                }
                break;
            }
        }
        return tagPrefix + tag;
    }
}
