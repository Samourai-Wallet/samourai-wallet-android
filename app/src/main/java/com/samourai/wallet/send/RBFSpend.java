package com.samourai.wallet.send;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RBFSpend    {

    private String strHash = null;
    private String strPrevHash = null;
    private List<String> changeAddrs = null;
    private String strSerializedTx = null;
    private HashMap<String,String> keyBag = null;

    public RBFSpend(String strHash, List<String> changeAddrs, String strSerializedTx, HashMap<String,String> keyBag)  {
        this.strHash = strHash;
        this.changeAddrs = changeAddrs;
        this.strSerializedTx = strSerializedTx;
        this.keyBag = keyBag;
    }

    public RBFSpend()  {
        changeAddrs = new ArrayList<String>();
        keyBag = new HashMap<String,String>();
    }

    public String getHash() {
        return strHash;
    }

    public void setHash(String strHash) {
        this.strHash = strHash;
    }

    public String getPrevHash() {
        return strPrevHash;
    }

    public void setPrevHash(String strPrevHash) {
        this.strPrevHash = strPrevHash;
    }

    public List<String> getChangeAddrs() {
        return changeAddrs;
    }

    public void setChangeAddrs(List<String> changeAddrs) {
        this.changeAddrs = changeAddrs;
    }

    public void addChangeAddr(String addr)   {
        changeAddrs.add(addr);
    }

    public boolean containsChangeAddr(String addr)   {
        return changeAddrs.contains(addr);
    }

    public void addKey(String outpoint, String path)   {
        keyBag.put(outpoint, path);
    }

    public boolean containsKey(String outpoint)   {
        return keyBag.containsKey(outpoint);
    }

    public String getSerializedTx() {
        return strSerializedTx;
    }

    public void setSerializedTx(String strSerializedTx) {
        this.strSerializedTx = strSerializedTx;
    }

    public HashMap<String,String> getKeyBag() {
        return keyBag;
    }

    public void setKeyBag(HashMap<String,String> keyBag) {
        this.keyBag = keyBag;
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();
        JSONArray array = null;
        try {
            array = new JSONArray();

            for(String addr : changeAddrs)   {
                array.put(addr);
            }

            jsonPayload.put("change_addresses", array);

            array = new JSONArray();

            for(String outpoint : keyBag.keySet())   {
                JSONObject jobj = new JSONObject();
                jobj.put("outpoint", outpoint);
                jobj.put("path", keyBag.get(outpoint));
                array.put(jobj);
            }

            jsonPayload.put("key_bag", array);

            if(strHash != null)    {
                jsonPayload.put("hash", strHash);
            }

            if(strPrevHash != null)    {
                jsonPayload.put("prev_hash", strPrevHash);
            }

            if(strSerializedTx != null)    {
                jsonPayload.put("tx", strSerializedTx);
            }

        }
        catch(JSONException je) {
            ;
        }

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

        try {

            if(jsonPayload.has("change_addresses"))    {

                JSONArray array = jsonPayload.getJSONArray("change_addresses");

                for(int i = 0; i < array.length(); i++)   {
                    changeAddrs.add((String)array.get(i));
                }

            }

            if(jsonPayload.has("key_bag"))    {

                JSONArray array = jsonPayload.getJSONArray("key_bag");

                for(int i = 0; i < array.length(); i++)   {
                    JSONObject jobj = (JSONObject) array.get(i);
                    keyBag.put(jobj.getString("outpoint"), jobj.getString("path"));
                }

            }

            if(jsonPayload.has("hash"))    {
                strHash = jsonPayload.getString("hash");
            }

            if(jsonPayload.has("prev_hash"))    {
                strPrevHash = jsonPayload.getString("prev_hash");
            }

            if(jsonPayload.has("tx"))    {
                strSerializedTx = jsonPayload.getString("tx");
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
