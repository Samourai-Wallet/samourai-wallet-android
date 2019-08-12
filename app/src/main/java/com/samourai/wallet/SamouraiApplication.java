package com.samourai.wallet;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.support.v4.app.NotificationCompat;

import com.samourai.wallet.tor.TorService;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.PrefsUtil;

public class SamouraiApplication extends Application {

    public static String TOR_CHANNEL_ID = "TOR_CHANNEL";
    public static String FOREGROUND_SERVICE_CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL_ID";

    @Override
    public void onCreate() {
        super.onCreate();
        setUpChannels();
        if (PrefsUtil.getInstance(this).getValue(PrefsUtil.ENABLE_TOR, false)) {
            startService();
        }

    }

    public void startService() {
        if (ConnectivityStatus.hasConnectivity(getApplicationContext()) && PrefsUtil.getInstance(getApplicationContext()).getValue(PrefsUtil.ENABLE_TOR, false)) {
            Intent startIntent = new Intent(this, TorService.class);
            startIntent.setAction(TorService.START_SERVICE);
            startService(startIntent);
        }
    }

    private void setUpChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    TOR_CHANNEL_ID,
                    "Tor service ",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);

            NotificationChannel refreshService = new NotificationChannel(
                    FOREGROUND_SERVICE_CHANNEL_ID,
                    "Samourai Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            refreshService.setSound(null, null);
            refreshService.setImportance(NotificationManager.IMPORTANCE_LOW);
            refreshService.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(refreshService);
            }
        }
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
