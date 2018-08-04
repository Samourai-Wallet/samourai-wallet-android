package com.samourai.wallet.hd;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.util.FormatsUtil;

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
    protected int mAID;
    private boolean isArchived = false;

    private HD_Chain mReceive = null;
    private HD_Chain mChange = null;

    protected String strXPUB = null;
    protected String strYPUB = null;
    protected String strZPUB = null;

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

        strXPUB = aKey.serializePubB58(SamouraiWallet.getInstance().getCurrentNetworkParams());
        strYPUB = aKey.serializePubB58(SamouraiWallet.getInstance().getCurrentNetworkParams(), 49);
        strZPUB = aKey.serializePubB58(SamouraiWallet.getInstance().getCurrentNetworkParams(), 84);

        mReceive = new HD_Chain(mParams, aKey, true);
        mChange = new HD_Chain(mParams, aKey, false);

    }

    public HD_Account(NetworkParameters params, String xpub, String label, int child) throws AddressFormatException {

        mParams = params;
        strLabel = label;
        mAID = child;

        // assign master key to account key
        aKey = createMasterPubKeyFromXPub(xpub);

        strXPUB = strYPUB = strZPUB = xpub;

        mReceive = new HD_Chain(mParams, aKey, true);
        mChange = new HD_Chain(mParams, aKey, false);

    }

    protected DeterministicKey createMasterPubKeyFromXPub(String xpubstr) throws AddressFormatException {

        byte[] xpubBytes = Base58.decodeChecked(xpubstr);

        ByteBuffer bb = ByteBuffer.wrap(xpubBytes);
        int version = bb.getInt();
        if(version != FormatsUtil.MAGIC_XPUB && version != FormatsUtil.MAGIC_TPUB && version != FormatsUtil.MAGIC_YPUB && version != FormatsUtil.MAGIC_UPUB && version != FormatsUtil.MAGIC_ZPUB && version != FormatsUtil.MAGIC_VPUB)   {
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

    public String ypubstr() {

        return strYPUB;

    }

    public String zpubstr() {

        return strZPUB;

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

    public JSONObject toJSON(int purpose) {
        try {
            JSONObject obj = new JSONObject();

            switch(purpose)    {
                case 49:
                    obj.put("ypub", ypubstr());
                    break;
                case 84:
                    obj.put("zpub", zpubstr());
                    break;
                // assume purpose == 44
                default:
                    obj.put("xpub", xpubstr());
                    break;
            }

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
