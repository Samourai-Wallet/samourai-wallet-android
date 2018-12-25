package com.samourai.wallet.cahoots;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class Cahoots {

    private int version = 1;
    private long ts = -1L;
    private String strID = null;
    private int type = -1;
    private int step = -1;
    private String strPSBT = null;

    public Cahoots()    { ; }

    public Cahoots(int type, int step, String strPSBT)    {
        this.ts = System.currentTimeMillis() / 1000L;
        this.strID = UUID.randomUUID().toString();
        this.type = type;
        this.step = step;
        this.strPSBT = strPSBT;
    }

    public JSONObject toJSON() {

        JSONObject obj = new JSONObject();

        try {
            obj.put("version", version);
            obj.put("ts", ts);
            obj.put("id", strID);
            obj.put("type", type);
            obj.put("step", step);
            obj.put("psbt", strPSBT);
        }
        catch(JSONException je) {
            ;
        }

        return obj;
    }

    public void fromJSON(JSONObject obj) {

        try {
            if(obj.has("type") && obj.has("step") && obj.has("psbt") && obj.has("ts") && obj.has("id") && obj.has("version"))    {
                this.version = obj.getInt("version");
                this.ts = obj.getLong("ts");
                this.strID = obj.getString("id");
                this.type = obj.getInt("type");
                this.step = obj.getInt("step");
                this.strPSBT = obj.getString("psbt");
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
