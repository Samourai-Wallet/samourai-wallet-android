package com.samourai.http.client;

import com.google.gson.Gson;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.WebUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * HTTP client used by Whirlpool.
 */
public class AndroidHttpClient implements IHttpClient {
    private Logger log = LoggerFactory.getLogger(AndroidHttpClient.class.getSimpleName());
    private Gson gson;
    private WebUtil webUtil;

    public AndroidHttpClient(WebUtil webUtil) {
        this.webUtil = webUtil;
        this.gson = new Gson();
    }

    @Override
    public <T> T getJson(String url, Class<T> entityClass, Map<String, String> headers) throws HttpException {
        log.debug("getJson: "+url);
        try {
            String responseString = webUtil.getURL(url, headers);
            return gson.fromJson(responseString, entityClass);
        }
        catch(Exception e) {
            String responseBody = e.getMessage();
            throw new HttpException(e, responseBody);
        }
    }

    @Override
    public <T> T postJsonOverTor(String url, Class<T> responseType, Map<String, String> headers, Object body) throws HttpException {
        log.debug("postJsonOverTor: "+url);
        try {
            String jsonString = gson.toJson(body);
            String responseString = webUtil.postURL(WebUtil.CONTENT_TYPE_APPLICATION_JSON, url, jsonString, headers);
            return gson.fromJson(responseString, responseType);
        }
        catch(Exception e) {
            String responseBody = e.getMessage();
            throw new HttpException(e, responseBody);
        }
    }

    @Override
    public <T> T postUrlEncoded(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> body) throws HttpException {
        log.debug("postUrlEncoded: "+url);
        try {
            String jsonString = queryString(body);
            log.debug("postUrlEncoded json string:" + jsonString);
            String responseString = webUtil.postURL(null, url, jsonString, headers);
            return gson.fromJson(responseString, responseType);
        }
        catch(Exception e) {
            String responseBody = e.getMessage();
            throw new HttpException(e, responseBody);
        }
    }

    public String queryString(final Map<String,String> parameters) throws UnsupportedEncodingException {
        String url = "";
        for (Map.Entry<String,String> parameter : parameters.entrySet()) {
            final String encodedKey = URLEncoder.encode(parameter.getKey(), "UTF-8");
            final String encodedValue = URLEncoder.encode(parameter.getValue(), "UTF-8");
            url += encodedKey + "=" + encodedValue+"&";
        }
        return url;
    }


}
