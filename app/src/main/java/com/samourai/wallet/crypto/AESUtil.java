package com.samourai.wallet.crypto;

import com.samourai.wallet.util.CharSequenceX;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import javax.annotation.Nullable;

public class AESUtil {

//    private static Logger mLogger = LoggerFactory.getLogger(AESUtil.class);

    public static final int DefaultPBKDF2Iterations = 5000;

    public static final int MODE_CBC = 0;
    public static final int MODE_OFB = 1;

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);
        return range;
    }

    // AES 256 PBKDF2 CBC iso10126 decryption
    // 16 byte IV must be prepended to ciphertext - Compatible with crypto-js
    public static String decrypt(String ciphertext, CharSequenceX password, int iterations) throws UnsupportedEncodingException, InvalidCipherTextException, DecryptionException {

        return decryptWithSetMode(ciphertext, password, iterations, MODE_CBC, new ISO10126d2Padding());
    }

    public static String decryptWithSetMode(String ciphertext, CharSequenceX password, int iterations, int mode, @Nullable BlockCipherPadding padding) throws InvalidCipherTextException, UnsupportedEncodingException, DecryptionException {
        final int AESBlockSize = 4;

        byte[] cipherdata = Base64.decodeBase64(ciphertext.getBytes());

        //Separate the IV and cipher data
        byte[] iv = copyOfRange(cipherdata, 0, AESBlockSize * 4);
        byte[] input = copyOfRange(cipherdata, AESBlockSize * 4, cipherdata.length);

        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toString().toCharArray()), iv, iterations);
        KeyParameter keyParam = (KeyParameter) generator.generateDerivedParameters(256);

        CipherParameters params = new ParametersWithIV(keyParam, iv);

        BlockCipher cipherMode;
        if (mode == MODE_CBC) {
            cipherMode = new CBCBlockCipher(new AESEngine());

        } else {
            //mode == MODE_OFB
            cipherMode = new OFBBlockCipher(new AESEngine(), 128);
        }

        BufferedBlockCipher cipher;
        if (padding != null) {
            cipher = new PaddedBufferedBlockCipher(cipherMode, padding);
        } else {
            cipher = new BufferedBlockCipher(cipherMode);
        }

        cipher.reset();
        cipher.init(false, params);

        // create a temporary buffer to decode into (includes padding)
        byte[] buf = new byte[cipher.getOutputSize(input.length)];
        int len = cipher.processBytes(input, 0, input.length, buf, 0);
        len += cipher.doFinal(buf, len);

        // remove padding
        byte[] out = new byte[len];
        System.arraycopy(buf, 0, out, 0, len);

        // return string representation of decoded bytes
        String result = new String(out, "UTF-8");
        if (result.isEmpty()) {
            throw new DecryptionException("Decrypted string is empty.");
        }

        return result;
    }

    // AES 256 PBKDF2 CBC iso10126 encryption
    public static String encrypt(String cleartext, CharSequenceX password, int iterations) throws DecryptionException, UnsupportedEncodingException {

        return encryptWithSetMode(cleartext, password, iterations, MODE_CBC, new ISO10126d2Padding());
    }

    public static String encryptWithSetMode(String cleartext, CharSequenceX password, int iterations, int mode, @Nullable BlockCipherPadding padding) throws DecryptionException, UnsupportedEncodingException {

        final int AESBlockSize = 4;

        if (password == null) {
            throw new DecryptionException("Password null");
        }

        // Use secure random to generate a 16 byte iv
        SecureRandom random = new SecureRandom();
        byte iv[] = new byte[AESBlockSize * 4];
        random.nextBytes(iv);

        byte[] clearbytes = cleartext.getBytes("UTF-8");

        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toString().toCharArray()), iv, iterations);
        KeyParameter keyParam = (KeyParameter) generator.generateDerivedParameters(256);

        CipherParameters params = new ParametersWithIV(keyParam, iv);

        BlockCipher cipherMode;
        if (mode == MODE_CBC) {
            cipherMode = new CBCBlockCipher(new AESEngine());

        } else {
            //mode == MODE_OFB
            cipherMode = new OFBBlockCipher(new AESEngine(), 128);
        }

        BufferedBlockCipher cipher;
        if (padding != null) {
            cipher = new PaddedBufferedBlockCipher(cipherMode, padding);
        } else {
            cipher = new BufferedBlockCipher(cipherMode);
        }

        cipher.reset();
        cipher.init(true, params);

        byte[] outBuf = cipherData(cipher, clearbytes);

        // Append to IV to the output
        int len1 = iv.length;
        int len2 = outBuf.length;
        byte[] ivAppended = new byte[len1 + len2];
        System.arraycopy(iv, 0, ivAppended, 0, len1);
        System.arraycopy(outBuf, 0, ivAppended, len1, len2);

//      String ret = Base64.encodeBase64String(ivAppended);
        byte[] raw = Base64.encodeBase64(ivAppended);
        return new String(raw);
    }

    private static byte[] cipherData(BufferedBlockCipher cipher, byte[] data) {
        int minSize = cipher.getOutputSize(data.length);
        byte[] outBuf = new byte[minSize];
        int len1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
        int len2 = -1;
        try {
            len2 = cipher.doFinal(outBuf, len1);
        } catch (InvalidCipherTextException icte) {
            icte.printStackTrace();
        }

        int actualLength = len1 + len2;
        byte[] result = new byte[actualLength];
        System.arraycopy(outBuf, 0, result, 0, result.length);
        return result;
    }

}
