package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.api.client.SamouraiApi;
import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.stomp.client.IStompClientService;
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

import java.io.File;

public class AndroidWhirlpoolWalletService extends WhirlpoolWalletService {
    private static AndroidWhirlpoolWalletService instance;

    public static AndroidWhirlpoolWalletService getInstance() {
        if (instance == null) {
            return new AndroidWhirlpoolWalletService();
        }
        return instance;
    }

    public WhirlpoolWallet getWhirlpoolWallet(Context ctx) throws Exception {
        WhirlpoolFee.getInstance(AndroidSecretPointFactory.getInstance());
        // configure whirlpool
        WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(ctx);
        HD_Wallet bip84w = BIP84Util.getInstance(ctx).getWallet();
        return openWallet(config, bip84w);
    }

    private WhirlpoolWalletConfig computeWhirlpoolWalletConfig(Context ctx) throws Exception {
        IHttpClient httpClient = new AndroidHttpClient(WebUtil.getInstance(ctx));
        IStompClientService stompClientService = new AndroidStompClientService();
        File fileIndex = File.createTempFile("whirlpool-state-", ".json"); // TODO permanent store
        File fileUtxo = File.createTempFile("whirlpool-utxos-", ".json");
        WhirlpoolWalletPersistHandler persistHandler =
                new FileWhirlpoolWalletPersistHandler(fileIndex, fileUtxo);

        WhirlpoolServer whirlpoolServer = WhirlpoolServer.TESTNET; // TODO

        boolean onion = false; // TODO
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        String backendUrl = BackendServer.TESTNET.getBackendUrl(onion);
        SamouraiApi samouraiApi = new SamouraiApi(httpClient, backendUrl, null);

        NetworkParameters params = whirlpoolServer.getParams();
        WhirlpoolWalletConfig whirlpoolWalletConfig =
                new WhirlpoolWalletConfig(
                        httpClient, stompClientService, persistHandler, serverUrl, params, samouraiApi);

        whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
        whirlpoolWalletConfig.setAutoMix(true); // disable auto-mix

        //whirlpoolWalletConfig.setScode("foo"); // TODO
        whirlpoolWalletConfig.setMaxClients(1);

        whirlpoolWalletConfig.setSecretPointFactory(AndroidSecretPointFactory.getInstance());
        whirlpoolWalletConfig.setTx0Service(new AndroidTx0Service(whirlpoolWalletConfig));
        return whirlpoolWalletConfig;
    }
}
