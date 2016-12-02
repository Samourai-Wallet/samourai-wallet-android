package com.samourai.wallet.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;

public class InterceptOutgoingReceiver extends BroadcastReceiver {

    private static final String ACTION = "android.intent.action.NEW_OUTGOING_CALL";

    @Override
    public void onReceive(Context context, Intent intent) {

        String strTelNo = null;

//        Log.i("InterceptOutgoing", "InterceptOutgoingReceiver fired");

        if(intent.getAction().equals(ACTION) && PrefsUtil.getInstance(context).getValue(PrefsUtil.ICON_HIDDEN, false) == true) {

//            Log.i("InterceptOutgoing", "ACTION recognized, ICON_HIDDEN == true");

            Bundle extras = intent.getExtras();

            if(extras != null) {

                strTelNo = extras.getString("android.intent.extra.PHONE_NUMBER");

//                Log.i("InterceptOutgoing", "strTelNo:" + strTelNo);

                if(strTelNo.startsWith("**") && strTelNo.endsWith("#")) {
                    String pin = strTelNo.substring(2, strTelNo.length() - 1);
//                    Log.i("InterceptOutgoing", "pin:" + pin);
                    if(pin.length() >= AccessFactory.MIN_PIN_LENGTH && pin.length() <= AccessFactory.MAX_PIN_LENGTH) {
//                        Log.i("InterceptOutgoing", "pin ok");
                        String accessHash = PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCESS_HASH, "");
                        String accessHash2 = PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCESS_HASH2, "");
                        if(AccessFactory.getInstance(context).validateHash(accessHash2, AccessFactory.getInstance(context).getGUID(), new CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations)) {
//                            Log.i("InterceptOutgoing", "access ok");
                            if(accessHash.equals(accessHash2))    {
                                AccessFactory.getInstance(context).setPIN(pin);
                            }
                            else    {
                                TimeOutUtil.getInstance().reset();
                                AccessFactory.getInstance(context).setIsLoggedIn(false);
                            }
                            Intent i = new Intent(context, MainActivity2.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            if(accessHash.equals(accessHash2))    {
                                i.putExtra("dialed", true);
                            }
                            context.startActivity(i);

                            setResultData(null);
                            abortBroadcast();
                            return;
                        }
                    }
                }

//                Log.i("InterceptOutgoing", "intercept ignored");

                setResultData(strTelNo);

            }

        }

    }

}
