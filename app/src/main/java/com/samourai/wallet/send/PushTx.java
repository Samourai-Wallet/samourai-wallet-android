package com.samourai.wallet.send;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

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

    public String samourai(String hexString, List<Integer> strictModeVouts) {

        String _url = "pushtx/";

        String strStrictVouts = "";
        if(strictModeVouts != null && strictModeVouts.size() > 0) {
            strStrictVouts = "&strict_mode_vouts=";
            for(int i = 0; i < strictModeVouts.size(); i++) {
                strStrictVouts += strictModeVouts.get(i);
                if(i < (strictModeVouts.size() - 1)) {
                    strStrictVouts += "|";
                }
            }
        }

        try {
            String response = null;

            if(!TorManager.getInstance(context).isRequired())    {
                String _base = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET : WebUtil.SAMOURAI_API2;
                response = WebUtil.getInstance(context).postURL(_base + _url + "?at=" + APIFactory.getInstance(context).getAccessToken(), "tx=" + hexString + strStrictVouts);
            }
            else    {
                String _base = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET_TOR : WebUtil.SAMOURAI_API2_TOR;
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("tx", hexString);
                if(strStrictVouts.length() > "&strict_mode_vouts=".length())    {
                    args.put("strict_mode_vouts", strStrictVouts.substring("&strict_mode_vouts=".length()));
                }
                response = WebUtil.getInstance(context).tor_postURL(_base + _url + "?at=" + APIFactory.getInstance(context).getAccessToken(), args);
            }

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
            if(DO_SPEND)    {
                response = PushTx.getInstance(context).samourai(hexTx, null);
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

            if(isOK)    {
                Toast.makeText(context, R.string.tx_sent, Toast.LENGTH_SHORT).show();
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
