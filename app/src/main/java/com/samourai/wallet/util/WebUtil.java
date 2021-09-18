package com.samourai.wallet.util;

import android.content.Context;
import android.util.Log;

import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.tor.TorManager;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import static com.samourai.wallet.util.LogUtil.info;

//import android.util.Log;

public class WebUtil {

    public static final String SAMOURAI_API = "https://api.samouraiwallet.com/";
    public static final String SAMOURAI_API_CHECK = "https://api.samourai.com/v1/status";
    public static final String SAMOURAI_API2 = "https://api.samouraiwallet.com/v2/";
    public static final String SAMOURAI_API2_TESTNET = "https://api.samouraiwallet.com/test/v2/";

    public static final String SAMOURAI_API2_TOR_DIST = "http://d2oagweysnavqgcfsfawqwql2rwxend7xxpriq676lzsmtfwbt75qbqd.onion/v2/";
    public static final String SAMOURAI_API2_TESTNET_TOR_DIST = "http://d2oagweysnavqgcfsfawqwql2rwxend7xxpriq676lzsmtfwbt75qbqd.onion/test/v2/";

    public static String SAMOURAI_API2_TOR = SAMOURAI_API2_TOR_DIST;
    public static String SAMOURAI_API2_TESTNET_TOR = SAMOURAI_API2_TESTNET_TOR_DIST;

    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    private static final int DefaultRequestRetry = 2;
    private static final int DefaultRequestTimeout = 60000;


    private static WebUtil instance = null;
    private Context context = null;

    private WebUtil(Context context) {
        this.context = context;
    }

    public static WebUtil getInstance(Context ctx) {

        if (instance == null) {

            instance = new WebUtil(ctx);
        }

        return instance;
    }

    public String postURL(String request, String urlParameters) throws Exception {
        return postURL(request, urlParameters, (Map<String,String>)null);
    }

    public String postURL(String request, String urlParameters, Map<String, String> headers) throws Exception {

        if (context == null) {
            return postURL(null, request, urlParameters, headers);
        } else {
            Log.v("WebUtil", "Tor required status:" + TorManager.INSTANCE.isRequired());
            if (TorManager.INSTANCE.isRequired()) {
                if (urlParameters.startsWith("tx=")) {
                    HashMap<String, String> args = new HashMap<String, String>();
                    args.put("tx", urlParameters.substring(3));
                    return tor_postURL(request, args, headers);
                } else {
                    return tor_postURL(request + urlParameters, new HashMap(), headers);
                }
            } else {
                return postURL(null, request, urlParameters, headers);
            }

        }

    }

    public String postURL(String contentType, String request, String urlParameters) throws Exception {
        return postURL(contentType, request, urlParameters, null);
    }

    public String postURL(String contentType, String request, String urlParameters, Map<String, String> headers) throws Exception {
        // default headers
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (!headers.containsKey("charset")) {
            headers.put("charset", "utf-8");
        }
        if (!headers.containsKey("User-Agent")) {
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
        }

        String error = null;

        for (int ii = 0; ii < DefaultRequestRetry; ++ii) {
            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));

                // set headers
                for (Map.Entry<String,String> e : headers.entrySet()) {
                    connection.setRequestProperty(e.getKey(), e.getValue());
                }

                connection.setUseCaches(false);

                connection.setConnectTimeout(DefaultRequestTimeout);
                connection.setReadTimeout(DefaultRequestTimeout);

                connection.connect();

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                connection.setInstanceFollowRedirects(false);

                if (connection.getResponseCode() == 200) {
//					System.out.println("postURL:return code 200");
                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
                } else {
                    error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
//                    System.out.println("postURL:return code " + error);
                }

                Thread.sleep(5000);
            } finally {
                connection.disconnect();
            }
        }

        throw new Exception("Invalid Response " + error);
    }

    public String deleteURL(String request, String urlParameters) throws Exception {

        String error = null;

        for (int ii = 0; ii < DefaultRequestRetry; ++ii) {
            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                connection.setUseCaches(false);

                connection.setConnectTimeout(DefaultRequestTimeout);
                connection.setReadTimeout(DefaultRequestTimeout);

                connection.connect();

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                connection.setInstanceFollowRedirects(false);

                if (connection.getResponseCode() == 200) {
//					System.out.println("postURL:return code 200");
                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
                } else {
                    error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
//                    System.out.println("postURL:return code " + error);
                }

                Thread.sleep(5000);
            } finally {
                connection.disconnect();
            }
        }

        throw new Exception("Invalid Response " + error);
    }

    public String getURL(String URL) throws Exception {
        return getURL(URL, null);
    }

    public String getURL(String URL, Map<String,String> headers) throws Exception {
        // default headers
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (!headers.containsKey("charset")) {
            headers.put("charset", "utf-8");
        }
        if (!headers.containsKey("User-Agent")) {
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
        }

        if (context == null) {
            return _getURL(URL, headers);
        } else {
            //if(TorUtil.getInstance(context).orbotIsRunning())    {
            Log.v("WebUtil", "Tor required status:" + TorManager.INSTANCE.isRequired());
            if (TorManager.INSTANCE.isRequired()) {
                return tor_getURL(URL, headers);
            } else {
                return _getURL(URL, headers);
            }

        }

    }

    private String _getURL(String URL, Map<String,String> headers) throws Exception {
        URL url = new URL(URL);

        String error = null;

        for (int ii = 0; ii < DefaultRequestRetry; ++ii) {

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try {
                connection.setRequestMethod("GET");

                // set headers
                for (Map.Entry<String,String> e : headers.entrySet()) {
                    connection.setRequestProperty(e.getKey(), e.getValue());
                }

                connection.setConnectTimeout(DefaultRequestTimeout);
                connection.setReadTimeout(DefaultRequestTimeout);

                connection.setInstanceFollowRedirects(false);

                connection.connect();

                if (connection.getResponseCode() == 200)
                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
                else
                    error = IOUtils.toString(connection.getErrorStream(), "UTF-8");

                Thread.sleep(5000);
            } finally {
                connection.disconnect();
            }
        }

        return error;
    }

    private String tor_getURL(String URL, Map<String,String> headers) throws Exception {

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(TorManager.INSTANCE.getProxy())
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }
        if(URL.contains("onion")){
            getHostNameVerifier(builder);
        }

        Request.Builder rb = new Request.Builder().url(URL);

        // set headers
        if (headers == null) {
            headers = new HashMap<>();
        }
        for (Map.Entry<String,String> e : headers.entrySet()) {
            rb = rb.header(e.getKey(), e.getValue());
        }

        Request request = rb.build();
        try (Response response = builder.build().newCall(request).execute()) {
            if(response.body() == null){
                return  "";
            }
            return response.body().string();

        }

    }

    public String tor_postURL(String URL, Map<String, String> args) throws Exception {
        return tor_postURL(URL, args, null);
    }

    public String tor_postURL(String URL, Map<String, String> args, Map<String,String> headers) throws Exception {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();

        if (args != null && args.size()!=0) {
            for (String key : args.keySet()) {
                formBodyBuilder.add(key, args.get(key));
            }
        }


        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(TorManager.INSTANCE.getProxy())
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS);

        if(URL.contains("onion")){
            getHostNameVerifier(builder);
        }

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        Request.Builder rb = new Request.Builder().url(URL);

        // set headers
        if (headers == null) {
            headers = new HashMap<>();
        }
        for (Map.Entry<String,String> e : headers.entrySet()) {
            rb = rb.header(e.getKey(), e.getValue());
        }

        Request request = rb
                .post(formBodyBuilder.build())
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if(response.body() == null){
                return  "";
            }
            else if(DojoUtil.getInstance(context).getDojoParams() != null)   {
                Headers _headers = response.headers();
                List<String> values = _headers.values("X-Dojo-Version");
                if(values != null && values.size() > 0)   {
                    info("WebUtil", "header:" + values.get(0));
                    DojoUtil.getInstance(context).setDojoVersion(values.get(0));
                }

            }
            else    {
                ;
            }
            return response.body().string();

        }

    }

    public String tor_postURL(String URL, JSONObject args, Map<String,String> headers) throws Exception {
        return tor_postURL(URL, args.toString(), headers);
    }

    public String tor_postURL(String URL, String jsonToString, Map<String,String> headers) throws Exception {
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, jsonToString);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(TorManager.INSTANCE.getProxy());

        if(URL.contains("onion")){
            getHostNameVerifier(builder);
        }

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        Request.Builder rb = new Request.Builder();

        // set headers
        if (headers == null) {
            headers = new HashMap<>();
        }
        for (Map.Entry<String,String> e : headers.entrySet()) {
            rb = rb.header(e.getKey(), e.getValue());
        }

        Request request = rb.url(URL)
                .post(body)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if(response.body() == null){
                return  "";
            }
            return response.body().string();

        }

    }


    private void getHostNameVerifier(OkHttpClient.Builder
                                             clientBuilder) throws
            Exception {

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();


        clientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        clientBuilder.hostnameVerifier((hostname, session) -> true);

    }

    public static String getAPIUrl(Context context){
        if(TorManager.INSTANCE.isRequired()){
            return   SamouraiWallet.getInstance().isTestNet() ? SAMOURAI_API2_TESTNET_TOR : SAMOURAI_API2_TOR;

        }else {
            return   SamouraiWallet.getInstance().isTestNet() ? SAMOURAI_API2_TESTNET : SAMOURAI_API2;
        }

    }

}
