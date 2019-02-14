package com.samourai.wallet.tor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.samourai.wallet.R;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.support.v4.app.NotificationCompat.GROUP_ALERT_SUMMARY;
import static com.samourai.wallet.SamouraiApplication.TOR_CHANNEL_ID;


public class TorService extends Service {

    public static String START_SERVICE = "START_SERVICE";
    public static String STOP_SERVICE = "STOP_SERVICE";
    public static String RESTART_SERVICE = "RESTART_SERVICE";
    public static int TOR_SERVICE_NOTIFICATION_ID   = 95;

    private static final String TAG = "TorService";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String title = "TOR";


    @Override
    public void onCreate() {
        super.onCreate();
//        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                "ExampleApp:Wakelock");
//        wakeLock.acquire();
        Log.d(TAG, "Wakelock acquired");

        Notification notification = new NotificationCompat.Builder(this, TOR_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Waiting...")
                .setOngoing(true)
                .setSound(null)
                .setSmallIcon(R.drawable.ic_launcher)
                .build();

        startForeground(TOR_SERVICE_NOTIFICATION_ID, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Objects.requireNonNull(intent.getAction()).equals(TorService.STOP_SERVICE)) {
            Disposable disposable = TorManager.getInstance(this)
                    .stopTor()
                    .subscribe(stat -> {
                        compositeDisposable.dispose();
                        stopSelf();
                    }, error -> {
                        compositeDisposable.dispose();
                        stopSelf();
                    });
            compositeDisposable.add(disposable);
        }

        if (Objects.requireNonNull(intent.getAction()).equals(TorService.RESTART_SERVICE)) {
            compositeDisposable.clear();
        }

        Disposable disposable = TorManager
                .getInstance(this)
                .startTor()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(proxy -> {
                    title = "Tor: Running...";
                    updateNotification("Running....");
                }, Throwable::printStackTrace);
        compositeDisposable.add(disposable);

        Disposable logger = Observable.interval(2, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> TorManager.getInstance(this).getLatestLogs())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNotification, Throwable::printStackTrace);
        compositeDisposable.add(logger);
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void updateNotification(String content) {
        if (compositeDisposable.isDisposed()) {
            return;
        }
        if (content.isEmpty()) {
            content = "Bootstrapping...";
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, TOR_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true)
                .setSound(null)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("Tor")
                .setGroupSummary(false)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_launcher);
        if (title.contains("Running")) {
            notification.setColorized(true);
            notification.setColor(ContextCompat.getColor(this, R.color.blue));
        } else {
            notification.setColor(ContextCompat.getColor(this, R.color.red));
        }
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.notify(TOR_SERVICE_NOTIFICATION_ID, notification.build());
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
