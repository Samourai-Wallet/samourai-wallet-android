package com.samourai.wallet.whirlpool;

import android.content.Context;

public class WhirlpoolMeta {

    private final static int WHIRLPOOL_ACCOUNT = Integer.MAX_VALUE - 1;

    private static WhirlpoolMeta instance = null;

    private static int index = 0;

    private static Context context = null;

    private WhirlpoolMeta() { ; }

    public static WhirlpoolMeta getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new WhirlpoolMeta();
        }

        return instance;
    }

    public int getWhirlpoolAccount() {
        return WHIRLPOOL_ACCOUNT;
    }

}
