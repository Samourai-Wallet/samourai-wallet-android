package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.wallet.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONException;

public class PushTx {

    private static PushTx instance = null;
    private static Context context = null;

    private String ip = "82.221.130.110";
    private int port = 9812;
    private char keystorepass[] = "SamSam".toCharArray();

    private SSLSocket socket = null;
    private ObjectInputStream in = null;
    private OutputStreamWriter out = null;

    private PushTx() { ; }

    public static PushTx getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new PushTx();
        }

        return instance;
    }

    public String samourai(String hexString){

        try {
            KeyStore ks = KeyStore.getInstance("BKS");
            InputStream keyin = context.getResources().openRawResource(R.raw.sslcert);
            ks.load(keyin, keystorepass);
            SSLSocketFactory socketFactory = new SSLSocketFactory(ks);
            socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            socket = (SSLSocket)socketFactory.createSocket(new Socket(ip, port), ip, port, false);
            socket.startHandshake();

            try {
//                Log.i(TAG, "Sending: " + hexString);
                out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
                out.write(hexString, 0, hexString.toString().length());
                out.flush();
            } catch (Exception e) {
                System.err.print(e);
            }

//            Log.i(TAG, "Waiting for "+hexString);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
//                    Log.i(TAG, "No response in 10 seconds.");
                    if(socket!=null && !socket.isClosed())  {
                        try {
                            socket.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }, 10000);//time-out after 10 seconds? just a guess

            if(socket.isClosed())   {
                return null;
            }

            in = new ObjectInputStream(socket.getInputStream());
            final String response = (String)in.readObject();
            timer.cancel();
//            System.out.println("Response: " + response);

            return response;

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {

            try {
                if(in!=null)    {
                    in.close();
                }
                if(out!=null)   {
                    out.close();
                }
                if(socket!=null && !socket.isClosed())  {
                    socket.close();
                }
            } catch (IOException e) {
                ;
            }

        }

        return null;
    }

    public String chainSo(String hexString) {

        try {
            String response = WebUtil.getInstance(null).postURL(WebUtil.CHAINSO_PUSHTX_URL, "tx_hex=" + hexString);
//        Log.i("Send response", response);
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

    public String blockchain(String hexString) {

        try {
            String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "pushtx", "tx=" + hexString);
//        Log.i("Send response", response);
            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

}
