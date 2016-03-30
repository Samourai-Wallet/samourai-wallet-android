package com.samourai.wallet.bip47;

import android.content.Context;
import android.widget.Toast;

import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.Util;
import com.samourai.wallet.hd.HD_WalletFactory;

import org.bitcoinj.core.bip47.Wallet;
import org.bitcoinj.core.bip47.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.PaymentAddress;

import java.io.IOException;

public class BIP47Util {

    private static Wallet wallet = null;

    private static Context context = null;
    private static BIP47Util instance = null;

    private BIP47Util() { ; }

    public static BIP47Util getInstance(Context ctx) {

        context = ctx;

        if(instance == null || wallet == null) {

            try {
                wallet = HD_WalletFactory.getInstance(context).getBIP47();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            }
            catch (MnemonicException.MnemonicLengthException mle) {
                mle.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            }

            instance = new BIP47Util();
        }

        return instance;
    }

    public void reset()  {
        wallet = null;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Address getNotificationAddress() {
        return Util.getInstance().getNotificationAddress(wallet);
    }

    public PaymentCode getPaymentCode() throws AddressFormatException   {
        return Util.getInstance().getPaymentCode(wallet);
    }

    /*
     Get incoming (receive) address for given index from provided payment code
     */
    public PaymentAddress getReceiveAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return Util.getInstance().getReceiveAddress(wallet, pcode, idx);
    }

    /*
     Get outgoing (send) address for given index to provided payment code
     */
    public PaymentAddress getSendAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return Util.getInstance().getSendAddress(wallet, pcode, idx);
    }

    public byte[] getIncomingMask(byte[] pubkey, byte[] outPoint) throws AddressFormatException, Exception    {
        return Util.getInstance().getIncomingMask(wallet, pubkey, outPoint);
    }

    private PaymentAddress getPaymentAddress(PaymentCode pcode, int idx, Address address) throws AddressFormatException, NotSecp256k1Exception {
        return Util.getInstance().getPaymentAddress(pcode, idx, address);
    }

}
