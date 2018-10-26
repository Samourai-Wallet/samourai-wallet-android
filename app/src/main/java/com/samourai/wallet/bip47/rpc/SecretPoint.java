package com.samourai.wallet.bip47.rpc;

import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPoint;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class SecretPoint implements ISecretPoint {

    private PrivateKey privKey = null;
    private PublicKey pubKey = null;

    private KeyFactory kf = null;

    private static final ECParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
    private static final String KEY_PROVIDER = "SC"; // Android: use spongycastle

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    private SecretPoint()    { ; }

    public SecretPoint(byte[] dataPrv, byte[] dataPub) throws InvalidKeySpecException, InvalidKeyException, IllegalStateException, NoSuchAlgorithmException, NoSuchProviderException {
        kf = KeyFactory.getInstance("ECDH", KEY_PROVIDER);
        privKey = loadPrivateKey(dataPrv);
        pubKey = loadPublicKey(dataPub);
    }

    public byte[] ECDHSecretAsBytes() throws InvalidKeyException, IllegalStateException, NoSuchAlgorithmException, NoSuchProviderException    {
        return ECDHSecret().getEncoded();
    }

    private SecretKey ECDHSecret() throws InvalidKeyException, IllegalStateException, NoSuchAlgorithmException, NoSuchProviderException    {

        KeyAgreement ka = KeyAgreement.getInstance("ECDH", KEY_PROVIDER);
        ka.init(privKey);
        ka.doPhase(pubKey, true);
        SecretKey secret = ka.generateSecret("AES");

        return secret;
    }

    private PublicKey loadPublicKey(byte[] data) throws InvalidKeySpecException    {
        ECPublicKeySpec pubKey = new ECPublicKeySpec(params.getCurve().decodePoint(data), params);
        return kf.generatePublic(pubKey);
    }

    private PrivateKey loadPrivateKey(byte[] data) throws InvalidKeySpecException  {
        ECPrivateKeySpec prvkey = new ECPrivateKeySpec(new BigInteger(1, data), params);
        return kf.generatePrivate(prvkey);
    }

}
