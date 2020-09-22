package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.cahoots.SorobanCahootsContributor;
import com.samourai.soroban.client.cahoots.SorobanCahootsInitiator;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AppUtil;

import org.bitcoinj.core.NetworkParameters;

import java.security.Security;

import io.reactivex.Observable;

public class AndroidSorobanClientService {
    private static final String PROVIDER = "SC"; // use spongycastle

    private static AndroidSorobanClientService instance;
    private Context ctx;
    private IHttpClient httpClient;
    private ManualCahootsService manualCahootsService;
    private OnlineCahootsService onlineCahootsService;
    private SorobanService sorobanService;
    private SorobanMeetingService sorobanMeetingService;

    private SorobanCahootsInitiator initiator;
    private SorobanCahootsContributor contributor;

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    public static AndroidSorobanClientService getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidSorobanClientService(ctx);
        }
        return instance;
    }

    private AndroidSorobanClientService(Context ctx) {
        this.ctx = ctx;

        BIP47Util bip47Util = BIP47Util.getInstance(ctx);
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
        CahootsWallet cahootsWallet = new AndroidCahootsWallet(ctx);
        BIP47Wallet bip47Wallet = bip47Util.getWallet();
        this.httpClient = AndroidHttpClient.getInstance(ctx);
        this.manualCahootsService = new ManualCahootsService(params, cahootsWallet);
        this.onlineCahootsService = new OnlineCahootsService(params, cahootsWallet);
        this.sorobanService = new SorobanService(bip47Util, params, PROVIDER, bip47Wallet, httpClient);
        this.sorobanMeetingService = new SorobanMeetingService(bip47Util, params, PROVIDER, bip47Wallet, httpClient);

        this.initiator = new SorobanCahootsInitiator(onlineCahootsService, sorobanService, sorobanMeetingService) {
            @Override
            protected void checkTor() throws Exception {
                doCheckTor();
            }
        };
        this.contributor = new SorobanCahootsContributor(onlineCahootsService, sorobanService, sorobanMeetingService) {
            @Override
            protected void checkTor() throws Exception {
                doCheckTor();
            }
        };
    }

    public SorobanCahootsInitiator initiator() {
        return initiator;
    }

    public SorobanCahootsContributor contributor() {
        return contributor;
    }

    protected void doCheckTor() throws Exception {
        // require Tor
        TorManager torManager = TorManager.INSTANCE;
        if (!torManager.isConnected() || !torManager.isRequired()) {
            throw new Exception("Tor connection is required for online Cahoots");
        }

        // require online
        if (AppUtil.getInstance(ctx).isOfflineMode()) {
            throw new Exception("Online mode is required for online Cahoots");
        }
    }

    public ManualCahootsService getManualCahootsService() {
        return manualCahootsService;
    }

    public OnlineCahootsService getOnlineCahootsService() {
        return onlineCahootsService;
    }

    public Observable<SorobanMessage> getOnInteraction() {
        return sorobanService.getOnInteraction();
    }
}
