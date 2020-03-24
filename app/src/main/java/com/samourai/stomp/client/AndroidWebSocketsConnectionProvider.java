package com.samourai.stomp.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.samourai.wallet.tor.TorManager;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;

import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import ua.naiksoftware.stomp.dto.LifecycleEvent;
import ua.naiksoftware.stomp.provider.AbstractConnectionProvider;

// this class overrides WebSocketsConnectionProvider to use Samourai Tor proxy
public class AndroidWebSocketsConnectionProvider extends AbstractConnectionProvider {

    private static final String TAG = AndroidWebSocketsConnectionProvider.class.getSimpleName();

    private final String mUri;
    @NonNull
    private final Map<String, String> mConnectHttpHeaders;

    private WebSocketClient mWebSocketClient;
    private boolean haveConnection;
    private TreeMap<String, String> mServerHandshakeHeaders;

    private TorManager torManager;

    /**
     * Support UIR scheme ws://host:port/path
     *
     * @param connectHttpHeaders may be null
     */
    public AndroidWebSocketsConnectionProvider(String uri, @Nullable Map<String, String> connectHttpHeaders, TorManager torManager) {
        mUri = uri;
        mConnectHttpHeaders = connectHttpHeaders != null ? connectHttpHeaders : new HashMap<>();
        this.torManager = torManager;
    }

    @Override
    public void rawDisconnect() {
        try {
            mWebSocketClient.closeBlocking();
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted while waiting for Websocket closing: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void createWebSocketConnection() {
        if (haveConnection)
            throw new IllegalStateException("Already have connection to web socket");

        mWebSocketClient = new WebSocketClient(URI.create(mUri), new Draft_6455(), mConnectHttpHeaders, 0) {

            @Override
            public void onWebsocketHandshakeReceivedAsClient(WebSocket conn, ClientHandshake request, @NonNull ServerHandshake response) throws InvalidDataException {
                Log.d(TAG, "onWebsocketHandshakeReceivedAsClient with response: " + response.getHttpStatus() + " " + response.getHttpStatusMessage());
                mServerHandshakeHeaders = new TreeMap<>();
                Iterator<String> keys = response.iterateHttpFields();
                while (keys.hasNext()) {
                    String key = keys.next();
                    mServerHandshakeHeaders.put(key, response.getFieldValue(key));
                }
            }

            @Override
            public void onOpen(@NonNull ServerHandshake handshakeData) {
                Log.d(TAG, "onOpen with handshakeData: " + handshakeData.getHttpStatus() + " " + handshakeData.getHttpStatusMessage());
                LifecycleEvent openEvent = new LifecycleEvent(LifecycleEvent.Type.OPENED);
                openEvent.setHandshakeResponseHeaders(mServerHandshakeHeaders);
                emitLifecycleEvent(openEvent);
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "onMessage: " + message);
                emitMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "onClose: code=" + code + " reason=" + reason + " remote=" + remote);
                haveConnection = false;
                emitLifecycleEvent(new LifecycleEvent(LifecycleEvent.Type.CLOSED));

                Log.d(TAG, "Disconnect after close.");
                disconnect();
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "onError", ex);
                emitLifecycleEvent(new LifecycleEvent(LifecycleEvent.Type.ERROR, ex));
            }
        };

        // Samourai override for TOR proxy
        if (torManager.isRequired()) {
            Proxy proxy = torManager.getProxy();
            Log.v("AndroidStompClient","using proxy: " + proxy.toString());
            mWebSocketClient.setProxy(proxy);
        }

        if (mUri.startsWith("wss")) {
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, null, null);
                SSLSocketFactory factory = sc.getSocketFactory();
                mWebSocketClient.setSocket(factory.createSocket());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mWebSocketClient.connect();
        haveConnection = true;
    }

    @Override
    protected void rawSend(String stompMessage) {
        mWebSocketClient.send(stompMessage);
    }

    @Override
    protected Object getSocket() {
        return mWebSocketClient;
    }
}