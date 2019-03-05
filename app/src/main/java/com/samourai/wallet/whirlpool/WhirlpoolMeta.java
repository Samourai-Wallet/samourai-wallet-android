package com.samourai.wallet.whirlpool;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class WhirlpoolMeta {

    private final static int WHIRLPOOL_PREMIX_ACCOUNT = Integer.MAX_VALUE - 2;
    private final static int WHIRLPOOL_POSTMIX = Integer.MAX_VALUE - 1;

    private static int preIdx  = 0;
    private static int postIdx  = 0;

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
        return WHIRLPOOL_POSTMIX;
    }

    public static int getPreIdx() {
        return preIdx;
    }

    public static void setPreIdx(int idx) {
        WhirlpoolMeta.preIdx = idx;
    }

    public static int getPostIdx() {
        return postIdx;
    }

    public static void setPostIdx(int idx) {
        WhirlpoolMeta.postIdx = idx;
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();
        try {

            jsonPayload.put("pre_index", preIdx);
            jsonPayload.put("post_index", postIdx);

        }
        catch(JSONException je) {
            ;
        }

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

        try {

            if(jsonPayload.has("pre_index"))    {
                preIdx = jsonPayload.getInt("pre_index");
            }
            if(jsonPayload.has("post_index"))    {
                postIdx = jsonPayload.getInt("post_index");
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
