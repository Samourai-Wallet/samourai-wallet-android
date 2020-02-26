package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;

import org.bitcoinj.core.NetworkParameters;

import java.text.DecimalFormat;

public class FormatsUtil extends FormatsUtilGeneric {

    private static FormatsUtil instance = null;

    private FormatsUtil() {
        super();
    }

    public static FormatsUtil getInstance() {

        if (instance == null) {
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

    public static int valueAsDp(Context context, int value) {
        float scale = context.getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (value * scale + 0.5f);
        return dpAsPixels;
    }

    public static String getBTCDecimalFormat(Long sats) {
        DecimalFormat format =  new DecimalFormat("0.########");
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(8);
        format.setMinimumFractionDigits(8);
        return format.format(sats / 1e8);
    }
    public static String getBTCDecimalFormat(Long sats,int fractions) {
        DecimalFormat format =  new DecimalFormat("0.########");
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(fractions);
        format.setMinimumFractionDigits(fractions);
        return format.format(sats / 1e8);
    }
}