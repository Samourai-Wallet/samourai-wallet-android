package com.samourai.wallet.bip47;

import android.content.Context;
import android.widget.Toast;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.SecretPoint;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.util.encoders.Hex;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.PaymentAddress;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class BIP47Util {

    private static BIP47Wallet wallet = null;

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

    public BIP47Wallet getWallet() {
        return wallet;
    }

    public HD_Address getNotificationAddress() {
        return wallet.getAccount(0).getNotificationAddress();
    }

    public HD_Address getNotificationAddress(int account) {
        return wallet.getAccount(account).getNotificationAddress();
    }

    public PaymentCode getPaymentCode() throws AddressFormatException   {
        String payment_code = wallet.getAccount(0).getPaymentCode();
        return new PaymentCode(payment_code);
    }

    public PaymentCode getPaymentCode(int account) throws AddressFormatException   {
        String payment_code = wallet.getAccount(account).getPaymentCode();
        return new PaymentCode(payment_code);
    }

    public PaymentCode getFeaturePaymentCode() throws AddressFormatException   {
        PaymentCode payment_code = getPaymentCode();
        return new PaymentCode(payment_code.makeSamouraiPaymentCode());
    }

    public PaymentCode getFeaturePaymentCode(int account) throws AddressFormatException   {
        PaymentCode payment_code = getPaymentCode(account);
        return new PaymentCode(payment_code.makeSamouraiPaymentCode());
    }

    public PaymentAddress getReceiveAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        HD_Address address = wallet.getAccount(0).addressAt(idx);
        return getPaymentAddress(pcode, 0, address);
    }

    public PaymentAddress getReceiveAddress(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception {
        HD_Address address = wallet.getAccount(account).addressAt(idx);
        return getPaymentAddress(pcode, 0, address);
    }

    public String getReceivePubKey(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        PaymentAddress paymentAddress = getReceiveAddress(pcode, idx);
        return Hex.toHexString(paymentAddress.getReceiveECKey().getPubKey());
    }

    public String getReceivePubKey(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        PaymentAddress paymentAddress = getReceiveAddress(pcode, account, idx);
        return Hex.toHexString(paymentAddress.getReceiveECKey().getPubKey());
    }

    public PaymentAddress getSendAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        HD_Address address = wallet.getAccount(0).addressAt(0);
        return getPaymentAddress(pcode, idx, address);
    }

    public PaymentAddress getSendAddress(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception {
        HD_Address address = wallet.getAccount(account).addressAt(0);
        return getPaymentAddress(pcode, idx, address);
    }

    public String getSendPubKey(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        PaymentAddress paymentAddress = getSendAddress(pcode, idx);
        return Hex.toHexString(paymentAddress.getSendECKey().getPubKey());
    }

    public String getSendPubKey(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        PaymentAddress paymentAddress = getSendAddress(pcode, account, idx);
        return Hex.toHexString(paymentAddress.getSendECKey().getPubKey());
    }

    public byte[] getIncomingMask(byte[] pubkey, byte[] outPoint) throws AddressFormatException, Exception    {

        HD_Address notifAddress = getNotificationAddress();
        DumpedPrivateKey dpk = new DumpedPrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), notifAddress.getPrivateKeyString());
        ECKey inputKey = dpk.getKey();
        byte[] privkey = inputKey.getPrivKeyBytes();
        byte[] mask = PaymentCode.getMask(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes(), outPoint);

        return mask;
    }

    public byte[] getIncomingMask(byte[] pubkey, int account, byte[] outPoint) throws AddressFormatException, Exception    {

        HD_Address notifAddress = getNotificationAddress(account);
        DumpedPrivateKey dpk = new DumpedPrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), notifAddress.getPrivateKeyString());
        ECKey inputKey = dpk.getKey();
        byte[] privkey = inputKey.getPrivKeyBytes();
        byte[] mask = PaymentCode.getMask(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes(), outPoint);

        return mask;
    }

    public PaymentAddress getPaymentAddress(PaymentCode pcode, int idx, HD_Address address) throws AddressFormatException, NotSecp256k1Exception {
        DumpedPrivateKey dpk = new DumpedPrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), address.getPrivateKeyString());
        ECKey eckey = dpk.getKey();
        PaymentAddress paymentAddress = new PaymentAddress(pcode, idx, eckey.getPrivKeyBytes());
        return paymentAddress;
    }

}
