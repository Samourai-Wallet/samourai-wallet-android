package com.masanari.wallet.hd;

import android.content.Context;
//import android.util.Log;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import com.masanari.wallet.MasanariWallet;
import com.masanari.wallet.bip47.BIP47Util;
import com.masanari.wallet.segwit.BIP49Util;
import com.masanari.wallet.segwit.BIP84Util;
import com.masanari.wallet.util.AppUtil;
import com.masanari.wallet.util.FormatsUtil;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.apache.commons.lang.ArrayUtils;

public class HD_WalletFactory	{

    public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

    private static HD_WalletFactory instance = null;
    private static List<HD_Wallet> wallets = null;

    private static Context context = null;
    private MnemonicCode mc;

    private HD_WalletFactory()	{ ; }

    public static HD_WalletFactory getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        return instance;
    }

    public HD_Wallet newWallet(int nbWords, String passphrase, int nbAccounts) throws IOException, MnemonicException.MnemonicLengthException   {

        HD_Wallet hdw = null;

        if((nbWords % 3 != 0) || (nbWords < 12 || nbWords > 24)) {
            nbWords = 12;
        }

        // len == 16 (12 words), len == 24 (18 words), len == 32 (24 words)
        int len = (nbWords / 3) * 4;

        if(passphrase == null) {
            passphrase = "";
        }

        NetworkParameters params = MasanariWallet.getInstance().getCurrentNetworkParams();

        AppUtil.getInstance(context).applyPRNGFixes();
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[len];
        random.nextBytes(seed);

        MnemonicCode mc = computeMnemonicCode();
        if (mc != null) {
            hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
        }

        BIP47Util.getInstance(context).reset();
        BIP49Util.getInstance(context).reset();
        BIP84Util.getInstance(context).reset();
        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet restoreWallet(String data, String passphrase, int nbAccounts) throws AddressFormatException, IOException, DecoderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException  {

        HD_Wallet hdw = null;

        if(passphrase == null) {
            passphrase = "";
        }

        NetworkParameters params = MasanariWallet.getInstance().getCurrentNetworkParams();

        MnemonicCode mc = computeMnemonicCode();
        if(mc != null) {
            List<String> words = null;

            byte[] seed = null;
            if(data.matches(FormatsUtil.XPUB)) {
                String[] xpub = data.split(":");
                hdw = new HD_Wallet(params, xpub);
            }
            else if(data.matches(FormatsUtil.HEX) && data.length() % 4 == 0) {
                seed = Hex.decodeHex(data.toCharArray());
                hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
            }
            else {
                data = data.toLowerCase().replaceAll("[^a-z]+", " ");             // only use for BIP39 English
                words = Arrays.asList(data.trim().split("\\s+"));
                seed = mc.toEntropy(words);
                hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
            }
        }

        BIP47Util.getInstance(context).reset();
        BIP49Util.getInstance(context).reset();
        BIP84Util.getInstance(context).reset();
        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet get() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets == null || wallets.size() < 1) {
            return null;
        }

        return wallets.get(0);
    }

    public BIP47Wallet getBIP47() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets == null || wallets.size() < 1) {
            return null;
        }

        BIP47Wallet hdw47 = null;
        MnemonicCode mc = computeMnemonicCode();
        if (mc != null) {
            String seed = HD_WalletFactory.getInstance(context).get().getSeedHex();
            String passphrase = HD_WalletFactory.getInstance(context).get().getPassphrase();
            hdw47 = new BIP47Wallet(47, mc, MasanariWallet.getInstance().getCurrentNetworkParams(), org.bouncycastle.util.encoders.Hex.decode(seed), passphrase, 1);
        }

        return hdw47;
    }

    public HD_Wallet getBIP49() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets == null || wallets.size() < 1) {
            return null;
        }

        HD_Wallet hdw49 = null;
        MnemonicCode mc = computeMnemonicCode();
        if (mc != null) {
            String seed = HD_WalletFactory.getInstance(context).get().getSeedHex();
            String passphrase = HD_WalletFactory.getInstance(context).get().getPassphrase();
            hdw49 = new HD_Wallet(49, mc, MasanariWallet.getInstance().getCurrentNetworkParams(), org.bouncycastle.util.encoders.Hex.decode(seed), passphrase, 1);
        }

        return hdw49;
    }

    public HD_Wallet getBIP84() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets == null || wallets.size() < 1) {
            return null;
        }

        HD_Wallet hdw84 = null;
        MnemonicCode mc = computeMnemonicCode();
        if (mc != null) {
            String seed = HD_WalletFactory.getInstance(context).get().getSeedHex();
            String passphrase = HD_WalletFactory.getInstance(context).get().getPassphrase();
            hdw84 = new HD_Wallet(84, mc, MasanariWallet.getInstance().getCurrentNetworkParams(), org.bouncycastle.util.encoders.Hex.decode(seed), passphrase, 1);
        }

        return hdw84;
    }

    public void set(HD_Wallet wallet)	{

        if(wallet != null)	{
            wallets.clear();
            wallets.add(wallet);
        }

    }

    public boolean holding()	{
        return (wallets.size() > 0);
    }

    public List<HD_Wallet> getWallets()    {
        return wallets;
    }

    public void clear() {
        wallets = null;
        context = null;
        instance = null;
    }

    private MnemonicCode computeMnemonicCode() throws IOException {
        if (mc == null) {
            InputStream wis = context.getAssets().open("BIP39/en.txt");
            if (wis != null) {
                mc = new MnemonicCode(wis, BIP39_ENGLISH_SHA256);
                wis.close();
            }
        }
        return mc;
    }

    // for tests
    public void __setMnemonicCode(MnemonicCode mc) {
        this.mc = mc;
    }

}
