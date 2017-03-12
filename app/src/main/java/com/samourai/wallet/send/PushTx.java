package com.samourai.wallet.send;

import android.content.Context;

import com.samourai.wallet.JSONRPC.JSONRPC;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.util.WebUtil;

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
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

    public String blockchain(String hexString) {

        try {
            String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "pushtx", "tx=" + hexString);
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

    public String samourai(String hexString) {

        try {
            String response = WebUtil.getInstance(context).postURL(WebUtil.SAMOURAI_API + "v1/pushtx", "tx=" + hexString);
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

    public String trustedNode(String hexString) {

        try {
            JSONRPC jsonrpc = new JSONRPC(TrustedNodeUtil.getInstance().getUser(), TrustedNodeUtil.getInstance().getPassword(), TrustedNodeUtil.getInstance().getNode(), TrustedNodeUtil.getInstance().getPort());
            String response = jsonrpc.pushTx(hexString).toString();
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

}
