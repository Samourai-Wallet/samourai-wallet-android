package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.tx0.AndroidTx0Service;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.persist.FileWhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.protocol.fee.WhirlpoolFee;

import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import java8.util.Optional;

public class AndroidWhirlpoolWalletService extends WhirlpoolWalletService {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidWhirlpoolWalletService.class);

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
        WhirlpoolFee.getInstance(AndroidSecretPointFactory.getInstance()); // fix for Android
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
        boolean testnet = true;
        boolean onion = false;
        int mixsTarget = 5;
        String scode = null;

        // TODO dojo backend support
        String backendUrl = BackendServer.get(testnet).getBackendUrl(onion);
        String backendApiKey = null;

        return computeWhirlpoolWalletConfig(ctx, walletIdentifier, testnet, onion, backendUrl, backendApiKey, mixsTarget, scode);
    }

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(Context ctx, String walletIdentifier, boolean testnet, boolean onion, String backendUrl, String backendApiKey, int mixsTarget, String scode) throws Exception {
        IHttpClient httpClient = new AndroidHttpClient(WebUtil.getInstance(ctx));
        IStompClientService stompClientService = new AndroidStompClientService();
        BackendApi backendApi = new BackendApi(httpClient, backendUrl, backendApiKey);

        File fileIndex = whirlpoolUtils.computeIndexFile(walletIdentifier, ctx);
        File fileUtxo = whirlpoolUtils.computeUtxosFile(walletIdentifier, ctx);
        WhirlpoolWalletPersistHandler persistHandler =
                new FileWhirlpoolWalletPersistHandler(fileIndex, fileUtxo);

        WhirlpoolServer whirlpoolServer = testnet ? WhirlpoolServer.TESTNET : WhirlpoolServer.MAINNET;
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        NetworkParameters params = whirlpoolServer.getParams();
        WhirlpoolWalletConfig whirlpoolWalletConfig =
                new WhirlpoolWalletConfig(
                        httpClient, stompClientService, persistHandler, serverUrl, params, backendApi);

        whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
        whirlpoolWalletConfig.setAutoMix(true); // enable auto-mix

        whirlpoolWalletConfig.setMixsTarget(mixsTarget);
        whirlpoolWalletConfig.setScode(scode);
        whirlpoolWalletConfig.setMaxClients(1);

        whirlpoolWalletConfig.setSecretPointFactory(AndroidSecretPointFactory.getInstance());
        whirlpoolWalletConfig.setTx0Service(new AndroidTx0Service(whirlpoolWalletConfig));
        return whirlpoolWalletConfig;
    }

    @Override
    protected WhirlpoolDataService newDataService(WhirlpoolWalletConfig config) {
        return new AndroidWhirlpoolDataService(config, this);
    }
}
