package com.samourai.wallet.utxos;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.util.FormatsUtil;

import org.bitcoinj.core.Address;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class UTXOUtil {

    public enum AddressTypes {
        LEGACY,
        SEGWIT_COMPAT,
        SEGWIT_NATIVE;
    }

    private static UTXOUtil instance = null;

    private static HashMap<String, List<String>> utxoAutoTags = null;
    private static HashMap<String,String> utxoNotes = null;
    private static HashMap<String,Integer> utxoScores = null;

    private UTXOUtil() {
        ;
    }

    public static UTXOUtil getInstance() {

        if(instance == null) {
            utxoAutoTags = new HashMap<String,List<String>>();
            utxoNotes = new HashMap<String,String>();
            utxoScores = new HashMap<String,Integer>();
            instance = new UTXOUtil();
        }

        return instance;
    }

    public void reset() {
        utxoAutoTags.clear();
        utxoNotes.clear();
        utxoScores.clear();
    }

    public void add(String utxo, String tag) {
        if(utxoAutoTags.containsKey(utxo)) {
            utxoAutoTags.get(utxo).add(tag);
        }
        else {
            List<String> tags = new ArrayList<String>();
            tags.add(tag);
            utxoAutoTags.put(utxo, tags);
        }
    }

    public List<String> get(String utxo) {
        if (utxoAutoTags.containsKey(utxo)) {
            return utxoAutoTags.get(utxo);
        } else {
            return null;
        }

    }

    public HashMap<String, List<String>> getTags() {
        return utxoAutoTags;
    }

    public void remove(String utxo) {
        utxoAutoTags.remove(utxo);
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

    public void addScore(String utxo, int score) {
        utxoScores.put(utxo, score);
    }

    public int getScore(String utxo) {
        if(utxoScores.containsKey(utxo))  {
            return utxoScores.get(utxo);
        }
        else    {
            return 0;
        }

    }

    public void incScore(String utxo, int score) {
        if(utxoScores.containsKey(utxo))  {
            utxoScores.put(utxo, utxoScores.get(utxo) + score);
        }
        else    {
            utxoScores.put(utxo, score);
        }

    }

    public HashMap<String,Integer> getScores() {
        return utxoScores;
    }

    public void removeScore(String utxo) {
        utxoScores.remove(utxo);
    }

    public JSONArray toJSON() {

        JSONArray utxos = new JSONArray();
        for (String key : utxoAutoTags.keySet()) {
            List<String> tags = utxoAutoTags.get(key);
            for(String t : tags) {
                JSONArray tag = new JSONArray();
                tag.put(key);
                tag.put(t);
                utxos.put(tag);
            }
        }

        return utxos;
    }

    public void fromJSON(JSONArray utxos) {
        try {
            for (int i = 0; i < utxos.length(); i++) {
                JSONArray tag = (JSONArray) utxos.get(i);

                if(utxoAutoTags.containsKey((String) tag.get(0))) {
                    utxoAutoTags.get((String) tag.get(0)).add((String) tag.get(1));
                }
                else     {
                    List<String> tags = new ArrayList<String>();
                    tags.add((String) tag.get(1));
                    utxoAutoTags.put((String) tag.get(0), tags);
                }

            }
        } catch (JSONException ex) {
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

    public JSONArray toJSON_scores() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoScores.keySet()) {
            JSONArray score = new JSONArray();
            score.put(key);
            score.put(utxoScores.get(key));
            utxos.put(score);
        }

        return utxos;
    }

    public void fromJSON_scores(JSONArray utxos) {
        try {
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray score = (JSONArray)utxos.get(i);
                utxoScores.put((String)score.get(0), (int)score.get(1));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static AddressTypes getAddressType(String address) {


        if (FormatsUtil.getInstance().isValidBech32(address)) {
            // is bech32: p2wpkh BIP84
            return AddressTypes.SEGWIT_NATIVE;
        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            // is P2SH wrapped segwit BIP49
            return AddressTypes.SEGWIT_COMPAT;
        } else {
            return AddressTypes.LEGACY;
        }
    }

}
