package com.samourai.wallet.bip47.rpc;

import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPoint;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class AndroidSecretPointFactory implements ISecretPointFactory {
    private static AndroidSecretPointFactory instance;

    public static AndroidSecretPointFactory getInstance() {
        if (instance == null) {
            instance = new AndroidSecretPointFactory();
        }
        return instance;
    }

    @Override
    public ISecretPoint newSecretPoint(byte[] dataPrv, byte[] dataPub) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException {
        return new SecretPoint(dataPrv, dataPub);
    }

}
