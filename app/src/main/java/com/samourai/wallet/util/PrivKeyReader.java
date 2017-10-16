package com.samourai.wallet.util;

import android.util.Base64;

import com.samourai.wallet.SamouraiWallet;

import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.MessageDigest;

//import android.util.Log;

public class PrivKeyReader {

    public final static String BASE58 = "base58";
    public final static String BASE64 = "base64";
    public final static String BIP38 = "bip38";
    public final static String HEX_UNCOMPRESSED = "hex_u";
//    public final static String HEX_COMPRESSED = "hex_c";
    public final static String MINI = "mini";
    public final static String WIF_COMPRESSED = "wif_c";
    public final static String WIF_UNCOMPRESSED = "wif_u";

    private CharSequenceX strPrivKey = null;
    private CharSequenceX strPassword = null;

    private PrivKeyReader()	 {
        ;
    }

    public PrivKeyReader(CharSequenceX strPrivKey)	 {
        this.strPrivKey = strPrivKey;
    }

    public PrivKeyReader(CharSequenceX strPrivKey, CharSequenceX strPassword)	 {
        this.strPrivKey = strPrivKey;
        this.strPassword = strPassword;
    }

    public String getFormat() throws Exception {

        if(strPrivKey == null)    {
            return null;
        }

        // 52 characters, always starts with 'c'
        if(SamouraiWallet.getInstance().isTestNet() && strPrivKey.toString().matches("^[c][1-9A-HJ-NP-Za-km-z]{51}$")) {
            return WIF_COMPRESSED;
        }
        // 51 characters base58, always starts with a '9'
        else if(SamouraiWallet.getInstance().isTestNet() && strPrivKey.toString().matches("^9[1-9A-HJ-NP-Za-km-z]{50}$")) {
            return WIF_UNCOMPRESSED;
        }
        // 52 characters, always starts with 'K' or 'L'
        else if(!SamouraiWallet.getInstance().isTestNet() && strPrivKey.toString().matches("^[LK][1-9A-HJ-NP-Za-km-z]{51}$")) {
            return WIF_COMPRESSED;
        }
        // 51 characters base58, always starts with a '5'
        else if(!SamouraiWallet.getInstance().isTestNet() && strPrivKey.toString().matches("^5[1-9A-HJ-NP-Za-km-z]{50}$")) {
            return WIF_UNCOMPRESSED;
        }
        else if(strPrivKey.toString().matches("^[1-9A-HJ-NP-Za-km-z]{44}$") || strPrivKey.toString().matches("^[1-9A-HJ-NP-Za-km-z]{43}$")) {
            return BASE58;
        }
        // assume uncompressed for hex (secret exponent)
        else if(strPrivKey.toString().matches("^[A-Fa-f0-9]{64}$")) {
            return HEX_UNCOMPRESSED;
        }
        else if(strPrivKey.toString().matches("^[A-Za-z0-9/=+]{44}$")) {
            return BASE64;
        }
        else if(strPrivKey.toString().matches("^6P[1-9A-HJ-NP-Za-km-z]{56}$")) {
            return BIP38;
        }
        else if(
//                strPrivKey.toString().matches("^S[1-9A-HJ-NP-Za-km-z]{21}$") ||
                strPrivKey.toString().matches("^S[1-9A-HJ-NP-Za-km-z]{22}$") ||
//                strPrivKey.toString().matches("^S[1-9A-HJ-NP-Za-km-z]{25}$") ||
//                strPrivKey.toString().matches("^S[1-9A-HJ-NP-Za-km-z]{29}$") ||
                strPrivKey.toString().matches("^S[1-9A-HJ-NP-Za-km-z]{30}$")) {

            byte[] testBytes = null;
            String data = strPrivKey.toString() + "?";
            try {
                testBytes = Sha256Hash.hash(data.getBytes("UTF-8"));
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }

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

    public ECKey getKey() throws Exception {

        String format = getFormat();

        if(format == null) {
            return null;
        }

        if(format.equals(WIF_COMPRESSED) || format.equals(WIF_UNCOMPRESSED)) {
            DumpedPrivateKey pk = DumpedPrivateKey.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), strPrivKey.toString());
            return pk.getKey();
        }
        else if(format.equals(BASE58)) {
            return decodeBase58PK(strPrivKey.toString());
        }
        else if(format.equals(BASE64)) {
            return decodeBase64PK(strPrivKey.toString());
        }
        else if(format.equals(HEX_UNCOMPRESSED)) {
            return decodeHexPK(strPrivKey.toString(), false);
        }
        else if(format.equals(BIP38)) {
            return parseBIP38(strPrivKey.toString(), strPassword);
        }
        else if(format.equals(MINI)) {

            try {
                byte[] hash = Sha256Hash.hash(strPrivKey.toString().getBytes("UTF-8"));
                // assume uncompressed
                return decodeHexPK(Hex.toHexString(hash), false);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        else {
            return null;
        }
    }

    public void setPassword(CharSequenceX strPassword) {
        this.strPassword = strPassword;
    }

    private ECKey decodeBase58PK(String base58Priv) throws Exception {
        byte[] privBytes = Base58.decode(base58Priv);
        // Prepend a zero byte to make the biginteger unsigned
        byte[] prependZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
        ECKey ecKey = new ECKey(new BigInteger(prependZeroByte), null, base58Priv.charAt(0) == '5' ? false : true);
        return ecKey;
    }

    private ECKey decodeBase64PK(String base64Priv) throws Exception {
        byte[] privBytes = Base64.decode(base64Priv, Base64.NO_PADDING);
        byte[] prependZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
        // assume uncompressed
        ECKey ecKey = new ECKey(new BigInteger(prependZeroByte), null, false);
        return ecKey;
    }

    private ECKey decodeHexPK(String hex, boolean compressed) throws Exception {
        byte[] privBytes = Hex.decode(hex);
        byte[] prependZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
        ECKey ecKey = new ECKey(new BigInteger(prependZeroByte), null, compressed);
        return ecKey;
    }

    private ECKey parseBIP38(String encryptedKey, CharSequenceX password)  {

        if(password == null)    {
            return null;
        }

        try {
            BIP38PrivateKey bip38 = new BIP38PrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), encryptedKey);
            final ECKey ecKey = bip38.decrypt(password.toString());
            if(ecKey != null && ecKey.hasPrivKey()) {
                return ecKey;
            }
        }
        catch(Exception e) {
            ;
        }

        return null;
    }

}
