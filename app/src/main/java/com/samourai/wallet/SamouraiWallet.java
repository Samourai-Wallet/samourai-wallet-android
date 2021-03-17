package com.samourai.wallet;

import android.content.Context;

import com.samourai.wallet.hd.HD_WalletFactory;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import java.io.IOException;
import java.math.BigInteger;

public class SamouraiWallet extends SamouraiWalletConst {

    private static NetworkParameters networkParams = null;

    public final static int SAMOURAI_ACCOUNT = 0;
    public final static int MIXING_ACCOUNT = 1;
//    public final static int PUBLIC_ACCOUNT = 2;

    public final static int NB_ACCOUNTS = 1;

    public static final BigInteger bFee = BigInteger.valueOf(Coin.parseCoin("0.00015").longValue());

    public static final BigInteger RBF_SEQUENCE_VAL_WITH_NLOCKTIME = BigInteger.valueOf(0xffffffffL - 1L);
    public static final BigInteger RBF_SEQUENCE_VAL = BigInteger.valueOf(0xffffffffL - 2L);
    public static final BigInteger NLOCKTIME_SEQUENCE_VAL = BigInteger.valueOf(0xffffffffL - 3L);

    private static SamouraiWallet instance = null;

    public static boolean MOCK_FEE = false;

    private static int currentSelectedAccount = 0;
    private static boolean showTotalBalance = false;

    private SamouraiWallet()    { ; }

    public static SamouraiWallet getInstance()  {

        if(instance == null)    {
            instance = new SamouraiWallet();
        }

        return instance;
    }

    public boolean hasPassphrase(Context ctx)	{
        String passphrase = HD_WalletFactory.getInstance(ctx).get().getPassphrase();

        if(passphrase != null && passphrase.length() > 0)	{
            return true;
        }
        else	{
            return false;
        }
    }

    public NetworkParameters getCurrentNetworkParams() {
        return (networkParams == null) ? MainNetParams.get() : networkParams;
    }

    public void setCurrentNetworkParams(NetworkParameters params) {
        networkParams = params;
    }

    public boolean isTestNet()  {
        return (networkParams == null) ? false : !(getCurrentNetworkParams() instanceof MainNetParams);
    }

}
