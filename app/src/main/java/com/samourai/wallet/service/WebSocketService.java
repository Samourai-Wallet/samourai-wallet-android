package com.samourai.wallet.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import org.bitcoinj.crypto.MnemonicException;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.HD_WalletFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
//import android.util.Log;

public class WebSocketService extends Service {

    private Context context = null;

    private Timer timer = new Timer();
    private static final long checkIfNotConnectedDelay = 15000L;
    private WebSocketHandler webSocketHandler = null;
    private final Handler handler = new Handler();
    private String[] xpubs = null;
    private String[] addrs = null;

    @Override
    public void onCreate() {

        super.onCreate();

        //
        context = this.getApplicationContext();

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

        try {
            xpubs = new String[]{ HD_WalletFactory.getInstance(context).get().getAccount(SamouraiWallet.SAMOURAI_ACCOUNT).xpubstr() };
            addrs = BIP47Meta.getInstance().getIncomingLookAhead(context);
            webSocketHandler = new WebSocketHandler(WebSocketService.this, xpubs, addrs);
            connectToWebsocketIfNotConnected();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectToWebsocketIfNotConnected();
                    }
                });
            }
        }, 5000, checkIfNotConnectedDelay);

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