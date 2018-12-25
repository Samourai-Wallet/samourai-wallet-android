package com.samourai.wallet.util;

import com.samourai.wallet.SamouraiWallet;

import org.bitcoinj.core.NetworkParameters;

public class FormatsUtil extends FormatsUtilGeneric {

	private static FormatsUtil instance = null;

	private FormatsUtil() { super(); }

	public static FormatsUtil getInstance() {

		if(instance == null) {
			instance = new FormatsUtil();
		}

		return instance;
	}

	private NetworkParameters getNetworkParams() {
		return SamouraiWallet.getInstance().getCurrentNetworkParams();
	}

	public String validateBitcoinAddress(final String address) {
		return super.validateBitcoinAddress(address, getNetworkParams());
	}

	public boolean isValidBitcoinAddress(final String address) {
		return super.isValidBitcoinAddress(address, getNetworkParams());
	}
}