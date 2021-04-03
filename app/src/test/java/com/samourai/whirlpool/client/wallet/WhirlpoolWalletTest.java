package com.samourai.whirlpool.client.wallet;

import com.samourai.http.client.AndroidHttpClientService;
import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.tor.TorManager;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.tx0.UnspentOutputWithKey;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
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

import java8.util.Optional;

@Ignore
public class WhirlpoolWalletTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTest.class.getSimpleName());
    private WhirlpoolWallet whirlpoolWallet;
    private HD_Wallet bip84w;
    private WhirlpoolWalletConfig config;

    private static final String SEED_WORDS = "wise never behave tornado tool pear aunt consider season swap custom human";
    private static final String SEED_PASSPHRASE = "test";

    @Before
    public void setUp() throws Exception {
        super.setUp(TestNet3Params.get());

        // configure wallet
        boolean testnet = true;
        boolean onion = false;
        String scode = null;

        // backendApi with mocked pushTx
        IHttpClientService httpClientService = AndroidHttpClientService.getInstance(getContext());
        IHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.BACKEND);
        BackendApi backendApi = new BackendApi(httpClient, BackendServer.TESTNET.getBackendUrl(onion), Optional.empty()) {
            @Override
            public void pushTx(String txHex) throws Exception {
                log.info("pushTX ignored for test: "+txHex);
            }
        };

        // instanciate WhirlpoolWallet
        bip84w = computeBip84w(SEED_WORDS, SEED_PASSPHRASE);
        config = whirlpoolWalletService.computeWhirlpoolWalletConfig(TorManager.INSTANCE, testnet, onion, scode, httpClientService, backendApi);

        /*
            @Override
            protected Tx0Data fetchTx0Data(String poolId) throws HttpException, NotifiableException {
                Tx0Data tx0Data = super.fetchTx0Data(poolId);
                // mock fee address for deterministic tests
                return new Tx0Data(tx0Data.getFeePaymentCode(), tx0Data.getFeeValue(), tx0Data.getFeeChange(), 0, tx0Data.getFeePayload(), "tb1qgyppvv58rv83eas60trmdgqc06yx9q53qs6skx", 123);
            }
        */

        File fileIndex = File.createTempFile("test-state", "test");
        File fileUtxo = File.createTempFile("test-utxos", "test");

        whirlpoolWallet = whirlpoolWalletService.openWallet(config, bip84w, fileIndex.getAbsolutePath(), fileUtxo.getAbsolutePath());
    }

    @Test
    @Ignore
    public void testStart() throws Exception {
        // start whirlpool wallet
        whirlpoolWallet.start();

        // list pools
        Collection<Pool> pools = whirlpoolWallet.getPoolSupplier().getPools();
        Assert.assertTrue(!pools.isEmpty());

        // find pool by poolId
        Pool pool = whirlpoolWallet.getPoolSupplier().findPoolById("0.01btc");
        Assert.assertNotNull(pool);

        // list premix utxos
        Collection<WhirlpoolUtxo> utxosPremix = whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.PREMIX);
        log.info(utxosPremix.size()+" PREMIX utxos:");
        ClientUtils.logWhirlpoolUtxos(utxosPremix);

        // list postmix utxos
        Collection<WhirlpoolUtxo> utxosPostmix = whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.POSTMIX);
        log.info(utxosPostmix.size()+" POSTMIX utxos:");
        ClientUtils.logWhirlpoolUtxos(utxosPostmix);

        // keep running
        for(int i=0; i<50; i++) {
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
        UnspentOutput unspentOutput = newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 500000000);
        unspentOutput.addr = new SegwitAddress(ecKey, networkParameters).getBech32AsString();
        spendFroms.add(new UnspentOutputWithKey(unspentOutput, ecKey.getPrivKeyBytes()));

        Pool pool = whirlpoolWallet.getPoolSupplier().findPoolById("0.01btc");
        Tx0Config tx0Config = whirlpoolWallet.getTx0Config();
        Tx0Preview tx0Preview = whirlpoolWallet.tx0Preview(pool, spendFroms, tx0Config, Tx0FeeTarget.BLOCKS_2, Tx0FeeTarget.BLOCKS_2);
        Tx0 tx0 = whirlpoolWallet.tx0(spendFroms, pool, tx0Config, Tx0FeeTarget.BLOCKS_2, Tx0FeeTarget.BLOCKS_2);

        Assert.assertEquals("dc398c99cf9ce18123ea916d69bb99da44a3979a625eeaac5e17837f879a8874", tx0.getTx().getHashAsString());
        Assert.assertEquals("01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a408a9eb379a44ff4d4579118c64b64bbd327cd95ba826ac68f334155fd9ca4e3acd64acdfd75dd7c3cc5bc34d31af6c6e68b4db37eac62b574890f6cfc7b904d9950c300000000000016001441021632871b0f1cf61a7ac7b6a0187e88628291b44b0f00000000001600147e4a4628dd8fbd638681a728e39f7d92ada04070e954bd1d00000000160014df3a4bc83635917ad18621f3ba78cef6469c5f5902483045022100c48f02762ab9877533b5c7b0bc729479ce7809596b89cb9f62b740ea3350068f02205ef46ca67df39d35f940e33223c5ddd56669d953b6ef4948e355c1f3430f32e10121032e46baef8bcde0c3a19cadb378197fa31d69adb21535de3f84de699a1cf88b4500000000", new String(Hex.encode(tx0.getTx().bitcoinSerialize())));
    }
}