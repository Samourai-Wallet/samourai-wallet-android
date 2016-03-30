package com.samourai.wallet.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "SoChain", "Blocktrail", "Blockchain", "Blockr.io", "Blockexplorer.com" };
    private static CharSequence[] blockExplorerUrls = { "https://chain.so/tx/BTC/", "https://www.blocktrail.com/BTC/tx/", "https://blockchain.info/tx/", "https://btc.blockr.io/tx/info/", "https://blockexplorer.com/tx/"  };

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

    public CharSequence[] getBlockExplorerUrls() {
        return blockExplorerUrls;
    }

}
