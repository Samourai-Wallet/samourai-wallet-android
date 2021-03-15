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
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.wallet.util.oauth.OAuthManager;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.minerFee.AndroidWalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.protocol.fee.WhirlpoolFee;

import org.bitcoinj.core.NetworkParameters;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import ch.qos.logback.classic.Level;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;
import java8.util.Optional;

public class AndroidWhirlpoolWalletService extends WhirlpoolWalletService {
    private FormatsUtilGeneric formatsUtilGeneric = FormatsUtilGeneric.getInstance();

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

        // set whirlpool log level
        ClientUtils.setLogLevel(Level.WARN, Level.WARN);
    }

    private WhirlpoolWallet getOrOpenWhirlpoolWallet(Context ctx) throws Exception {
        Optional<WhirlpoolWallet> whirlpoolWalletOpt = getWhirlpoolWallet();
        if (!whirlpoolWalletOpt.isPresent()) {
            // make sure utxos are loaded - we need it to initialize Whirlpool
            if (getWhirlpoolWalletResponse() == null) {
                throw new NotifiableException("Wallet is not synchronized yet, please retry later");
            }

            WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(ctx);

            // wallet closed => open WhirlpoolWallet
            HD_Wallet bip84w = BIP84Util.getInstance(ctx).getWallet();
            String walletIdentifier = whirlpoolUtils.computeWalletIdentifier(bip84w);
            File fileIndex = whirlpoolUtils.computeIndexFile(walletIdentifier, ctx);
            File fileUtxo = whirlpoolUtils.computeUtxosFile(walletIdentifier, ctx);
            return openWallet(config, bip84w, fileIndex.getAbsolutePath(), fileUtxo.getAbsolutePath());
        }
        // wallet already opened
        return whirlpoolWalletOpt.get();
    }

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(Context ctx) throws Exception {
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

        IHttpClientService httpClientService = AndroidHttpClientService.getInstance(ctx);
        IHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.BACKEND);
        BackendApi backendApi = new BackendApi(httpClient, backendUrl, oAuthManager);

        return computeWhirlpoolWalletConfig(torManager, testnet, onion, scode, httpClientService, backendApi);
    }

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(TorManager torManager, boolean testnet, boolean onion, String scode, IHttpClientService httpClientService, BackendApi backendApi) {
        IStompClientService stompClientService = new AndroidStompClientService(torManager);
        TorClientService torClientService = new AndroidWhirlpoolTorService(torManager);

        WhirlpoolServer whirlpoolServer = testnet ? WhirlpoolServer.TESTNET : WhirlpoolServer.MAINNET;
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        ServerApi serverApi = new ServerApi(serverUrl, httpClientService);
        NetworkParameters params = whirlpoolServer.getParams();
        WhirlpoolWalletConfig whirlpoolWalletConfig =
                new WhirlpoolWalletConfig(
                        httpClientService, stompClientService, torClientService, serverApi, params, true, backendApi);

        whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
        whirlpoolWalletConfig.setAutoMix(true); // enable auto-mix

        whirlpoolWalletConfig.setScode(scode);
        whirlpoolWalletConfig.setMaxClients(1);
        whirlpoolWalletConfig.setLiquidityClient(false); // disable concurrent liquidity thread

        whirlpoolWalletConfig.setSecretPointFactory(AndroidSecretPointFactory.getInstance());

        for (Map.Entry<String,String> configEntry : whirlpoolWalletConfig.getConfigInfo().entrySet()) {
            Log.v(TAG, "whirlpoolWalletConfig["+configEntry.getKey()+"] = "+configEntry.getValue());
        }
        return whirlpoolWalletConfig;
    }

    @Override
    protected WalletDataSupplier computeWalletDataSupplier(WalletSupplier walletSupplier, PoolSupplier poolSupplier, MessageListener<WhirlpoolUtxoChanges> utxoChangesListener, String utxoConfigFileName, WhirlpoolWalletConfig config) {
        return new AndroidWalletDataSupplier(
                config.getRefreshUtxoDelay(),
                walletSupplier,
                poolSupplier,
                utxoChangesListener,
                utxoConfigFileName,
                config);
    }

    public Completable startService(Context ctx) {
        if (source.hasObservers())
            source.onNext(ConnectionStates.STARTING);
        return Completable.fromCallable(() -> {
            try {
                this.getOrOpenWhirlpoolWallet(ctx).start();
                if (source.hasObservers()) {
                    source.onNext(ConnectionStates.CONNECTED);
                }
                return true;
            } catch (Exception e) {
                // start failed
                stop();
                throw e;
            }
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

    public BehaviorSubject<ConnectionStates> listenConnectionStatus() {
        return source;
    }

    public WhirlpoolWallet getWhirlpoolWalletOrNull() {
        return getWhirlpoolWallet().orElse(null);
    }


    private JSONObject whirlpoolWalletResponse = null;
    public synchronized void setWhirlpoolWalletResponse(JSONObject mixMultiAddrObj) throws Exception {
        // update Whirlpool data
        whirlpoolWalletResponse = mixMultiAddrObj;

        // expire utxos
        WhirlpoolWallet whirlpoolWallet = getWhirlpoolWalletOrNull();
        if (whirlpoolWallet != null) {
            whirlpoolWallet.getUtxoSupplier().expire();
        }
    }

    public JSONObject getWhirlpoolWalletResponse() {
        return whirlpoolWalletResponse;
    }
}
