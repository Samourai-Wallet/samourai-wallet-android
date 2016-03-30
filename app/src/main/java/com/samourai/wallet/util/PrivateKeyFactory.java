package com.samourai.wallet.util;

import android.util.Base64;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.MainNetParams;

import org.apache.commons.lang.ArrayUtils;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.crypto.generators.SCrypt;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

//import android.util.Log;

public class PrivateKeyFactory	{
	
	public final static String BASE58 = "base58";
	public final static String BASE64 = "base64";
	public final static String BIP38 = "bip38";
	public final static String HEX_UNCOMPRESSED = "hex_u";
	public final static String HEX_COMPRESSED = "hex_c";
	public final static String MINI = "mini";
	public final static String WIF_COMPRESSED = "wif_c";
	public final static String WIF_UNCOMPRESSED = "wif_u";
	
    private static PrivateKeyFactory instance = null;

    private PrivateKeyFactory()	 { ; }

    public static PrivateKeyFactory getInstance()	 {

    	if(instance == null)	 {
    		instance = new PrivateKeyFactory();
    	}

    	return instance;
    }

	public String getFormat(String key) throws Exception {
		// 51 characters base58, always starts with a '5'
		if(key.matches("^5[1-9A-HJ-NP-Za-km-z]{50}$")) {
			return WIF_UNCOMPRESSED;
		}
		// 52 characters, always starts with 'K' or 'L'
		else if(key.matches("^[LK][1-9A-HJ-NP-Za-km-z]{51}$")) {
			return WIF_COMPRESSED;
		}
		else if(key.matches("^[1-9A-HJ-NP-Za-km-z]{44}$") || key.matches("^[1-9A-HJ-NP-Za-km-z]{43}$")) {
			return BASE58;
		}
		// assume uncompressed for hex (secret exponent)
		else if(key.matches("^[A-Fa-f0-9]{64}$")) {
			return HEX_UNCOMPRESSED;
		}
		else if(key.matches("^[A-Za-z0-9/=+]{44}$")) {
			return BASE64;
		}
		else if(key.matches("^6P[1-9A-HJ-NP-Za-km-z]{56}$")) {
			return BIP38;
		}
		else if(key.matches("^S[1-9A-HJ-NP-Za-km-z]{21}$") ||
				key.matches("^S[1-9A-HJ-NP-Za-km-z]{25}$") ||
				key.matches("^S[1-9A-HJ-NP-Za-km-z]{29}$") ||
				key.matches("^S[1-9A-HJ-NP-Za-km-z]{30}$")) {

			byte[] testBytes = null;
			String data = key + "?";
			try {
				Hash hash = new Hash(MessageDigest.getInstance("SHA-256").digest(data.getBytes("UTF-8")));
				testBytes = hash.getBytes();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

//			if(testBytes[0] == 0x00 || testBytes[0] == 0x01) {
			if(testBytes[0] == 0x00) {
				return MINI;
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

    public ECKey getKey(String format, String data) throws Exception {
		return getKey(format, data, null); 
	}

	public ECKey getKey(String format, String data, CharSequenceX password) throws Exception {

        if(format == null) {
            return null;
        }

		if(format.equals(WIF_UNCOMPRESSED) || format.equals(WIF_COMPRESSED)) {
			DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), data);
			return pk.getKey();
		}
		else if(format.equals(BASE58)) {
			return decodeBase58PK(data);
		}
		else if(format.equals(BASE64)) {
			return decodeBase64PK(data);
		}
		else if(format.equals(HEX_UNCOMPRESSED)) {
			return decodeHexPK(data);
		}
        else if(format.equals(HEX_COMPRESSED)) {
            return decodeHexPK(data, true);
        }
		/*
		else if(format.equals(BIP38)) {
			return parseBIP38(data, password);
		}
		*/
		else if(format.equals(MINI)) {

			try {
				Hash hash = new Hash(MessageDigest.getInstance("SHA-256").digest(data.getBytes("UTF-8")));
				return decodeHexPK(hash.toString());
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

		}
		else {
			return null;
		}
	}

	private ECKey decodeBase58PK(String base58Priv) throws Exception {
		byte[] privBytes = Base58.decode(base58Priv);
		// Prepend a zero byte to make the biginteger unsigned
		byte[] prependZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
		ECKey ecKey = new ECKey(new BigInteger(prependZeroByte), null, true);
		return ecKey;
	}

	private ECKey decodeBase64PK(String base64Priv) throws Exception {
		byte[] privBytes = Base64.decode(base64Priv, Base64.NO_PADDING);
		// Prepend a zero byte to make the biginteger unsigned
		byte[] prependZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
		ECKey ecKey = new ECKey(new BigInteger(prependZeroByte), null, true);
		return ecKey;
	}

	// assumes uncompressed private key
	private ECKey decodeHexPK(String hex) throws Exception {
		byte[] privBytes = Hex.decode(hex);
		// Prepend a zero byte to make the biginteger unsigned
		byte[] prependZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
		ECKey ecKey = new ECKey(new BigInteger(prependZeroByte), null, true);
		return ecKey;
	}

    private ECKey decodeHexPK(String hex, boolean compressed) throws Exception {
        byte[] privBytes = Hex.decode(hex);
        // Prepend a zero byte to make the biginteger unsigned
        byte[] prependZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
        ECKey ecKey = new ECKey(new BigInteger(prependZeroByte), null, compressed);
        return ecKey;
    }

	private String decryptPK(String base58Priv) throws Exception {

		return base58Priv;
	}

	private ECKey decodePK(String base58Priv) throws Exception {
		return decodeBase58PK(decryptPK(base58Priv));
	}

	private byte[] hash(byte[] data, int offset, int len)	{
		try	{
			MessageDigest a = MessageDigest.getInstance("SHA-256");
			a.update(data, offset, len);
			return a.digest(a.digest());
		}
		catch(NoSuchAlgorithmException e)	{
			throw new RuntimeException(e);
		}
	}

	private byte[] hash(byte[] data)	{
		return hash(data, 0, data.length);
	}

}
