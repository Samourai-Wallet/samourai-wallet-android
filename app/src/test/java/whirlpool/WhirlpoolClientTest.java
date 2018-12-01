package whirlpool;

import com.google.common.util.concurrent.SettableFuture;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
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

@Ignore
public class WhirlpoolClientTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(WhirlpoolClientTest.class.getSimpleName());
    private WhirlpoolClient whirlpoolClient;

    private static final NetworkParameters networkParameters = TestNet3Params.get();
    private static final String SERVER = "127.0.0.1:8080";
    private static final String POOL_ID = "1btc";

    @Before
    public void setUp() throws Exception {
        super.setUp(networkParameters);

        // client configuration (server...)
        WhirlpoolClientConfig config = new WhirlpoolClientConfig(whirlpoolHttpClient, stompClient, SERVER, networkParameters);
        config.setTestMode(true); // TODO skipping utxo validations

        // instanciate client
        this.whirlpoolClient = WhirlpoolClientImpl.newClient(config);
    }

    @Test
    public void testFetchPools() {
        try {
            System.out.println("fetchPools...");
            Pools pools = whirlpoolClient.fetchPools();
            for (Pool pool : pools.getPools()) {
                System.out.println(pool.getPoolId()+" : "+pool.getDenomination()+"sats, " + pool.getNbRegistered() + " registered in pool, " + pool.getMixNbConfirmed() + " confirmed for mix");
            }
            Assert.assertFalse(pools.getPools().isEmpty());
        } catch(Exception e) {
            log.error("", e);
        }
    }

    @Test
    public void testMix() throws Exception {
        // pool
        Pool pool = findPool(POOL_ID);

        // input
        UtxoWithBalance utxo = new UtxoWithBalance("7ea75da574ebabf8d17979615b059ab53aae3011926426204e730d164a0d0f16", 2, 100000102);
        String utxoKey = "cUwS52vEv4ursFBdGJWgHiZyBNqqSF5nFTsunUpocRBYGLY72z4j";
        ECKey ecKey = new DumpedPrivateKey(networkParameters, utxoKey).getKey();
        IPremixHandler premixHandler = new PremixHandler(utxo, ecKey);

        // output
        BIP47Wallet bip47w = computeBip47wallet("all all all all all all all all all all all all", "w0");
        int paymentCodeIndex = 0; // TODO always increment
        IPostmixHandler postmixHandler = new PostmixHandler(bip47w, paymentCodeIndex, bip47Util);

        // listener will be notified of whirlpool progress in realtime
        final SettableFuture<Boolean> success = SettableFuture.create();
        WhirlpoolClientListener listener = new LoggingWhirlpoolClientListener(){

            @Override
            public void success(int nbMixs, MixSuccess mixSuccess) {
                super.success(nbMixs, mixSuccess);
                // override with custom code here: all mixs success
                success.set(true);
            }

            @Override
            public void fail(int currentMix, int nbMixs) {
                super.fail(currentMix, nbMixs);
                // override with custom code here: failure
                success.set(false);
            }

            @Override
            public void progress(int currentMix, int nbMixs, MixStep step, String stepInfo, int stepNumber, int nbSteps) {
                super.progress(currentMix, nbMixs, step, stepInfo, stepNumber, nbSteps);
                // override with custom code here: mix progress
            }

            @Override
            public void mixSuccess(int currentMix, int nbMixs, MixSuccess mixSuccess) {
                super.mixSuccess(currentMix, nbMixs, mixSuccess);
                // override with custom code here: one mix success (check if more mixs remaining with currentMix==nbMixs)
            }

            @Override
            protected void log(String message) {
                super.log(message);
                System.out.println("whirlpool: "+message);
            }
        };

        // start mixing
        MixParams mixParams = new MixParams(pool, premixHandler, postmixHandler);
        whirlpoolClient.whirlpool(mixParams, 1, listener);

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

        log.info("receiveKey=" + ((PostmixHandler)postmixHandler).getReceiveKey());

    }

    private Pool findPool(String poolId) throws Exception {
        Pools pools = whirlpoolClient.fetchPools();
        return pools.findPoolById(poolId);
    }

}
