package com.samourai.wallet.tor;


import android.content.Context;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

import java.net.InetSocketAddress;
import java.net.Proxy;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class TorManager {

    static TorManager instance;


    private Context context;
    private Proxy proxy = null;
    public Subject<Boolean> torStatus = PublishSubject.create();
    private boolean isTorConnecting;
    OnionProxyManager onionProxyManager;

    public static TorManager getInstance(Context context) {
        if (instance == null) {
            instance = new TorManager(context);
        }
        return instance;
    }

    private TorManager(Context context) {
        this.context = context;
        torStatus.onNext(false);
        String fileStorageLocation = "torfiles";
        onionProxyManager = new AndroidOnionProxyManager(context, fileStorageLocation);

    }

    public Observable<Proxy> startTor() {

        return Observable.fromCallable(() -> {
            isTorConnecting = true;

            int totalSecondsPerTorStartup = 4 * 60;
            int totalTriesPerTorStartup = 5;
            try {
                boolean ok = onionProxyManager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                if (!ok) {
                    System.out.println("Couldn't start tor");
                    throw new RuntimeException("Couldn't start tor");
                }
                while (!onionProxyManager.isRunning())
                    Thread.sleep(90);
                System.out.println("Tor initialized on port " + onionProxyManager.getIPv4LocalHostSocksPort());
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", onionProxyManager.getIPv4LocalHostSocksPort()));
                if (torStatus.hasObservers()) {
                    torStatus.onNext(true);
                }
                isTorConnecting = false;
                return proxy;
            } catch (Exception e) {
                isTorConnecting = false;
                if (torStatus.hasObservers()) {
                    torStatus.onNext(false);
                }
                e.printStackTrace();
                return null;
            }
        });

    }

    public String getLatestLogs() {
        if (onionProxyManager != null)
            return onionProxyManager.getLastLog();
        else
            return "";
    }

    Proxy getProxy() {

        return proxy;
    }

    public Observable<Boolean> stopTor() {
        torStatus.onNext(false);
        return Observable.fromCallable(() -> {
            try {
                onionProxyManager.stop();
            } catch (Exception ex) {
                return false;
            }

            return true;
        });

    }

    public Subject<Boolean> getTorStatus() {
        return torStatus;
    }

    Observable<Boolean> isRunning() {
        return Observable.fromCallable(() -> {
            if (onionProxyManager != null) {
                try {

                    return onionProxyManager.isRunning();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return false;
                }
            }
            return false;
        });

    }

}
