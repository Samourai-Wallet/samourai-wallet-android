package com.masanari.http.client;

import com.google.gson.Gson;
import com.masanari.wallet.util.WebUtil;
import com.samourai.http.client.HttpException;
import com.samourai.http.client.IHttpClient;

/**
 * HTTP client used by Whirlpool.
 */
public class AndroidHttpClient implements IHttpClient {
    private Gson gson;
    private WebUtil webUtil;

    public AndroidHttpClient(WebUtil webUtil) {
        this.webUtil = webUtil;
        this.gson = new Gson();
    }

    @Override
    public <T> T parseJson(String url, Class<T> entityClass) throws HttpException {
        try {
            String responseString = webUtil.getURL(url);
            return gson.fromJson(responseString, entityClass);
        }
        catch(Exception e) {
            String responseBody = null; // TODO get server response
            throw new HttpException(e, responseBody);
        }
    }

    @Override
    public void postJsonOverTor(String url, Object body) throws HttpException {
        try {
            String jsonString = gson.toJson(body);
            webUtil.postURL(WebUtil.CONTENT_TYPE_APPLICATION_JSON, url, jsonString);
            //webUtil.tor_postURL("application/json", url, body) // TODO use TOR
        }
        catch(Exception e) {
            String responseBody = e.getMessage();
            throw new HttpException(e, responseBody);
        }
    }

}
