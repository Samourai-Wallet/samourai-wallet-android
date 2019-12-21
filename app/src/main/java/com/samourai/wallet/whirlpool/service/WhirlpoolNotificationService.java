package com.samourai.wallet.whirlpool.service;

import android.app.ActivityManager;
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
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
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
    private MixingState mixingState;

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


        try {
            mixingState = androidWhirlpoolWalletService.getWallet().getMixingState();
            updateNotification();
            Disposable stateDisposable = androidWhirlpoolWalletService.getWallet().getMixingState().getObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(mixingState1 -> {
                        updateNotification();
                    });

            Disposable repeatedChecks = Observable.fromCallable(() -> {
                mixingState = androidWhirlpoolWalletService.getWallet().getMixingState();
                return true;
            }).repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS)).subscribe(aBoolean -> {
                updateNotification();
            }, er -> {

            });
            compositeDisposable.add(repeatedChecks);
            compositeDisposable.add(stateDisposable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Objects.requireNonNull(intent.getAction()).equals(WhirlpoolNotificationService.ACTION_START)) {
            Disposable startDisposable = androidWhirlpoolWalletService
                    .startService(getApplicationContext())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::listenService, er -> {
                        Log.e(TAG, "onStartCommand: ".concat(er.getMessage()));
                    });
            compositeDisposable.add(startDisposable);

        } else if (Objects.requireNonNull(intent.getAction()).equals(WhirlpoolNotificationService.ACTION_STOP)) {
            compositeDisposable.clear();
            androidWhirlpoolWalletService.getWallet().stop();
            this.stopSelf();
        }

        return START_STICKY;

    }


    void updateNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, WHIRLPOOL_CHANNEL)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setContentTitle("Samourai Whirlpool")
                .setOngoing(true)
                .setSound(null)
                .setGroup("service")
                .addAction(getStopAction())
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setGroupSummary(true)
                .setSmallIcon(R.drawable.ic_whirlpool)
                .setColor(getResources().getColor(R.color.blue));
        setMixState(builder);
//                 .build();
        Notification notification = builder.build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(WHIRLPOOL_SERVICE_NOTIFICATION_ID, notification);
    }

    private void setMixState(NotificationCompat.Builder builder) {
        if (mixingState != null) {
            builder.setContentTitle("Whirlpool online: ".concat(String.valueOf(mixingState.getNbMixing()))
                    .concat(" MIXING :")
                    .concat(String.valueOf(mixingState.getNbQueued())).concat(" QUEUED"));
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            for (WhirlpoolUtxo whirlpoolUtxo : mixingState.getUtxosMixing()) {
                if (whirlpoolUtxo.getUtxoState().getMessage() != null && whirlpoolUtxo.getUtxoConfig().getPoolId() != null)
                    inboxStyle.addLine(whirlpoolUtxo.getUtxoConfig().getPoolId().concat(" : ").concat(whirlpoolUtxo.getUtxoState().getMessage()));
            }
            builder.setStyle(inboxStyle);
        } else {
            builder.setContentText("Whirlpool");
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
    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WhirlpoolNotificationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
