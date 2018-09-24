package util;

import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class MultiClientManager {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<WhirlpoolClient> clients;
    private List<MultiWhirlpoolClientListener> listeners;


    public MultiClientManager(int nbClients) {
        clients = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public MultiWhirlpoolClientListener register(WhirlpoolClient whirlpoolClient) {
        int i=clients.size()+1;
        log.info("Register client#"+i);
        MultiWhirlpoolClientListener listener = new MultiWhirlpoolClientListener();
        listener.setLogPrefix("client#"+i);
        this.clients.add(whirlpoolClient);
        this.listeners.add(listener);
        return listener;
    }

    public void exit() {
        for (WhirlpoolClient whirlpoolClient : clients) {
            if (whirlpoolClient != null) {
                whirlpoolClient.exit();
            }
        }
    }

    public void waitAllClientsSuccess() {
        boolean success = true;
        do {
            for (int i=0; i<clients.size(); i++) {
                MultiWhirlpoolClientListener listener = listeners.get(i);
                if (listener == null) {
                    success = false;
                    log.debug("Client#" + i + ": null");
                } else {
                    log.debug("Client#" + i + ": mixStatus=" + listener.getMixStatus() + ", mixStep=" + listener.getMixStep());
                    if (!MixStatus.SUCCESS.equals(listener.getMixStatus())) {
                        success = false;
                    }
                }
            }
        } while( !success);
    }

    private void debugClients() {
        if (log.isDebugEnabled()) {
            log.debug("%%% debugging clients states... %%%");
            int i=0;
            for (WhirlpoolClient whirlpoolClient : clients) {
                if (whirlpoolClient != null) {
                    MultiWhirlpoolClientListener listener = listeners.get(i);
                    log.debug("Client#" + i + ": mixStatus=" + listener.getMixStatus()+", mixStep=" + listener.getMixStep());
                }
                i++;
            }
        }
    }

    public MultiWhirlpoolClientListener getListener(int i) {
        return listeners.get(i);
    }


}
