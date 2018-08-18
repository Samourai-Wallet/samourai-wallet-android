package com.samourai.wallet.whirlpool;

import android.content.Context;

public class WhirlpoolMeta {

    private final static int WHIRLPOOL_PREMIX_ACCOUNT = Integer.MAX_VALUE - 1;

    private final static int WHIRLPOOL_POSTMIX = Integer.MAX_VALUE;
    private final static int WHIRLPOOL_POSTMIX_CP = Integer.MAX_VALUE - 1;

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

    public int getWhirlpoolPremixAccount() {
        return WHIRLPOOL_PREMIX_ACCOUNT;
    }

    public int getWhirlpoolPostmix() {
        return WHIRLPOOL_POSTMIX;
    }

    public int getWhirlpoolPostmixCP() {
        return WHIRLPOOL_POSTMIX_CP;
    }

}
