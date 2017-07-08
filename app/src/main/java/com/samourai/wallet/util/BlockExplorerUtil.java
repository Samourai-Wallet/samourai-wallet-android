package com.samourai.wallet.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Bisq", "Smartbit" };
    private static CharSequence[] blockExplorerTxUrls = { "https://explorer.bisq.io:8443/insight/tx/", "https://www.smartbit.com.au/tx/" };
    private static CharSequence[] blockExplorerAddressUrls = { "https://explorer.bisq.io:8443/insight/address/", "https://www.smartbit.com.au/address/" };

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
