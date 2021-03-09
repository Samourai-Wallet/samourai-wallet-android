package com.samourai.wallet.whirlpool.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;

import static androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY;
import static com.samourai.wallet.SamouraiApplication.WHIRLPOOL_CHANNEL;
import static com.samourai.wallet.SamouraiApplication.WHIRLPOOL_NOTIFICATIONS;
import static com.samourai.wallet.util.FormatsUtil.getBTCDecimalFormat;

public class WhirlpoolNotificationService extends Service {

    private static final String TAG = "WhirlpoolNotification";
    public static int WHIRLPOOL_SERVICE_NOTIFICATION_ID = 15;
    public static String ACTION_START = "WHRL_START";
    public static String ACTION_STOP = "WHRL_STOP";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private List<String> notifiedUtxos = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new NotificationCompat.Builder(this, WHIRLPOOL_CHANNEL)
                .setContentTitle("Samourai Whirlpool")
                .setContentText("Waiting...")
                .setOngoing(true)
                .setSound(null)
                .setOnlyAlertOnce(true)
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
            WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
            if (whirlpoolWallet == null) {
                // whirlpool wallet not opened yet
                return;
            }

            // whirlpool wallet is opened
            updateNotification();
            Disposable stateDisposable = whirlpoolWallet.getMixingState().getObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(mixingState -> updateNotification());

            Disposable repeatedChecks = Observable.fromCallable(() -> true).repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS)).subscribe(aBoolean -> {
                updateNotification();
                notifySuccessMixes(whirlpoolWallet.getMixingState());
            }, er -> {

            });
            compositeDisposable.add(repeatedChecks);
            compositeDisposable.add(stateDisposable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void notifySuccessMixes(MixingState mixingState) {
        for (WhirlpoolUtxo utxo : new ArrayList<>(mixingState.getUtxosMixing())) {
            if (utxo.getUtxoState().getStatus() == WhirlpoolUtxoStatus.MIX_SUCCESS) {
                String utxoId = utxo.getUtxo().tx_hash.concat(":").concat(String.valueOf(utxo.getUtxo().tx_output_n));
                if (!notifiedUtxos.contains(utxoId)) {
//                    showMixSuccessNotification(utxo);
                    notifiedUtxos.add(utxoId);
                }
            }

        }


    }

    private void showMixSuccessNotification(WhirlpoolUtxo utxo) {
        String message = getBTCDecimalFormat(utxo.getUtxo().value).concat(" BTC").concat(" ").concat(" mix completed");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, WHIRLPOOL_NOTIFICATIONS)
                .setContentTitle("Mix completed")
                .setTicker("Mix completed")
                .setColorized(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_whirlpool)
                .setColor(getResources().getColor(R.color.green_ui_2));
        Notification notification = builder.build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.notify(utxo.getUtxo().tx_hash, utxo.getUtxo().tx_output_n, notification);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Objects.requireNonNull(intent.getAction()).equals(WhirlpoolNotificationService.ACTION_START)) {
            Disposable startDisposable = AndroidWhirlpoolWalletService.getInstance()
                    .startService(getApplicationContext())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::listenService, er -> {
                        // start failed
                        Log.e(TAG, "onStartCommand: ".concat(er.getMessage()));
                        Toast.makeText(getApplicationContext(), er.getMessage(), Toast.LENGTH_LONG).show();
                        stopWhirlPoolService();
                    });
            compositeDisposable.add(startDisposable);

        } else if (Objects.requireNonNull(intent.getAction()).equals(WhirlpoolNotificationService.ACTION_STOP)) {
            this.stopWhirlPoolService();
        }

        return START_STICKY;

    }

    private void stopWhirlPoolService() {
        compositeDisposable.clear();

        AndroidWhirlpoolWalletService.getInstance().stop();
        this.stopSelf();
    }


    void updateNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, WHIRLPOOL_CHANNEL)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setContentTitle("Samourai Whirlpool")
                .setOngoing(true)
                .setSound(null)
                .setGroup("service")
                .setOnlyAlertOnce(true)
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
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
        if (whirlpoolWallet != null) {
            MixingState mixingState = whirlpoolWallet.getMixingState();
            builder.setContentTitle("Whirlpool online: ".concat(String.valueOf(mixingState.getNbMixing()))
                    .concat(" MIXING :")
                    .concat(String.valueOf(mixingState.getNbQueued())).concat(" QUEUED"));
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            for (WhirlpoolUtxo whirlpoolUtxo : mixingState.getUtxosMixing()) {
                if (whirlpoolUtxo.getUtxoState().getMessage() != null && whirlpoolUtxo.getPoolId() != null)
                    inboxStyle.addLine(whirlpoolUtxo.getPoolId().concat(" : ").concat(whirlpoolUtxo.getUtxoState().getMessage()));
            }
            builder.setStyle(inboxStyle);
        } else {
            builder.setContentText("Whirlpool");
        }
    }


    @Override
    public void onDestroy() {
        try {
            this.stopWhirlPoolService();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static void startService(Context context) {
        Intent startIntent = new Intent(context, WhirlpoolNotificationService.class);
        startIntent.setAction(WhirlpoolNotificationService.ACTION_START);
        context.startService(startIntent);
    }

    public static void stopService(Context context) {
        Intent startIntent = new Intent(context, WhirlpoolNotificationService.class);
        startIntent.setAction(WhirlpoolNotificationService.ACTION_STOP);
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
