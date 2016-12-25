package com.samourai.wallet.hd;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HD_Chain {

    private DeterministicKey cKey = null;
    private boolean isReceive;
    private String strPath = null;

    private int addrIdx = 0;

    private NetworkParameters mParams = null;
    
    private HD_Chain() { ; }

    public HD_Chain(NetworkParameters params, DeterministicKey aKey, boolean isReceive) {

        mParams = params;
        this.isReceive = isReceive;
        int chain = isReceive ? 0 : 1;
        cKey = HDKeyDerivation.deriveChildKey(aKey, chain);

        strPath = cKey.getPath().toString();
    }

    public boolean isReceive() {
        return isReceive;
    }

    public HD_Address getAddressAt(int addrIdx) {
    	return new HD_Address(mParams, cKey, addrIdx);
    }

    public int getAddrIdx() {
        return addrIdx;
    }

    public void setAddrIdx(int idx) {
        addrIdx = idx;
    }

    public void incAddrIdx() {
        addrIdx++;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("path", strPath);
            obj.put("idx", addrIdx);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
