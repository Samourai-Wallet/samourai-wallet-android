package com.samourai.wallet.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TxAuxUtil {

    private static TxAuxUtil instance = null;
    private static HashMap<String,Tx> txs = null;

    private TxAuxUtil() { ; }

    public static TxAuxUtil getInstance() {

        if(instance == null) {
            instance = new TxAuxUtil();
            txs = new HashMap<String,Tx>();
        }

        return instance;
    }

    public void reset()  {
        txs.clear();
    }

    public HashMap<String,Tx> getTxs() {
        return txs;
    }

    public Tx get(String hash) {
        return txs.get(hash);
    }

    public void put(Tx tx)  {
        txs.put(tx.getHash(), tx);
    }

    public void prune() {
        List<Tx> _txs = new ArrayList<Tx>();
        _txs.addAll(txs.values());
        Collections.sort(_txs, new APIFactory.TxMostRecentDateComparator());
        if(_txs.size() > 50)    {
            _txs = new ArrayList(_txs.subList(0, 50));
            txs.clear();
            for(Tx tx : _txs)   {
                txs.put(tx.getHash(), tx);
            }
        }
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();
        JSONArray array = null;
        try {
            array = new JSONArray();

            for(String hash : txs.keySet())   {
                array.put(txs.get(hash).toJSON());
            }

            jsonPayload.put("auxTx", array);

        }
        catch(JSONException je) {
            ;
        }

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

        try {

            if(jsonPayload.has("auxTx"))    {

                JSONArray array = jsonPayload.getJSONArray("auxTx");

                for(int i = 0; i < array.length(); i++)   {
                    Tx tx = new Tx((JSONObject) array.get(i));
                    txs.put(tx.getHash(), tx);
                }

            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
