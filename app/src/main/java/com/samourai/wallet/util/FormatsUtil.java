package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.BtcFixedFormat;
import org.bitcoinj.utils.BtcFormat;
import org.bitcoinj.utils.MonetaryFormat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FormatsUtil extends FormatsUtilGeneric {

    private static FormatsUtil instance = null;
    private static final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    private static final DecimalFormat df = new DecimalFormat("#", symbols);
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

    public static String formatBTC(Long sats) {
        return  formatBTCWithoutUnit(sats).concat(" ").concat(MonetaryUtil.getInstance().getBTCUnits());
    }

    public static String formatBTCWithoutUnit(Long sats) {
        return   BtcFormat
                .builder()
                .locale(Locale.ENGLISH)
                .fractionDigits(8)
                .build().format(sats);
    }


    public static String formatSats(Long sats) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat _df = new DecimalFormat("#", symbols);
        _df.setMinimumIntegerDigits(1);
        _df.setMaximumIntegerDigits(16);
        _df.setGroupingUsed(true);
        _df.setGroupingSize(3);
        return _df.format(sats).concat(" ").concat(MonetaryUtil.getInstance().getSatoshiUnits());
    }

    public static String getPoolBTCDecimalFormat(Long sats) {
        DecimalFormat format = new DecimalFormat("0.###");
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(1);
        return format.format(sats / 1e8);
    }

    public static String getBTCDecimalFormat(Long sats) {
        DecimalFormat format = new DecimalFormat("0.########");
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(8);
        format.setMinimumFractionDigits(8);
        return format.format(sats / 1e8);
    }

    public static String getBTCDecimalFormat(Long sats, int fractions) {
        DecimalFormat format = new DecimalFormat("0.########");
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(fractions);
        format.setMinimumFractionDigits(fractions);
        return format.format(sats / 1e8);
    }
}