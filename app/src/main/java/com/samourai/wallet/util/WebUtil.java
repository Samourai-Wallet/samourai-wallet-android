package com.samourai.wallet.util;

import android.content.Context;
//import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import info.guardianproject.onionkit.trust.StrongHttpsClient;
import info.guardianproject.onionkit.ui.OrbotHelper;

public class WebUtil	{

    public static final String BLOCKCHAIN_DOMAIN = "https://blockchain.info/";
    public static final String SAMOURAI_API = "https://api.samouraiwallet.com/";
    public static final String SAMOURAI_API_CHECK = "https://api.samourai.io/status";

    public static final String LBC_EXCHANGE_URL = "https://localbitcoins.com/bitcoinaverage/ticker-all-currencies/";
    public static final String BTCe_EXCHANGE_URL = "https://btc-e.com/api/3/ticker/";
    public static final String BFX_EXCHANGE_URL = "https://api.bitfinex.com/v1/pubticker/btcusd";
    public static final String AVG_EXCHANGE_URL = "https://api.bitcoinaverage.com/ticker/global/all";
    public static final String VALIDATE_SSL_URL = SAMOURAI_API;

    public static final String BTCX_FEE_URL = "http://bitcoinexchangerate.org/fees";

    public static final String CHAINSO_TX_PREV_OUT_URL = "https://chain.so/api/v2/tx/BTC/";
    public static final String CHAINSO_PUSHTX_URL = "https://chain.so/api/v2/send_tx/BTC/";

    public static final String RECOMMENDED_BIP47_URL = "http://samouraiwallet.com/api/v1/get-pcodes";

    private static final int DefaultRequestRetry = 2;
    private static final int DefaultRequestTimeout = 60000;

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

        return postURL(null, request, urlParameters);

    }

    public String postURL(String contentType, String request, String urlParameters) throws Exception {

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
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                connection.setUseCaches (false);

                connection.setConnectTimeout(DefaultRequestTimeout);
                connection.setReadTimeout(DefaultRequestTimeout);

                connection.connect();

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
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
            OrbotHelper oc = new OrbotHelper(context.getApplicationContext());

            if(!oc.isOrbotRunning())  {
                return _getURL(URL);
            }
            else  {
                return tor_getURL(URL);
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
        /*
                throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException,
                KeyStoreException, CertificateException, IOException

         */

        StrongHttpsClient httpclient = new StrongHttpsClient(context.getApplicationContext());

        httpclient.useProxy(true, StrongHttpsClient.TYPE_HTTP, "127.0.0.1", 8118);

        HttpGet httpget = new HttpGet(URL);
        HttpResponse response = httpclient.execute(httpget);

        StringBuffer sb = new StringBuffer();
        sb.append(response.getStatusLine()).append("\n\n");

        InputStream is = response.getEntity().getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }

//        Log.i("WebUtil", sb.toString());
//        Log.i("WebUtil", sb.toString().substring(sb.length() - 10));
        return sb.toString();
    }

}
