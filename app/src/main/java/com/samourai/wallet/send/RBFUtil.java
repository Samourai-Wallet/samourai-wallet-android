package com.samourai.wallet.send;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.HashMap;

public class RBFUtil {

    //
    // A transaction is considered to have opted in to allowing replacement of itself
    // if any of its inputs have an nSequence number less than (0xffffffff - 1)
    //
    public static final long RBF_THRESHOLD = BigInteger.valueOf(0xffffffffL).subtract(BigInteger.ONE).longValue();

    private static RBFUtil instance = null;
    private static HashMap<String,RBFSpend> rbfs = null;

    private RBFUtil() { ; }

    public static RBFUtil getInstance() {

        if(instance == null) {
            instance = new RBFUtil();
            rbfs = new HashMap<String,RBFSpend>();
        }

        return instance;
    }

    public RBFSpend get(String hash)    {
        return rbfs.get(hash);
    }

    public void add(RBFSpend rbf)    {
        rbfs.put(rbf.getHash(), rbf);
    }

    public void remove(RBFSpend rbf)   {
        if(rbf != null && rbfs.containsKey(rbf.getHash()))  {
            rbfs.remove(rbf.getHash());
        }
    }

    public void remove(String hash)    {
        if(hash != null && rbfs.containsKey(hash))  {
            rbfs.remove(hash);
        }
    }

    public boolean contains(String hash)   {
        return rbfs.containsKey(hash);
    }

    public void clear()    {
        rbfs.clear();
    }

    public JSONArray toJSON() {

        JSONArray array = new JSONArray();
        for(String hash : rbfs.keySet())   {
            array.put(rbfs.get(hash).toJSON());
        }

        return array;
    }

    public void fromJSON(JSONArray array) {

        try {

            for(int i = 0; i < array.length(); i++)   {
                RBFSpend rbf = new RBFSpend();
                rbf.fromJSON((JSONObject) array.get(i));
                rbfs.put(rbf.getHash(), rbf);
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
