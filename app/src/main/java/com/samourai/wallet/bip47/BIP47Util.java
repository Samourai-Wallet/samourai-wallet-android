package com.samourai.wallet.bip47;

import android.content.Context;
import android.widget.Toast;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.SecretPoint;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPoint;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class BIP47Util extends BIP47UtilGeneric {

    private static BIP47Wallet wallet = null;

    private static Context context = null;
    private static BIP47Util instance = null;

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

    private BIP47Util() {
        super(secretPointFactory);
    }
    private static final ISecretPointFactory secretPointFactory = new ISecretPointFactory() {
        @Override
        public ISecretPoint newSecretPoint(byte[] dataPrv, byte[] dataPub) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException {
            return new SecretPoint(dataPrv, dataPub);
        }
    };
    
    private NetworkParameters getNetworkParams() {
        return SamouraiWallet.getInstance().getCurrentNetworkParams();
    }

    public void reset()  {
        wallet = null;
    }

    public BIP47Wallet getWallet() {
        return wallet;
    }

    public HD_Address getNotificationAddress() {
        return super.getNotificationAddress(wallet);
    }

    public HD_Address getNotificationAddress(int account) {
        return super.getNotificationAddress(wallet, account);
    }

    public PaymentCode getPaymentCode() throws AddressFormatException   {
        return super.getPaymentCode(wallet);
    }

    public PaymentCode getPaymentCode(int account) throws AddressFormatException   {
        return super.getPaymentCode(wallet, account);
    }

    public PaymentCode getFeaturePaymentCode() throws AddressFormatException   {
        return super.getFeaturePaymentCode(wallet);
    }

    public PaymentCode getFeaturePaymentCode(int account) throws AddressFormatException   {
        return super.getFeaturePaymentCode(wallet, account);
    }

    public PaymentAddress getReceiveAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return super.getReceiveAddress(wallet, pcode, idx, getNetworkParams());
    }

    public PaymentAddress getReceiveAddress(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return super.getReceiveAddress(wallet, account, pcode, idx, getNetworkParams());
    }

    public String getReceivePubKey(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getReceivePubKey(wallet, pcode, idx, getNetworkParams());
    }

    public String getReceivePubKey(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getReceivePubKey(wallet, account, pcode, idx, getNetworkParams());
    }

    public PaymentAddress getSendAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return super.getSendAddress(wallet, pcode, idx, getNetworkParams());
    }

    public PaymentAddress getSendAddress(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return getSendAddress(wallet, account, pcode, idx, getNetworkParams());
    }

    public String getSendPubKey(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getSendPubKey(wallet, pcode, idx, getNetworkParams());
    }

    public String getSendPubKey(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getSendPubKey(wallet, account, pcode, idx, getNetworkParams());
    }

    public byte[] getIncomingMask(byte[] pubkey, byte[] outPoint) throws AddressFormatException, Exception    {
        return super.getIncomingMask(wallet, pubkey, outPoint, getNetworkParams());
    }

    public byte[] getIncomingMask(byte[] pubkey, int account, byte[] outPoint) throws AddressFormatException, Exception    {
        return super.getIncomingMask(wallet, account, pubkey, outPoint, getNetworkParams());
    }

    public PaymentAddress getPaymentAddress(PaymentCode pcode, int idx, HD_Address address) throws AddressFormatException, NotSecp256k1Exception {
        return super.getPaymentAddress(pcode, idx, address, getNetworkParams());
    }
}
