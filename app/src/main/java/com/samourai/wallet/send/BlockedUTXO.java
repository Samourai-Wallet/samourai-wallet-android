package com.samourai.wallet.send;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockedUTXO {

    private static BlockedUTXO instance = null;
    private static HashMap<String,Long> blockedUTXO = null;
    private static List<String> dustedUTXO = null;

    public final static long BLOCKED_UTXO_THRESHOLD = 1000L;

    private BlockedUTXO() { ; }

    public static BlockedUTXO getInstance() {

        if(instance == null) {
            instance = new BlockedUTXO();
            blockedUTXO = new HashMap<String,Long>();
            dustedUTXO = new ArrayList<String>();
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

    public void addDusted(String hash)    {
        if(!dustedUTXO.contains(hash))    {
            dustedUTXO.add(hash);
        }
    }

    public void remove(String hash)   {
        if(dustedUTXO.contains(hash))    {
            dustedUTXO.remove(hash);
        }
    }

    public boolean containsDusted(String hash)   {
        return dustedUTXO.contains(hash);
    }

    public void clearDusted()    {
        dustedUTXO.clear();
    }

    public JSONArray toJSON() {

        JSONArray array = new JSONArray();
        try {
            for(String id : blockedUTXO.keySet())   {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("value", blockedUTXO.get(id));
                array.put(obj);
            }
        }
        catch(JSONException je) {
            ;
        }

        return array;
    }

    public void fromJSON(JSONArray array) {

        try {

            for(int i = 0; i < array.length(); i++)   {
                JSONObject obj = array.getJSONObject(i);
                blockedUTXO.put(obj.getString("id"), obj.getLong("value"));
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
