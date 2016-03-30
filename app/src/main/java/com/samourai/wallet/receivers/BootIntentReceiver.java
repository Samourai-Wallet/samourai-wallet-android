package com.samourai.wallet.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samourai.wallet.service.BroadcastReceiverService;
//import android.util.Log;

public class BootIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        context.startService(new Intent(context, BroadcastReceiverService.class));

    }
}
