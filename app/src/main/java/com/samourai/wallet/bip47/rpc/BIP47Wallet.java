package com.samourai.wallet.bip47.rpc;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Wallet;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

/**
 *
 * BIP47Wallet.java : BIP47 wallet
 *
 */
public class BIP47Wallet extends HD_Wallet {

    private BIP47Account mAccount = null;

    /**
     * Constructor for wallet.
     *
     * @param int purpose
     * @param MnemonicCode mc mnemonic code object
     * @param NetworkParameters params
     * @param byte[] seed seed for this wallet
     * @param String passphrase optional BIP39 passphrase
     * @param int nbAccounts number of accounts to create
     *
     */
    public BIP47Wallet(int purpose, MnemonicCode mc, NetworkParameters params, byte[] seed, String passphrase, int nbAccounts) throws MnemonicException.MnemonicLengthException {

        super(purpose, mc, params, seed, passphrase, nbAccounts);

        mAccount = new BIP47Account(SamouraiWallet.getInstance().getCurrentNetworkParams(), mRoot, 0);

    }

    /**
     * Return account for submitted account id.
     *
     * @param int accountId
     *
     * @return Account
     *
     */
    public BIP47Account getAccount(int accountId) {
        return mAccount;
    }

}
