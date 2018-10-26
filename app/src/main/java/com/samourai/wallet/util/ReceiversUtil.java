package com.samourai.wallet.util;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.samourai.wallet.permissions.PermissionsUtil;
import com.samourai.wallet.receivers.InterceptOutgoingReceiver;
import com.samourai.wallet.receivers.SMSReceiver;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SIMUtil;
import com.samourai.wallet.util.SMSSender;
import com.samourai.wallet.R;

import java.util.ArrayList;
import java.util.List;
//import android.util.Log;

public class ReceiversUtil  {

    private static ReceiversUtil instance = null;

    private static Context context = null;

    private static TelephonyManager tm = null;

    private static IntentFilter ocFilter = null;
    private static BroadcastReceiver ocReceiver = null;
    private static IntentFilter isFilter = null;
    private static BroadcastReceiver isReceiver = null;

    private static List<BroadcastReceiver> receivers = new ArrayList<BroadcastReceiver>();

    private ReceiversUtil() { ; }

    public static ReceiversUtil getInstance(Context ctx)   {

        context = ctx;

        if(instance == null)    {
            instance = new ReceiversUtil();
        }

        return instance;
    }

    public void initReceivers() {

        boolean hideIcon = PrefsUtil.getInstance(context).getValue(PrefsUtil.ICON_HIDDEN, false);
        boolean acceptRemote = PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCEPT_REMOTE, false);

        if(hideIcon && PermissionsUtil.getInstance(context).hasPermission(Manifest.permission.PROCESS_OUTGOING_CALLS)) {
            if(!receivers.contains(ocReceiver)) {
                ocFilter = new IntentFilter();
                ocFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                ocFilter.setPriority(1001);
                ocReceiver = new InterceptOutgoingReceiver();
                receivers.add(ocReceiver);
                context.getApplicationContext().registerReceiver(ocReceiver, ocFilter);
            }
        }
        else {
            if(receivers.contains(ocReceiver)) {
                context.getApplicationContext().unregisterReceiver(ocReceiver);
                receivers.remove(ocReceiver);
            }
        }

        if(acceptRemote && PermissionsUtil.getInstance(context).hasPermission(Manifest.permission.RECEIVE_SMS)) {
            if(!receivers.contains(isReceiver)) {
                isFilter = new IntentFilter();
                isFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
                isFilter.setPriority(2147483647);
                isReceiver = new SMSReceiver();
                context.getApplicationContext().registerReceiver(isReceiver, isFilter);
            }
        }
        else {
            if(receivers.contains(isReceiver)) {
                context.getApplicationContext().unregisterReceiver(isReceiver);
                receivers.remove(isReceiver);
            }
        }

    }

    public void checkSIMSwitch() {

        //
        // check for SIM switch
        //
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.CHECK_SIM, false) == true && PrefsUtil.getInstance(context).getValue(PrefsUtil.ALERT_MOBILE_NO, "").length() > 0 && PermissionsUtil.getInstance(context).hasPermission(Manifest.permission.READ_PHONE_STATE)) {

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

    }

}
