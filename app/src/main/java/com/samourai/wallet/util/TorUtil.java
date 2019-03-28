package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.wallet.tor.TorManager;

import org.json.JSONException;
import org.json.JSONObject;

public class TorUtil {

    private static Context context = null;
    private static TorUtil instance = null;

    private static boolean statusFromBroadcast = false;

    private TorUtil()   { ; }

    public static TorUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new TorUtil();
        }

        return instance;

    }

    public boolean statusFromBroadcast() {
        return TorManager.getInstance(context).isConnected();
    }

    public void setStatusFromBroadcast(boolean status) {
        statusFromBroadcast = status;
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();

        try {

            jsonPayload.put("active", TorManager.getInstance(context).isConnected());

        }
        catch(JSONException je) {
            ;
        }

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

        try {

            if(jsonPayload.has("active"))    {
                statusFromBroadcast = jsonPayload.getBoolean("active");
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}