package com.samourai.http.client;

import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;

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
public class AndroidHttpClient extends JacksonHttpClient {
    private WebUtil webUtil;
    private TorManager torManager;

    public AndroidHttpClient(WebUtil webUtil, TorManager torManager) {
        this.webUtil = webUtil;
        this.torManager = torManager;
    }

    @Override
    protected String requestJsonGet(String url, Map<String, String> headers) throws Exception {
        return webUtil.getURL(url, headers);
    }

    @Override
    protected String requestJsonPostOverTor(String url, Map<String, String> headers, String jsonBody) throws Exception {
        if (torManager.isRequired()) {
            return webUtil.tor_postURL(url, jsonBody, headers);
        } else {
            return webUtil.postURL(WebUtil.CONTENT_TYPE_APPLICATION_JSON, url, jsonBody, headers);
        }
    }

    @Override
    protected String requestJsonPostUrlEncoded(String url, Map<String, String> headers, Map<String, String> body) throws Exception {
        if (torManager.isRequired()) {
            // tor enabled
            return webUtil.tor_postURL(url, body, headers);
        } else {
            // tor disabled
            String jsonString = queryString(body);
            return webUtil.postURL(null, url, jsonString, headers);
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

    @Override
    protected <T> Observable<Optional<T>> httpObservable(final Callable<T> supplier) {
        Observable<Optional<T>> observable = super.httpObservable(supplier);
        return observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
