package com.samourai.stomp.client;

import com.google.gson.Gson;
import com.samourai.whirlpool.client.utils.MessageErrorListener;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.LifecycleEvent;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompHeader;
import ua.naiksoftware.stomp.client.StompClient;
import ua.naiksoftware.stomp.client.StompCommand;
import ua.naiksoftware.stomp.client.StompMessage;

public class AndroidStompClient implements IStompClient {
    private Logger log = LoggerFactory.getLogger(AndroidStompClient.class.getSimpleName());
    private static final long TIMEOUT = 20000;
    private Gson gson;
    private StompClient stompClient;

    public AndroidStompClient() {
        this.gson = new Gson();
    }

    @Override
    public void connect(String url, Map<String, String> stompHeaders, final MessageErrorListener<IStompMessage, Throwable> onConnectOnDisconnectListener) {
        try {
            log.info("connecting to " + url);
            stompClient = Stomp.over(Stomp.ConnectionProvider.JWS, url);
            stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<LifecycleEvent>() {
                    @Override
                    public void accept(LifecycleEvent lifecycleEvent) {
                        log.info("connect accept: "+lifecycleEvent.getMessage());
                        switch (lifecycleEvent.getType()) {
                            case OPENED:
                                log.info("connected");
                                // send back headers: no way to get connected headers on Android?
                                onConnectOnDisconnectListener.onMessage(null);
                                break;
                            case ERROR:
                                log.error("Stomp connection error", lifecycleEvent.getException());
                                break;
                            case CLOSED:
                                log.info("disconnected");
                                disconnect();
                                onConnectOnDisconnectListener.onError(new Exception("disconnected"));
                        }
                    }
                });
            List<StompHeader> myHeaders = computeHeaders(stompHeaders);
            stompClient.connect(myHeaders);
        }catch(Exception e) {
            log.error("connect error", e);
            onConnectOnDisconnectListener.onError(new Exception("connect error"));
            throw e;
        }
    }

    @Override
    public String getSessionId() {
        return null; // TODO
    }

    @Override
    public void subscribe(Map<String, String> stompHeaders, final MessageErrorListener<IStompMessage, String> onMessageOnErrorListener) {
        try {
            String destination = getDestination(stompHeaders);
            List<StompHeader> myHeaders = computeHeaders(stompHeaders);
            log.info("subscribing " + destination);
            stompClient.topic(destination, myHeaders)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<StompMessage>() {
                        @Override
                        public void accept(StompMessage stompMessage) throws Exception {
                            try {
                                String messageType = stompMessage.findHeader(WhirlpoolProtocol.HEADER_MESSAGE_TYPE);
                                String jsonPayload = stompMessage.getPayload();
                                Object objectPayload = gson.fromJson(jsonPayload, Class.forName(messageType));
                                AndroidStompMessage androidStompMessage = new AndroidStompMessage(stompMessage, objectPayload);
                                onMessageOnErrorListener.onMessage(androidStompMessage);
                            } catch(Exception e) {
                                log.error("stompClient.accept error", e);
                                onMessageOnErrorListener.onError(e.getMessage());
                            }
                        }
                    });
        }
        catch (Exception e) {
            log.error("subscribe error", e);
            onMessageOnErrorListener.onError(e.getMessage());
        }
        log.info("subscribed");
    }

    @Override
    public void send(Map<String, String> stompHeaders, Object payload) {
        try {
            String destination = getDestination(stompHeaders);
            List<StompHeader> myHeaders = computeHeaders(stompHeaders);
            String jsonPayload = gson.toJson(payload);
            StompMessage stompMessage = new StompMessage(StompCommand.SEND, myHeaders, jsonPayload);

            log.info("sending " + destination + ": " + jsonPayload);
            stompClient.send(stompMessage)
                    .compose(applySchedulers())
                    .subscribe(new Action() {
                        @Override
                        public void run() throws Exception {
                            log.info("send: success");
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            log.info("send: error", throwable);
                        }
                    });
        } catch(Exception e) {
            log.error("send error", e);
        }
    }

    @Override
    public void disconnect() {
        if (stompClient != null) {
            try {
                stompClient.disconnect();
            } catch(Exception e) {}
        }
    }

    @Override
    public IStompClient copyForNewClient() {
        return new AndroidStompClient();
    }

    private String getDestination(Map<String, String> stompHeaders) {
        return stompHeaders.get(StompHeader.DESTINATION);
    }

    private CompletableTransformer applySchedulers() {
        return new CompletableTransformer() {
            @Override
            public CompletableSource apply(Completable upstream) {
                return upstream
                        .unsubscribeOn(Schedulers.newThread())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    private List<StompHeader> computeHeaders(Map<String,String> mapHeaders) {
        List<StompHeader> stompHeaders = new ArrayList<>();
        for (Map.Entry<String,String> entry : mapHeaders.entrySet()) {
            StompHeader stompHeader = new StompHeader(entry.getKey(), entry.getValue());
            stompHeaders.add(stompHeader);
        }
        return stompHeaders;
    }

    /*@Override // TODO
    protected void onDestroy() {
        stompClient.disconnect();
        if (mRestPingDisposable != null) mRestPingDisposable.dispose();
        super.onDestroy();
    }*/
}
