package com.samourai.http.client;

import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;

/**
 * HTTP client manager for Whirlpool.
 */
public class AndroidHttpClientService implements IHttpClientService {
    private WebUtil webUtil;
    private TorManager torManager;

    public AndroidHttpClientService(WebUtil webUtil, TorManager torManager) {
        this.webUtil = webUtil;
        this.torManager = torManager;
    }

    @Override
    public AndroidHttpClient getHttpClient(HttpUsage httpUsage) {
        AndroidHttpClient httpClient = new AndroidHttpClient(webUtil, torManager);
        return httpClient;
    }
}
