package com.samourai.wallet.hd;

import android.content.Context;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import com.google.common.base.Joiner;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;

import org.apache.commons.codec.DecoderException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HD_Wallet {

    private byte[] mSeed = null;
    private String strPassphrase = null;
    private List<String> mWordList = null;

    private DeterministicKey mKey = null;
    protected DeterministicKey mRoot = null;

    protected ArrayList<HD_Account> mAccounts = null;

    private NetworkParameters mParams = null;

    private HD_Wallet() { ; }

    public HD_Wallet(int purpose, MnemonicCode mc, NetworkParameters params, byte[] seed, String passphrase, int nbAccounts) throws MnemonicException.MnemonicLengthException {

        mParams = params;
        mSeed = seed;
        strPassphrase = passphrase;

        mWordList = mc.toMnemonic(mSeed);
        byte[] hd_seed = MnemonicCode.toSeed(mWordList, strPassphrase);
        mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        DeterministicKey t1 = HDKeyDerivation.deriveChildKey(mKey, purpose|ChildNumber.HARDENED_BIT);
        int coin = SamouraiWallet.getInstance().isTestNet() ? (1 | ChildNumber.HARDENED_BIT) : ChildNumber.HARDENED_BIT;
        mRoot = HDKeyDerivation.deriveChildKey(t1, coin);

        mAccounts = new ArrayList<HD_Account>();
        for(int i = 0; i < nbAccounts; i++) {
            String acctName = String.format("account %02d", i);
            mAccounts.add(new HD_Account(mParams, mRoot, acctName, i));
        }

    }

    public HD_Wallet(Context ctx, int purpose, JSONObject jsonobj, NetworkParameters params) throws DecoderException, JSONException, IOException, MnemonicException.MnemonicLengthException {

        mParams = params;
        int nbAccounts = SamouraiWallet.NB_ACCOUNTS;
        mSeed = org.apache.commons.codec.binary.Hex.decodeHex(((String)jsonobj.get("seed")).toCharArray());
        strPassphrase = jsonobj.getString("passphrase");

        InputStream wis = ctx.getResources().getAssets().open("BIP39/en.txt");
        MnemonicCode mc = null;
        if(wis != null) {
            mc = new MnemonicCode(wis, HD_WalletFactory.BIP39_ENGLISH_SHA256);
            wis.close();
        }

        mWordList = mc.toMnemonic(mSeed);
        byte[] hd_seed = MnemonicCode.toSeed(mWordList, strPassphrase);
        mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        DeterministicKey t1 = HDKeyDerivation.deriveChildKey(mKey, purpose|ChildNumber.HARDENED_BIT);
        mRoot = HDKeyDerivation.deriveChildKey(t1, ChildNumber.HARDENED_BIT);

        mAccounts = new ArrayList<HD_Account>();
        for(int i = 0; i < nbAccounts; i++) {
            String acctName = String.format("account %02d", i);
            mAccounts.add(new HD_Account(mParams, mRoot, acctName, i));
        }

    }

    /*
    create from account xpub key(s)
     */
    public HD_Wallet(NetworkParameters params, String[] xpub) throws AddressFormatException {

        mParams = params;
        DeterministicKey aKey = null;
        mAccounts = new ArrayList<HD_Account>();
        for(int i = 0; i < xpub.length; i++) {
            mAccounts.add(new HD_Account(mParams, xpub[i], "", i));
        }

    }

    public String getSeedHex() {
        return org.bouncycastle.util.encoders.Hex.toHexString(mSeed);
    }

    public String getMnemonic() {
        return Joiner.on(" ").join(mWordList);
    }

    public String getPassphrase() {
        return strPassphrase;
    }

    public List<HD_Account> getAccounts() {
        return mAccounts;
    }

    public HD_Account getAccount(int accountId) {
        return mAccounts.get(accountId);
    }

    public HD_Account getAccountAt(int accountIdx) {
        return new HD_Account(mParams, mRoot, "", accountIdx);
    }

    public void addAccount() {
        String strName = String.format("Account %d", mAccounts.size());
        mAccounts.add(new HD_Account(mParams, mRoot, strName, mAccounts.size()));
    }

    public String[] getXPUBs() {

        String[] ret = new String[mAccounts.size()];

        for(int i = 0; i < mAccounts.size(); i++) {
            ret[i] = mAccounts.get(i).xpubstr();
        }

        return ret;
    }

}
