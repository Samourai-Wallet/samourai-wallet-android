package com.samourai.wallet.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.samourai.wallet.receivers.InterceptOutgoingReceiver;
import com.samourai.wallet.receivers.SMSReceiver;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SIMUtil;
import com.samourai.wallet.util.SMSSender;
import com.samourai.R;

import java.util.ArrayList;
import java.util.List;
//import android.util.Log;

public class BroadcastReceiverService extends Service {

    private Service thisService = this;

    private Context context = null;
    private ContentResolver contentResolver = null;

    private TelephonyManager tm = null;

    private IntentFilter ocFilter = null;
    private BroadcastReceiver ocReceiver = null;
    private IntentFilter isFilter = null;
    private BroadcastReceiver isReceiver = null;

    private static List<BroadcastReceiver> receivers = new ArrayList<BroadcastReceiver>();

    @Override
    public void onCreate() {
        ;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //
        context = this.getApplicationContext();

        boolean hideIcon = PrefsUtil.getInstance(context).getValue(PrefsUtil.ICON_HIDDEN, false);
        boolean acceptRemote = PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCEPT_REMOTE, false);

        if(hideIcon) {
            if(!receivers.contains(ocReceiver)) {
                ocFilter = new IntentFilter();
                ocFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                ocFilter.setPriority(1001);
                ocReceiver = new InterceptOutgoingReceiver();
                receivers.add(ocReceiver);
                context.registerReceiver(ocReceiver, ocFilter);
            }
        }
        else {
            if(receivers.contains(ocReceiver)) {
                context.unregisterReceiver(ocReceiver);
                receivers.remove(ocReceiver);
            }
        }

        if(acceptRemote) {
            if(!receivers.contains(isReceiver)) {
                isFilter = new IntentFilter();
                isFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
                isFilter.setPriority(2147483647);
                isReceiver = new SMSReceiver();
                context.registerReceiver(isReceiver, isFilter);
            }
        }
        else {
            if(receivers.contains(isReceiver)) {
                context.unregisterReceiver(isReceiver);
                receivers.remove(isReceiver);
            }
        }

        //
        // check for SIM switch
        //
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.CHECK_SIM, false) == true && PrefsUtil.getInstance(context).getValue(PrefsUtil.ALERT_MOBILE_NO, "").length() > 0) {

            new Thread() {
                public void run() {

                    try {
                        sleep(1000 * 90);
                    }
                    catch(Exception e) {
                        ;
                    }

                    if(SIMUtil.getInstance(context).isSIMSwitch()) {
                        SMSSender.getInstance(context).send(context.getResources().getString(R.string.sim_warning), PrefsUtil.getInstance(context).getValue(PrefsUtil.ALERT_MOBILE_NO, ""));
//                        SIMUtil.getInstance(context).setStoredSIM();
                    }
                    else {
                        ;
                    }

                }
            }.start();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void stop() {
        ;
    }

    @Override
    public void onDestroy()
    {
        stop();
        super.onDestroy();
    }

}
