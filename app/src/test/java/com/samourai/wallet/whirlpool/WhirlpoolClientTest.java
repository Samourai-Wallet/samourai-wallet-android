package com.samourai.wallet.whirlpool;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;

import junit.framework.Assert;

import org.bitcoinj.core.NetworkParameters;
import org.junit.Before;
import org.junit.Test;

public class WhirlpoolClientTest {
    private WhirlpoolClient whirlpoolClient;

    @Before
    public void setUp() {
        // client configuration (server...)
        String server = "server:port";
        NetworkParameters networkParameters = SamouraiWallet.getInstance().getCurrentNetworkParams();
        WhirlpoolClientConfig config = new WhirlpoolClientConfig(server, networkParameters);

        // http client implementation
        final WebUtil webUtil = WebUtil.getInstance(null);
        WhirlpoolHttpClient whirlpoolHttpClient = new WhirlpoolHttpClient(webUtil);

        // instanciate client
        this.whirlpoolClient = WhirlpoolClientImpl.newClient(config, whirlpoolHttpClient);
    }

    @Test
    public void testFetchPools() {
        try {
            System.out.println("fetchPools...");
            Pools pools = whirlpoolClient.fetchPools();
            for (Pool pool : pools.getPools()) {
                System.out.println(pool.getPoolId()+" : "+pool.getDenomination());
            }
            Assert.assertFalse(pools.getPools().isEmpty());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}