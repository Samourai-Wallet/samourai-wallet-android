package com.masanari.wallet.segwit;

import android.content.Context;
import android.widget.Toast;

import com.masanari.wallet.MasanariWallet;
import com.masanari.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;

public class BIP84Util {

    private static HD_Wallet wallet = null;

    private static Context context = null;
    private static BIP84Util instance = null;

    private BIP84Util() { ; }

    public static BIP84Util getInstance(Context ctx) {

        context = ctx;

        if(instance == null || wallet == null) {

            try {
                wallet = HD_WalletFactory.getInstance(context).getBIP84();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            }
            catch (MnemonicException.MnemonicLengthException mle) {
                mle.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            }

            instance = new BIP84Util();
        }

        return instance;
    }

    public void reset()  {
        wallet = null;
    }

    public HD_Wallet getWallet() {
        return wallet;
    }

    public SegwitAddress getAddressAt(int chain, int idx) {
        HD_Address addr = getWallet().getAccount(0).getChain(chain).getAddressAt(idx);
        SegwitAddress segwitAddress = new SegwitAddress(addr.getPubKey(), MasanariWallet.getInstance().getCurrentNetworkParams());
        return segwitAddress;
    }

}
