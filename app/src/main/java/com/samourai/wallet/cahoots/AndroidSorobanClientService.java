package com.samourai.wallet.cahoots;

import android.content.Context;
import android.widget.Toast;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.soroban.client.SorobanClientService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;

import io.reactivex.subjects.Subject;

public class AndroidSorobanClientService extends SorobanClientService {
    private static AndroidSorobanClientService instance;
    private static final int TIMEOUT_MS = 60*1000;
    private Context ctx;

    public static AndroidSorobanClientService getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidSorobanClientService(ctx);
        }
        return instance;
    }

    private AndroidSorobanClientService(Context ctx) {
        super(SamouraiWallet.getInstance().getCurrentNetworkParams(),
                new AndroidCahootsWallet(ctx),
                BIP47Util.getInstance(ctx).getWallet(),
                0,  // TODO ZL
                new AndroidHttpClient(WebUtil.getInstance(ctx), TorManager.getInstance(ctx)),
                TIMEOUT_MS);
        this.ctx = ctx;
    }

    @Override
    public synchronized void startListening(PaymentCode paymentCodeInitiator) throws Exception {
        checkTor();

        // stop first
        if (isStartedListening()) {
            try {
                stopListening();
            } catch (Exception e) {
            }
        }

        // start
        super.startListening(paymentCodeInitiator);
    }

    @Override
    public synchronized void stopListening() throws Exception {
        super.stopListening();
        Toast.makeText(ctx, "Stopped looking for Cahoots", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Subject<SorobanMessage> newStonewallx2(long amount, String address, PaymentCode paymentCodeCounterparty) throws Exception {
        checkTor();
        return super.newStonewallx2(amount, address, paymentCodeCounterparty);
    }

    @Override
    public Subject<SorobanMessage> newStowaway(long amount, PaymentCode paymentCodeCounterparty) throws Exception {
        checkTor();
        return super.newStowaway(amount, paymentCodeCounterparty);
    }

    private void checkTor() throws Exception {
        // require Tor
        TorManager torManager = TorManager.getInstance(ctx);
        if (!torManager.isConnected() || !torManager.isRequired()) {
            throw new Exception("Tor connection is required for online Cahoots");
        }
    }
}
