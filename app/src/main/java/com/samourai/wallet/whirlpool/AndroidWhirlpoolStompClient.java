package com.samourai.wallet.whirlpool;

import com.samourai.whirlpool.client.mix.transport.IWhirlpoolStompClient;

import java.util.Map;

import javax.websocket.MessageHandler;

public class AndroidWhirlpoolStompClient implements IWhirlpoolStompClient {
    @Override
    public void connect(String url, Map<String, String> stompHeaders, MessageHandler.Whole<String> onConnect, MessageHandler.Whole<Throwable> onDisconnect) throws Exception {

    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public void subscribe(Map<String, String> stompHeaders, MessageHandler.Whole<Object> onMessage, MessageHandler.Whole<String> onError) {

    }

    @Override
    public void send(Map<String, String> stompHeaders, Object payload) {

    }

    @Override
    public void disconnect() {

    }
}
