package com.samourai.wallet.hd;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

public class HD_Account {

    protected DeterministicKey aKey = null;
    private String strLabel = null;
    protected int	mAID;
    private boolean isArchived = false;

    private HD_Chain mReceive = null;
    private HD_Chain mChange = null;

    protected String strXPUB = null;

    protected NetworkParameters mParams = null;

    protected HD_Account() { ; }

    public HD_Account(NetworkParameters params, DeterministicKey mKey, String label, int child) {

        mParams = params;
        strLabel = label;
        mAID = child;

        // L0PRV & STDVx: private derivation.
        int childnum = child;
        childnum |= ChildNumber.HARDENED_BIT;
        aKey = HDKeyDerivation.deriveChildKey(mKey, childnum);

        strXPUB = aKey.serializePubB58(MainNetParams.get());

        mReceive = new HD_Chain(mParams, aKey, true, 1);
        mChange = new HD_Chain(mParams, aKey, false, 1);

    }

    public HD_Account(NetworkParameters params, String xpub, String label, int child) throws AddressFormatException {

        mParams = params;
        strLabel = label;
        mAID = child;

        // assign master key to account key
        aKey = createMasterPubKeyFromXPub(xpub);

        strXPUB = xpub;

        mReceive = new HD_Chain(mParams, aKey, true, 10);
        mChange = new HD_Chain(mParams, aKey, false, 10);

    }

    protected DeterministicKey createMasterPubKeyFromXPub(String xpubstr) throws AddressFormatException {

        byte[] xpubBytes = Base58.decodeChecked(xpubstr);

        ByteBuffer bb = ByteBuffer.wrap(xpubBytes);
        if(bb.getInt() != 0x0488B21E)   {
            throw new AddressFormatException("invalid xpub version");
        }

        byte[] chain = new byte[32];
        byte[] pub = new byte[33];
        // depth:
        bb.get();
        // parent fingerprint:
        bb.getInt();
        // child no.
        bb.getInt();
        bb.get(chain);
        bb.get(pub);

        return HDKeyDerivation.createMasterPubKeyFromBytes(pub, chain);
    }

    public String xpubstr() {

        return strXPUB;

    }

    public String getLabel() {
        return strLabel;
    }

    public void setLabel(String label) {
        strLabel = label;
    }

    public int getId() {
        return mAID;
    }

    public HD_Chain getReceive() {
        return mReceive;
    }

    public HD_Chain getChange() {
        return mChange;
    }

    public HD_Chain getChain(int idx) {
        return (idx == 0) ? mReceive : mChange;
    }

    public int size() {
        return mReceive.length() + mChange.length();
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("xpub", xpubstr());
            obj.put("receiveIdx", getReceive().getAddrIdx());
            obj.put("changeIdx", getChange().getAddrIdx());
            obj.put("id", mAID);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
}
