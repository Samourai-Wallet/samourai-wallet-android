package com.samourai.wallet.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
//import android.widget.Toast;
//import android.util.Log;

public class SMSSender {

    private static Context context = null;
    private static PendingIntent sentPI = null;
    private static PendingIntent deliveredPI = null;

    private static SMSSender instance = null;

    private SMSSender()	{ ; }

    public static SMSSender getInstance(Context ctx)	{
        context = ctx;

        if(instance == null)	{
            instance = new SMSSender();
        }

        return instance;
    }

    public void send(String text, String dest)	{
        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(dest, null, text, null, null);
    }

    public void setReceivers()	{
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);
        deliveredPI = PendingIntent.getBroadcast(context, 0, new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        context.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
//	                    Toast.makeText(((Activity)context).getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//	                    Toast.makeText(((Activity)context).getBaseContext(), "SMS not sent: Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
//	                    Toast.makeText(((Activity)context).getBaseContext(), "SMS not sent: No service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
//	                    Toast.makeText(((Activity)context).getBaseContext(), "SMS not sent: Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
//	                    Toast.makeText(((Activity)context).getBaseContext(), "SMS not sent: Radio off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        context.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
//	                    Toast.makeText(((Activity)context).getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
//	                    Toast.makeText(((Activity)context).getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));
    }
}
