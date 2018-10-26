package com.samourai.stomp.client;

import ua.naiksoftware.stomp.client.StompMessage;

public class AndroidStompMessage implements IStompMessage {
    private Object payload;
    private StompMessage stompMessage;

    public AndroidStompMessage(StompMessage stompMessage, Object payload) {
        this.payload = payload;
        this.stompMessage = stompMessage;
    }

    @Override
    public String getStompHeader(String headerName) {
        return stompMessage.findHeader(headerName);
    }

    @Override
    public Object getPayload() {
        return payload;
    }
}
