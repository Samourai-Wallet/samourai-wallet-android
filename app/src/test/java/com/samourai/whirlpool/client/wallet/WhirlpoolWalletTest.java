package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.MixOrchestratorState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;

import junit.framework.Assert;

import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import whirlpool.AbstractWhirlpoolTest;

@Ignore
public class WhirlpoolWalletTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTest.class.getSimpleName());
    private WhirlpoolWallet whirlpoolWallet;

    private static final String SEED_WORDS = "ancient script resist own media cotton edit december waste supply humor plunge";//"all all all all all all all all all all all all";
    private static final String SEED_PASSPHRASE = "test";

    @Before
    public void setUp() throws Exception {
        super.setUp(TestNet3Params.get());

        // configure wallet
        boolean testnet = true;
        boolean onion = false;
        String backendUrl = BackendServer.TESTNET.getBackendUrl(onion);
        String backendApiKey = null;
        int mixsTarget = 5;
        String scode = null;

        // instanciate WhirlpoolWallet
        HD_Wallet bip84w = computeBip84w(SEED_WORDS, SEED_PASSPHRASE);
        String walletIdentifier = whirlpoolUtils.computeWalletIdentifier(bip84w);
        WhirlpoolWalletConfig config = whirlpoolWalletService.computeWhirlpoolWalletConfig(getContext(), walletIdentifier, testnet, onion, backendUrl, backendApiKey, mixsTarget, scode);
        whirlpoolWallet = whirlpoolWalletService.openWallet(config, bip84w);
    }

    @Test
    public void test() throws Exception {
        // start whirlpool wallet
        whirlpoolWallet.start();

        // list pools
        Collection<Pool> pools = whirlpoolWallet.getPools();
        Assert.assertTrue(!pools.isEmpty());

        // find pool by poolId
        Pool pool = whirlpoolWallet.findPoolById("0.01btc");
        Assert.assertNotNull(pool);

        // list premix utxos
        Collection<WhirlpoolUtxo> utxosPremix = whirlpoolWallet.getUtxosPremix();
        log.info(utxosPremix.size()+" PREMIX utxos:");
        ClientUtils.logWhirlpoolUtxos(utxosPremix);

        // list postmix utxos
        Collection<WhirlpoolUtxo> utxosPostmix = whirlpoolWallet.getUtxosPremix();
        log.info(utxosPostmix.size()+" POSTMIX utxos:");
        ClientUtils.logWhirlpoolUtxos(utxosPostmix);

        // keep running
        while(true) {
            MixOrchestratorState mixState = whirlpoolWallet.getState().getMixState();
            log.debug("WHIRLPOOL: "+mixState.getNbQueued()+" queued, "+mixState.getNbMixing()+" mixing: "+mixState.getUtxosMixing());

            synchronized (this) {
                wait(10000);
            }
        }
    }
}