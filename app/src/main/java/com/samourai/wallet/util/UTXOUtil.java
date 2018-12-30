package com.samourai.wallet.util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;

public class UTXOUtil {

    private static UTXOUtil instance = null;

    private static HashMap<String,String> utxoTags = null;

    private UTXOUtil() { ; }

    public static UTXOUtil getInstance() {

        if(instance == null) {
            utxoTags = new HashMap<String,String>();
            instance = new UTXOUtil();
        }

        return instance;
    }

    public void reset() {
        utxoTags.clear();
    }

    public void add(String utxo, String tag) {
        utxoTags.put(utxo, tag);
    }

    public String get(String utxo) {
        if(utxoTags.containsKey(utxo))  {
            return utxoTags.get(utxo);
        }
        else    {
            return null;
        }

    }

    public HashMap<String,String> getTags() {
        return utxoTags;
    }

    public void remove(String utxo) {
        utxoTags.remove(utxo);
    }

    public JSONArray toJSON() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoTags.keySet()) {
            JSONArray tag = new JSONArray();
            tag.put(key);
            tag.put(utxoTags.get(key));
            utxos.put(tag);
        }

        return utxos;
    }

    public void fromJSON(JSONArray utxos) {
        try {
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray tag = (JSONArray)utxos.get(i);
                utxoTags.put((String)tag.get(0), (String)tag.get(1));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
