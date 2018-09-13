package com.samourai.wallet.whirlpool;

import com.google.gson.Gson;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.whirlpool.httpClient.IWhirlpoolHttpClient;
import com.samourai.whirlpool.client.whirlpool.httpClient.WhirlpoolHttpException;

/**
 * HTTP client used by Whirlpool.
 */
public class AndroidWhirlpoolHttpClient implements IWhirlpoolHttpClient {
    private Gson gson;
    private WebUtil webUtil;

    public AndroidWhirlpoolHttpClient(WebUtil webUtil) {
        this.webUtil = webUtil;
        this.gson = new Gson();
    }

    @Override
    public <T> T getJsonAsEntity(String url, Class<T> entityClass) throws WhirlpoolHttpException {
        try {
            String responseString = webUtil.getURL(url);
            return gson.fromJson(responseString, entityClass);
        }
        catch(Exception e) {
            String responseBody = null; // TODO get server response
            throw new WhirlpoolHttpException(e, responseBody);
        }
    }

    @Override
    public void postJsonOverTor(String url, Object body) throws WhirlpoolHttpException {
        try {
            String jsonString = gson.toJson(body);
            webUtil.postURL(url, jsonString);
            //webUtil.tor_postURL(url, body) // TODO use TOR
        }
        catch(Exception e) {
            String responseBody = null; // TODO get server response
            throw new WhirlpoolHttpException(e, responseBody);
        }
    }

}
