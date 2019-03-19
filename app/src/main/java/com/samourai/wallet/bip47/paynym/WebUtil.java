package com.samourai.wallet.bip47.paynym;

import android.content.Context;
import android.util.Log;

import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.R;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.TorUtil;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpDelete;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class WebUtil {

    public static final String PAYNYM_API = "https://paynym.is/";

    private static final int DefaultRequestRetry = 2;
    private static final int DefaultRequestTimeout = 60000;

    private static final String strProxyType = StrongHttpsClient.TYPE_SOCKS;
    private static final String strProxyIP = "127.0.0.1";
    private static final int proxyPort = 9050;

    private static WebUtil instance = null;
    private Context context = null;

    private WebUtil(Context ctx) {
        this.context = ctx;
    }

    public static WebUtil getInstance(Context ctx) {

        if (instance == null) {

            instance = new WebUtil(ctx);
        }

        return instance;
    }

    public String postURL(String request, String urlParameters) throws Exception {

        if (context == null) {
            return postURL(null, null, request, urlParameters);
        } else {
            Log.i("WebUtil", "Tor enabled status:" + TorUtil.getInstance(context).statusFromBroadcast());
            if (TorUtil.getInstance(context).statusFromBroadcast()) {
                if (urlParameters.startsWith("tx=")) {
                    HashMap<String, String> args = new HashMap<String, String>();
                    args.put("tx", urlParameters.substring(3));
                    return tor_postURL(request, args);
                } else {
                    return tor_postURL(request + urlParameters, null);
                }
            } else {
                return postURL(null, null, request, urlParameters);
            }

        }

    }

    public String postURL(String contentType, String authToken, String requestURL, String urlParameters) throws Exception {

        MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(JSON, urlParameters);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();


        builder.callTimeout(DefaultRequestTimeout, TimeUnit.MILLISECONDS);

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }
        Request.Builder rbuilder = new Request.Builder()
                .url(requestURL)
                .addHeader("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType);

        if (authToken != null) {
            rbuilder.addHeader("auth-token", authToken);
        }

        Request request = rbuilder
                .post(body)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();
        }

    }

    public String tor_postURL(String URL, HashMap<String, String> args) throws Exception {

        FormBody.Builder formBodyBuilder = new FormBody.Builder();

        if (args != null) {
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            for (String key : args.keySet()) {
                formBodyBuilder.add(key, args.get(key));
            }
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(TorManager.getInstance(this.context).getProxy());

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }
        Request request = new Request.Builder()
                .url(URL)
                .post(formBodyBuilder.build())
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if(response.body() == null){
                return  "";
            }
            return response.body().string();
        }

    }

}
