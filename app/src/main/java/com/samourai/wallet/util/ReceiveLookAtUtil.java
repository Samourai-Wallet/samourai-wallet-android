package com.samourai.wallet.util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ReceiveLookAtUtil {

    private static final int LOOKBEHIND_LENGTH = 10;

    private static ReceiveLookAtUtil instance = null;

    private static List<String> receiveAddresses = null;

    private ReceiveLookAtUtil() { ; }

    public static ReceiveLookAtUtil getInstance() {

        if(instance == null) {
            receiveAddresses = new ArrayList<String>();
            instance = new ReceiveLookAtUtil();
        }

        return instance;
    }

    public void add(String addr)    {
        if(!receiveAddresses.contains(addr))    {
            receiveAddresses.add(addr);
        }
    }

    public boolean contains(String addr)   {
        return receiveAddresses.contains(addr);
    }

    public int size()   {
        return receiveAddresses.size();
    }

    public List<String> getReceives()    {
        return receiveAddresses;
    }

    public JSONArray toJSON() {

        if(receiveAddresses.size() > LOOKBEHIND_LENGTH)    {
            receiveAddresses = receiveAddresses.subList(receiveAddresses.size() - LOOKBEHIND_LENGTH, receiveAddresses.size());
        }

        JSONArray receives = new JSONArray();
        for(String addr : receiveAddresses) {
            receives.put(addr);
        }

        return receives;
    }

    public void fromJSON(JSONArray receives) {
        try {
            for(int i = 0; i < receives.length(); i++) {
                receiveAddresses.add((String)receives.get(i));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
