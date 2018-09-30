package com.masanari.wallet.send;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.masanari.wallet.JSONRPC.JSONRPC;
import com.masanari.wallet.JSONRPC.TrustedNodeUtil;
import com.masanari.wallet.R;
import com.masanari.wallet.MasanariWallet;
import com.masanari.wallet.util.PrefsUtil;
import com.masanari.wallet.util.TorUtil;
import com.masanari.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class PushTx {

    private static boolean DO_SPEND = true;

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

    public String masanari(String hexString) {

        String _url = MasanariWallet.getInstance().isTestNet() ? "test/v2/pushtx/" : "v2/pushtx/";

        try {
            String response = null;

            if(!TorUtil.getInstance(context).statusFromBroadcast())    {
                response = WebUtil.getInstance(context).postURL(WebUtil.MASANARI_API + _url, "tx=" + hexString);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("tx", hexString);
                response = WebUtil.getInstance(context).tor_postURL(WebUtil.MASANARI_API + _url, args);
            }

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

    public boolean pushTx(String hexTx) {

        String response = null;
        boolean isOK = false;

        try {
            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true)    {
                if(TrustedNodeUtil.getInstance().isSet())    {
                    if(DO_SPEND)    {
                        response = PushTx.getInstance(context).trustedNode(hexTx);
                        JSONObject jsonObject = new org.json.JSONObject(response);
                        if(jsonObject.has("result"))    {
                            if(jsonObject.getString("result").matches("^[A-Za-z0-9]{64}$"))    {
                                isOK = true;
                            }
                            else    {
                                Toast.makeText(context, R.string.trusted_node_tx_error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    else    {
                        Log.d("PushTx", hexTx);
                        isOK = true;
                    }
                }
                else    {
                    Toast.makeText(context, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                }
            }
            else    {
                if(DO_SPEND)    {
                    response = PushTx.getInstance(context).masanari(hexTx);
                    if(response != null)    {
                        JSONObject jsonObject = new org.json.JSONObject(response);
                        if(jsonObject.has("status"))    {
                            if(jsonObject.getString("status").equals("ok"))    {
                                isOK = true;
                            }
                        }
                    }
                    else    {
                        Toast.makeText(context, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                    }
                }
                else    {
                    Log.d("PushTx", hexTx);
                    isOK = true;
                }
            }

            if(isOK)    {
                if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == false)    {
                    Toast.makeText(context, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                }
                else    {
                    Toast.makeText(context, R.string.trusted_node_tx_sent, Toast.LENGTH_SHORT).show();
                }
            }
            else    {
                Toast.makeText(context, R.string.tx_failed, Toast.LENGTH_SHORT).show();
            }

        }
        catch(JSONException je) {
            Toast.makeText(context, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return isOK;

    }

}
