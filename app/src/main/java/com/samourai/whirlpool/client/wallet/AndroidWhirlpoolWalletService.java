package com.samourai.whirlpool.client.wallet;

import android.content.Context;
import android.util.Log;

import com.samourai.http.client.AndroidHttpClientService;
import com.samourai.http.client.AndroidOAuthManager;
import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.util.oauth.OAuthManager;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.AndroidUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.protocol.fee.WhirlpoolFee;

import org.bitcoinj.core.NetworkParameters;

import java.io.File;
import java.util.Map;

import ch.qos.logback.classic.Level;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;
import java8.util.Optional;

public class AndroidWhirlpoolWalletService extends WhirlpoolWalletService {
    public static final int MIXS_TARGET_DEFAULT = 5;
    private Context ctx;

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

    public static AndroidWhirlpoolWalletService getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidWhirlpoolWalletService(ctx);
        }
        return instance;
    }

    protected AndroidWhirlpoolWalletService(Context ctx) {
        super();
        this.ctx = ctx;

        source.onNext(ConnectionStates.LOADING);
        WhirlpoolFee.getInstance(AndroidSecretPointFactory.getInstance()); // fix for Android

        // set whirlpool log level
        ClientUtils.setLogLevel(Level.WARN, Level.WARN);
    }

    private WhirlpoolWallet getOrOpenWhirlpoolWallet() throws Exception {
        Optional<WhirlpoolWallet> whirlpoolWalletOpt = getWhirlpoolWallet();
        if (!whirlpoolWalletOpt.isPresent()) {
            WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig();
            WhirlpoolDataService dataService = new WhirlpoolDataService(config);

            // wallet closed => open WhirlpoolWallet
            HD_Wallet bip84w = BIP84Util.getInstance(ctx).getWallet();
            String walletIdentifier = whirlpoolUtils.computeWalletIdentifier(bip84w);
            File fileIndex = whirlpoolUtils.computeIndexFile(walletIdentifier, ctx);
            File fileUtxo = whirlpoolUtils.computeUtxosFile(walletIdentifier, ctx);
            return openWallet(dataService, bip84w, fileIndex.getAbsolutePath(), fileUtxo.getAbsolutePath());
        }
        // wallet already opened
        return whirlpoolWalletOpt.get();
    }

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig() throws Exception {
        WebUtil webUtil = WebUtil.getInstance(ctx);
        TorManager torManager = TorManager.INSTANCE;

        String dojoParams = DojoUtil.getInstance(ctx).getDojoParams();
        boolean useDojo = (dojoParams != null);

        boolean testnet = SamouraiWallet.getInstance().isTestNet();
        boolean onion = useDojo || torManager.isRequired();

        Log.v(TAG, "whirlpoolWalletConfig[Tor] = onion="+onion+", useDojo="+useDojo+", torManager.isRequired="+torManager.isRequired());

        String scode = WhirlpoolMeta.getInstance(ctx).getSCODE();

        // backend configuration
        String backendUrl;
        Optional<OAuthManager> oAuthManager;
        if (useDojo) {
            // dojo backend
            backendUrl = DojoUtil.getInstance(ctx).getUrl(dojoParams);
            APIFactory apiFactory = APIFactory.getInstance(ctx);
            oAuthManager = Optional.of(new AndroidOAuthManager(apiFactory));
        } else {
            // samourai backend
            backendUrl = BackendServer.get(testnet).getBackendUrl(onion);
            oAuthManager = Optional.empty();
        }

        IHttpClientService httpClientService = new AndroidHttpClientService(webUtil, torManager);
        IHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.BACKEND);
        BackendApi backendApi = new BackendApi(httpClient, backendUrl, oAuthManager);

        return computeWhirlpoolWalletConfig(torManager, testnet, onion, MIXS_TARGET_DEFAULT, scode, httpClientService, backendApi);
    }

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(TorManager torManager, boolean testnet, boolean onion, int mixsTarget, String scode, IHttpClientService httpClientService, BackendApi backendApi) {
        IStompClientService stompClientService = new AndroidStompClientService(torManager);

        WhirlpoolServer whirlpoolServer = testnet ? WhirlpoolServer.TESTNET : WhirlpoolServer.MAINNET;
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        ServerApi serverApi = new ServerApi(serverUrl, httpClientService);
        NetworkParameters params = whirlpoolServer.getParams();
        WhirlpoolWalletConfig whirlpoolWalletConfig =
                new WhirlpoolWalletConfig(
                        httpClientService, stompClientService, serverApi, params, true, backendApi);

        whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
        whirlpoolWalletConfig.setAutoMix(true); // enable auto-mix

        whirlpoolWalletConfig.setMixsTarget(mixsTarget);
        whirlpoolWalletConfig.setScode(scode);
        whirlpoolWalletConfig.setMaxClients(1);

        whirlpoolWalletConfig.setSecretPointFactory(AndroidSecretPointFactory.getInstance());

        for (Map.Entry<String,String> configEntry : whirlpoolWalletConfig.getConfigInfo().entrySet()) {
            Log.v(TAG, "whirlpoolWalletConfig["+configEntry.getKey()+"] = "+configEntry.getValue());
        }
        return whirlpoolWalletConfig;
    }

    @Override
    protected UtxoSupplier computeUtxoSupplier(WhirlpoolWalletConfig config, WalletSupplier walletSupplier, UtxoConfigSupplier utxoConfigSupplier) {
        APIFactory apiFactory = APIFactory.getInstance(ctx);
        BIP84Util bip84Util = BIP84Util.getInstance(ctx);
        WhirlpoolMeta whirlpoolMeta = WhirlpoolMeta.getInstance(ctx);
        return new AndroidUtxoSupplier(config.getRefreshUtxoDelay(), walletSupplier, utxoConfigSupplier, config.getBackendApi(), computeUtxoChangesListener(), apiFactory, bip84Util, whirlpoolMeta);
    }

    public Completable startService() {
        if (source.hasObservers())
            source.onNext(ConnectionStates.STARTING);
        return Completable.fromCallable(() -> {
            this.getOrOpenWhirlpoolWallet().start();
            if (source.hasObservers()) {
                source.onNext(ConnectionStates.CONNECTED);
            }
            return true;
        });
    }

    public void stop() {
        if (source.hasObservers()) {
            source.onNext(ConnectionStates.DISCONNECTED);
        }
        if (getWhirlpoolWallet().isPresent()) {
            closeWallet();
        }
    }

    public Completable restart() {
        if (!getWhirlpoolWallet().isPresent()) {
            // wallet not opened => nothing to do
            Completable.fromCallable(() -> true);
        }
        Log.v(TAG, "Restarting WhirlpoolWallet...");
        stop();
        return startService();
    }

    public BehaviorSubject<ConnectionStates> listenConnectionStatus() {
        return source;
    }

    public WhirlpoolWallet getWhirlpoolWalletOrNull() {
        return getWhirlpoolWallet().orElse(null);
    }
}
