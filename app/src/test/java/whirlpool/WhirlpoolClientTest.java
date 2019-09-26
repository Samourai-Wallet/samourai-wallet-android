package whirlpool;

import com.google.common.util.concurrent.SettableFuture;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.Bip84PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.persist.FileWhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import com.samourai.whirlpool.client.whirlpool.listener.LoggingWhirlpoolClientListener;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;

import junit.framework.Assert;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Ignore
public class WhirlpoolClientTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(WhirlpoolClientTest.class.getSimpleName());
    private WhirlpoolClient whirlpoolClient;

    private static final NetworkParameters networkParameters = TestNet3Params.get();
    private static final String SERVER = WhirlpoolServer.LOCAL_TESTNET.getServerUrlClear();//WhirlpoolServer.TESTNET.getServerUrlClear();
    private static final String POOL_ID = "0.01btc";

    @Before
    public void setUp() throws Exception {
        super.setUp(networkParameters);

        // persistance
        File fileIndex = File.createTempFile("whirlpool-state-", ".json"); // TODO non temporary files
        File fileUtxo = File.createTempFile("whirlpool-utxos-", ".json");
        WhirlpoolWalletPersistHandler persistHandler = new FileWhirlpoolWalletPersistHandler(fileIndex, fileUtxo);

        // client configuration (server...)
        WhirlpoolClientConfig config = new WhirlpoolClientConfig(whirlpoolHttpClient, stompClientService, persistHandler, SERVER, networkParameters);
        config.setTestMode(true); // TODO skipping utxo validations

        // instanciate client
        this.whirlpoolClient = WhirlpoolClientImpl.newClient(config);
    }

    @Test
    public void testFetchPools() {
        try {
            log.info("Pools:");
            Pools pools = whirlpoolClient.fetchPools();
            for (Pool pool : pools.getPools()) {
                log.info(pool.getPoolId()+" : "+pool.getDenomination()+"sats, " + pool.getNbRegistered() + " registered in pool, " + pool.getNbConfirmed() + " confirmed");
            }
            Assert.assertFalse(pools.getPools().isEmpty());
        } catch(Exception e) {
            log.error("", e);
        }
    }

    @Test
    public void testMix() throws Exception {
        // pool
        Pool pool = whirlpoolClient.fetchPools().findPoolById(POOL_ID);

        // input
        UtxoWithBalance utxo = new UtxoWithBalance("7ea75da574ebabf8d17979615b059ab53aae3011926426204e730d164a0d0f16", 2, 1000102);
        String utxoKey = "cUwS52vEv4ursFBdGJWgHiZyBNqqSF5nFTsunUpocRBYGLY72z4j";
        ECKey ecKey = new DumpedPrivateKey(networkParameters, utxoKey).getKey();
        IPremixHandler premixHandler = new PremixHandler(utxo, ecKey);

        // output
        Bip84Wallet bip84Wallet = computeBip84wallet("all all all all all all all all all all all all", "w0");
        IPostmixHandler postmixHandler = new Bip84PostmixHandler(bip84Wallet);

        // listener will be notified of whirlpool progress
        final SettableFuture<Boolean> success = SettableFuture.create();
        WhirlpoolClientListener listener = new LoggingWhirlpoolClientListener(){

            @Override
            public void success(MixSuccess mixSuccess) {
                super.success(mixSuccess);
                // override with custom code here: mix success
                success.set(true);
            }

            @Override
            public void fail(MixFailReason failReason, String notifiableError) {
                super.fail(failReason, notifiableError);
                // override with custom code here: mix failed
                success.set(false);
            }

            @Override
            public void progress(MixStep step) {
                super.progress(step);
                // override with custom code here: mix progress
            }
        };

        // start mixing
        MixParams mixParams = new MixParams(pool, premixHandler, postmixHandler);
        whirlpoolClient.whirlpool(mixParams, listener);

        do {
            if (success.get() != null) {
                if (success.get() == false) {
                    // fail
                    Assert.assertTrue(false);
                    return;
                }
                else {
                    // success
                    Assert.assertTrue(true);
                    return;
                }
            }
            Thread.sleep(1000);
        }
        while(success.get() == null);
    }

}