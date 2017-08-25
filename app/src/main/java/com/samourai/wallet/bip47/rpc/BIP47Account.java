package com.samourai.wallet.bip47.rpc;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Account;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.util.FormatsUtil;

import java.nio.ByteBuffer;

/**
 *
 * BIP47Account.java : an account in a BIP47 wallet
 *
 */
public class BIP47Account extends HD_Account {

    private String strPaymentCode = null;

    /**
     * Constructor for account.
     *
     * @param NetworkParameters params
     * @param DeterministicKey mwey deterministic key for this account
     * @param int child id within the wallet for this account
     *
     */
    public BIP47Account(NetworkParameters params, DeterministicKey wKey, int child) {
        super(params, wKey, "", child);
        strPaymentCode = createPaymentCodeFromAccountKey();
    }

    /**
     * Constructor for watch-only account.
     *
     * @param NetworkParameters params
     * @param String data XPUB or payment code for this account
     *
     */
    public BIP47Account(NetworkParameters params, String data) throws AddressFormatException {

        mParams = params;
        mAID = -1;

        // assign master key to account key
        if(FormatsUtil.getInstance().isValidPaymentCode(data))  {
            aKey = createMasterPubKeyFromPaymentCode(data);
            strPaymentCode = data;
        }
        else if(FormatsUtil.getInstance().isValidXpub(data))  {
            aKey = createMasterPubKeyFromXPub(data);
            strXPUB = data;
            strPaymentCode = createPaymentCodeFromAccountKey();
        }
        else    {
            ;
        }

    }

    /**
     * Return notification address.
     *
     * @return Address
     *
     */
    public HD_Address getNotificationAddress() {
        return addressAt(0);
    }

    /**
     * Return address at idx.
     *
     * @param int idx
     * @return Address
     *
     */
    public HD_Address addressAt(int idx) {
        return new HD_Address(SamouraiWallet.getInstance().getCurrentNetworkParams(), aKey, idx);
    }

    private String createPaymentCodeFromAccountKey() {

        PaymentCode pcode = new PaymentCode(aKey.getPubKey(), aKey.getChainCode());

        return pcode.toString();

    }

    /**
     * Return payment code string for this account.
     *
     * @return String
     *
     */
    public String getPaymentCode() {

        if(strPaymentCode != null)  {
            return strPaymentCode;
        }
        else  {
            return null;
        }

    }

    /**
     * Restore watch-only account deterministic public key from payment code.
     *
     * @return DeterministicKey
     *
     */
    private DeterministicKey createMasterPubKeyFromPaymentCode(String payment_code_str) throws AddressFormatException {

        byte[] paymentCodeBytes = Base58.decodeChecked(payment_code_str);

        ByteBuffer bb = ByteBuffer.wrap(paymentCodeBytes);
        if(bb.get() != 0x47)   {
            throw new AddressFormatException("invalid payment code version");
        }

        byte[] chain = new byte[32];
        byte[] pub = new byte[33];
        // type:
        bb.get();
        // features:
        bb.get();

        bb.get(pub);
        bb.get(chain);

        return HDKeyDerivation.createMasterPubKeyFromBytes(pub, chain);
    }

}
