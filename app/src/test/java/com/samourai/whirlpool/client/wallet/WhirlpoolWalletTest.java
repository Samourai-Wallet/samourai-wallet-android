package com.samourai.whirlpool.client.wallet;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.tx0.UnspentOutputWithKey;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.persist.FileWhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;

import junit.framework.Assert;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

@Ignore
public class WhirlpoolWalletTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTest.class.getSimpleName());
    private WhirlpoolWallet whirlpoolWallet;
    private HD_Wallet bip84w;
    private WhirlpoolWalletConfig config;

    private static final String SEED_WORDS = "all all all all all all all all all all all all";
    private static final String SEED_PASSPHRASE = "test";

    @Before
    public void setUp() throws Exception {
        super.setUp(TestNet3Params.get());

        // configure wallet
        boolean testnet = true;
        boolean onion = false;
        int mixsTarget = 5;
        String scode = null;

        // backendApi with mocked pushTx
        IHttpClient httpClient = new AndroidHttpClient(WebUtil.getInstance(getContext()));
        BackendApi backendApi = new BackendApi(httpClient, BackendServer.TESTNET.getBackendUrl(onion), null) {
            @Override
            public void pushTx(String txHex) throws Exception {
                log.info("pushTX ignored for test: "+txHex);
            }
        };

        File fileIndex = File.createTempFile("test-state", "test");
        File fileUtxo = File.createTempFile("test-utxos", "test");
        WhirlpoolWalletPersistHandler persistHandler =
                new FileWhirlpoolWalletPersistHandler(fileIndex, fileUtxo);

        // instanciate WhirlpoolWallet
        bip84w = computeBip84w(SEED_WORDS, SEED_PASSPHRASE);
        config = whirlpoolWalletService.computeWhirlpoolWalletConfig(getContext(), persistHandler, testnet, onion, mixsTarget, scode, httpClient, backendApi);
        whirlpoolWallet = whirlpoolWalletService.openWallet(config, bip84w);
    }

    @Test
    public void testStart() throws Exception {
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
        ClientUtils.logWhirlpoolUtxos(utxosPremix, whirlpoolWallet.getConfig().getMixsTarget());

        // list postmix utxos
        Collection<WhirlpoolUtxo> utxosPostmix = whirlpoolWallet.getUtxosPremix();
        log.info(utxosPostmix.size()+" POSTMIX utxos:");
        ClientUtils.logWhirlpoolUtxos(utxosPostmix, whirlpoolWallet.getConfig().getMixsTarget());

        // keep running
        for(int i=0; i<2; i++) {
            MixingState mixingState = whirlpoolWallet.getMixingState();
            log.debug("WHIRLPOOL: "+mixingState.getNbQueued()+" queued, "+mixingState.getNbMixing()+" mixing: "+mixingState.getUtxosMixing());

            synchronized (this) {
                wait(10000);
            }
        }
    }

    @Test
    public void testTx0() throws Exception {
        Collection<UnspentOutputWithKey> spendFroms = new LinkedList<>();

        ECKey ecKey = bip84w.getAccountAt(0).getChain(0).getAddressAt(61).getECKey();
        UnspentResponse.UnspentOutput unspentOutput = newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 500000000);
        unspentOutput.addr = new SegwitAddress(ecKey, networkParameters).getBech32AsString();
        spendFroms.add(new UnspentOutputWithKey(unspentOutput, ecKey.getPrivKeyBytes()));

        Pool pool = whirlpoolWallet.findPoolById("0.01btc");
        Tx0Config tx0Config = whirlpoolWallet.getTx0Config().setMaxOutputs(1);
        Tx0Preview tx0Preview = whirlpoolWallet.tx0Preview(pool, spendFroms, tx0Config, Tx0FeeTarget.BLOCKS_2);
        Tx0 tx0 = whirlpoolWallet.tx0(pool, spendFroms, tx0Config, Tx0FeeTarget.BLOCKS_2);

        Assert.assertEquals("9ae94965aee6102b96f6204ef6719137bba1f33cb1bca65cdc02c366f3d015b5", tx0.getTx().getHashAsString());
        Assert.assertEquals("01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a408a9eb379a4aaf4d4579118c64b64bbd327cd95ba826ac68f334155fd9ca4e3acd64acdfd75dd7c3cc5bc34d31af6c6e68b4db37eac62b574890f6cfc7b904d9950c300000000000016001441021632871b0f1cf61a7ac7b6a0187e886282915c670f00000000001600147e4a4628dd8fbd638681a728e39f7d92ada040704139bd1d00000000160014df3a4bc83635917ad18621f3ba78cef6469c5f5902483045022100f38c8f05bc665ad67181767c38cafb8ab0651e7c30ad54a8b0b9ad60020ff277022074b23406a73753c521452d43d3a78265989d493a592a8bf9bd65c902841eb5d10121032e46baef8bcde0c3a19cadb378197fa31d69adb21535de3f84de699a1cf88b4500000000", new String(Hex.encode(tx0.getTx().bitcoinSerialize())));
    }
}