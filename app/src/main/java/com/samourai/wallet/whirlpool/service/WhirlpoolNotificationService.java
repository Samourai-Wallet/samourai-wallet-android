package com.samourai.wallet.whirlpool.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.samourai.wallet.R;
import com.samourai.wallet.tor.TorBroadCastReceiver;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;

import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.support.v4.app.NotificationCompat.GROUP_ALERT_SUMMARY;
import static com.samourai.wallet.SamouraiApplication.WHIRLPOOL_CHANNEL;

public class WhirlpoolNotificationService extends Service {

    private static final String TAG = "WhirlpoolNotification";
    public static int WHIRLPOOL_SERVICE_NOTIFICATION_ID = 15;
    public static String ACTION_START = "WHRL_START";
    public static String ACTION_STOP = "WHRL_STOP";
    private AndroidWhirlpoolWalletService androidWhirlpoolWalletService = AndroidWhirlpoolWalletService.getInstance();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new NotificationCompat.Builder(this, WHIRLPOOL_CHANNEL)
                .setContentTitle("Samourai Whirlpool")
                .setContentText("Waiting...")
                .setOngoing(true)
                .setSound(null)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("service")
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_whirlpool)
                .build();

        startForeground(WHIRLPOOL_SERVICE_NOTIFICATION_ID, notification);
        listenService();
    }

    private void listenService() {
        //TODO: Listen to events from whirlpool service

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Objects.requireNonNull(intent.getAction()).equals(WhirlpoolNotificationService.ACTION_START)) {
            Disposable startDisposable = androidWhirlpoolWalletService
                    .startService(getApplicationContext())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(() -> {
                        updateNotification("Online");
                    }, er -> {
                        Log.e(TAG, "onStartCommand: ".concat(er.getMessage()));
                    });
            compositeDisposable.add(startDisposable);

        } else if (Objects.requireNonNull(intent.getAction()).equals(WhirlpoolNotificationService.ACTION_STOP)) {
            androidWhirlpoolWalletService.getWallet().stop();
            this.stopSelf();
        }

        return START_STICKY;

    }


    void updateNotification(String content) {
        Notification notification = new NotificationCompat.Builder(this, WHIRLPOOL_CHANNEL)
                .setContentTitle("Samourai Whirlpool")
                .setContentText(content)
                .setOngoing(true)
                .setSound(null)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("service")
                .addAction(getStopAction())
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_whirlpool)
                .build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.notify(WHIRLPOOL_SERVICE_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private NotificationCompat.Action getStopAction() {

        Intent broadcastIntent = new Intent(this, WhirlpoolBroadCastReceiver.class);
        broadcastIntent.setAction(WhirlpoolNotificationService.ACTION_STOP);

        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action(R.drawable.ic_close_white_24dp, "STOP", actionIntent);
    }

    public static void StartService(Context context) {
        Intent startIntent = new Intent(context, WhirlpoolNotificationService.class);
        startIntent.setAction(WhirlpoolNotificationService.ACTION_START);
        context.startService(startIntent);
    }
}
