package com.samourai.wallet.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "bisq UASF", "Blocktrail", "Blockchain", "Blockr.io", "SoChain", "Blockexplorer.com" };
    private static CharSequence[] blockExplorerTxUrls = { "https://explorer.bisq.io:8443/insight/tx/", "https://www.blocktrail.com/BTC/tx/", "https://blockchain.info/tx/", "https://btc.blockr.io/tx/info/", "https://chain.so/tx/BTC/", "https://blockexplorer.com/tx/" };
    private static CharSequence[] blockExplorerAddressUrls = { "https://explorer.bisq.io:8443/insight/address/", "https://www.blocktrail.com/BTC/address/", "https://blockchain.info/address/", "https://btc.blockr.io/address/info/", "https://chain.so/address/BTC/", "https://blockexplorer.com/address/" };

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
