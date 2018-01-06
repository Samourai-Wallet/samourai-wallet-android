package com.samourai.wallet.initialization;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.WebUtil;

public class ExchangeRateThread {
    private static final String TAG = LogUtil.getTag();
    
    private Context mContext;
    
    public ExchangeRateThread(Context context) {
        mContext = context;
    }
    
    public void startExchangeRateCheck() {
        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (LogUtil.DEBUG) Log.d(TAG, "exchangeRateThread loop run");
                Looper.prepare();

                String response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.LBC_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(mContext).setDataLBC(response);
                    ExchangeRateFactory.getInstance(mContext).parseLBC();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_usd");
                    ExchangeRateFactory.getInstance(mContext).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(mContext).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_rur");
                    ExchangeRateFactory.getInstance(mContext).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(mContext).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_eur");
                    ExchangeRateFactory.getInstance(mContext).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(mContext).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BFX_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(mContext).setDataBFX(response);
                    ExchangeRateFactory.getInstance(mContext).parseBFX();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {}
                });

                Looper.loop();

            }
        }).start();
    }
}
