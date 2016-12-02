package com.samourai.wallet.util;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.MainNetParams;

import java.security.SignatureException;

public class MessageSignUtil {

    private static MessageSignUtil instance = null;

    private MessageSignUtil() { ; }

    public static MessageSignUtil getInstance() {

        if(instance == null) {
            instance = new MessageSignUtil();
        }

        return instance;
    }

    public String signMessage(ECKey key, String strMessage) {

        if(key == null || strMessage == null || !key.hasPrivKey())    {
            return null;
        }

        return key.signMessage(strMessage);
    }

    public String signMessageArmored(ECKey key, String strMessage) {

        String sig = signMessage(key, strMessage);
        String ret = null;

        if(sig != null)    {
            ret = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n";
            ret += strMessage;
            ret += "\n";
            ret += "-----BEGIN BITCOIN SIGNATURE-----\n";
            ret += "Version: Bitcoin-qt (1.0)\n";
            ret += "Address: " + key.toAddress(MainNetParams.get()).toString() + "\n\n";
            ret += sig;
            ret += "\n";
            ret += "-----END BITCOIN SIGNATURE-----\n";
        }

        return ret;
    }

    public ECKey signedMessageToKey(String strMessage, String strSignature) throws SignatureException {

        if(strMessage == null || strSignature == null)    {
            return null;
        }

        return ECKey.signedMessageToKey(strMessage, strSignature);
    }

    public boolean verifyMessage(ECKey key, String strMessage, String strSignature) throws SignatureException {

        if(key == null || strMessage == null || strSignature == null)    {
            return false;
        }

        return signedMessageToKey(strMessage, strSignature).getPublicKeyAsHex().equals(key.getPublicKeyAsHex());
    }

    public ECKey.ECDSASignature signMessageECDSA(ECKey key, byte[] hash) {

        if(key == null || hash == null)    {
            return null;
        }

        ECKey.ECDSASignature sig = null;

        /*
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(strMessage.getBytes());
            sig = key.sign(Sha256Hash.of(hash));
        }
        catch(NoSuchAlgorithmException nsae) {
            return null;
        }
        */

        sig = key.sign(Sha256Hash.of(hash));

        return sig;
    }

    public boolean verifyMessageECDSA(ECKey key, ECKey.ECDSASignature sig, byte[] hash) {

        if(key == null || sig == null || hash == null)    {
            return false;
        }

        return key.verify(Sha256Hash.of(hash), sig);
    }

}
