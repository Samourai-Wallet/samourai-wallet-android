package com.samourai.wallet.service;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
//import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.NotificationsFactory;
import com.samourai.wallet.R;

import org.bitcoinj.params.MainNetParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketHandler {

    private WebSocket mConnection = null;

    private Timer pingTimer = null;
    private final long pingInterval = 20000L;
    private final long pongTimeout = 5000L;
    private boolean pingPongSuccess = false;

    private String[] addrs = null;

    private static List<String> seenHashes = new ArrayList<String>();

    private static Context context = null;

    public WebSocketHandler(Context ctx, String[] addrs) {
        this.context = ctx;
        this.addrs = addrs;
    }

    public void send(String message) {

        try {
            if (mConnection != null && mConnection.isOpen()) {
                Log.i("WebSocketHandler", "Websocket subscribe:" + message);
                mConnection.sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void subscribe() {

        send("{\"op\":\"blocks_sub\"}");

        for(int i = 0; i < addrs.length; i++) {
            if(addrs[i] != null && addrs[i].length() > 0) {
                send("{\"op\":\"addr_sub\", \"addr\":\""+ addrs[i] + "\"}");
//                    Log.i("WebSocketHandler", "{\"op\":\"addr_sub\",\"addr\":\"" + addrs[i] + "\"}");
            }
        }

    }

    public boolean isConnected() {
        return  mConnection != null && mConnection.isOpen();
    }

    public void stop() {

        stopPingTimer();

        if(mConnection != null && mConnection.isOpen()) {
            mConnection.disconnect();
        }
    }

    public void start() {

        try {
            stop();
            connect();
            startPingTimer();
        }
        catch (IOException | com.neovisionaries.ws.client.WebSocketException e) {
            e.printStackTrace();
        }

    }

    private void connect() throws IOException, WebSocketException
    {
        new ConnectionTask().execute();
    }

    private void updateBalance(final String rbfHash, final String blkHash)    {
        new Thread() {
            public void run() {

                Looper.prepare();

                Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                intent.putExtra("rbf", rbfHash);
                intent.putExtra("notifTx", true);
                intent.putExtra("fetch", true);
                intent.putExtra("hash", blkHash);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                Looper.loop();

            }
        }.start();
    }

    private void updateReceive(final String address)    {
        new Thread() {
            public void run() {

                Looper.prepare();

                Intent intent = new Intent("com.samourai.wallet.ReceiveFragment.REFRESH");
                intent.putExtra("received_on", address);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                Looper.loop();

            }
        }.start();
    }

    private class ConnectionTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... args) {

            try {

                mConnection = new WebSocketFactory()
                        .createSocket(SamouraiWallet.getInstance().isTestNet() ? "wss://api.samourai.io/test/v2/inv" : "wss://api.samourai.io/v2/inv")
                        .addListener(new WebSocketAdapter() {

                            @Override
                            public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                                super.onPongFrame(websocket, frame);
                                pingPongSuccess = true;
                            }

                            public void onTextMessage(WebSocket websocket, String message) {
                                    Log.d("WebSocket", message);
                                try {
                                    JSONObject jsonObject = null;
                                    try {
                                        jsonObject = new JSONObject(message);
                                    } catch (JSONException je) {
//                                            Log.i("WebSocketHandler", "JSONException:" + je.getMessage());
                                        jsonObject = null;
                                    }

                                    if (jsonObject == null) {
//                                            Log.i("WebSocketHandler", "jsonObject is null");
                                        return;
                                    }

//                                        Log.i("WebSocketHandler", jsonObject.toString());

                                    String op = (String) jsonObject.get("op");

                                    if(op.equals("block") && jsonObject.has("x"))    {

                                        JSONObject objX = (JSONObject) jsonObject.get("x");

                                        String hash = null;

                                        if (objX.has("hash")) {
                                            hash = objX.getString("hash");
                                            if(seenHashes.contains(hash)){
                                                return;
                                            }
                                            else    {
                                                seenHashes.add(hash);
                                            }
                                        }

                                        updateBalance(null, hash);

                                        return;
                                    }

                                    if (op.equals("utx") && jsonObject.has("x")) {

                                        JSONObject objX = (JSONObject) jsonObject.get("x");

                                        long value = 0L;
                                        long total_value = 0L;
                                        long ts = 0L;
                                        String in_addr = null;
                                        String out_addr = null;
                                        String hash = null;

                                        if (objX.has("time")) {
                                            ts = objX.getLong("time");
                                        }

                                        if (objX.has("hash")) {
                                            hash = objX.getString("hash");
                                        }

                                        boolean isRBF = false;

                                        if (objX.has("inputs")) {
                                            JSONArray inputArray = (JSONArray) objX.get("inputs");
                                            JSONObject inputObj = null;
                                            for (int j = 0; j < inputArray.length(); j++) {
                                                inputObj = (JSONObject) inputArray.get(j);

                                                if(inputObj.has("sequence") && inputObj.getLong("sequence") <= SamouraiWallet.RBF_SEQUENCE_NO)    {
                                                    isRBF = true;
                                                }

                                                if (inputObj.has("prev_out")) {
                                                    JSONObject prevOutObj = (JSONObject) inputObj.get("prev_out");
                                                    if (prevOutObj.has("value")) {
                                                        value = prevOutObj.getLong("value");
                                                    }
                                                    if (prevOutObj.has("xpub")) {
                                                        total_value -= value;
                                                    }
                                                    else if (prevOutObj.has("addr")) {
                                                        if (in_addr == null) {
                                                            in_addr = (String) prevOutObj.get("addr");
                                                        }
                                                        else {
                                                            ;
                                                        }
                                                    }
                                                    else {
                                                        ;
                                                    }
                                                }
                                            }
                                        }

                                        if (objX.has("out")) {
                                            JSONArray outArray = (JSONArray) objX.get("out");
                                            JSONObject outObj = null;
                                            for (int j = 0; j < outArray.length(); j++) {
                                                outObj = (JSONObject) outArray.get(j);
                                                if (outObj.has("value")) {
                                                    value = outObj.getLong("value");
                                                }
                                                if(outObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr(outObj.getString("addr")) != null)   {
                                                    total_value += value;
                                                    out_addr = outObj.getString("addr");
//                                                    Log.i("WebSocketHandler", "received from " + out_addr);

                                                    String pcode = BIP47Meta.getInstance().getPCode4Addr(outObj.getString("addr"));
                                                    int idx = BIP47Meta.getInstance().getIdx4Addr(outObj.getString("addr"));
                                                    if(pcode != null && idx > -1)    {

                                                        SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                                                        String strTS = sd.format(ts * 1000L);
                                                        String event = strTS + " " + context.getString(R.string.received) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) total_value / 1e8) + " BTC";
                                                        BIP47Meta.getInstance().setLatestEvent(pcode, event);
                                                        List<String> _addrs = new ArrayList<String>();

                                                        idx++;
                                                        for(int i = idx; i < (idx + BIP47Meta.INCOMING_LOOKAHEAD); i++)   {
                                                            PaymentAddress receiveAddress = BIP47Util.getInstance(context).getReceiveAddress(new PaymentCode(pcode), i);
//                                                            Log.i("WebSocketHandler", "receive from " + i + ":" + receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                            BIP47Meta.getInstance().setIncomingIdx(pcode.toString(), i, receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                            BIP47Meta.getInstance().getIdx4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), i);
                                                            BIP47Meta.getInstance().getPCode4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), pcode.toString());

                                                            _addrs.add(receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                        }

                                                        idx--;
                                                        if(idx >= 2)    {
                                                            for(int i = idx; i >= (idx - (BIP47Meta.INCOMING_LOOKAHEAD - 1)); i--)   {
                                                                PaymentAddress receiveAddress = BIP47Util.getInstance(context).getReceiveAddress(new PaymentCode(pcode), i);
//                                                                Log.i("WebSocketHandler", "receive from " + i + ":" + receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                                BIP47Meta.getInstance().setIncomingIdx(pcode.toString(), i, receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                                BIP47Meta.getInstance().getIdx4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), i);
                                                                BIP47Meta.getInstance().getPCode4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), pcode.toString());

                                                                _addrs.add(receiveAddress.getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                            }
                                                        }

                                                        addrs = _addrs.toArray(new String[_addrs.size()]);

                                                        start();

                                                    }
                                                }
                                                else if(outObj.has("addr"))   {
                                                    if(outObj.has("xpub") && outObj.getJSONObject("xpub").has("path") && outObj.getJSONObject("xpub").getString("path").startsWith("M/1/"))    {
                                                        return;
                                                    }
                                                    else    {
                                                        total_value += value;
                                                        out_addr = outObj.getString("addr");
                                                    }
                                                }
                                                else    {
                                                    ;
                                                }
                                            }
                                        }

                                        String title = context.getString(R.string.app_name);
                                        if (total_value > 0L) {
                                            String marquee = context.getString(R.string.received_bitcoin) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) total_value / 1e8) + " BTC";
                                            if (in_addr !=null && in_addr.length() > 0) {
                                                marquee += " from " + in_addr;
                                            }

                                            NotificationsFactory.getInstance(context).setNotification(title, marquee, marquee, R.drawable.ic_launcher, MainActivity2.class, 1000);
                                        }

                                        updateBalance(isRBF ? hash : null, null);

                                        if(out_addr != null)    {
                                            updateReceive(out_addr);
                                        }

                                    }
                                    else {
                                        ;
                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                if(mConnection != null)    {
                    mConnection.connect();
                }

                subscribe();

            }
            catch(Exception e)	{
                e.printStackTrace();
            }

            return null;
        }
    }

    private void startPingTimer(){

        pingTimer = new Timer();
        try {
            pingTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (mConnection != null) {
                        pingPongSuccess = false;
                        if(mConnection.isOpen())   {
                            mConnection.sendPing();
                        }
                        startPongTimer();
                    }
                }
            }, pingInterval, pingInterval);
        }
        catch(IllegalStateException ise) {
            pingTimer = null;
        }
    }

    private void stopPingTimer(){
        if(pingTimer != null) pingTimer.cancel();
    }

    private void startPongTimer(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!pingPongSuccess) {
                    start();
                }
            }
        }, pongTimeout);
    }

}
