package com.samourai.wallet.util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;

public class UTXOUtil {

    private static UTXOUtil instance = null;

    private static HashMap<String,String> utxoTags = null;
    private static HashMap<String,String> utxoNotes = null;

    private UTXOUtil() { ; }

    public static UTXOUtil getInstance() {

        if(instance == null) {
            utxoTags = new HashMap<String,String>();
            utxoNotes = new HashMap<String,String>();
            instance = new UTXOUtil();
        }

        return instance;
    }

    public void reset() {
        utxoTags.clear();
        utxoNotes.clear();
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

    public void addNote(String utxo, String note) {
        utxoNotes.put(utxo, note);
    }

    public String getNote(String utxo) {
        if(utxoNotes.containsKey(utxo))  {
            return utxoNotes.get(utxo);
        }
        else    {
            return null;
        }

    }

    public HashMap<String,String> getNotes() {
        return utxoNotes;
    }

    public void removeNote(String utxo) {
        utxoNotes.remove(utxo);
    }

    public JSONArray toJSON() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoTags.keySet()) {
            JSONArray tag = new JSONArray();
            tag.put(key);
            tag.put(utxoTags.get(key));
            utxos.put(tag);
        }
        for(String key : utxoNotes.keySet()) {
            JSONArray note = new JSONArray();
            note.put(key);
            note.put(utxoNotes.get(key));
            utxos.put(note);
        }

        return utxos;
    }

    public void fromJSON(JSONArray utxos) {
        try {
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray tag = (JSONArray)utxos.get(i);
                utxoTags.put((String)tag.get(0), (String)tag.get(1));
            }
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray note = (JSONArray)utxos.get(i);
                utxoNotes.put((String)note.get(0), (String)note.get(1));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public JSONArray toJSON_notes() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoNotes.keySet()) {
            JSONArray note = new JSONArray();
            note.put(key);
            note.put(utxoNotes.get(key));
            utxos.put(note);
        }

        return utxos;
    }

    public void fromJSON_notes(JSONArray utxos) {
        try {
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray note = (JSONArray)utxos.get(i);
                utxoNotes.put((String)note.get(0), (String)note.get(1));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
