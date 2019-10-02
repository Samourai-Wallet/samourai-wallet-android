package com.samourai.stomp.client;

public class AndroidStompClientService implements IStompClientService {
    @Override
    public IStompClient newStompClient() {
        return new AndroidStompClient();
    }
}
