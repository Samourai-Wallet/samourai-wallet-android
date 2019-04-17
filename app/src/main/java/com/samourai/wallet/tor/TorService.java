package com.samourai.wallet.tor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.rtp.AudioStream;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.common.base.Verify;
import com.samourai.wallet.R;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.TorUtil;

import java.net.ConnectException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.support.v4.app.NotificationCompat.GROUP_ALERT_SUMMARY;
import static com.samourai.wallet.SamouraiApplication.TOR_CHANNEL_ID;


public class TorService extends Service {

    public static String START_SERVICE = "START_SERVICE";
    public static String STOP_SERVICE = "STOP_SERVICE";
    public static String RESTART_SERVICE = "RESTART_SERVICE";
    public static int TOR_SERVICE_NOTIFICATION_ID = 95;
    private static final String TAG = "TorService";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String title = "TOR";
    private PendingIntent contentIntent;
    private Disposable torDisposable;


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
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("Tor")
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_launcher)
                .build();

        startForeground(TOR_SERVICE_NOTIFICATION_ID, notification);

    }


    private NotificationCompat.Action getAction(String message) {

        Intent broadcastIntent = new Intent(this, TorBroadCastReceiver.class);
        broadcastIntent.setAction(STOP_SERVICE);

        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action(R.drawable.tor_on, message, actionIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Objects.requireNonNull(intent.getAction()).equals(TorService.STOP_SERVICE)) {
            Disposable disposable = TorManager.getInstance(getApplicationContext())
                    .stopTor()
                    .subscribe(stat -> {
                        compositeDisposable.dispose();
                        stopSelf();
                    }, error -> {
//                        compositeDisposable.dispose();
//                        stopSelf();
                    });
            compositeDisposable.add(disposable);

        } else if (intent.getAction().equals(TorService.RESTART_SERVICE)) {
            title = "Tor: Disconnected";
            if (TorManager
                    .getInstance(getApplicationContext()).isConnected()) {
                stopTOr();
            } else {
                startTOR();
            }
            return START_STICKY;
        } else if (Objects.requireNonNull(intent.getAction()).equals(TorService.START_SERVICE)) {
            if (TorManager.getInstance(getApplicationContext()).isProcessRunning) {
                restartTorProcess();
            } else {
                startTOR();
            }
        }

        return START_STICKY;

    }

    private void startTOR() {
        title = "Tor: Waiting";
        updateNotification("Connecting....");
        if (torDisposable != null) {
            compositeDisposable.delete(torDisposable);
            Log.i(TAG, "startTOR: ".concat(String.valueOf(torDisposable.isDisposed())));
        }

        torDisposable = TorManager
                .getInstance(getApplicationContext())
                .startTor()
                .debounce(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(proxy -> {
                    title = "Tor: Running";
                    updateNotification("Running....");
                }, Throwable::printStackTrace);
        compositeDisposable.add(torDisposable);


        Disposable statusDisposable = TorManager.getInstance(this)
                .torStatus
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {

                    if (state == TorManager.CONNECTION_STATES.CONNECTED) {
                        TorUtil.getInstance(this).setStatusFromBroadcast(true);
                    } else {
                        TorUtil.getInstance(this).setStatusFromBroadcast(false);
                    }
                });
        logger();
        compositeDisposable.add(statusDisposable);
    }

    private void logger() {
        Disposable logger = Observable.interval(2, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> TorManager.getInstance(getApplicationContext()).getLatestLogs())
                .observeOn(AndroidSchedulers.mainThread())
                .retryWhen(errors -> errors.zipWith(Observable.range(1, 3), (n, i) -> i))
                .subscribe(this::updateNotification, error -> {
                    error.printStackTrace();
                    logger();
                    updateNotification("Disconnected");
                });
        compositeDisposable.add(logger);

    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void updateNotification(String content) {
//        Log.i(TAG, "Tor Log: ".concat(content));
        if (content.isEmpty()) {
            content = "Bootstrapping...";
        }
        if (TorManager.getInstance(this).state == TorManager.CONNECTION_STATES.CONNECTED) {
            title = "Tor: Connected";
        }

        if (TorManager.getInstance(this).state == TorManager.CONNECTION_STATES.DISCONNECTED) {
            title = "Tor: Disconnected";
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, TOR_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("Tor")
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_launcher);
        switch (TorManager.getInstance(getApplicationContext()).state) {
            case CONNECTED: {
                notification.setColorized(true);
                notification.addAction(getAction("Stop"));
                notification.setColor(ContextCompat.getColor(this, R.color.green_ui_2));
                break;
            }
            case CONNECTING: {
                break;
            }
            case DISCONNECTED: {
                notification.addAction(getAction("Stop"));
                notification.setColor(ContextCompat.getColor(this, R.color.red));
                break;
            }
        }
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.notify(TOR_SERVICE_NOTIFICATION_ID, notification.build());
        }

    }

    private void restartTorProcess() {
        if (TorManager.getInstance(getApplicationContext()).isProcessRunning) {
            Disposable disposable = TorManager.getInstance(this)
                    .stopTor()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(stat -> {
                        compositeDisposable.dispose();
                        TorManager.getInstance(this).setTorState(TorManager.CONNECTION_STATES.DISCONNECTED);
                        updateNotification("Restarting....");
                        startTOR();
                    }, error -> {
                        error.printStackTrace();
                        compositeDisposable.dispose();
                        updateNotification("Restarting....");
                        startTOR();
                    });
            compositeDisposable.add(disposable);
        } else {
            startTOR();
        }
    }

    private void stopTOr() {
        if (TorManager.getInstance(getApplicationContext()).isProcessRunning) {

            //
            Disposable disposable = TorManager.getInstance(this)
                    .stopTor()
                    .subscribe(state -> {

                        TorUtil.getInstance(this).setStatusFromBroadcast(false);

                    }, Throwable::printStackTrace);
            compositeDisposable.add(disposable);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
