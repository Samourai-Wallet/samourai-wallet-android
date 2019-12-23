package com.samourai.http.client;

import com.google.gson.Gson;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.WebUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;

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
    public <T> Observable<Optional<T>> postJsonOverTor(String url, Class<T> responseType, Map<String, String> headers, Object body) {
        log.debug("postJsonOverTor: "+url);
        return httpObservable(() -> {
            String jsonString = gson.toJson(body);
            String responseString = webUtil.postURL(WebUtil.CONTENT_TYPE_APPLICATION_JSON, url, jsonString, headers);
            T response = null;
            if (responseType != null) {
                response = gson.fromJson(responseString, responseType);
            }
            return response;
        });
    }

    @Override
    public <T> T postUrlEncoded(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> body) throws HttpException {
        log.debug("postUrlEncoded: "+url);
        try {
            String jsonString = queryString(body);
            log.debug("postUrlEncoded json string:" + jsonString);
            String responseString = webUtil.postURL(null, url, jsonString, headers);
            T response = null;
            if (responseType != null) {
                response = gson.fromJson(responseString, responseType);
            }
            return response;
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

    private <T> Observable<Optional<T>> httpObservable(Callable<T> supplier) {
        return Observable.fromCallable(() -> {
            try {
                return Optional.ofNullable(supplier.call());
            }
            catch(Exception e) {
                String responseBody = e.getMessage();
                throw new HttpException(e, responseBody);
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
}
