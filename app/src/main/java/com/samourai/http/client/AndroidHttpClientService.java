package com.samourai.http.client;

import android.content.Context;

/**
 * HTTP client manager for Whirlpool.
 */
public class AndroidHttpClientService implements IHttpClientService {
    private static AndroidHttpClientService instance;

    public static AndroidHttpClientService getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidHttpClientService(ctx);
        }
        return instance;
    }

    private Context ctx;

    private AndroidHttpClientService(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public AndroidHttpClient getHttpClient(HttpUsage httpUsage) {
        return AndroidHttpClient.getInstance(ctx);
    }
}
