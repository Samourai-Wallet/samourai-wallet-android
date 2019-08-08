package com.samourai.wallet.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.payload.PayloadUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

//import android.util.Log;

public class ExchangeRateFactory {

    private static Context context = null;

    private static String strDataLBC = null;
    private static String strDataBTCe = null;
    private static String strDataBFX = null;

    private static HashMap<String, Double> fxRatesLBC = null;
    private static HashMap<String, Double> fxRatesBTCe = null;
    private static HashMap<String, Double> fxRatesBFX = null;
//    private static HashMap<String,String> fxSymbols = null;

    private static ExchangeRateFactory instance = null;

    private static String[] currencies = {
            "CNY", "EUR", "GBP", "RUB", "USD"
    };

    private static String[] currencyLabels = {
            "United States Dollar - USD",
            "Euro - EUR",
            "British Pound Sterling - GBP",
            "Chinese Yuan - CNY",
            "Russian Rouble - RUB"
    };

    private static String[] currencyLabelsBTCe = {
            "United States Dollar - USD",
            "Euro - EUR",
            "Russian Rouble - RUR"
    };

    private static String[] exchangeLabels = {
            "LocalBitcoins.com",
            "WEX (ex-'BTC-e')",
            "Bitfinex",
    };

    private ExchangeRateFactory() {
        ;
    }

    public static ExchangeRateFactory getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            fxRatesLBC = new HashMap<String, Double>();
            fxRatesBTCe = new HashMap<String, Double>();
            fxRatesBFX = new HashMap<String, Double>();
//            fxSymbols = new HashMap<String,String>();

            instance = new ExchangeRateFactory();
        }

        return instance;
    }

    public double getAvgPrice(String currency) {
        int fxSel = PrefsUtil.getInstance(context).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0);
        HashMap<String, Double> fxRates = null;
        if (fxSel == 1) {
            fxRates = fxRatesBTCe;
        } else if (fxSel == 2) {
            fxRates = fxRatesBFX;
        } else {
            fxRates = fxRatesLBC;
        }

        if (fxRates.get(currency) != null && fxRates.get(currency) > 0.0) {
            PrefsUtil.getInstance(context).setValue("CANNED_" + currency, Double.toString(fxRates.get(currency)));
            return fxRates.get(currency);
        } else {
            return Double.parseDouble(PrefsUtil.getInstance(context).getValue("CANNED_" + currency, "0.0"));
        }
    }

    public String[] getCurrencies() {
        return currencies;
    }

    public String[] getCurrencyLabels() {
        return currencyLabels;
    }

    public String[] getCurrencyLabelsBTCe() {
        return currencyLabelsBTCe;
    }

    public String[] getExchangeLabels() {
        return exchangeLabels;
    }

    public void setDataLBC(String data) {
        strDataLBC = data;
    }

    public void setDataBTCe(String data) {
        strDataBTCe = data;
    }

    public void setDataBFX(String data) {
        strDataBFX = data;
    }

    public void parseLBC() {
        for (int i = 0; i < currencies.length; i++) {
            getLBC(currencies[i]);
        }
    }

    public void parseBTCe() {
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equals("GBP") || currencies[i].equals("CNY")) {
                continue;
            }
            if (currencies[i].equals("RUB")) {
                getBTCe("RUR");
            } else {
                getBTCe(currencies[i]);
            }
        }
    }

    public void parseBFX() {
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equals("USD")) {
                getBFX("USD");
            } else {
                continue;
            }
        }
    }

    public double getBitfinexPrice(String currency) {

        HashMap<String, Double> fxRates = fxRatesBFX;

        if (fxRates.get(currency) != null && fxRates.get(currency) > 0.0) {
            PrefsUtil.getInstance(context).setValue("CANNED_" + currency, Double.toString(fxRates.get(currency)));
            return fxRates.get(currency);
        } else {
            return Double.parseDouble(PrefsUtil.getInstance(context).getValue("CANNED_" + currency, "0.0"));
        }
    }

    public void exchangeRateThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                String response = null;
                try {
                    if (!AppUtil.getInstance(context).isOfflineMode()) {
                        response = WebUtil.getInstance(null).getURL(WebUtil.LBC_EXCHANGE_URL);
                    } else {
                        response = PayloadUtil.getInstance(context).deserializeFX_LBC().toString();
                    }
                    ExchangeRateFactory.getInstance(context).setDataLBC(response);
                    ExchangeRateFactory.getInstance(context).parseLBC();

                    if (!AppUtil.getInstance(context).isOfflineMode()) {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_usd");
                    } else {
                        response = PayloadUtil.getInstance(context).deserializeFX_BTCe_USD().toString();
                    }
                    ExchangeRateFactory.getInstance(context).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(context).parseBTCe();

                    if (!AppUtil.getInstance(context).isOfflineMode()) {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_rur");
                    } else {
                        response = PayloadUtil.getInstance(context).deserializeFX_BTCe_RUR().toString();
                    }
                    ExchangeRateFactory.getInstance(context).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(context).parseBTCe();

                    if (!AppUtil.getInstance(context).isOfflineMode()) {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_eur");
                    } else {
                        response = PayloadUtil.getInstance(context).deserializeFX_BTCe_EUR().toString();
                    }
                    ExchangeRateFactory.getInstance(context).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(context).parseBTCe();

                    if (!AppUtil.getInstance(context).isOfflineMode()) {
                        response = WebUtil.getInstance(null).getURL(WebUtil.BFX_EXCHANGE_URL);
                    } else {
                        response = PayloadUtil.getInstance(context).deserializeFX_BFX().toString();
                    }
                    ExchangeRateFactory.getInstance(context).setDataBFX(response);
                    ExchangeRateFactory.getInstance(context).parseBFX();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Looper.loop();

            }
        }).start();
    }

    private void getLBC(String currency) {
        try {
            JSONObject jsonObject = new JSONObject(strDataLBC);
            if (jsonObject != null) {
                JSONObject jsonCurr = jsonObject.getJSONObject(currency);
                if (jsonCurr != null) {
                    double avg_price = 0.0;
                    if (jsonCurr.has("avg_12h")) {
                        avg_price = jsonCurr.getDouble("avg_12h");
                    } else if (jsonCurr.has("avg_24h")) {
                        avg_price = jsonCurr.getDouble("avg_24h");
                    }
                    fxRatesLBC.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "LBC:" + currency + " " + Double.valueOf(avg_price));
                }
                PayloadUtil.getInstance(context).serializeFX_LBC(jsonObject);
            }
        } catch (JSONException je) {
            fxRatesLBC.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        } catch (IOException | DecryptionException e) {
            ;
        }
    }

    private void getBTCe(String currency) {
        try {
            JSONObject jsonObject = new JSONObject(strDataBTCe);
            if (jsonObject != null) {
                JSONObject jsonCurr = jsonObject.getJSONObject("btc_" + currency.toLowerCase());
                if (jsonCurr != null) {
                    double avg_price = 0.0;
                    if (jsonCurr.has("avg")) {
                        avg_price = jsonCurr.getDouble("avg");
                    }
                    if (currency.equals("RUR")) {
                        fxRatesBTCe.put("RUB", Double.valueOf(avg_price));
                    }
                    fxRatesBTCe.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "BTCe:" + currency + " " + Double.valueOf(avg_price));
                }
                if (currency.equalsIgnoreCase("USD")) {
                    PayloadUtil.getInstance(context).serializeFX_BTCe_USD(jsonObject);
                } else if (currency.equalsIgnoreCase("RUR")) {
                    PayloadUtil.getInstance(context).serializeFX_BTCe_RUR(jsonObject);
                } else if (currency.equalsIgnoreCase("EUR")) {
                    PayloadUtil.getInstance(context).serializeFX_BTCe_EUR(jsonObject);
                } else {
                    ;
                }
            }
        } catch (JSONException je) {
            fxRatesBTCe.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        } catch (IOException | DecryptionException e) {
            ;
        }
    }

    private void getBFX(String currency) {
        try {
            JSONObject jsonObject = new JSONObject(strDataBFX);
            if (jsonObject != null && jsonObject.has("last_price")) {
                String strLastPrice = jsonObject.getString("last_price");
                double avg_price = Double.parseDouble(strLastPrice);
                fxRatesBFX.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "BFX:" + currency + " " + Double.valueOf(avg_price));
                PayloadUtil.getInstance(context).serializeFX_BFX(jsonObject);
            }
        } catch (JSONException je) {
            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        } catch (NumberFormatException nfe) {
            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        } catch (IOException | DecryptionException e) {
            ;
        }
    }

}