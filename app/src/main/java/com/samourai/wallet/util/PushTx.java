package com.samourai.wallet.util;

import android.content.Context;

public class PushTx {

    private static PushTx instance = null;
    private static Context context = null;

    private PushTx() { ; }

    public static PushTx getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new PushTx();
        }

        return instance;
    }

    public String chainSo(String hexString) {

        try {
            String response = WebUtil.getInstance(null).postURL(WebUtil.CHAINSO_PUSHTX_URL, "tx_hex=" + hexString);
//        Log.i("Send response", response);
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

    public String blockchain(String hexString) {

        try {
            String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "pushtx", "tx=" + hexString);
//        Log.i("Send response", response);
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

    public String samourai(String hexString) {

        try {
            String response = WebUtil.getInstance(null).postURL(WebUtil.SAMOURAI_API + "v1/pushtx", "tx=" + hexString);
//        Log.i("Send response", response);
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

}
