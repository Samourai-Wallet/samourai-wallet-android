package com.samourai.wallet.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import org.bitcoinj.crypto.MnemonicException;

import com.auth0.android.jwt.JWT;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
//import android.util.Log;

public class WebSocketService extends Service {

    private Context context = null;

    private Timer timer = new Timer();
    private static final long checkIfNotConnectedDelay = 15000L;
    private WebSocketHandler webSocketHandler = null;
    private final Handler handler = new Handler();
    private String[] addrs = null;

    public static List<String> addrSubs = null;

    @Override
    public void onCreate() {

        super.onCreate();

        //
        context = this.getApplicationContext();

        APIFactory.getInstance(context).stayingAlive();

        try {
            if(HD_WalletFactory.getInstance(context).get() == null)    {
                return;
            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
        }

        //
        // prune BIP47 lookbehind
        //
        BIP47Meta.getInstance().pruneIncoming();

        addrSubs = new ArrayList<String>();
        addrSubs.add(AddressFactory.getInstance(context).account2xpub().get(0));
        addrSubs.add(BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr());
        addrSubs.add(BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr());
        addrSubs.addAll(Arrays.asList(BIP47Meta.getInstance().getIncomingLookAhead(context)));
        String[] addrs = addrSubs.toArray(new String[addrSubs.size()]);

        webSocketHandler = new WebSocketHandler(WebSocketService.this, addrs);
        connectToWebsocketIfNotConnected();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void connectToWebsocketIfNotConnected()
    {
        try {
            if(!webSocketHandler.isConnected()) {
                webSocketHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if(webSocketHandler != null)    {
                webSocketHandler.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy()
    {
        stop();
        super.onDestroy();
    }

}