package com.samourai.wallet.cahoots;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CahootsFactory {

    private static CahootsFactory instance = null;

    private static List<Cahoots> cahoots = null;

    private CahootsFactory() { ; }

    public static CahootsFactory getInstance() {

        if(instance == null) {
            cahoots = new ArrayList<Cahoots>();
            instance = new CahootsFactory();
        }

        return instance;
    }

    public void add(Cahoots c) {
        cahoots.add(c);
    }

    public void clear() {
        cahoots.clear();
    }

    public List<Cahoots> getCahoots() {
        return cahoots;
    }

    public JSONArray toJSON() {

        JSONArray array = new JSONArray();
        for(Cahoots c : cahoots) {
            JSONObject obj = c.toJSON();
            array.put(obj);
        }

        return array;
    }

    public void fromJSON(JSONArray array) {

        cahoots.clear();

        try {
            for(int i = 0; i < array.length(); i++) {
                Cahoots c = new Cahoots();
                c.fromJSON(array.getJSONObject(i));
                cahoots.add(c);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
