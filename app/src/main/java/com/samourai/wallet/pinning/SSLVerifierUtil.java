package com.samourai.wallet.pinning;

import android.content.Context;

import com.samourai.wallet.util.WebUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.thoughtcrime.ssl.pinning.util.PinningHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

// openssl s_client -showcerts -connect samourai.io:443

public class SSLVerifierUtil {

    private static SSLVerifierUtil instance = null;
    private static Context context = null;

    public static final int STATUS_POTENTIAL_SERVER_DOWN = 0;
    public static final int STATUS_PINNING_FAIL = 1;
    public static final int STATUS_PINNING_SUCCESS = 2;

    private SSLVerifierUtil() {
        ;
    }

    public static SSLVerifierUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new SSLVerifierUtil();
        }

        return instance;
    }

    public boolean isValidHostname() {

        int responseCode = -1;
        boolean ret = false;

        DefaultHttpClient client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        HostnameVerifier verifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
        socketFactory.setHostnameVerifier((X509HostnameVerifier) verifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
        HttpPost httpPost = new HttpPost(WebUtil.VALIDATE_SSL_URL);
        try {

            HttpResponse response = httpClient.execute(httpPost);
            responseCode = response.getStatusLine().getStatusCode();

            return true;
        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }

    }

    public int certificatePinned() {

        // DER encoded public key:
        // samourai.io
        // 30820222300d06092a864886f70d01010105000382020f003082020a0282020100b1ea258ba61430ba05233fb9c5bcabfee7f157a321bac74236660b46b96537832986c3f6ceb4ff2d6456d85bf7599cad36df493a3f91472bdc6ab6098bd77a0089619c07421eb0f5bdd70133c8f7b671a4624e0e129b5a3e13a40fbfe3bf3d1c88c8fc4aa747ec11f245568754f3305010ccab53ce55bf7794749d8a7c12bdd4497f7bbf60c380d7a9303f016cd3e52a769f51a1d14e6c7348233ab32dc00b2a4abdb6c9299fe120b71a9b385ec91f0b05e99edb3a294de4857e2067faeea9d6c2425dae3b1d76d5c53cdfa9c4b17281efd8bb9d3e6b7a2e99e65c426b638ba98771484e1cd094c37fdcbd2fae40794ea43ffc66df753cb73498f97eb0b37149858411b61000ca5972ba9b1836862c4d05c54b4aef0521a6a2376e03cdc2cd92b7ed9509fc5d7683da7af2d379cf4eba8da6fa37147870bb13495413c6a5fe06e0605ed24b413fb845e38d9ff76256e11fdcd30eceea53161cdea6c93ce84836f48bee6923a877ec762ca1856dcd8c7bd1b8d68467a8ea1916503e9e08dd8111b9ce0ca76a93f68f2692b9c6a43659ee1921550f7d7a30ebf3a0e4081e4112ffc5298cf1956093400690c81af4a1defaf2ea67c3b4b5abf76a4f915d6c2ca6dd911be6705371f2de85dcbe28e85a21bce2979864cc015fa670250d47abe5048d479482d015e26d1f9d81f365115a0cdfb7ddb875ddad030f3cd50f1c61f31a230203010001
        String[] pins = new String[]{"90abe5561984eb7647128b6efef9189d488fd587"};    // SHA-1 hash of DER encoded public key byte array

        URL url = null;
        try {
            url = new URL(WebUtil.VALIDATE_SSL_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpsURLConnection connection = null;
        try {
            connection = PinningHelper.getPinnedHttpsURLConnection(context, pins, url);
        } catch (IOException e) {
            e.printStackTrace();
            return STATUS_POTENTIAL_SERVER_DOWN;
        }

        try {
            byte[] data = new byte[4096];
            connection.getInputStream().read(data);
            return STATUS_PINNING_SUCCESS;

        } catch (IOException e) {
            e.printStackTrace();

            if(e instanceof SSLHandshakeException){
                return STATUS_PINNING_FAIL;
            }else{
                return STATUS_POTENTIAL_SERVER_DOWN;
            }
        }
    }

}
