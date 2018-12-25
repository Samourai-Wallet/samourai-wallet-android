package com.samourai.wallet.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SentToFromBIP47Util {

    private static SentToFromBIP47Util instance = null;

    private static HashMap<String,List<String>> sendToFromBIP47 = null;

    private SentToFromBIP47Util() { ; }

    public static SentToFromBIP47Util getInstance() {

        if(instance == null) {
            sendToFromBIP47 = new HashMap<String,List<String>>();
            instance = new SentToFromBIP47Util();
        }

        return instance;
    }

    public void reset() {
        sendToFromBIP47.clear();
    }

    public void add(String addr, String hash) {
        List<String> hashes = null;
        if(sendToFromBIP47.containsKey(addr))    {
            hashes = sendToFromBIP47.get(addr);
        }
        else    {
            hashes = new ArrayList<String>();
        }
        hashes.add(hash);
        sendToFromBIP47.put(addr, hashes);
    }

    public List<String> get(String addr) {
        if(sendToFromBIP47.containsKey(addr))    {
            return sendToFromBIP47.get(addr);
        }
        else    {
            return null;
        }
    }

    public String getByHash(String hash) {
        for(String addr : sendToFromBIP47.keySet())  {
            if(sendToFromBIP47.get(addr).contains(hash))    {
                return addr;
            }
        }
        return null;
    }

    public List<String> getAllHashes() {

        List<String> ret = new ArrayList<String>();

        for(String addr : sendToFromBIP47.keySet())  {
            ret.addAll(sendToFromBIP47.get(addr));
        }

        return ret;
    }

    public void remove(String addr) {
        sendToFromBIP47.remove(addr);
    }

    public void removeHash(String hash) {
        List<String> hashes = null;
        for(String addr : sendToFromBIP47.keySet())  {
            hashes = sendToFromBIP47.get(addr);
            if(hashes.contains(hash))    {
                hashes.remove(hash);
                sendToFromBIP47.put(addr, hashes);
            }
        }
    }

    public JSONArray toJSON() {

        JSONArray sentBIP47 = new JSONArray();

        for(String key : sendToFromBIP47.keySet()) {
            JSONArray array = new JSONArray();
            array.put(key);
            List<String> hashes = sendToFromBIP47.get(key);
            for(String h : hashes)   {
                array.put(h);
            }
            sentBIP47.put(array);
        }

        return sentBIP47;
    }

    public void fromJSON(JSONArray tos) {

        try {
            for(int i = 0; i < tos.length(); i++)   {
                JSONArray array = tos.getJSONArray(i);
                if(array.length() > 0)    {
                    String addr = array.getString(0);
                    if(array.length() > 1)    {
                        List<String> hashes = new ArrayList<String>();
                        for(int j = 1; j < array.length(); j++)   {
                            hashes.add(array.getString(j));
                        }
                        sendToFromBIP47.put(addr, hashes);
                    }
                }
            }
        }
        catch(JSONException je) {
            throw new RuntimeException(je);
        }
    }

}
