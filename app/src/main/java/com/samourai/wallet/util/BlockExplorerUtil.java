package com.samourai.wallet.util;

import com.samourai.wallet.SamouraiWallet;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Smartbit", "UASF Explorer", "Blockchain Reader (Yogh)" };
    private static CharSequence[] blockExplorerTxUrls = { "https://www.smartbit.com.au/tx/", "https://uasf-explorer.satoshiportal.com/tx/", "http://srv1.yogh.io/#tx:id:" };
    private static CharSequence[] blockExplorerAddressUrls = { "https://www.smartbit.com.au/address/", "https://uasf-explorer.satoshiportal.com/address/", "http://srv1.yogh.io/#addr:id:" };

    private static CharSequence[] tBlockExplorers = { "Blocktrail" };
    private static CharSequence[] tBlockExplorerTxUrls = { "https://www.blocktrail.com/tBTC/tx/" };
    private static CharSequence[] tBlockExplorerAddressUrls = { "https://www.blocktrail.com/tBTC/address/" };

    private static BlockExplorerUtil instance = null;

    private BlockExplorerUtil() { ; }

    public static BlockExplorerUtil getInstance() {

        if(instance == null) {
            instance = new BlockExplorerUtil();
        }

        return instance;
    }

    public CharSequence[] getBlockExplorers() {

        if(SamouraiWallet.getInstance().isTestNet())    {
            return tBlockExplorers;
        }
        else    {
            return blockExplorers;
        }

    }

    public CharSequence[] getBlockExplorerTxUrls() {

        if(SamouraiWallet.getInstance().isTestNet())    {
            return tBlockExplorerTxUrls;
        }
        else    {
            return blockExplorerTxUrls;
        }

    }

    public CharSequence[] getBlockExplorerAddressUrls() {

        if(SamouraiWallet.getInstance().isTestNet())    {
            return tBlockExplorerAddressUrls;
        }
        else    {
            return blockExplorerAddressUrls;
        }

    }

}
