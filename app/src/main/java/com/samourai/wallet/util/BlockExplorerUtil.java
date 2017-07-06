package com.samourai.wallet.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Blocktrail", "Blockchain", "Blockr.io", "SoChain", "Blockexplorer.com" };
    private static CharSequence[] blockExplorerTxUrls = { "https://www.blocktrail.com/BTC/tx/", "https://blockchain.info/tx/", "https://btc.blockr.io/tx/info/", "https://chain.so/tx/BTC/", "https://blockexplorer.com/tx/" };
    private static CharSequence[] blockExplorerAddressUrls = { "https://www.blocktrail.com/BTC/address/", "https://blockchain.info/address/", "https://btc.blockr.io/address/info/", "https://chain.so/address/BTC/", "https://blockexplorer.com/address/" };

    public static final int SOCHAIN = 0;
    public static final int BLOCKTRAIL = 1;
    public static final int BLOCKCHAIN = 2;
    public static final int BLOCKR = 3;
    public static final int CLASSIC = 4;

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
