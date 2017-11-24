package com.samourai.wallet.segwit;

import android.content.Context;
import android.widget.Toast;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;

public class BIP49Util {

    private static HD_Wallet wallet = null;

    private static Context context = null;
    private static BIP49Util instance = null;

    private BIP49Util() { ; }

    public static BIP49Util getInstance(Context ctx) {

        context = ctx;

        if(instance == null || wallet == null) {

            try {
                wallet = HD_WalletFactory.getInstance(context).getBIP49();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            }
            catch (MnemonicException.MnemonicLengthException mle) {
                mle.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            }

            instance = new BIP49Util();
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
        SegwitAddress p2shp2wpkh = new SegwitAddress(addr.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
        return p2shp2wpkh;
    }

}
