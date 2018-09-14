package com.samourai.wallet.whirlpool;

import com.google.common.util.concurrent.SettableFuture;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IMixHandler;
import com.samourai.whirlpool.client.mix.handler.MixHandler;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import com.samourai.whirlpool.client.whirlpool.listener.LoggingWhirlpoolClientListener;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;

import junit.framework.Assert;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;

import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class WhirlpoolClientTest {
    private WhirlpoolClient whirlpoolClient;

    @Before
    public void setUp() {
        // mock main thread
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(Callable<Scheduler> schedulerCallable) throws Exception {
                return Schedulers.trampoline();
            }
        });

        // client configuration (server...)
        AndroidWhirlpoolHttpClient whirlpoolHttpClient = new AndroidWhirlpoolHttpClient(WebUtil.getInstance(null));
        AndroidWhirlpoolStompClient stompClient = new AndroidWhirlpoolStompClient();
        String server = "server:port";
        NetworkParameters networkParameters = SamouraiWallet.getInstance().getCurrentNetworkParams();
        WhirlpoolClientConfig config = new WhirlpoolClientConfig(whirlpoolHttpClient, stompClient, server, networkParameters);

        // instanciate client
        this.whirlpoolClient = WhirlpoolClientImpl.newClient(config);
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

    @Test
    public void testMix() throws Exception {
        // mix handler
        ECKey ecKey = null; // TODO
        BIP47Wallet bip47w = null; // TODO
        int paymentCodeIndex = 0; // TODO
        IMixHandler mixHandler = new MixHandler(ecKey, bip47w, paymentCodeIndex);

        // mix params (input, output)
        String utxoHash = "5369dfb71b36ed2b91ca43f388b869e617558165e4f8306b80857d88bdd624f2";
        long utxoIndex = 3;
        long utxoBalance = 0; // TODO
        MixParams mixParams = new MixParams(utxoHash, utxoIndex, utxoBalance, mixHandler);

        String poolId = "0.1btc";
        long denomination = 100000102;

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
        int nbMixs = 1; // number of mixs to achieve
        whirlpoolClient.whirlpool(poolId, denomination, mixParams, nbMixs, listener);

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