package com.samourai.wallet.JSONRPC;

import android.util.Log;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static org.spongycastle.util.Arrays.reverse;

public class BlockUtil {

    private static BlockUtil instance = null;

    private BlockUtil() { ; }

    public static BlockUtil getInstance()   {

        if(instance == null)    {
            instance = new BlockUtil();
        }

        return instance;
    }

    public String getHash(JSONObject resultObj) {

        String hash = null;

        try {
            String strVersionHex = Hex.toHexString(reverse(Hex.decode(resultObj.getString("versionHex"))));
            Log.i("BlockUtil", "version:" + strVersionHex);
            String strPrevBlock = Hex.toHexString(reverse(Hex.decode(resultObj.getString("previousblockhash"))));
            Log.i("BlockUtil", "prev block:" + strPrevBlock);
            String strMerkleRoot = Hex.toHexString(reverse(Hex.decode(resultObj.getString("merkleroot"))));
            Log.i("BlockUtil", "merkle root:" + strMerkleRoot);

            long ts = resultObj.getLong("time");
            String strTS = Hex.toHexString(reverse(Hex.decode(Long.toHexString(ts))));
            Log.i("BlockUtil", "timestamp:" + strTS);

            String strBits = Hex.toHexString(reverse(Hex.decode(resultObj.getString("bits"))));
            Log.i("BlockUtil", "bits:" + strBits);

            long nonce = resultObj.getLong("nonce");
            String strNonce = Hex.toHexString(reverse(Hex.decode(Long.toHexString(nonce))));
            Log.i("BlockUtil", "nonce:" + strNonce);

            String strHeader = strVersionHex + strPrevBlock + strMerkleRoot + strTS + strBits + strNonce;

            byte[] buf = Hex.decode(strHeader);
            hash = Hex.toHexString(reverse(Sha256Hash.hashTwice(buf)));
            Log.i("BlockUtil", "hash:" + hash);
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
    public boolean checkPoW(JSONObject resultObj) {

        String hash = getHash(resultObj);

        try {

            double dDifficulty = resultObj.getDouble("difficulty");
            Log.i("BlockUtil", "difficulty:" + dDifficulty);

            long difficultyTarget = Long.parseLong(resultObj.getString("bits"), 16);
            BigInteger target = Utils.decodeCompactBits(difficultyTarget);
            if (target.signum() <= 0 || target.compareTo(MainNetParams.get().getMaxTarget()) > 0) {
                Log.i("BlockUtil", "invalid target");
                return false;
            }
            else if(new Sha256Hash(hash).toBigInteger().compareTo(target) > 0)    {
                Log.i("BlockUtil", "hash is higher than target");
                Log.i("BlockUtil", "target as integer:" + target.toString());
                Log.i("BlockUtil", "hash as integer:" + new Sha256Hash(hash).toBigInteger().toString());
                return false;
            }
            else    {
                Log.i("BlockUtil", "target as integer:" + target.toString());
                Log.i("BlockUtil", "hash as integer:" + new Sha256Hash(hash).toBigInteger().toString());
                return true;
            }

        }
        catch(JSONException je) {
            return false;
        }

    }

}
