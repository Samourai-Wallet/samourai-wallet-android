package com.samourai.wallet.hd;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HD_Chain {

    private DeterministicKey cKey = null;
    private boolean isReceive;
    private String strPath = null;

    private ArrayList<HD_Address> mAddresses = null;
    
    private int addrIdx = 0;

    static private final int DESIRED_MARGIN = 32;
    static private final int ADDRESS_GAP_MAX = 20;

    private NetworkParameters mParams = null;
    
    private HD_Chain() { ; }

    public HD_Chain(NetworkParameters params, DeterministicKey aKey, boolean isReceive, int nbAddrs) {

        mParams = params;
        this.isReceive = isReceive;
        int chain = isReceive ? 0 : 1;
        cKey = HDKeyDerivation.deriveChildKey(aKey, chain);

        mAddresses = new ArrayList<HD_Address>();
        for(int i = 0; i < nbAddrs; i++) {
            mAddresses.add(new HD_Address(mParams, cKey, i));
        }

        strPath = cKey.getPath().toString();
    }

    public boolean isReceive() {
        return isReceive;
    }

    public HD_Address getAddressAt(int addrIdx) {
    	return new HD_Address(mParams, cKey, addrIdx);
    }

    public int length() {
        return mAddresses.size();
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

            JSONArray addresses = new JSONArray();
            for(HD_Address addr : mAddresses) {
                addresses.put(addr.toJSON());
            }
            obj.put("addresses", addresses);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
