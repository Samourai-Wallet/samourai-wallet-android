package com.samourai.wallet.ricochet;

import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import android.util.Log;

public class RicochetMeta {

    public static final int STATUS_NOT_SENT = -1;
    public static final int STATUS_SENT_NO_CFM = 0;
    public static final int STATUS_SENT_CFM = 1;

    private static RicochetMeta instance = null;

    private static int index = 0;
    private static Queue<JSONObject> fifo = null;

    private RicochetMeta() { ; }

    public static RicochetMeta getInstance() {

        if(instance == null) {
            Queue<JSONObject> fifo = new LinkedList<JSONObject>();

            instance = new RicochetMeta();
        }

        return instance;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        RicochetMeta.index = index;
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();
        try {

            jsonPayload.put("index", index);

            JSONArray array = new JSONArray();
            while(!fifo.isEmpty())   {
                JSONObject obj = fifo.remove();
                array.put(obj);
            }
            jsonPayload.put("queue", array);

        }
        catch(JSONException je) {
            ;
        }

//        Log.i("RicochetMeta", jsonPayload.toString());

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

//        Log.i("RicochetMeta", jsonPayload.toString());

        try {

            if(jsonPayload.has("index"))    {
                index = jsonPayload.getInt("index");
            }
            if(jsonPayload.has("queue"))    {

                JSONArray array = jsonPayload.getJSONArray("queue");
                for(int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    fifo.add(obj);
                }

            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
