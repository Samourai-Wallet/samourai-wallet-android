package com.samourai.wallet.send;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockedUTXO {

    private static BlockedUTXO instance = null;
    private static HashMap<String,Long> blockedUTXO = null;
    private static List<String> notDustedUTXO = null;

    public final static long BLOCKED_UTXO_THRESHOLD = 1000L;

    private BlockedUTXO() { ; }

    public static BlockedUTXO getInstance() {

        if(instance == null) {
            instance = new BlockedUTXO();
            blockedUTXO = new HashMap<String,Long>();
            notDustedUTXO = new ArrayList<String>();
        }

        return instance;
    }

    public long get(String hash, int idx)    {
        return blockedUTXO.get(hash + "-" + Integer.toString(idx));
    }

    public void add(String hash, int idx, long value)    {
        blockedUTXO.put(hash + "-" + Integer.toString(idx), value);
    }

    public void remove(String hash, int idx)   {
        if(blockedUTXO != null && blockedUTXO.containsKey(hash + "-" + Integer.toString(idx)))  {
            blockedUTXO.remove(hash + "-" + Integer.toString(idx));
        }
    }

    public boolean contains(String hash, int idx)   {
        return blockedUTXO.containsKey(hash + "-" + Integer.toString(idx));
    }

    public void clear()    {
        blockedUTXO.clear();
    }

    public long getTotalValueBlocked()  {
        long ret = 0L;
        for(String id : blockedUTXO.keySet())   {
            ret += blockedUTXO.get(id);
        }
        return ret;
    }

    public void addNotDusted(String hash, int idx)    {
        if(!notDustedUTXO.contains(hash + "-" + Integer.toString(idx)))    {
            notDustedUTXO.add(hash + "-" + Integer.toString(idx));
        }
    }

    public void removeNotDusted(String hash, int idx)   {
        if(notDustedUTXO.contains(hash + "-" + Integer.toString(idx)))    {
            notDustedUTXO.remove(hash + "-" + Integer.toString(idx));
        }
    }

    public void removeNotDusted(String s)   {
        if(notDustedUTXO.contains(s))    {
            notDustedUTXO.remove(s);
        }
    }

    public boolean containsNotDusted(String hash, int idx)   {
        return notDustedUTXO.contains(hash + "-" + Integer.toString(idx));
    }

    public HashMap<String, Long> getBlockedUTXO() {
        return blockedUTXO;
    }

    public List<String> getNotDustedUTXO() {
        return notDustedUTXO;
    }

    public JSONObject toJSON() {

        JSONObject blockedObj = new JSONObject();

        JSONArray array = new JSONArray();
        try {
            for(String id : blockedUTXO.keySet())   {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("value", blockedUTXO.get(id));
                array.put(obj);
            }
            blockedObj.put("blocked", array);

            JSONArray notDusted = new JSONArray();
            for(String s : notDustedUTXO)   {
                notDusted.put(s);
            }
            blockedObj.put("notDusted", notDusted);

        }
        catch(JSONException je) {
            ;
        }

        return blockedObj;
    }

    public void fromJSON(JSONObject blockedObj) {

        try {

            if(blockedObj.has("blocked"))    {
                JSONArray array = blockedObj.getJSONArray("blocked");

                for(int i = 0; i < array.length(); i++)   {
                    JSONObject obj = array.getJSONObject(i);
                    Log.d("BlockedUTXO", obj.getString("id") + ":" + obj.getLong("value"));
                    blockedUTXO.put(obj.getString("id"), obj.getLong("value"));
                }
            }

            if(blockedObj.has("notDusted"))  {
                JSONArray array = blockedObj.getJSONArray("notDusted");

                for(int i = 0; i < array.length(); i++)   {
                    notDustedUTXO.add(array.getString(i));
                }
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
