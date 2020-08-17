package com.samourai.wallet.tor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiApplication;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static androidx.core.app.NotificationCompat.Action;
import static androidx.core.app.NotificationCompat.Builder;
import static androidx.core.app.NotificationCompat.CATEGORY_SERVICE;
import static androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY;
import static androidx.core.app.NotificationCompat.InboxStyle;

public class TorService extends Service {


    public static String START_SERVICE = "START_SERVICE";
    public static String STOP_SERVICE = "STOP_SERVICE";
    public static String RESTART_SERVICE = "RESTART_SERVICE";
    public static String RENEW_IDENTITY = "RENEW_IDENTITY";
    public static int TOR_SERVICE_NOTIFICATION_ID = 95;
    public static String TAG = "TorService";

    private Long lastRead = -1L;
    private Long lastWritten = -1L;
    private Long mTotalTrafficWritten = 0L;
    private Long mTotalTrafficRead = 0L;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String title = "TOR";
    private Disposable torDisposable;
    private boolean identityChanging = false;
    private int bootStrapProgress = 0;

    private boolean stopping = false;
    private String log = "";
    private String bandwidth = "";
    private String circuit = "";
    NumberFormat mNumberFormat = NumberFormat.getInstance(Locale.getDefault()); //localized numbers!

    @Override
    public void onCreate() {
        super.onCreate();

        Notification notification = new Builder(this, SamouraiApplication.TOR_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Tor: Waiting...")
                .setSound(null)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("Tor")
                .setCategory(CATEGORY_SERVICE)
                .setGroupSummary(true)
                .setSmallIcon(R.drawable.ic_samourai_and_tor_notif_icon)
                .build();
        startForeground(TOR_SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action == null) {
            return START_NOT_STICKY;
        }

        if (action.equals(STOP_SERVICE)) {
            stopping = true;

            Disposable disposable = TorManager.getInstance(getApplicationContext())
                    .stopTor().subscribe(() -> {
                        compositeDisposable.clear();
                        this.stopForeground(true);
                        this.stopSelf();
                    }, throwable -> {
                        compositeDisposable.clear();
                        this.stopForeground(true);
                        this.stopSelf();
                    });
            compositeDisposable.add(disposable);
        } else if (action.equals(START_SERVICE)) {
            startTor();
        } else if (action.equals(RENEW_IDENTITY)) {
            renewIdentity();
        }
        return START_STICKY;
    }

    private void renewIdentity() {
        log = "Renewing Tor identity...";
        updateNotification();
        Disposable disposable = TorManager.getInstance(getApplicationContext()).renew()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    new Handler().postDelayed(() -> {
                        log = "Connected";
                        updateNotification();
                    }, 2000);
                });
        //Update tor state after re-new
        compositeDisposable.add(disposable);
    }

    private void startTor() {
        title = "Tor: Waiting";
        log = "Connecting....";
        updateNotification();
        if (torDisposable != null) {
            compositeDisposable.delete(torDisposable);
        }
        torDisposable = TorManager.getInstance(getApplicationContext())
                .startTor()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    logger();
                    updateNotification();
                });

        compositeDisposable.add(torDisposable);
    }

    private void logger() {

        Disposable status = TorManager.getInstance(getApplicationContext())
                .getTorStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectionStatus -> {
                    updateNotification();
                });

        Disposable logs = TorManager.getInstance(getApplicationContext()).getTorLogs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(logMessage -> {
                    this.log = logMessage;
                    updateNotification();
                });


        Disposable circLog = TorManager.getInstance(getApplicationContext()).getCircuitLogs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(circuitMessage -> {
                   if(circuitMessage.contains("NOTICE: Rate limiting NEWNYM")){
                       Toast.makeText(getApplicationContext(),circuitMessage,Toast.LENGTH_LONG).show();
                   }
                    handleBootstrappedMsg(circuitMessage);
                    this.circuit = circuitMessage;
                    updateNotification();
                });

        Disposable bandW = TorManager.getInstance(getApplicationContext()).getBandWidth()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(bandwidth -> {
                    if (bandwidth != null && !stopping) {
                        Long read = (bandwidth.containsKey("read")) ? bandwidth.get("read") : 0L;

                        Long written = (bandwidth.containsKey("written")) ? bandwidth.get("written") : 0L;
                        if (read != lastRead || written != lastWritten) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(formatCount(read));
                            sb.append(" \u2193");
                            sb.append(" / ");
                            sb.append(formatCount(written));
                            sb.append(" \u2191");
                            this.bandwidth = sb.toString();
                            updateNotification();
                        }
                        lastWritten = written;
                        lastRead = read;
                    }
                });


        compositeDisposable.add(logs);
        compositeDisposable.add(circLog);
        compositeDisposable.add(status);
        compositeDisposable.add(bandW);
    }

    private void updateNotification() {

        SpannableString logSpannable = new SpannableString("Log: ".concat(this.log));
        logSpannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                0, 4,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString circuitSpan = new SpannableString("Circuit: ".concat(this.circuit));
        circuitSpan.setSpan(
                new StyleSpan(Typeface.BOLD),
                0, 8,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


        SpannableString bandWidthSpan = new SpannableString("BandWidth: ".concat(this.bandwidth));
        bandWidthSpan.setSpan(
                new StyleSpan(Typeface.BOLD),
                0, 8,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, SamouraiApplication.TOR_CHANNEL_ID)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("Tor")
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_samourai_and_tor_notif_icon);
        if (bootStrapProgress != 0 && bootStrapProgress != 100) {
            notification.setContentTitle("Tor : Bootstrapping..");
            notification.setProgress(100, bootStrapProgress, false);
            notification.setContentText(String.valueOf(bootStrapProgress).concat("%"));
        } else {
            switch (TorManager.getInstance(getApplicationContext()).getStatus()) {
                case IDLE: {
                    notification.setContentTitle("Tor : Waiting...");
                    break;
                }
                case CONNECTED: {
                    notification.setStyle(new InboxStyle()
//                            .addLine(circuitSpan)
                            .addLine(bandWidthSpan)
                    )
                            .setContentInfo(bandWidthSpan);
                    notification.setContentTitle("Tor : Connected");
                    notification.addAction(getRenewAction());
                    break;
                }

                case CONNECTING: {
                    notification.setContentTitle("Tor : Connecting...");
                    break;
                }

                default: {
                    //No-op
                }

            }

        }

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(TOR_SERVICE_NOTIFICATION_ID, notification.build());

    }

    private Action getRenewAction() {
        Intent broadcastIntent = new Intent(this, TorBroadCastReceiver.class);
        broadcastIntent.setAction(RENEW_IDENTITY);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Action(R.drawable.ic_onion, "New identity", actionIntent);
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleBootstrappedMsg(String msg) {
        if (msg.contains("Bootstrapped")) {
            String numberOnly = msg.replaceAll("[^0-9]", "");
            try {
                bootStrapProgress = Integer.parseInt(numberOnly);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private String formatCount(long count) {
        // Converts the supplied argument into a string.

        // Under 2Mb, returns "xxx.xKb"
        // Over 2Mb, returns "xxx.xxMb"
        if (mNumberFormat != null)
            if (count < 1e6)
                return mNumberFormat.format(Math.round((float) ((int) (count * 10 / 1024)) / 10)) + "kbps";
            else
                return mNumberFormat.format(Math.round((float) ((int) (count * 100 / 1024 / 1024)) / 100)) + "mbps";
        else
            return "";
    }
}
