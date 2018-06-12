package com.samourai.wallet.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samourai.wallet.util.ReceiversUtil;
//import android.util.Log;

public class BootIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ReceiversUtil.getInstance(context).initReceivers();
        ReceiversUtil.getInstance(context).checkSIMSwitch();

    }
}
