package com.samourai.wallet.bip47.rpc;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Address;

import org.apache.commons.lang3.tuple.Pair;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class PaymentCode {

    private static final int PUBLIC_KEY_Y_OFFSET = 2;
    private static final int PUBLIC_KEY_X_OFFSET = 3;
    private static final int CHAIN_OFFSET = 35;
    private static final int PUBLIC_KEY_X_LEN = 32;
    private static final int PUBLIC_KEY_Y_LEN = 1;
    private static final int CHAIN_LEN = 32;
    private static final int PAYLOAD_LEN = 80;

    private static final int SAMOURAI_FEATURE_BYTE = 79;
    private static final int SAMOURAI_SEGWIT_BIT = 0;

    private String strPaymentCode = null;
    private byte[] pubkey = null;
    private byte[] chain = null;

    public PaymentCode()    {
        strPaymentCode = null;
        pubkey = null;
        chain = null;
    }

    public PaymentCode(String payment_code) throws AddressFormatException {
        strPaymentCode = payment_code;
        this.pubkey = parse().getLeft();
        this.chain = parse().getRight();
    }

    public PaymentCode(byte[] payload)    {
        if(payload.length != 80)  {
          return;
        }

        pubkey = new byte[PUBLIC_KEY_Y_LEN + PUBLIC_KEY_X_LEN];
        chain = new byte[CHAIN_LEN];

        System.arraycopy(payload, PUBLIC_KEY_Y_OFFSET, pubkey, 0, PUBLIC_KEY_Y_LEN + PUBLIC_KEY_X_LEN);
        System.arraycopy(payload, CHAIN_OFFSET, chain, 0, CHAIN_LEN);

        strPaymentCode = makeV1();

    }

    public PaymentCode(byte[] pubkey, byte[] chain)    {
        this.pubkey = pubkey;
        this.chain = chain;
        strPaymentCode = makeV1();
    }

    public HD_Address notificationAddress() throws AddressFormatException {
        return addressAt(0);
    }

    public HD_Address addressAt(int idx) throws AddressFormatException {
        return new BIP47Account(SamouraiWallet.getInstance().getCurrentNetworkParams(), strPaymentCode).addressAt(idx);
    }

    public byte[] getPayload() throws AddressFormatException    {
        byte[] pcBytes = Base58.decodeChecked(strPaymentCode);

        byte[] payload = new byte[PAYLOAD_LEN];
        System.arraycopy(pcBytes, 1, payload, 0, payload.length);

        return payload;
    }

    public int getType() throws AddressFormatException    {
        byte[] payload = getPayload();

        ByteBuffer bb = ByteBuffer.wrap(payload);
        int type = (int)bb.get();

        return type;
    }

    public String toString()    {
        return strPaymentCode;
    }

    public static byte[] getMask(byte[] sPoint, byte[] oPoint)  {

        Mac sha512_HMAC = null;
        byte[] mac_data = null;

        try {
            sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretkey = new SecretKeySpec(oPoint, "HmacSHA512");
            sha512_HMAC.init(secretkey);
            mac_data = sha512_HMAC.doFinal(sPoint);
        }
        catch(InvalidKeyException jse) {
            ;
        }
        catch(NoSuchAlgorithmException nsae) {
            ;
        }

        return mac_data;
    }

    public static byte[] blind(byte[] payload, byte[] mask) throws AddressFormatException {

        byte[] ret = new byte[PAYLOAD_LEN];
        byte[] pubkey = new byte[PUBLIC_KEY_X_LEN];
        byte[] chain = new byte[CHAIN_LEN];
        byte[] buf0 = new byte[PUBLIC_KEY_X_LEN];
        byte[] buf1 = new byte[CHAIN_LEN];

        System.arraycopy(payload, 0, ret, 0, PAYLOAD_LEN);

        System.arraycopy(payload, PUBLIC_KEY_X_OFFSET, pubkey, 0, PUBLIC_KEY_X_LEN);
        System.arraycopy(payload, CHAIN_OFFSET, chain, 0, CHAIN_LEN);
        System.arraycopy(mask, 0, buf0, 0, PUBLIC_KEY_X_LEN);
        System.arraycopy(mask, PUBLIC_KEY_X_LEN, buf1, 0, CHAIN_LEN);

        System.arraycopy(xor(pubkey, buf0), 0, ret, PUBLIC_KEY_X_OFFSET, PUBLIC_KEY_X_LEN);
        System.arraycopy(xor(chain, buf1), 0, ret, CHAIN_OFFSET, CHAIN_LEN);

        return ret;
    }

    private Pair<byte[], byte[]> parse() throws AddressFormatException    {
        byte[] pcBytes = Base58.decodeChecked(strPaymentCode);

        ByteBuffer bb = ByteBuffer.wrap(pcBytes);
        if(bb.get() != 0x47)   {
            throw new AddressFormatException("invalid payment code version");
        }

        byte[] chain = new byte[CHAIN_LEN];
        byte[] pub = new byte[PUBLIC_KEY_X_LEN + PUBLIC_KEY_Y_LEN];

        // type:
        bb.get();
        // features:
        bb.get();

        bb.get(pub);
        if(pub[0] != 0x02 && pub[0] != 0x03)   {
            throw new AddressFormatException("invalid public key");
        }

        bb.get(chain);

        return Pair.of(pub, chain);
    }

    private String makeV1() {
        return make(0x01);
    }

    private String make(int type) {

        String ret = null;

        byte[] payload = new byte[PAYLOAD_LEN];
        byte[] payment_code = new byte[PAYLOAD_LEN + 1];

        for(int i = 0; i < payload.length; i++)   {
            payload[i] = (byte)0x00;
        }

        // byte 0: type.
        payload[0] = (byte)type;
        // byte 1: features bit field. All bits must be zero except where specified elsewhere in this specification
        //      bit 0: Bitmessage notification
        //      bits 1-7: reserved
        payload[1] = (byte)0x00;

        // replace sign & x code (33 bytes)
        System.arraycopy(pubkey, 0, payload, PUBLIC_KEY_Y_OFFSET, pubkey.length);
        // replace chain code (32 bytes)
        System.arraycopy(chain, 0, payload, CHAIN_OFFSET, chain.length);

        // add version byte
        payment_code[0] = (byte)0x47;
        System.arraycopy(payload, 0, payment_code, 1, payload.length);

        // append checksum
        return base58EncodeChecked(payment_code);
    }

    public String makeSamouraiPaymentCode() throws AddressFormatException {

        byte[] payload = getPayload();
        // set bit0 = 1 in 'Samourai byte' for segwit. Can send/receive P2PKH, P2SH-P2WPKH, P2WPKH (bech32)
        payload[SAMOURAI_FEATURE_BYTE] = setBit(payload[SAMOURAI_FEATURE_BYTE], SAMOURAI_SEGWIT_BIT);
        byte[] payment_code = new byte[PAYLOAD_LEN + 1];
        // add version byte
        payment_code[0] = (byte)0x47;
        System.arraycopy(payload, 0, payment_code, 1, payload.length);

        // append checksum
        return base58EncodeChecked(payment_code);
    }

    private String base58EncodeChecked(byte[] buf)  {
        byte[] checksum = Arrays.copyOfRange(Sha256Hash.hashTwice(buf), 0, 4);
        byte[] bufChecked = new byte[buf.length + checksum.length];
        System.arraycopy(buf, 0, bufChecked, 0, buf.length);
        System.arraycopy(checksum, 0, bufChecked, bufChecked.length - 4, checksum.length);

        return Base58.encode(bufChecked);
    }

    private byte setBit(byte b, int pos)    {
        return (byte)(b | (1 << pos));
    }

    private DeterministicKey createMasterPubKeyFromBytes(byte[] pub, byte[] chain) throws AddressFormatException {
        return HDKeyDerivation.createMasterPubKeyFromBytes(pub, chain);
    }

    private static byte[] xor(byte[] a, byte[] b) {

        if(a.length != b.length)    {
            return null;
        }

        byte[] ret = new byte[a.length];

        for(int i = 0; i < a.length; i++)   {
            ret[i] = (byte)((int)b[i] ^ (int)a[i]);
        }

        return ret;
    }

    public boolean isValid(){

  		try {
  			byte[] pcodeBytes = Base58.decodeChecked(strPaymentCode);

  			ByteBuffer byteBuffer = ByteBuffer.wrap(pcodeBytes);
  			if(byteBuffer.get() != 0x47)   {
  				throw new AddressFormatException("invalid version: " + strPaymentCode);
  			}
  			else	{

  				byte[] chain = new byte[32];
  				byte[] pub = new byte[33];
  				// type:
  				byteBuffer.get();
  				// feature:
  				byteBuffer.get();
  				byteBuffer.get(pub);
  				byteBuffer.get(chain);

  				ByteBuffer pubBytes = ByteBuffer.wrap(pub);
  				int firstByte = pubBytes.get();
  				if(firstByte == 0x02 || firstByte == 0x03){
  					return true;
  				}
          else{
            return false;
  				}
  			}
  		}
      catch(BufferUnderflowException bue)	{
  			return false;
  		}
      catch(AddressFormatException afe)	{
  			return false;
  		}
  	}

}
