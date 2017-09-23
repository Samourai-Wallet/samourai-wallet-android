package com.samourai.wallet.segwit;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.script.Script;

import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class P2SH_P2WPKH {

    private ECKey ecKey = null;
    private List<ECKey> keys = null;
    private NetworkParameters params = null;

    private P2SH_P2WPKH()   { ; }

    public P2SH_P2WPKH(NetworkParameters params) {
        this.params = params;
        keys = new ArrayList<ECKey>();
    }

    public P2SH_P2WPKH(ECKey ecKey, NetworkParameters params) {
        this.ecKey = ecKey;
        this.params = params;
        keys = new ArrayList<ECKey>();
    }

    //
    // use only compressed public keys for SegWit
    //
    public P2SH_P2WPKH(byte[] pubkey, NetworkParameters params) {
        this.ecKey = ECKey.fromPublicOnly(pubkey);
        this.params = params;
        keys = new ArrayList<ECKey>();
    }

    public ECKey getECKey() {
        return ecKey;
    }

    public void setECKey(ECKey ecKey) {
        this.ecKey = ecKey;
    }

    public List<ECKey> getECKeys() {
        return keys;
    }

    public void setECKeys(List<ECKey> keys) {
        this.keys = keys;
    }

    public Address segWitAddress()    {

        return Address.fromP2SHScript(params, segWitOutputScript());

    }

    public String getAddressAsString()    {

        return segWitAddress().toString();

    }

    public Script segWitOutputScript()    {

        //
        // OP_HASH160 hash160(redeemScript) OP_EQUAL
        //
        byte[] hash = getRIPEMD160(getSHA("SHA-256", segWitRedeemScript().getProgram()));
        byte[] buf = new byte[3 + hash.length];
        buf[0] = (byte)0xa9;    // HASH160
        buf[1] = (byte)0x14;    // push 20 bytes
        System.arraycopy(hash, 0, buf, 2, hash.length); // keyhash
        buf[22] = (byte)0x87;   // OP_EQUAL

        return new Script(buf);
    }

    public Script segWitRedeemScript()    {

        //
        // The P2SH segwit redeemScript is always 22 bytes. It starts with a OP_0, followed by a canonical push of the keyhash (i.e. 0x0014{20-byte keyhash})
        //
        byte[] hash = getRIPEMD160(getSHA("SHA-256", ecKey.getPubKey()));
        byte[] buf = new byte[2 + hash.length];
        buf[0] = (byte)0x00;  // OP_0
        buf[1] = (byte)0x14;  // push 20 bytes
        System.arraycopy(hash, 0, buf, 2, hash.length); // keyhash

        return new Script(buf);
    }

    private boolean hasPrivKey() {

        if(ecKey != null && ecKey.hasPrivKey())    {
            return true;
        }
        else    {
            return false;
        }

    }

    private byte[] getRIPEMD160(byte[] data) {

        byte[] out = null;

        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(data, 0, data.length);
        out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);

        return out;

    }

    private byte[] getSHA(String algo, byte[] data) {

        byte[] out = null;

        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            md.update(data);
            out = md.digest();
        }
        catch(NoSuchAlgorithmException nsae) {
            ;
        }

        return out;

    }

}
