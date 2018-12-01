package com.masanari.wallet.util;

import android.content.Context;
import android.util.Log;
//import android.util.Log;

import com.masanari.wallet.R;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpDelete;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import info.guardianproject.netcipher.client.StrongHttpsClient;

public class WebUtil	{

    public static final String MASANARI_API = "https://api.samouraiwallet.com/";
    public static final String MASANARI_API_CHECK = "https://api.samourai.com/v1/status";
    public static final String MASANARI_API2 = "https://api.samouraiwallet.com/v2/";
    public static final String MASANARI_API2_TESTNET = "https://api.samouraiwallet.com/test/v2/";

    public static final String LBC_EXCHANGE_URL = "https://localbitcoins.com/bitcoinaverage/ticker-all-currencies/";
    public static final String KRAKEN_EXCHANGE_URL = "https://api.kraken.com/0/public/Ticker?pair=XBTEUR";
    public static final String BFX_EXCHANGE_URL = "https://api.bitfinex.com/v1/pubticker/btcusd";
    public static final String VALIDATE_SSL_URL = MASANARI_API;

    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    private static final int DefaultRequestRetry = 2;
    private static final int DefaultRequestTimeout = 60000;

    private static final String strProxyType = StrongHttpsClient.TYPE_SOCKS;
    private static final String strProxyIP = "127.0.0.1";
    private static final int proxyPort = 9050;

    /*
    private static final String strProxyType = StrongHttpsClient.TYPE_HTTP;
    private static final String strProxyIP = "127.0.0.1";
    private static final int proxyPort = 8118;
    */

    private static WebUtil instance = null;
    private static Context context = null;

    private WebUtil()   {
        ;
    }

    public static WebUtil getInstance(Context ctx)  {

        context = ctx;
        if(instance == null)  {

            instance = new WebUtil();
        }

        return instance;
    }

    public String postURL(String request, String urlParameters) throws Exception {

        if(context == null) {
            return postURL(null, request, urlParameters);
        }
        else    {
            Log.i("WebUtil", "Tor enabled status:" + TorUtil.getInstance(context).statusFromBroadcast());
            if(TorUtil.getInstance(context).statusFromBroadcast())    {
                if(urlParameters.startsWith("tx="))    {
                    HashMap<String,String> args = new HashMap<String,String>();
                    args.put("tx", urlParameters.substring(3));
                    return tor_postURL(request, args);
                }
                else    {
                    return tor_postURL(request + urlParameters, null);
                }
            }
            else    {
                return postURL(null, request, urlParameters);
            }

        }

    }

    public String postURL(String contentType, String request, String urlParameters) throws Exception {

        String error = null;

        for (int ii = 0; ii < DefaultRequestRetry; ++ii) {
            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                connection.setUseCaches (false);

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
                }
                else {
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
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
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

                connection.setUseCaches (false);

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
                }
                else {
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

        if(context == null) {
            return _getURL(URL);
        }
        else    {
            //if(TorUtil.getInstance(context).orbotIsRunning())    {
            Log.i("WebUtil", "Tor enabled status:" + TorUtil.getInstance(context).statusFromBroadcast());
            if(TorUtil.getInstance(context).statusFromBroadcast())    {
                return tor_getURL(URL);
            }
            else    {
                return _getURL(URL);
            }

        }

    }

    private String _getURL(String URL) throws Exception {

        URL url = new URL(URL);

        String error = null;

        for (int ii = 0; ii < DefaultRequestRetry; ++ii) {

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try {
                connection.setRequestMethod("GET");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

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

    private String tor_getURL(String URL) throws Exception {

        StrongHttpsClient httpclient = new StrongHttpsClient(context, R.raw.debiancacerts);

        httpclient.useProxy(true, strProxyType, strProxyIP, proxyPort);

        HttpGet httpget = new HttpGet(URL);
        HttpResponse response = httpclient.execute(httpget);

        StringBuffer sb = new StringBuffer();
        sb.append(response.getStatusLine()).append("\n\n");

        InputStream is = response.getEntity().getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null)  {
            sb.append(line);
        }

        httpclient.close();

        String result = sb.toString();
//        Log.d("WebUtil", "GET result via Tor:" + result);
        int idx = result.indexOf("{");
        if(idx != -1)    {
            return result.substring(idx);
        }
        else    {
            return result;
        }

    }

    public String tor_postURL(String URL, HashMap<String,String> args) throws Exception {

        StrongHttpsClient httpclient = new StrongHttpsClient(context, R.raw.debiancacerts);

        httpclient.useProxy(true, strProxyType, strProxyIP, proxyPort);

        HttpPost httppost = new HttpPost(new URI(URL));
        httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httppost.setHeader("charset", "utf-8");
        httppost.setHeader("Accept", "application/json");
        httppost.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

        if(args != null)    {
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            for(String key : args.keySet())   {
                urlParameters.add(new BasicNameValuePair(key, args.get(key)));
            }
            httppost.setEntity(new UrlEncodedFormEntity(urlParameters));
        }

        HttpResponse response = httpclient.execute(httppost);

        StringBuffer sb = new StringBuffer();
        sb.append(response.getStatusLine()).append("\n\n");

        InputStream is = response.getEntity().getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null)  {
            sb.append(line);
        }

        httpclient.close();

        String result = sb.toString();
//        Log.d("WebUtil", "POST result via Tor:" + result);
        int idx = result.indexOf("{");
        if(idx != -1)    {
            return result.substring(idx);
        }
        else    {
            return result;
        }

    }

    public String tor_deleteURL(String URL, HashMap<String,String> args) throws Exception {

        StrongHttpsClient httpclient = new StrongHttpsClient(context, R.raw.debiancacerts);

        httpclient.useProxy(true, strProxyType, strProxyIP, proxyPort);

        HttpDelete httpdelete = new HttpDelete(new URI(URL));
        httpdelete.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpdelete.setHeader("charset", "utf-8");
        httpdelete.setHeader("Accept", "application/json");
        httpdelete.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

        if(args != null)    {
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            for(String key : args.keySet())   {
                urlParameters.add(new BasicNameValuePair(key, args.get(key)));
            }
//            httpdelete.setEntity(new UrlEncodedFormEntity(urlParameters));
        }

        HttpResponse response = httpclient.execute(httpdelete);

        StringBuffer sb = new StringBuffer();
        sb.append(response.getStatusLine()).append("\n\n");

        InputStream is = response.getEntity().getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null)  {
            sb.append(line);
        }

        httpclient.close();

        String result = sb.toString();
//        Log.d("WebUtil", "POST result via Tor:" + result);
        int idx = result.indexOf("{");
        if(idx != -1)    {
            return result.substring(idx);
        }
        else    {
            return result;
        }

    }

}
