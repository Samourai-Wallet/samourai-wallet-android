package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.stomp.client.AndroidStompClient;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandler;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.whirlpool.client.utils.ClientUtils;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.Callable;

import ch.qos.logback.classic.Level;
import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public abstract class AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(AndroidStompClient.class.getSimpleName());

    protected Context context = null; //new MockContext(); // TODO sdk>=29 required
    protected BIP47Util bip47Util = BIP47Util.getInstance(context);
    protected HD_WalletFactory hdWalletFactory = HD_WalletFactory.getInstance(context);
    protected HD_WalletFactoryGeneric hdWalletFactoryGeneric;
    protected NetworkParameters networkParameters;
    protected AndroidWhirlpoolWalletService whirlpoolWalletService = new AndroidWhirlpoolWalletService();
    protected WhirlpoolUtils whirlpoolUtils = WhirlpoolUtils.getInstance();

    public void setUp(NetworkParameters networkParameters) throws Exception {
        this.networkParameters = networkParameters;

        ClientUtils.setLogLevel(Level.TRACE, Level.TRACE);

        // mock main thread
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(Callable<Scheduler> schedulerCallable) throws Exception {
                return Schedulers.trampoline();
            }
        });

        // resolve mnemonicCode
        InputStream wis = getClass().getResourceAsStream("/BIP39/en.txt");
        MnemonicCode mc = new MnemonicCode(wis, HD_WalletFactory.BIP39_ENGLISH_SHA256);
        hdWalletFactory.__setMnemonicCode(mc);
        wis.close();
        hdWalletFactoryGeneric = new HD_WalletFactoryGeneric(mc);

        // init Samourai Wallet
        SamouraiWallet.getInstance().setCurrentNetworkParams(networkParameters);
    }

    protected ECKey computeECKey(String utxoKey) {
        // input utxo key
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(networkParameters, utxoKey);
        ECKey ecKey = dumpedPrivateKey.getKey();
        return ecKey;
    }

    protected HD_Wallet computeBip84w(String seedWords, String passphrase) throws Exception {
        byte[] seed = hdWalletFactoryGeneric.computeSeedFromWords(seedWords);
        HD_Wallet bip84w = hdWalletFactoryGeneric.getBIP84(seed, passphrase, networkParameters);
        return bip84w;
    }

    protected Bip84Wallet computeBip84wallet(String seedWords, String passphrase) throws Exception {
        HD_Wallet bip84w = computeBip84w(seedWords, passphrase);
        Bip84Wallet bip84Wallet = new Bip84Wallet(bip84w, 0, new MemoryIndexHandler(), new MemoryIndexHandler());
        return bip84Wallet;
    }

    protected Context getContext()  {
        return context;
    }

    protected UnspentOutput newUnspentOutput(String hash, int index, long value) {
        UnspentOutput spendFrom = new UnspentOutput();
        spendFrom.tx_hash = hash;
        spendFrom.tx_output_n = index;
        spendFrom.value = value;
        spendFrom.script = "foo";
        spendFrom.addr = "foo";
        spendFrom.confirmations = 1234;
        spendFrom.xpub = new UnspentOutput.Xpub();
        spendFrom.xpub.path = "foo";
        return spendFrom;
    }
}