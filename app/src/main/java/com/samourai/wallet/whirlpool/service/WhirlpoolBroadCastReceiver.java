package com.samourai.wallet.whirlpool.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class WhirlpoolBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, WhirlpoolNotificationService.class);
        serviceIntent.setAction(intent.getAction());
        context.startService(serviceIntent);

    }
}
