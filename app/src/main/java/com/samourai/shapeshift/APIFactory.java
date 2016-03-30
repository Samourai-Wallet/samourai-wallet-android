package com.samourai.shapeshift;

import com.samourai.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class APIFactory	{

    private static APIFactory instance = null;

    private static final String API_PUB_KEY = "bfc13499cd599823ef7bbfff599da9a7621468c25d8523a7ca661e6ba7ab190809830ac9b4dcf304ec00beb203ddd41773ee3e1ab86947a59332f80ae53078f0";

    public static final String SHAPESHIFT_URL = "https://shapeshift.io/";
    public static final String SHAPESHIFT_GETCOINS = SHAPESHIFT_URL + "getcoins";
    public static final String SHAPESHIFT_MARKETINFO = SHAPESHIFT_URL + "marketinfo";
    public static final String SHAPESHIFT_RATE = SHAPESHIFT_URL + "rate/";
    public static final String SHAPESHIFT_LIMIT = SHAPESHIFT_URL + "limit/";
    public static final String SHAPESHIFT_SHIFT = SHAPESHIFT_URL + "shift/";
    public static final String SHAPESHIFT_SEND = SHAPESHIFT_URL + "sendamount/";
    public static final String SHAPESHIFT_TX_STATUS = SHAPESHIFT_URL + "txStat/";

    public static final String DEFAULT_PAIR = "dash_btc";

    private APIFactory()	{ ; }

    public static APIFactory getInstance() {

        if (instance == null) {
            instance = new APIFactory();
        }

        return instance;
    }

    public String availableCoins() {
        try {
            return WebUtil.getInstance(null).getURL(SHAPESHIFT_GETCOINS);
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String marketInfo() {
        try {
            return WebUtil.getInstance(null).getURL(SHAPESHIFT_MARKETINFO);
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String rate(String pair) {
        try {
            return WebUtil.getInstance(null).getURL(SHAPESHIFT_RATE + pair);
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String limit(String pair) {
        try {
            return WebUtil.getInstance(null).getURL(SHAPESHIFT_LIMIT + pair);
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String txStatus(String addr) {
        try {
            return WebUtil.getInstance(null).getURL(SHAPESHIFT_TX_STATUS + addr);
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isCoinAvailable(String data, String s) {

        boolean ret = false;
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(data);
            if(jsonObject.has(s.toUpperCase())) {
                ret = true;
            }
        }
        catch(JSONException je) {
            je.printStackTrace();
            jsonObject = null;
            ret = false;
        }

        return ret;
    }

    public boolean isBTCAvailable(String data) {

        boolean ret = false;
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(data);
            if(jsonObject.has("BTC")) {
                ret = true;
            }
        }
        catch(JSONException je) {
            je.printStackTrace();
            jsonObject = null;
            ret = false;
        }

        return ret;
    }

    public String normalTx(String pair, String withdrawalAddress, String returnAddress) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("pair", pair);
            jsonObject.put("withdrawal", withdrawalAddress);
            if(returnAddress != null) {
                jsonObject.put("returnAddress", returnAddress);
            }
            jsonObject.put("apiPubKey", API_PUB_KEY);

            return WebUtil.getInstance(null).postURL("application/json", SHAPESHIFT_SHIFT, jsonObject.toString());
        }
        catch(JSONException je) {
            je.printStackTrace();
            jsonObject = null;
        }
        catch(Exception e) {
            e.printStackTrace();
            jsonObject = null;
        }

        return null;
    }

    public JSONObject fixedAmountTxRequest(double amount, String pair, String withdrawalAddress, String returnAddress) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("amount", amount);
            jsonObject.put("pair", pair);
            jsonObject.put("withdrawal", withdrawalAddress);
            if(returnAddress != null) {
                jsonObject.put("returnAddress", returnAddress);
            }
            jsonObject.put("apiPubKey", API_PUB_KEY);
        }
        catch(JSONException je) {
            je.printStackTrace();
            jsonObject = null;
        }

        return jsonObject;
    }

    public NormalTx normalTxResult(String data) {

        NormalTx ret = null;
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(data);
            if(jsonObject.has("success")) {
                JSONObject payload = jsonObject.getJSONObject("success");

                if(payload.has("withdrawal") && payload.has("withdrawalAmount")
                        && payload.has("deposit") && payload.has("depositAmount")
                        ) {

                    ret = new NormalTx();
                    ret.withdrawal = payload.getString("withdrawal");
                    ret.withdrawalAmount = payload.getDouble("withdrawalAmount");
                    ret.deposit = payload.getString("withdrawal");
                    ret.depositAmount = payload.getDouble("depositAmount");
                    ret.apiPubKey = payload.getString("apiPubKey");
                }
                else {
                    ret = null;
                }
            }
            else {
                ret = null;
            }
        }
        catch(JSONException je) {
            je.printStackTrace();
            jsonObject = null;
            ret = null;
        }

        return ret;
    }

    public FixedAmountTx fixedAmountTxResult(String data) {

        FixedAmountTx ret = null;
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(data);
            if(jsonObject.has("success")) {
                JSONObject payload = jsonObject.getJSONObject("success");

                if(payload.has("pair") && payload.has("withdrawal") && payload.has("withdrawalAmount")
                        && payload.has("deposit") && payload.has("depositAmount")
                        && payload.has("expiration") && payload.has("quotedRate")
                        && payload.has("quotedRate")) {

                    ret = new FixedAmountTx();
                    ret.pair = payload.getString("pair");
                    ret.withdrawal = payload.getString("withdrawal");
                    ret.withdrawalAmount = payload.getDouble("withdrawalAmount");
                    ret.deposit = payload.getString("withdrawal");
                    ret.depositAmount = payload.getDouble("depositAmount");
                    ret.expiration = payload.getLong("expiration");
                    ret.quotedRate = payload.getDouble("quotedRate");
                    ret.apiPubKey = payload.getString("apiPubKey");
                }
                else {
                    ret = null;
                }
            }
            else {
                ret = null;
            }
        }
        catch(JSONException je) {
            je.printStackTrace();
            jsonObject = null;
            ret = null;
        }

        return ret;
    }

    public Tx txStatusResult(String data) {

        Tx ret = null;
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(data);
            if(jsonObject.has("status")) {

                ret = new Tx();
                String status = (String)jsonObject.get("status");
                if(status.equals("received")) {
                    ret.status = "received";
                    ret.incomingCoin = jsonObject.getDouble("incomingCoin");
                    ret.incomingType = jsonObject.getString("incomingType");
                    ret.address = jsonObject.getString("address");
                }
                else if(status.equals("complete")) {
                    ret.status = "complete";
                    ret.incomingCoin = jsonObject.getDouble("incomingCoin");
                    ret.outgoingCoin = jsonObject.getDouble("outgoingCoin");
                    ret.incomingType = jsonObject.getString("incomingType");
                    ret.outgoingType = jsonObject.getString("outgoingType");
                    ret.withdraw = jsonObject.getString("withdraw");
                    ret.transaction = jsonObject.getString("transaction");
                    ret.address = jsonObject.getString("address");
                }
                // assume 'no_deposits'
                else {
                    ret.status = "no_deposits";
                    ret.address = jsonObject.getString("address");
                }
            }
            else {
                ret = null;
            }
        }
        catch(JSONException je) {
            je.printStackTrace();
            jsonObject = null;
            ret = null;
        }

        return ret;
    }

}
