package com.samourai.wallet.cahoots;

import com.samourai.wallet.cahoots.psbt.PSBT;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;

public class Cahoots {

    public static final int CAHOOTS_STONEWALLx2 = 0;
    public static final int CAHOOTS_STOWAWAY = 1;

    protected int version = 1;
    protected long ts = -1L;
    protected String strID = null;
    protected int type = -1;
    protected int step = -1;
    protected PSBT psbt = null;
    protected long spendAmount = 0L;
    protected HashMap<String,Long> outpoints = null;
    protected NetworkParameters params = null;

    public Cahoots()    { outpoints = new HashMap<String,Long>(); }

    protected Cahoots(Cahoots c)    {
        this.version = c.getVersion();
        this.ts = c.getTS();
        this.strID = c.getID();
        this.type = c.getType();
        this.step = c.getStep();
        this.psbt = c.getPSBT();
        this.spendAmount = c.spendAmount;
        this.outpoints = c.outpoints;
        this.params = c.getParams();
    }

    protected Cahoots(long spendAmount, int type, NetworkParameters params)    {
        this.ts = System.currentTimeMillis() / 1000L;
        this.strID = Hex.toHexString(Sha256Hash.hash(BigInteger.valueOf(new SecureRandom().nextLong()).toByteArray()));
        this.type = type;
        this.step = 0;
        this.spendAmount = spendAmount;
        this.outpoints = new HashMap<String,Long>();
        this.params = params;
    }

    public int getVersion() {
        return version;
    }

    public long getTS() { return ts; }

    public String getID() {
        return strID;
    }

    public int getType() {
        return type;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public PSBT getPSBT() {
        return psbt;
    }

    public Transaction getTransaction() {
        return psbt.getTransaction();
    }

    public long getSpendAmount() {
        return spendAmount;
    }

    public HashMap<String, Long> getOutpoints() {
        return outpoints;
    }

    public void setOutpoints(HashMap<String, Long> outpoints) {
        this.outpoints = outpoints;
    }

    public NetworkParameters getParams() {
        return params;
    }

    public static boolean isCahoots(JSONObject obj)   {
        try {
            return obj.has("cahoots") && obj.getJSONObject("cahoots").has("type");
        }
        catch(JSONException je) {
            return false;
        }
    }

    public static boolean isCahoots(String s)   {
        try {
            JSONObject obj = new JSONObject(s);
            return isCahoots(obj);
        }
        catch(JSONException je) {
            return false;
        }
    }

    public JSONObject toJSON() {

        JSONObject cObj = new JSONObject();

        try {
            JSONObject obj = new JSONObject();

            obj.put("version", version);
            obj.put("ts", ts);
            obj.put("id", strID);
            obj.put("type", type);
            obj.put("step", step);
            obj.put("psbt", psbt == null ? "" : psbt.toZ85String());
            obj.put("spend_amount", spendAmount);
            JSONArray _outpoints = new JSONArray();
            for(String outpoint : outpoints.keySet())   {
                JSONObject entry = new JSONObject();
                entry.put("outpoint", outpoint);
                entry.put("value", outpoints.get(outpoint));
                _outpoints.put(entry);
            }
            obj.put("outpoints", _outpoints);
            obj.put("params", params instanceof TestNet3Params ? "testnet" : "mainnet");

            cObj.put("cahoots", obj);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }

        return cObj;
    }

    public void fromJSON(JSONObject cObj) {

        try {
            JSONObject obj = null;
            if(cObj.has("cahoots"))    {
                obj = cObj.getJSONObject("cahoots");
            }
            if(obj != null && obj.has("type") && obj.has("step") && obj.has("psbt") && obj.has("ts") && obj.has("id") && obj.has("version") && obj.has("spend_amount") && obj.has("params"))    {
                this.params = obj.getString("params").equals("testnet") ? TestNet3Params.get() : MainNetParams.get();
                this.version = obj.getInt("version");
                this.ts = obj.getLong("ts");
                this.strID = obj.getString("id");
                this.type = obj.getInt("type");
                this.step = obj.getInt("step");
                /*
                this.psbt = obj.getString("psbt").equals("") ? null : new PSBT(obj.getString("psbt"), params);
                if(this.psbt != null)    {
                    this.psbt.read();
                }
                */
                this.spendAmount = obj.getLong("spend_amount");
                JSONArray _outpoints = obj.getJSONArray("outpoints");
                for(int i = 0; i < _outpoints.length(); i++)   {
                    JSONObject entry = _outpoints.getJSONObject(i);
                    outpoints.put(entry.getString("outpoint"), entry.getLong("value"));
                }
                this.psbt = obj.getString("psbt").equals("") ? null : new PSBT(obj.getString("psbt"), params);
                if(this.psbt != null)    {
                    this.psbt.read();
                }
            }
        }
        catch(Exception e) {
//            throw new RuntimeException(e);
            ;
        }
    }

}
