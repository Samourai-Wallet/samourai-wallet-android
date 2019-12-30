package com.samourai.wallet.whirlpool;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;

import org.json.JSONException;
import org.json.JSONObject;

public class WhirlpoolMeta {

    public final static double WHIRLPOOL_FEE_RATE_POOL_DENOMINATION = 0.05;

    private final static int WHIRLPOOL_BADBANK_ACCOUNT = Integer.MAX_VALUE - 3;
    private final static int WHIRLPOOL_PREMIX_ACCOUNT = Integer.MAX_VALUE - 2;
    private final static int WHIRLPOOL_POSTMIX_ACCOUNT = Integer.MAX_VALUE - 1;

    private static WhirlpoolMeta instance = null;

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
        return WHIRLPOOL_POSTMIX_ACCOUNT;
    }

    public int getWhirlpoolBadBank() {
        return WHIRLPOOL_BADBANK_ACCOUNT;
    }

}
