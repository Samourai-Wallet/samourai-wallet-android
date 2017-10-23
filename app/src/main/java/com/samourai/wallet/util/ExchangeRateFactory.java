package com.samourai.wallet.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.regex.*;

//import android.util.Log;

public class ExchangeRateFactory	{

    private static Context context = null;

    private static String strDataLBC = null;
    private static String strDataBTCe = null;
    private static String strDataBFX = null;
    private static String strDataLuno = null;

    private static HashMap<String,Double> fxRatesLBC = null;
    private static HashMap<String,Double> fxRatesBTCe = null;
    private static HashMap<String,Double> fxRatesBFX = null;
    private static HashMap<String,Double> fxRatesLuno = null;
//    private static HashMap<String,String> fxSymbols = null;

    private static ExchangeRateFactory instance = null;

    private static String[] currencies = {
            "CNY", "EUR", "GBP", "RUB", "USD", "ZAR"
    };

    private static String[] currencyLabels = {
            "United States Dollar - USD",
            "Euro - EUR",
            "British Pound Sterling - GBP",
            "Chinese Yuan - CNY",
            "Russian Rouble - RUB",
            "South African Rand - ZAR"
    };

    private static String[] currencyLabelsBTCe = {
            "United States Dollar - USD",
            "Euro - EUR",
            "Russian Rouble - RUR"
    };

    private static String[] lunoCurrencies = {
            "ZAR", "NGN", "MYR", "IDR"
    };

    private static String[] currencyLabelsLuno = {
            "South African Rand - ZAR",
            "Nigerian Naira - NGN",
            "Malaysian Ringgit - MYR",
            "Indonesian Rupiah - IDR"
    };

    private static String[] exchangeLabels = {
            "LocalBitcoins.com",
//            "BTC-e",
            "Bitfinex",
            "Luno"
    };

    private ExchangeRateFactory()	 { ; }

    public static ExchangeRateFactory getInstance(Context ctx)	 {

        context = ctx;

        if(instance == null)	 {
            fxRatesLBC = new HashMap<String,Double>();
            fxRatesBTCe = new HashMap<String,Double>();
            fxRatesBFX = new HashMap<String,Double>();
            fxRatesLuno = new HashMap<String,Double>();
//            fxSymbols = new HashMap<String,String>();

            instance = new ExchangeRateFactory();
        }

        return instance;
    }

    public double getAvgPrice(String currency)	 {
        int fxSel = PrefsUtil.getInstance(context).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0);
        HashMap<String,Double> fxRates = null;
        /*
        if(fxSel == 1)	 {
            fxRates = fxRatesBTCe;
        }
        */
        if(fxSel == 1)	 {
            fxRates = fxRatesBFX;
        }else if(fxSel == 2){
            fxRates = fxRatesLuno;
        }
        else	 {
            fxRates = fxRatesLBC;
        }

        if(fxRates.get(currency) != null && fxRates.get(currency) > 0.0)	 {
            PrefsUtil.getInstance(context).setValue("CANNED_" + currency, Double.toString(fxRates.get(currency)));
            return fxRates.get(currency);
        }
        else	 {
            return Double.parseDouble(PrefsUtil.getInstance(context).getValue("CANNED_" + currency, "0.0"));
        }
    }

    public String[] getCurrencies()	 {
        return currencies;
    }

    public String[] getCurrencyLabels()	 {
        return currencyLabels;
    }

    public String[] getCurrencyLabelsBTCe()	 {
        return currencyLabelsBTCe;
    }

    public String[] getCurrencyLabelsLuno()	 {
        return currencyLabelsLuno;
    }

    public String[] getExchangeLabels()	 {
        return exchangeLabels;
    }

    public void setDataLBC(String data)	 {
        strDataLBC = data;
    }

    public void setDataBTCe(String data)	 {
        strDataBTCe = data;
    }

    public void setDataBFX(String data)	 {
        strDataBFX = data;
    }

    public void setDataLuno(String data)	 {
        strDataLuno = data;
    }

    public void parseLBC()	 {
        for(int i = 0; i < currencies.length; i++)	 {
            getLBC(currencies[i]);
        }
    }

    public void parseBTCe()	 {
        for(int i = 0; i < currencies.length; i++)	 {
            if(currencies[i].equals("GBP") || currencies[i].equals("CNY"))	 {
                continue;
            }
            if(currencies[i].equals("RUB"))	 {
                getBTCe("RUR");
            }
            else	 {
                getBTCe(currencies[i]);
            }
        }
    }

    public void parseBFX()	 {
        for(int i = 0; i < currencies.length; i++)	 {
            if(currencies[i].equals("USD"))	 {
                getBFX("USD");
            }
            else	 {
                continue;
            }
        }
    }

    public void parseLuno()	 {
        getLuno();
    }

//    public void parseLuno()	 {
//        for(int i = 0; i < currencies.length; i++)	 {
//            if(currencies[i].equals("ZAR"))	 {
//                getLuno("ZAR");
//            }
//            else	 {
//                continue;
//            }
//        }
//    }

    public double getBitfinexPrice(String currency)	 {

        HashMap<String,Double> fxRates = fxRatesBFX;

        if(fxRates.get(currency) != null && fxRates.get(currency) > 0.0)	 {
            PrefsUtil.getInstance(context).setValue("CANNED_" + currency, Double.toString(fxRates.get(currency)));
            return fxRates.get(currency);
        }
        else	 {
            return Double.parseDouble(PrefsUtil.getInstance(context).getValue("CANNED_" + currency, "0.0"));
        }
    }

    private void getLBC(String currency)	 {
        try {
            JSONObject jsonObject = new JSONObject(strDataLBC);
            if(jsonObject != null)	{
                JSONObject jsonCurr = jsonObject.getJSONObject(currency);
                if(jsonCurr != null)	{
                    double avg_price = 0.0;
                    if(jsonCurr.has("avg_12h"))	{
                        avg_price = jsonCurr.getDouble("avg_12h");
                    }
                    else if(jsonCurr.has("avg_24h"))	{
                        avg_price = jsonCurr.getDouble("avg_24h");
                    }
                    fxRatesLBC.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "LBC:" + currency + " " + Double.valueOf(avg_price));
                }
            }
        } catch (JSONException je) {
            fxRatesLBC.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        }
    }

    private void getBTCe(String currency)	 {
        try {
            JSONObject jsonObject = new JSONObject(strDataBTCe);
            if(jsonObject != null)	{
                JSONObject jsonCurr = jsonObject.getJSONObject("btc_" + currency.toLowerCase());
                if(jsonCurr != null)	{
                    double avg_price = 0.0;
                    if(jsonCurr.has("avg"))	{
                        avg_price = jsonCurr.getDouble("avg");
                    }
                    if(currency.equals("RUR"))	{
                        fxRatesBTCe.put("RUB", Double.valueOf(avg_price));
                    }
                    fxRatesBTCe.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "BTCe:" + currency + " " + Double.valueOf(avg_price));
                }
            }
        } catch (JSONException je) {
            fxRatesBTCe.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(cur c crency, null);
        }
    }

    private void getBFX(String currency)	 {
        try {
            JSONObject jsonObject = new JSONObject(strDataBFX);
            if(jsonObject != null && jsonObject.has("last_price"))	{
                String strLastPrice = jsonObject.getString("last_price");
                double avg_price = Double.parseDouble(strLastPrice);
                fxRatesBFX.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "BFX:" + currency + " " + Double.valueOf(avg_price));
            }
        }
        catch (JSONException je) {
            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        }
        catch (NumberFormatException nfe) {
            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        }
    }

//    private void getLuno(String currency)	 {
//        try {
//            JSONObject jsonObject = new JSONObject(strDataLuno);
//            if(jsonObject != null && jsonObject.has("last_trade"))	{
//                String strLastPrice = jsonObject.getString("last_trade");
//                double avg_price = Double.parseDouble(strLastPrice);
//                fxRatesLuno.put(currency, Double.valueOf(avg_price));
//                Log.i("ExchangeRateFactory", "Luno:" + currency + " " + Double.valueOf(avg_price));
//            }
//        }
//        catch (JSONException je) {
//            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//        }
//        catch (NumberFormatException nfe) {
//            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//        }
//    }


//    try{
//        JSONObject jsonObject = new JSONObject(strDataLuno);
//        Log.i("ExchangeRateFactory", jsonObject.toString());
//        Log.i("ExchangeRateFactory", "test");
//        if(jsonObject != null){
//            double last_trade = 0.0;
//            Log.i("ExchangeRateFactory", "XBT"+lunoCurrency);
//
//            //begin loop through the JSON string
//            for(){
//
//            }
//
//            String strLastPrice = jsonObject.getString("last_trade");
//            last_trade = Double.parseDouble(strLastPrice);
//            Log.i("ExchangeRateFactory", "test2");
//            Log.i("ExchangeRateFactory", "last_trade" + last_trade);
//            if(strLastPrice != null){
//                fxRatesLuno.put(lunoCurrency, Double.valueOf(last_trade));
//                Log.i("ExchangeRateFactory", "Luno:" + lunoCurrency + " " + Double.valueOf(last_trade));
//            }
//        }
//    }catch(JSONException je){
//        fxRatesLuno.put(lunoCurrency, Double.valueOf(-1.0));
//    }

    private void getLuno(){
        Pattern pattern = Pattern.compile("(\"timestamp\")(.*?\")(.*?\")(.*?\")(.*?\")(.*?\")(.*?\")(.*?\")(.*?\".)|(.)(\"rolling_24_hour_volume\")(.*?\")(.*?\",)|(\\{\"tickers\":[\\{)|\\{|\\}]\\}|\\}|,)");
        Matcher matcher = pattern.matcher(strDataLuno);
        String split[] = pattern.split(strDataLuno);

        double last_trade = 0;
        String pair = "";
        int triggerAdd = 0;
        for (int y = 0; y < split.length; y++) {
            if(split[y].contains("last_trade")){
                String secondSplit[] = split[y].split(":");
                secondSplit[1] = secondSplit[1].replace("\"","");
                last_trade = Double.parseDouble(secondSplit[1]);
                triggerAdd++;
            }else if(split[y].contains("pair")){
                String secondSplit[] = split[y].split(":");
                secondSplit[1] = secondSplit[1].replace("\"","");
                pair = secondSplit[1];
                triggerAdd++;
            }

            if(triggerAdd == 2){
                fxRatesLuno.put(pair.replace("XBT",""), last_trade);
                triggerAdd = 0;
            }
        }

    }


}
