package com.samourai.wallet.tor;

import android.content.Context;

import com.invertedx.torservice.TorProxyManager;
import com.samourai.wallet.util.PrefsUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Proxy;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class TorManager {

    static TorManager instance;

    public static TorManager getInstance(Context context) {
        if (instance == null) {
            instance = new TorManager(context);
        }
        return instance;
    }

    Context context;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    TorProxyManager torProxyManager;
    TorProxyManager.ConnectionStatus status = TorProxyManager.ConnectionStatus.IDLE;

    private TorManager(Context context) {
        this.context = context;
        torProxyManager = new TorProxyManager(context);
        Disposable disposable = torProxyManager.torStatus
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status ->{
                    this.status = status;
                });
        compositeDisposable.add(disposable);
    }

    Observable<Boolean> startTor() {
        return Observable.fromCallable(() -> {
            torProxyManager.startTor();
            return true;
        });
    }

    public boolean isConnected() {
        try {
            return status == TorProxyManager.ConnectionStatus.CONNECTED;
        } catch (Exception Ex) {
            return false;
        }
    }

    public TorProxyManager.ConnectionStatus getStatus() {
        return status;
    }

    public Completable stopTor() {
        return torProxyManager.stopTor();
    }

    public Completable renew() {
        return torProxyManager.newIdentity();
    }

    public BehaviorSubject<TorProxyManager.ConnectionStatus> getTorStatus() {
        return torProxyManager.torStatus;
    }

    public boolean isRequired() {
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.ENABLE_TOR, false);
    }

    public Subject<String> getTorLogs() {
        return torProxyManager.torLogs;
    }

    public Subject<String> getCircuitLogs() {
        return torProxyManager.torCircuitStatus;
    }

    public Subject<Map<String, Long>> getBandWidth() {
        return torProxyManager.bandWidthStatus;
    }

    public Proxy getProxy() {
        return torProxyManager.getProxy();
    }

    public void dispose() {
        compositeDisposable.dispose();
    }

    public void fromJSON(JSONObject jsonPayload) {

        try {

            if (jsonPayload.has("active")) {
                PrefsUtil.getInstance(context).setValue(PrefsUtil.ENABLE_TOR, jsonPayload.getBoolean("active"));
            }

        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();

        try {

            jsonPayload.put("active", PrefsUtil.getInstance(context).getValue(PrefsUtil.ENABLE_TOR, false));

        } catch (JSONException je) {
            ;
        }

        return jsonPayload;
    }

}
