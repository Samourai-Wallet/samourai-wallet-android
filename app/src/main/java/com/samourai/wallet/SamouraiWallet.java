package com.samourai.wallet;

import android.content.Context;

import com.samourai.wallet.hd.HD_WalletFactory;

import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.math.BigInteger;

public class SamouraiWallet {

    public final static int SAMOURAI_ACCOUNT = 0;
    public final static int MIXING_ACCOUNT = 1;
//    public final static int PUBLIC_ACCOUNT = 2;

    public final static int NB_ACCOUNTS = 2;

    public static final BigInteger bDust = BigInteger.valueOf(Coin.parseCoin("0.00005460").longValue());    // https://github.com/bitcoin/bitcoin/pull/2760
    public static final BigInteger bFee = BigInteger.valueOf(Coin.parseCoin("0.00015").longValue());
    public static final BigInteger bPriority = BigInteger.valueOf(Coin.parseCoin("0.0005").longValue());
    public static final BigInteger bAddPriority = BigInteger.valueOf(Coin.parseCoin("0.0003").longValue());

    private static SamouraiWallet instance = null;

    private static int currentSelectedAccount = 0;
    private static boolean showTotalBalance = false;

    private SamouraiWallet()    { ; }

    public static SamouraiWallet getInstance()  {

        if(instance == null)    {
            instance = new SamouraiWallet();
        }

        return instance;
    }

    public void setCurrentSelectedAccount(int account) {
        currentSelectedAccount = account;
    }

    public int getCurrentSelectedAccount() {
        return currentSelectedAccount;
    }

    public void setShowTotalBalance(boolean show) {
        showTotalBalance = show;
    }

    public boolean getShowTotalBalance() {
        return showTotalBalance;
    }

    public boolean hasPassphrase(Context ctx)	{
        String passphrase = null;
        try {
            passphrase = HD_WalletFactory.getInstance(ctx).get().getPassphrase();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
        }

        if(passphrase != null && passphrase.length() > 0)	{
            return true;
        }
        else	{
            return false;
        }
    }


}
