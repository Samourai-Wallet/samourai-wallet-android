package com.samourai.wallet.JSONRPC;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static org.spongycastle.util.Arrays.reverse;

import android.util.Log;

public class PoW {

    private String strHash = null;
    private String strVersionHex = null;
    private String strPrevBlock = null;
    private String strMerkleRoot = null;
    private long ts = -1L;
    private String strTS = null;
    private String strBits = null;
    private long nonce = -1L;
    private String strNonce = null;
    private BigInteger target = null;

    private PoW()   { ; }

    public PoW(String hash)    {
        strHash = hash;
    }

    public String getHash() {
        return strHash;
    }

    public String getVersionHex() {
        return strVersionHex;
    }

    public String getPrevBlock() {
        return strPrevBlock;
    }

    public String getMerkleRoot() {
        return strMerkleRoot;
    }

    public long getTs() {
        return ts;
    }

    public String getBits() {
        return strBits;
    }

    public long getNonce() {
        return nonce;
    }

    public BigInteger getTarget()   {
        return target;
    }

    public String calcHash(JSONObject resultObj) {

        String hash = null;

        try {
            strVersionHex = Hex.toHexString(reverse(Hex.decode(resultObj.getString("versionHex"))));
            Log.i("PoW", "version:" + strVersionHex);
            strPrevBlock = Hex.toHexString(reverse(Hex.decode(resultObj.getString("previousblockhash"))));
            Log.i("PoW", "prev block:" + strPrevBlock);
            strMerkleRoot = Hex.toHexString(reverse(Hex.decode(resultObj.getString("merkleroot"))));
            Log.i("PoW", "merkle root:" + strMerkleRoot);

            ts = resultObj.getLong("time");
            strTS = Hex.toHexString(reverse(Hex.decode(Long.toHexString(ts))));
            Log.i("PoW", "timestamp:" + strTS);

            strBits = Hex.toHexString(reverse(Hex.decode(resultObj.getString("bits"))));
            Log.i("PoW", "bits:" + strBits);

            nonce = resultObj.getLong("nonce");
            strNonce = Hex.toHexString(reverse(Hex.decode(Long.toHexString(nonce))));
            Log.i("PoW", "nonce:" + strNonce);

            String strHeader = strVersionHex + strPrevBlock + strMerkleRoot + strTS + strBits + strNonce;

            byte[] buf = Hex.decode(strHeader);
            hash = Hex.toHexString(reverse(Sha256Hash.hashTwice(buf)));
            Log.i("PoW", "hash:" + hash);
        }
        catch(JSONException je) {
            return null;
        }

        return hash;
    }

    //
    // Prove the block was as difficult to make as it claims to be.
    // Check value of difficultyTarget to prevent and attack that might have us read a different chain.
    //
    public boolean check(JSONObject resultObj) {

        String hash = calcHash(resultObj);

        try {

            double dDifficulty = resultObj.getDouble("difficulty");
            Log.i("PoW", "difficulty:" + dDifficulty);

            long difficultyTarget = Long.parseLong(resultObj.getString("bits"), 16);
            target = Utils.decodeCompactBits(difficultyTarget);
            if (target.signum() <= 0 || target.compareTo(MainNetParams.get().getMaxTarget()) > 0) {
                Log.i("PoW", "invalid target");
                return false;
            }
            else if(new Sha256Hash(hash).toBigInteger().compareTo(target) > 0)    {
                Log.i("PoW", "hash is higher than target");
                Log.i("PoW", "target as integer:" + target.toString());
                Log.i("PoW", "hash as integer:" + new Sha256Hash(hash).toBigInteger().toString());
                return false;
            }
            else    {
                Log.i("PoW", "target as integer:" + target.toString());
                Log.i("PoW", "hash as integer:" + new Sha256Hash(hash).toBigInteger().toString());
                return true;
            }

        }
        catch(JSONException je) {
            return false;
        }

    }

}
