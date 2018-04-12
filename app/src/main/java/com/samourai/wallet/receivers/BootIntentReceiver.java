package com.samourai.wallet.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.samourai.wallet.service.BroadcastReceiverService;
//import android.util.Log;

public class BootIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, BroadcastReceiverService.class));
        }
        else {
            context.startService(new Intent(context, BroadcastReceiverService.class));
        }

    }
}
