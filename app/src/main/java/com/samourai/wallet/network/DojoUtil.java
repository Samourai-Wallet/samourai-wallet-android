package com.samourai.wallet.network;

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

            if(obj.has("pairing"))    {

                JSONObject pObj = obj.getJSONObject("pairing");
                if(pObj.has("type") && pObj.has("version") && pObj.has("apiKey") && pObj.has("url"))    {
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
            return pObj.getString("apiKey");
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
