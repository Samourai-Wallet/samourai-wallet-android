package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.cahoots.SorobanCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.soroban.cahoots.ManualCahootsService;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AppUtil;

import org.bitcoinj.core.NetworkParameters;

import java.security.Provider;
import java.security.Security;

public class AndroidSorobanCahootsService extends SorobanCahootsService {
    private static final Provider PROVIDER = new org.spongycastle.jce.provider.BouncyCastleProvider(); // use spongycastle

    private static AndroidSorobanCahootsService instance;
    private Context ctx;
    private ManualCahootsService manualCahootsService;

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    public static AndroidSorobanCahootsService getInstance(Context ctx) {
        if (instance == null) {
            CahootsWallet cahootsWallet = new AndroidCahootsWallet(ctx);
            BIP47Util bip47Util = BIP47Util.getInstance(ctx);
            NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
            BIP47Wallet bip47Wallet = bip47Util.getWallet();
            IHttpClient httpClient = AndroidHttpClient.getInstance(ctx);
            boolean onion = TorManager.INSTANCE.isRequired();
            RpcClient rpcClient = new RpcClient(httpClient, onion, params);
            instance = new AndroidSorobanCahootsService(
                    new OnlineCahootsService(cahootsWallet),
                    new SorobanService(bip47Util, params, PROVIDER, bip47Wallet, rpcClient),
                    new SorobanMeetingService(bip47Util, params, PROVIDER, bip47Wallet, rpcClient),
                    new ManualCahootsService(cahootsWallet),
                    ctx
            );
        }
        return instance;
    }

    private AndroidSorobanCahootsService(OnlineCahootsService onlineCahootsService,
                                         SorobanService sorobanService,
                                         SorobanMeetingService sorobanMeetingService,
                                         ManualCahootsService manualCahootsService,
                                         Context ctx) {
        super(onlineCahootsService, sorobanService, sorobanMeetingService);
        this.ctx = ctx;
        this.manualCahootsService = manualCahootsService;
    }

    @Override
    protected void checkTor() throws Exception {
        // require online
        if (AppUtil.getInstance(ctx).isOfflineMode()) {
            throw new Exception("Online mode is required for online Cahoots");
        }
    }

    public ManualCahootsService getManualCahootsService() {
        return manualCahootsService;
    }
}
