package com.samourai.wallet.whirlpool;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class Tx0DisplayUtil {

    private static int PRUNE_LIMIT = 25;

    private static List<String> seen = null;

    private static Tx0DisplayUtil instance = null;

    private Tx0DisplayUtil() {
        ;
    }

    public static Tx0DisplayUtil getInstance() {

        if(instance == null) {
            seen = new ArrayList<String>();
            instance = new Tx0DisplayUtil();
        }

        return instance;
    }

    public boolean contains(String hash) {
        return seen.contains(hash);
    }

    public void add(String hash) {
        if(!seen.contains(hash)) {
            seen.add(hash);
        }
    }

    public void clear() {
        seen.clear();
    }

    public JSONArray toJSON() {

        JSONArray hashes = new JSONArray();
        for (String hash : seen) {
            hashes.put(hash);
        }

        return hashes;
    }

    public void fromJSON(JSONArray hashes) {
        try {
            for (int i = 0; i < hashes.length(); i++) {
                seen.add(hashes.getString(i));
            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }

        if(seen.size() > PRUNE_LIMIT) {
            seen = seen.subList(seen.size() - PRUNE_LIMIT, seen.size() - 1);
        }

    }

}
