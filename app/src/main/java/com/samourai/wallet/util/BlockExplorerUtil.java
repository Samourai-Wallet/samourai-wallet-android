package com.samourai.wallet.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Smartbit", "UASF Explorer", "Blockchain Reader (Yogh)" };
    private static CharSequence[] blockExplorerTxUrls = { "https://www.smartbit.com.au/tx/", "https://uasf-explorer.satoshiportal.com/tx/", "http://srv1.yogh.io/#tx:id:" };
    private static CharSequence[] blockExplorerAddressUrls = { "https://www.smartbit.com.au/address/", "https://uasf-explorer.satoshiportal.com/address/", "http://srv1.yogh.io/#addr:id:" };

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
