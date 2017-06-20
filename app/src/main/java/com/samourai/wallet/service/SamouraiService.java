package com.samourai.wallet.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.WebUtil;

import java.util.Timer;
import java.util.TimerTask;

public class SamouraiService extends Service {

    public static final String ACTION_UPDATE_BALANCE = "com.samourai.wallet.service.UPDATE_BALANCE";
    public static final String ACTION_UPDATE_UTXOS = "com.samourai.wallet.service.UPDATE_UTXOS";
    public static final String ACTION_UPDATE_EXCHANGE_RATES = "com.samourai.wallet.service.UPDATE_EXCHANGERATES";

    public static final int MESSAGE_FROM_SERVICE = 0;

    private IBinder binder = new SamouraiBinder();

    private Context context = null;

    private Timer timer = null;
    private Handler handler = null;

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            ;
        }
    };

    @Override
    public void onCreate() {

        Log.i("SamouraiService", "creating service");

        this.context = this;

        setFilter();

        start();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {

        Log.i("SamouraiService", "exiting service");

        super.onDestroy();
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_BALANCE);
        filter.addAction(ACTION_UPDATE_UTXOS);
        filter.addAction(ACTION_UPDATE_EXCHANGE_RATES);
        registerReceiver(serviceReceiver, filter);
    }

    public class SamouraiBinder extends Binder {
        public SamouraiService getService() {
            return SamouraiService.this;
        }
    }

    private void start()    {

        timer = new Timer();
        handler = new Handler();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("SamouraiService", "do exchange rates");
                        doExchangeRates();
                    }
                });
            }
        }, 2000, 60000 * 15);

//        doAPIUpdate();

    }

    public void stop()    {
        ;
    }

    private void doExchangeRates()  {

        if(ConnectivityStatus.hasConnectivity(SamouraiService.this))    {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();

                    String response = null;
                    try {
                        response = WebUtil.getInstance(null).getURL(WebUtil.LBC_EXCHANGE_URL);
                        ExchangeRateFactory.getInstance(SamouraiService.this).setDataLBC(response);
                        ExchangeRateFactory.getInstance(SamouraiService.this).parseLBC();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }

                    response = null;
                    try {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_usd");
                        ExchangeRateFactory.getInstance(SamouraiService.this).setDataBTCe(response);
                        ExchangeRateFactory.getInstance(SamouraiService.this).parseBTCe();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }

                    response = null;
                    try {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_eur");
                        ExchangeRateFactory.getInstance(SamouraiService.this).setDataBTCe(response);
                        ExchangeRateFactory.getInstance(SamouraiService.this).parseBTCe();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }

                    response = null;
                    try {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_rur");
                        ExchangeRateFactory.getInstance(SamouraiService.this).setDataBTCe(response);
                        ExchangeRateFactory.getInstance(SamouraiService.this).parseBTCe();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }

                    response = null;
                    try {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BFX_EXCHANGE_URL);
                        ExchangeRateFactory.getInstance(SamouraiService.this).setDataBFX(response);
                        ExchangeRateFactory.getInstance(SamouraiService.this).parseBFX();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }

                    Looper.loop();

                }
            }).start();

        }

    }

    private void doAPIUpdate()   {
        Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
        intent.putExtra("notifTx", false);
        intent.putExtra("fetch", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    //
    // calls from Activities
    //
    public void write(byte[] data) {
        ;
    }

}
