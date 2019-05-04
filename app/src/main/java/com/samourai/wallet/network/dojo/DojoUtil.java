package com.samourai.wallet.network.dojo;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class DojoUtil {

    private static String dojoParams = null;
    private static DojoUtil instance = null;

    private static Context context = null;

    private DojoUtil()  { ; }

    public static DojoUtil getInstance(Context ctx)    {

        context = ctx;

        if(instance == null)    {
            instance = new DojoUtil();
        }

        return instance;
    }

    public void clear() {
        dojoParams = null;
    }

    public boolean isValidPairingPayload(String data) {

        try {
            JSONObject obj = new JSONObject(data);

            if(obj.has("pairing"))    {

                JSONObject pObj = obj.getJSONObject("pairing");
                if(pObj.has("type") && pObj.has("version") && pObj.has("apikey") && pObj.has("url"))    {
                    return true;
                }
                else    {
                    return false;
                }

            }
            else    {
                return false;
            }
        }
        catch(JSONException je) {
            return false;
        }

    }

    public String getDojoParams() {
        return dojoParams;
    }

    public void setDojoParams(String dojoParams) {
        DojoUtil.dojoParams = dojoParams;

        if(SamouraiWallet.getInstance().isTestNet())    {
            String url = getUrl(dojoParams);
            WebUtil.SAMOURAI_API2_TESTNET_TOR = url.replace("/v2/", "/test/v2/");
        }
        else    {
            WebUtil.SAMOURAI_API2_TOR = getUrl(dojoParams);
        }

        String apiToken = getApiKey(dojoParams);
        APIFactory.getInstance(context).setAppToken(apiToken);
        APIFactory.getInstance(context).getToken();

    }

    public void removeDojoParams() {
        DojoUtil.dojoParams = null;

        if(SamouraiWallet.getInstance().isTestNet())    {
            WebUtil.SAMOURAI_API2_TESTNET_TOR = WebUtil.SAMOURAI_API2_TESTNET_TOR_DIST;
        }
        else    {
            WebUtil.SAMOURAI_API2_TOR = WebUtil.SAMOURAI_API2_TOR_DIST;
        }

        APIFactory.getInstance(context).setAppToken(null);
        APIFactory.getInstance(context).getToken();

    }

    public String getVersion(String data)  {

        if(!isValidPairingPayload(data))    {
            return null;
        }

        try {
            JSONObject obj = new JSONObject(data);
            JSONObject pObj = obj.getJSONObject("pairing");
            return pObj.getString("version");
        }
        catch(JSONException je) {
            return null;
        }

    }

    public String getApiKey(String data)  {

        if(!isValidPairingPayload(data))    {
            return null;
        }

        try {
            JSONObject obj = new JSONObject(data);
            JSONObject pObj = obj.getJSONObject("pairing");
            return pObj.getString("apikey");
        }
        catch(JSONException je) {
            return null;
        }

    }

    public String getUrl(String data)  {

        if(!isValidPairingPayload(data))    {
            return null;
        }

        try {
            JSONObject obj = new JSONObject(data);
            JSONObject pObj = obj.getJSONObject("pairing");
            return pObj.getString("url");
        }
        catch(JSONException je) {
            return null;
        }

    }

    public JSONObject toJSON() {

        JSONObject obj = null;

        try {
            obj = new JSONObject(dojoParams);
        }
        catch(JSONException je) {
            ;
        }

        return obj;
    }

    public void fromJSON(JSONObject obj) {

        if(isValidPairingPayload(obj.toString())) {

            dojoParams = obj.toString();

            if (dojoParams != null) {

                if (SamouraiWallet.getInstance().isTestNet()) {
                    WebUtil.SAMOURAI_API2_TESTNET_TOR = getUrl(dojoParams);
                } else {
                    WebUtil.SAMOURAI_API2_TOR = getUrl(dojoParams);
                }

                String apiToken = getApiKey(dojoParams);
                APIFactory.getInstance(context).setAppToken(apiToken);

            }

        }

    }

}
