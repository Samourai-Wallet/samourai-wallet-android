package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.SorobanCahootsContributor;
import com.samourai.soroban.client.cahoots.SorobanCahootsInitiator;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AppUtil;

import org.bitcoinj.core.NetworkParameters;

public class AndroidSorobanClientService {
    private static AndroidSorobanClientService instance;
    private Context ctx;
    private IHttpClient httpClient;
    private CahootsService cahootsService;
    private SorobanService sorobanService;
    private SorobanMeetingService sorobanMeetingService;

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
        this.cahootsService = new CahootsService(params, cahootsWallet);
        this.sorobanService = new SorobanService(bip47Util, params, bip47Wallet, httpClient);
        this.sorobanMeetingService = new SorobanMeetingService(bip47Util, params, bip47Wallet, httpClient);
    }

    public SorobanCahootsInitiator initiator(int account) {
        return new SorobanCahootsInitiator(cahootsService, sorobanService, sorobanMeetingService) {

            @Override
            protected void checkTor() throws Exception {
                doCheckTor();
            }
        };
    }

    public SorobanCahootsContributor contributor(int account) {
        return new SorobanCahootsContributor(cahootsService, sorobanService, sorobanMeetingService) {

            @Override
            protected void checkTor() throws Exception {
                doCheckTor();
            }
        };
    }

    protected void doCheckTor() throws Exception {
        // require Tor
        TorManager torManager = TorManager.getInstance(ctx);
        if (!torManager.isConnected() || !torManager.isRequired()) {
            throw new Exception("Tor connection is required for online Cahoots");
        }

        // require online
        if (AppUtil.getInstance(ctx).isOfflineMode()) {
            throw new Exception("Online mode is required for online Cahoots");
        }
    }

    public CahootsService getCahootsService() {
        return cahootsService;
    }
}
