package com.samourai.wallet.util;

import org.json.JSONException;
import org.json.JSONObject;

public class TrustedNodeUtil {

    private static String node = null;
    private static int port = 8332;
    private static CharSequenceX password = null;

    private static TrustedNodeUtil instance = null;

    private TrustedNodeUtil() { ; }

    public static TrustedNodeUtil getInstance() {

        if(instance == null) {
            instance = new TrustedNodeUtil();
        }

        return instance;
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();
        try {

            jsonPayload.put("node", node);
            jsonPayload.put("port", port);
            jsonPayload.put("password", password.toString());

        }
        catch(JSONException je) {
            ;
        }

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

        try {

            if(jsonPayload.has("node")) {
                node = jsonPayload.getString("node");
            }

            if(jsonPayload.has("port")) {
                port = jsonPayload.getInt("port");
            }

            if(jsonPayload.has("password")) {
                password = new CharSequenceX(jsonPayload.getString("password"));
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
