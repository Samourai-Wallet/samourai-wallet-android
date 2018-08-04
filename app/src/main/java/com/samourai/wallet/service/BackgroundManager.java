package com.samourai.wallet.service;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BackgroundManager implements Application.ActivityLifecycleCallbacks {

    public static final long CHECK_DELAY = 500L;

    public interface Listener {

        public void onBecameForeground();

        public void onBecameBackground();

    }

    private static BackgroundManager instance = null;

    private Handler handler = new Handler();
    private boolean foreground = false;
    private boolean paused = true;
    private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private Runnable check = null;

    public static BackgroundManager get(Context ctx){
        if (instance == null) {
            instance = new BackgroundManager();
            Context appCtx = ctx.getApplicationContext();
            if (appCtx instanceof Application) {
                ((Application)appCtx).registerActivityLifecycleCallbacks(instance);
            }
//            throw new IllegalStateException("BackgroundManager is not initialised and cannot obtain the Application object");
        }
        return instance;
    }

    public boolean isForeground(){
        return foreground;
    }

    public boolean isBackground(){
        return !foreground;
    }

    public void addListener(Listener listener){
        listeners.add(listener);
    }

    public void removeListener(Listener listener){
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;

        if (check != null)
            handler.removeCallbacks(check);

        if (wasBackground){
            Log.i("BackgroundManager", "return to foreground");
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground();
                } catch (Exception exc) {
                    Log.e("BackgroundManager", "Listener threw exception!", exc);
                }
            }
        } else {
            Log.i("BackgroundManager", "still foreground");
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused = true;

        if (check != null)
            handler.removeCallbacks(check);

        handler.postDelayed(check = new Runnable(){
            @Override
            public void run() {
                if (foreground && paused) {
                    foreground = false;
                    Log.i("BackgroundManager", "went to background");
                    for (Listener l : listeners) {
                        try {
                            l.onBecameBackground();
                        } catch (Exception exc) {
                            Log.e("BackgroundManager", "Listener threw exception!", exc);
                        }
                    }
                } else {
                    Log.i("BackgroundManager", "in foreground");
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
