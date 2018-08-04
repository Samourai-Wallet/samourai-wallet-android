package com.samourai.wallet.util;

import android.util.Patterns;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.bech32.Bech32;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import android.util.Log;

public class FormatsUtil {

	private Pattern emailPattern = Patterns.EMAIL_ADDRESS;
	private Pattern phonePattern = Pattern.compile("(\\+[1-9]{1}[0-9]{1,2}+|00[1-9]{1}[0-9]{1,2}+)[\\(\\)\\.\\-\\s\\d]{6,16}");

	private String URI_BECH32 = "(^bitcoin:(tb|bc)1([qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)(\\?amount\\=([0-9.]+))?$)|(^bitcoin:(TB|BC)1([QPZRY9X8GF2TVDW0S3JN54KHCE6MUA7L]+)(\\?amount\\=([0-9.]+))?$)";
	private String URI_BECH32_LOWER = "^bitcoin:((tb|bc)1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)(\\?amount\\=([0-9.]+))?$";

	public static final int MAGIC_XPUB = 0x0488B21E;
	public static final int MAGIC_TPUB = 0x043587CF;
	public static final int MAGIC_YPUB = 0x049D7CB2;
	public static final int MAGIC_UPUB = 0x044A5262;
	public static final int MAGIC_ZPUB = 0x04B24746;
	public static final int MAGIC_VPUB = 0x045F1CF6;

	public static final int MAGIC_XPRV = 0x0488ADE4;
	public static final int MAGIC_TPRV = 0x04358394;
	public static final int MAGIC_YPRV = 0x049D7878;
	public static final int MAGIC_UPRV = 0x044A4E28;
	public static final int MAGIC_ZPRV = 0x04B2430C;
	public static final int MAGIC_VPRV = 0x045F18BC;

	public static final String XPUB = "^[xtyu]pub[1-9A-Za-z][^OIl]+$";
	public static final String HEX = "^[0-9A-Fa-f]+$";

	private static FormatsUtil instance = null;

	private FormatsUtil() { ; }

	public static FormatsUtil getInstance() {

		if(instance == null) {
			instance = new FormatsUtil();
		}

		return instance;
	}

	public String validateBitcoinAddress(final String address) {

		if(isValidBitcoinAddress(address)) {
			return address;
		}
		else {
			String addr = getBitcoinAddress(address);
			if(addr != null) {
				return addr;
			}
			else {
				return null;
			}
		}
	}

	public boolean isBitcoinUri(final String s) {

		boolean ret = false;
		BitcoinURI uri = null;

		try {
			uri = new BitcoinURI(s);
			ret = true;
		}
		catch(BitcoinURIParseException bupe) {
			if(s.matches(URI_BECH32))	{
				ret = true;
			}
			else	{
				ret = false;
			}
		}

		return ret;
	}

	public String getBitcoinUri(final String s) {

		String ret = null;
		BitcoinURI uri = null;

		try {
			uri = new BitcoinURI(s);
			ret = uri.toString();
		}
		catch(BitcoinURIParseException bupe) {
			if(s.matches(URI_BECH32))	{
				return s;
			}
			else	{
				ret = null;
			}
		}

		return ret;
	}

	public String getBitcoinAddress(final String s) {

		String ret = null;
		BitcoinURI uri = null;

		try {
			uri = new BitcoinURI(s);
			ret = uri.getAddress().toString();
		}
		catch(BitcoinURIParseException bupe) {
			if(s.toLowerCase().matches(URI_BECH32_LOWER))	{
				Pattern pattern = Pattern.compile(URI_BECH32_LOWER);
				Matcher matcher = pattern.matcher(s.toLowerCase());
				if(matcher.find() && matcher.group(1) != null)    {
					return matcher.group(1);
				}
			}
			else	{
				ret = null;
			}
		}

		return ret;
	}

	public String getBitcoinAmount(final String s) {

		String ret = null;
		BitcoinURI uri = null;

		try {
			uri = new BitcoinURI(s);
			if(uri.getAmount() != null) {
				ret = uri.getAmount().toString();
			}
			else {
				ret = "0.0000";
			}
		}
		catch(BitcoinURIParseException bupe) {
			if(s.toLowerCase().matches(URI_BECH32_LOWER))	{
				Pattern pattern = Pattern.compile(URI_BECH32_LOWER);
				Matcher matcher = pattern.matcher(s.toLowerCase());
				if(matcher.find() && matcher.group(4) != null)    {
					String amt = matcher.group(4);
					try	{
						return Long.toString(Math.round(Double.valueOf(amt) * 1e8));
					}
					catch(NumberFormatException nfe)	{
						ret = "0.0000";
					}
				}
			}
			else	{
				ret = null;
			}
		}

		return ret;
	}

	public boolean isValidBitcoinAddress(final String address) {

		boolean ret = false;
		Address addr = null;

		if((!SamouraiWallet.getInstance().isTestNet() && address.toLowerCase().startsWith("bc")) ||
				(SamouraiWallet.getInstance().isTestNet() && address.toLowerCase().startsWith("tb")))	{

			try	{
				Pair<Byte, byte[]> pair = Bech32Segwit.decode(address.substring(0, 2), address);
				if(pair.getLeft() == null || pair.getRight() == null)	{
					;
				}
				else	{
					ret = true;
				}
			}
			catch(Exception e)	{
				e.printStackTrace();
			}

		}
		else	{

			try {
				addr = new Address(SamouraiWallet.getInstance().getCurrentNetworkParams(), address);
				if(addr != null) {
					ret = true;
				}
			}
			catch(WrongNetworkException wne) {
				ret = false;
			}
			catch(AddressFormatException afe) {
				ret = false;
			}

		}

		return ret;
	}

	public boolean isValidBech32(final String address) {

		boolean ret = false;

		try	{
			Pair<String, byte[]> pair0 = Bech32.bech32Decode(address);
			if(pair0.getLeft() == null || pair0.getRight() == null)	{
				ret = false;
			}
			else	{
				Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(address.substring(0, 2), address);
				if(pair1.getLeft() == null || pair1.getRight() == null)	{
					ret = false;
				}
				else	{
					ret = true;
				}
			}
		}
		catch(Exception e)	{
			ret = false;
		}

		return ret;
	}

	public boolean isValidXpub(String xpub){

		try {
			byte[] xpubBytes = Base58.decodeChecked(xpub);

			if(xpubBytes.length != 78)	{
				return false;
			}

			ByteBuffer byteBuffer = ByteBuffer.wrap(xpubBytes);
			int version = byteBuffer.getInt();
			if(version != MAGIC_XPUB && version != MAGIC_TPUB && version != MAGIC_YPUB && version != MAGIC_UPUB && version != MAGIC_ZPUB && version != MAGIC_VPUB)   {
				throw new AddressFormatException("invalid version: " + xpub);
			}
			else	{

				byte[] chain = new byte[32];
				byte[] pub = new byte[33];
				// depth:
				byteBuffer.get();
				// parent fingerprint:
				byteBuffer.getInt();
				// child no.
				byteBuffer.getInt();
				byteBuffer.get(chain);
				byteBuffer.get(pub);

				ByteBuffer pubBytes = ByteBuffer.wrap(pub);
				int firstByte = pubBytes.get();
				if(firstByte == 0x02 || firstByte == 0x03){
					return true;
				}else{
					return false;
				}
			}
		}
		catch(Exception e)	{
			return false;
		}
	}

	public boolean isValidXprv(String xprv){

		try {
			byte[] xprvBytes = Base58.decodeChecked(xprv);

			if(xprvBytes.length != 78)	{
				return false;
			}

			ByteBuffer byteBuffer = ByteBuffer.wrap(xprvBytes);
			int version = byteBuffer.getInt();
			if(version != MAGIC_XPRV && version != MAGIC_TPRV && version != MAGIC_YPRV && version != MAGIC_UPRV && version != MAGIC_ZPRV && version != MAGIC_VPRV)   {
				throw new AddressFormatException("invalid version: " + xprv);
			}
			else	{

				return true;

			}
		}
		catch(Exception e)	{
			return false;
		}
	}

	public boolean isValidPaymentCode(String pcode){

		try {
			PaymentCode paymentCode = new PaymentCode(pcode);
			return paymentCode.isValid();
		}
		catch(Exception e)	{
			return false;
		}
	}

	public boolean isValidBIP47OpReturn(String op_return){

		byte[] buf = Hex.decode(op_return);

		if(buf.length == 80 && buf[0] == 0x01 && buf[1] == 0x00 && (buf[2] == 0x02 || buf[2] == 0x03))    {
			return true;
		}
		else    {
			return false;
		}

	}

}