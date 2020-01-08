package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.tx0.AndroidTx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.persist.FileWhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.protocol.fee.WhirlpoolFee;

import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.Level;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java8.util.Optional;

public class AndroidWhirlpoolWalletService extends WhirlpoolWalletService {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidWhirlpoolWalletService.class);
    private Subject<String> events = PublishSubject.create();

    public enum ConnectionStates {
        CONNECTED,
        STARTING,
        LOADING,
        DISCONNECTED
    }
    private BehaviorSubject<ConnectionStates> source = BehaviorSubject.create();

    private static final String TAG = "AndroidWhirlpoolWalletS";
    private static AndroidWhirlpoolWalletService instance;
    private WhirlpoolUtils whirlpoolUtils = WhirlpoolUtils.getInstance();
    private WhirlpoolWallet wallet;

    public static AndroidWhirlpoolWalletService getInstance() {
        if (instance == null) {
            instance = new AndroidWhirlpoolWalletService();
        }
        return instance;
    }

    protected AndroidWhirlpoolWalletService() {
        super();
        source.onNext(ConnectionStates.LOADING);
        WhirlpoolFee.getInstance(AndroidSecretPointFactory.getInstance()); // fix for Android
        setLogLevel(Level.OFF, Level.OFF);
    }

    public void setLogLevel(Level whirlpoolLevel, Level whirlpoolClientLevel) {
        // set whirlpool log level
        ClientUtils.setLogLevel(whirlpoolLevel, whirlpoolClientLevel);
    }

    public WhirlpoolWallet getWhirlpoolWallet(Context ctx) throws Exception {
        Optional<WhirlpoolWallet> whirlpoolWalletOpt = getWhirlpoolWallet();
        if (!whirlpoolWalletOpt.isPresent()) {
            // open WhirlpoolWallet
            HD_Wallet bip84w = BIP84Util.getInstance(ctx).getWallet();
            String walletIdentifier = whirlpoolUtils.computeWalletIdentifier(bip84w);
            WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(ctx, walletIdentifier);
            return openWallet(config, bip84w);
        }
        return whirlpoolWalletOpt.get();
    }

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(Context ctx, String walletIdentifier) throws Exception {
        // TODO user preferences
        boolean testnet = SamouraiWallet.getInstance().isTestNet();
        boolean onion = false;
        int mixsTarget = 5;
        String scode = null;

        // TODO dojo backend support
        String backendUrl = BackendServer.get(testnet).getBackendUrl(onion);
        String backendApiKey = null;

        IHttpClient httpClient = new AndroidHttpClient(WebUtil.getInstance(ctx));
        BackendApi backendApi = new BackendApi(httpClient, backendUrl, backendApiKey);

        File fileIndex = whirlpoolUtils.computeIndexFile(walletIdentifier, ctx);
        File fileUtxo = whirlpoolUtils.computeUtxosFile(walletIdentifier, ctx);
        WhirlpoolWalletPersistHandler persistHandler =
                new FileWhirlpoolWalletPersistHandler(fileIndex, fileUtxo);

        return computeWhirlpoolWalletConfig(ctx, persistHandler, testnet, onion, mixsTarget, scode, httpClient, backendApi);
    }

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(Context ctx, WhirlpoolWalletPersistHandler persistHandler, boolean testnet, boolean onion, int mixsTarget, String scode, IHttpClient httpClient, BackendApi backendApi) throws Exception {
        IStompClientService stompClientService = new AndroidStompClientService();

        WhirlpoolServer whirlpoolServer = testnet ? WhirlpoolServer.TESTNET : WhirlpoolServer.MAINNET;
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        NetworkParameters params = whirlpoolServer.getParams();
        WhirlpoolWalletConfig whirlpoolWalletConfig =
                new WhirlpoolWalletConfig(
                        httpClient, stompClientService, persistHandler, serverUrl, params, true, backendApi);

        whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
        whirlpoolWalletConfig.setAutoMix(true); // enable auto-mix

        whirlpoolWalletConfig.setMixsTarget(mixsTarget);
        whirlpoolWalletConfig.setScode(scode);
        whirlpoolWalletConfig.setMaxClients(1);

        whirlpoolWalletConfig.setSecretPointFactory(AndroidSecretPointFactory.getInstance());
        whirlpoolWalletConfig.setTx0Service(new AndroidTx0Service(whirlpoolWalletConfig));
        return whirlpoolWalletConfig;
    }



    public Completable startService(Context context) {
        if (source.hasObservers())
            source.onNext(ConnectionStates.STARTING);
        return Completable.fromCallable(() -> {
            this.wallet = this.getWhirlpoolWallet(context);
            this.wallet.start();
            if (source.hasObservers()) {
                source.onNext(ConnectionStates.CONNECTED);
            }
            return true;
        });
    }


    public void stop() {
        if (source.hasObservers())
            source.onNext(ConnectionStates.DISCONNECTED);
         wallet.stop();
    }

    public BehaviorSubject<ConnectionStates> listenConnectionStatus() {
        return source;
    }
    //get the current instance of the wallet
    public WhirlpoolWallet getWallet() {
        return wallet;
    }

    public Observable<String> getEvents() {
        return events.subscribeOn(Schedulers.io());
    }

    @Override
    protected WhirlpoolDataService newDataService(WhirlpoolWalletConfig config) {
        return new AndroidWhirlpoolDataService(config, this);

    }
}
