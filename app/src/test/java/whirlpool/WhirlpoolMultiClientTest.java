package whirlpool;

import com.masanari.stomp.client.AndroidStompClient;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.utils.MultiClientManager;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class WhirlpoolMultiClientTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(AndroidStompClient.class.getSimpleName());
    private WhirlpoolClient whirlpoolClient;
    private WhirlpoolClientConfig config;
    private MultiClientManager multiClientManager;

    private static final NetworkParameters networkParameters = TestNet3Params.get();
    private static final String SERVER = "127.0.0.1:8080";
    private static final String POOL_ID = "1btc";

    @Before
    public void setUp() throws Exception {
        super.setUp(networkParameters);

        // client configuration (server...)
        config = new WhirlpoolClientConfig(whirlpoolHttpClient, stompClient, SERVER, networkParameters);
        config.setTestMode(true); // TODO skipping utxo validations

        // instanciate one client for fetching pools
        this.whirlpoolClient = WhirlpoolClientImpl.newClient(config);
    }

    @Test
    public void testMultiMix() throws Exception {
        // instanciate multiClientManager
        multiClientManager = new MultiClientManager();

        // pool
        Pool pool = findPool(POOL_ID);
        int nbMixs = 1; // number of mixs to achieve

        // TODO assign unique UTXOS

        // client 1
        WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(config);
        UtxoWithBalance utxo = new UtxoWithBalance("7ea75da574ebabf8d17979615b059ab53aae3011926426204e730d164a0d0f16", 100000102, 2);
        IPremixHandler premixHandler = new PremixHandler(utxo, computeECKey("cUwS52vEv4ursFBdGJWgHiZyBNqqSF5nFTsunUpocRBYGLY72z4j"));
        IPostmixHandler postmixHandler = new PostmixHandler(computeBip47wallet("all all all all all all all all all all all all", "w0"),1, bip47Util);
        MixParams mixParams = new MixParams(pool, premixHandler, postmixHandler);
        WhirlpoolClientListener listener = multiClientManager.register(whirlpoolClient);
        whirlpoolClient.whirlpool(mixParams, nbMixs, listener);

        // client 2
        whirlpoolClient = WhirlpoolClientImpl.newClient(config);
        utxo = new UtxoWithBalance("7ea75da574ebabf8d17979615b059ab53aae3011926426204e730d164a0d0f16", 100000102, 2);
        premixHandler = new PremixHandler(utxo, computeECKey("cUwS52vEv4ursFBdGJWgHiZyBNqqSF5nFTsunUpocRBYGLY72z4j"));
        postmixHandler = new PostmixHandler(computeBip47wallet("all all all all all all all all all all all all", "w0"),1, bip47Util);
        mixParams = new MixParams(pool, premixHandler, postmixHandler);
        listener = multiClientManager.register(whirlpoolClient);
        whirlpoolClient.whirlpool(mixParams, nbMixs, listener);

        // client 3
        whirlpoolClient = WhirlpoolClientImpl.newClient(config);
        utxo = new UtxoWithBalance("7ea75da574ebabf8d17979615b059ab53aae3011926426204e730d164a0d0f16", 100000102, 2);
        premixHandler = new PremixHandler(utxo, computeECKey("cUwS52vEv4ursFBdGJWgHiZyBNqqSF5nFTsunUpocRBYGLY72z4j"));
        postmixHandler = new PostmixHandler(computeBip47wallet("all all all all all all all all all all all all", "w0"),1, bip47Util);
        mixParams = new MixParams(pool, premixHandler, postmixHandler);
        listener = multiClientManager.register(whirlpoolClient);
        whirlpoolClient.whirlpool(mixParams, nbMixs, listener);

        // client 4
        whirlpoolClient = WhirlpoolClientImpl.newClient(config);
        utxo = new UtxoWithBalance("7ea75da574ebabf8d17979615b059ab53aae3011926426204e730d164a0d0f16", 100000102, 2);
        premixHandler = new PremixHandler(utxo, computeECKey("cUwS52vEv4ursFBdGJWgHiZyBNqqSF5nFTsunUpocRBYGLY72z4j"));
        postmixHandler = new PostmixHandler(computeBip47wallet("all all all all all all all all all all all all", "w0"),1, bip47Util);
        mixParams = new MixParams(pool, premixHandler, postmixHandler);
        listener = multiClientManager.register(whirlpoolClient);
        whirlpoolClient.whirlpool(mixParams, nbMixs, listener);

        // client 5
        whirlpoolClient = WhirlpoolClientImpl.newClient(config);
        utxo = new UtxoWithBalance("7ea75da574ebabf8d17979615b059ab53aae3011926426204e730d164a0d0f16", 100000102, 2);
        premixHandler = new PremixHandler(utxo, computeECKey("cUwS52vEv4ursFBdGJWgHiZyBNqqSF5nFTsunUpocRBYGLY72z4j"));
        postmixHandler = new PostmixHandler(computeBip47wallet("all all all all all all all all all all all all", "w0"),1, bip47Util);
        mixParams = new MixParams(pool, premixHandler, postmixHandler);
        listener = multiClientManager.register(whirlpoolClient);
        whirlpoolClient.whirlpool(mixParams, nbMixs, listener);

        // wait for all clients success
        multiClientManager.waitDone();
    }

    private Pool findPool(String poolId) throws Exception {
        Pools pools = whirlpoolClient.fetchPools();
        return pools.findPoolById(poolId);
    }

    @After
    public void tearDown() {
        if (multiClientManager != null) {
            multiClientManager.exit();
        }
    }

}
