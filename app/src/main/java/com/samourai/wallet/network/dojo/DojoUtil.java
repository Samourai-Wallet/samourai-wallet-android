package com.samourai.wallet.network.dojo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/*

{
  "pairing": {
    "type": "dojo.api",
    "version": "1.0.0",
    "apikey": "myApiKey",
    "url": "http://nh2blhfu7gt3ld6c.onion/v2"
  }
}

 */

public class DojoUtil {

    private static DojoUtil instance = null;

    private DojoUtil()  { ; }

    public static DojoUtil getInstance()    {

        if(instance == null)    {
            instance = new DojoUtil();
        }

        return instance;
    }

    public boolean isValidPairingPayload(String data) {

        try {
            JSONObject obj = new JSONObject(data);

            Log.d("DojoUtil", obj.toString());

            if(obj.has("pairing"))    {

                JSONObject pObj = obj.getJSONObject("pairing");
                Log.d("DojoUtil", pObj.toString());
                if(pObj.has("type") && pObj.has("version") && pObj.has("apikey") && pObj.has("url"))    {
                    Log.d("DojoUtil", "true");
                    return true;
                }
                else    {
                    Log.d("DojoUtil", "false");
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

}
