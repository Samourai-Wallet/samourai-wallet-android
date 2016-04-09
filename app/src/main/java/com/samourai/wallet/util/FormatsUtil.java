package com.samourai.wallet.util;

import android.util.Patterns;

import com.samourai.wallet.bip47.rpc.PaymentCode;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;
//import android.util.Log;

public class FormatsUtil {

	private Pattern emailPattern = Patterns.EMAIL_ADDRESS;
	private Pattern phonePattern = Pattern.compile("(\\+[1-9]{1}[0-9]{1,2}+|00[1-9]{1}[0-9]{1,2}+)[\\(\\)\\.\\-\\s\\d]{6,16}");

    public static final String XPUB = "^xpub[1-9A-Za-z][^OIl]+$";
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
			String addr = uri2BitcoinAddress(address);
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
			ret = false;
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
			ret = null;
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
			ret = null;
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
			ret = null;
		}

		return ret;
	}

	public boolean isValidBitcoinAddress(final String address) {

		boolean ret = false;
		Address addr = null;
		
		try {
			addr = new Address(MainNetParams.get(), address);
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

		return ret;
	}

	private String uri2BitcoinAddress(final String address) {
		
		String ret = null;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(address);
			ret = uri.getAddress().toString();
		}
		catch(BitcoinURIParseException bupe) {
			ret = null;
		}
		
		return ret;
	}

	public boolean isValidXpub(String xpub){

		try {
			byte[] xpubBytes = Base58.decodeChecked(xpub);

			ByteBuffer byteBuffer = ByteBuffer.wrap(xpubBytes);
			if(byteBuffer.getInt() != 0x0488B21E)   {
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

	public boolean isValidPaymentCode(String pcode){

		try {
			PaymentCode paymentCode = new PaymentCode(pcode);
			return paymentCode.isValid();
		}
		catch(Exception e)	{
			return false;
		}
	}

}
