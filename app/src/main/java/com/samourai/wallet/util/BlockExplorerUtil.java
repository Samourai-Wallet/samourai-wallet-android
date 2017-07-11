package com.samourai.wallet.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Smartbit", "UASF Explorer", "Bisq"  };
    private static CharSequence[] blockExplorerTxUrls = { "https://www.smartbit.com.au/tx/", "https://uasf-explorer.satoshiportal.com/tx/", "https://explorer.bisq.io:8443/insight/tx/" };
    private static CharSequence[] blockExplorerAddressUrls = { "https://www.smartbit.com.au/address/", "https://uasf-explorer.satoshiportal.com/address/", "https://explorer.bisq.io:8443/insight/address/" };

    private static BlockExplorerUtil instance = null;

    private BlockExplorerUtil() { ; }

    public static BlockExplorerUtil getInstance() {

        if(instance == null) {
            instance = new BlockExplorerUtil();
        }

        return instance;
    }

    public CharSequence[] getBlockExplorers() {
        return blockExplorers;
    }

    public CharSequence[] getBlockExplorerTxUrls() {
        return blockExplorerTxUrls;
    }

    public CharSequence[] getBlockExplorerAddressUrls() {
        return blockExplorerAddressUrls;
    }

}
