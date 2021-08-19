package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.utils.BtcFixedFormat;
import org.bitcoinj.utils.BtcFormat;
import org.bitcoinj.utils.MonetaryFormat;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
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

    public static String xlatXPUB(String xpub, boolean toSegwit) throws AddressFormatException {

        byte[] xpubBytes = Base58.decodeChecked(xpub);

        ByteBuffer bb = ByteBuffer.wrap(xpubBytes);
        int ver = bb.getInt();
        if(ver != MAGIC_XPUB && ver != MAGIC_TPUB && ver != MAGIC_YPUB && ver != MAGIC_UPUB && ver != MAGIC_ZPUB && ver != MAGIC_VPUB)   {
            throw new AddressFormatException("invalid xpub version");
        }

        int xlatVer = 0;
        switch(ver)    {
            case FormatsUtilGeneric.MAGIC_XPUB:
                xlatVer = toSegwit ? MAGIC_ZPUB : MAGIC_YPUB;
                break;
            case FormatsUtilGeneric.MAGIC_YPUB:
                xlatVer = MAGIC_XPUB;
                break;
            case FormatsUtilGeneric.MAGIC_TPUB:
                xlatVer = toSegwit ? MAGIC_VPUB : MAGIC_UPUB;
                break;
            case FormatsUtilGeneric.MAGIC_UPUB:
                xlatVer = MAGIC_TPUB;
                break;
            case FormatsUtilGeneric.MAGIC_ZPUB:
                xlatVer = toSegwit ? MAGIC_YPUB : MAGIC_XPUB;
                break;
            case FormatsUtilGeneric.MAGIC_VPUB:
                xlatVer = toSegwit ? MAGIC_UPUB : MAGIC_TPUB;
                break;
        }

        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(xlatVer);
        byte[] bVer = b.array();

        System.arraycopy(bVer, 0, xpubBytes, 0, bVer.length);

        // append checksum
        byte[] checksum = Arrays.copyOfRange(Sha256Hash.hashTwice(xpubBytes), 0, 4);
        byte[] xlatXpub = new byte[xpubBytes.length + checksum.length];
        System.arraycopy(xpubBytes, 0, xlatXpub, 0, xpubBytes.length);
        System.arraycopy(checksum, 0, xlatXpub, xlatXpub.length - 4, checksum.length);

        String ret = Base58.encode(xlatXpub);

        return ret;
    }

}