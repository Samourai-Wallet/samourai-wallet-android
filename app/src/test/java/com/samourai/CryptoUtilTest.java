package com.samourai;

import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.crypto.impl.ECDHKeySet;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import org.bitcoinj.core.ECKey;
import org.junit.Assert;
import org.junit.Test;

import java.security.Security;


public class CryptoUtilTest {
  private CryptoUtil cryptoUtil = CryptoUtil.getInstance(new BouncyCastleProvider());
  static {
    Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
  }

  @Test
  public void encryptDecrypt() throws Exception {
    doEncryptDecrypt("encryption test ...", new ECKey(), new ECKey());
  }

  private void doEncryptDecrypt(String data, ECKey keySender, ECKey keyReceiver) throws Exception {
    // encrypt
    ECDHKeySet ecdhKeySet1 = cryptoUtil.getSharedSecret(keySender, keyReceiver);
    byte[] encrypted = cryptoUtil.encrypt(data, ecdhKeySet1);

    // decrypt
    ECDHKeySet ecdhKeySet2 = cryptoUtil.getSharedSecret(keyReceiver, keySender);
    String decrypted = cryptoUtil.decryptString(encrypted, ecdhKeySet2);

    Assert.assertEquals(data, decrypted);
  }

  @Test
  public void createSignatureVerify() throws Exception {
    doCreateSignatureVerify("signature test ...".getBytes(), new ECKey());
  }

  private void doCreateSignatureVerify(byte[] data, ECKey key) throws Exception {
    // sign
    byte[] signature = cryptoUtil.createSignature(key, data);

    // verify
    Assert.assertTrue(cryptoUtil.verifySignature(key, data, signature));
    Assert.assertFalse(cryptoUtil.verifySignature(key, "wrong data".getBytes(), signature));
    Assert.assertFalse(cryptoUtil.verifySignature(key, data, "wrong signature".getBytes()));
    Assert.assertFalse(cryptoUtil.verifySignature(new ECKey(), data, signature));
  }
}
