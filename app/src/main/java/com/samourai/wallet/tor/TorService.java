package com.samourai.wallet.tor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.R;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.util.WebUtil;

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
    public static String RENEW_IDENTITY = "RENEW_IDENTITY";
    public static int TOR_SERVICE_NOTIFICATION_ID = 95;
    private static final String TAG = "TorService";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String title = "TOR";
    private Disposable torDisposable;
    private boolean identityChanging;

    @Override

    public void onCreate() {
        super.onCreate();
        Notification notification = new NotificationCompat.Builder(this, TOR_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Waiting...")
                .setOngoing(true)
                .setSound(null)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("Tor")
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_samourai_and_tor_notif_icon)
                .build();

        startForeground(TOR_SERVICE_NOTIFICATION_ID, notification);

    }


    private NotificationCompat.Action getStopAction(String message) {

        Intent broadcastIntent = new Intent(this, TorBroadCastReceiver.class);
        broadcastIntent.setAction(STOP_SERVICE);

        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action(R.drawable.tor_on, message, actionIntent);
    }


    private NotificationCompat.Action getRestartAction() {

        Intent broadcastIntent = new Intent(this, TorBroadCastReceiver.class);
        broadcastIntent.setAction(RENEW_IDENTITY);

        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action(R.drawable.tor_on, "New identity", actionIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Objects.requireNonNull(intent.getAction()).equals(TorService.STOP_SERVICE)) {

            if (DojoUtil.getInstance(getApplicationContext()).getDojoParams() != null && !intent.hasExtra("KILL_TOR")) {
                Toast.makeText(getApplicationContext(), "You cannot stop Tor service when dojo is connected", Toast.LENGTH_SHORT).show();
                return START_STICKY;
            }

            Disposable disposable = TorManager.getInstance(getApplicationContext())
                    .stopTor()
                    .subscribe(stat -> {
                        compositeDisposable.dispose();
                        stopSelf();
                    }, error -> {
//
                    });
            compositeDisposable.add(disposable);

        } else if (intent.getAction().equals(TorService.RENEW_IDENTITY)) {
            renewIdentity();
            return START_STICKY;
        } else if (Objects.requireNonNull(intent.getAction()).equals(TorService.START_SERVICE)) {
            if (!TorManager.getInstance(getApplicationContext()).isProcessRunning) {
                startTor();
            }
        }

        return START_STICKY;

    }

    private void renewIdentity() {
        if (identityChanging) {
            return;
        }
        identityChanging = true;
        updateNotification("Renewing Tor identity...");
        Disposable disposable = Observable.fromCallable(() -> TorManager.getInstance(getApplicationContext()).newIDentity()).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(t -> {
                    Log.i(TAG, "renewIdentity: ".concat(String.valueOf(t)));
                    Disposable disposable1 = Observable.fromCallable(() -> TorManager.getInstance(getApplicationContext()).getLatestLogs()).observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(s -> {
                                if (s.contains("NEWNYM")) {
                                    Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(getApplicationContext(), "Tor identity renewed", Toast.LENGTH_SHORT).show();
                                }

                            }, err -> {
                                err.printStackTrace();
                            });
                    compositeDisposable.add(disposable1);
                    identityChanging = false;
                    if (BuildConfig.DEBUG) {
                        Disposable disposable2 = checkIp()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(o -> {
                                    Log.i(TAG, "restart: ".concat(o));

                                });

                        compositeDisposable.add(disposable2);

                    }

                }, error -> {
                    Log.i(TAG, "restart: ".concat(error.getMessage()));
                    error.printStackTrace();
                });
        compositeDisposable.add(disposable);
    }

    // for testing purpose
    private Observable<String> checkIp() {
        return Observable.fromCallable(() -> WebUtil.getInstance(getApplicationContext()).getURL("http://checkip.amazonaws.com"));

    }

    private void startTor() {
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
                    if (!TorManager.getInstance(getApplicationContext()).isConnected())
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
                .setSmallIcon(R.drawable.ic_samourai_and_tor_notif_icon);

        switch (TorManager.getInstance(getApplicationContext()).state) {
            case CONNECTED: {
                notification.setColorized(true);
                notification.addAction(getStopAction("Stop"));
                notification.addAction(getRestartAction());
                notification.setColor(ContextCompat.getColor(this, R.color.green_ui_2));
                break;
            }
            case CONNECTING: {
                break;
            }
            case DISCONNECTED: {
                notification.addAction(getStopAction("Stop"));
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
                        updateNotification("Restarting...");
                        startTor();
                    }, error -> {
                        error.printStackTrace();
                        compositeDisposable.dispose();
                        updateNotification("Restarting...");
                        startTor();
                    });
            compositeDisposable.add(disposable);
        } else {
            startTor();
        }
    }

    private void stopTor() {
        if (TorManager.getInstance(getApplicationContext()).isProcessRunning) {

            //
            Disposable disposable = TorManager.getInstance(this)
                    .stopTor()
                    .subscribe(state -> {


                    }, Throwable::printStackTrace);
            compositeDisposable.add(disposable);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
