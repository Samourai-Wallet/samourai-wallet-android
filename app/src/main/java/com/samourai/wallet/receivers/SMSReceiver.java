package com.samourai.wallet.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsMessage;
//import android.util.Log;

import org.bitcoinj.crypto.MnemonicException;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SMSSender;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class SMSReceiver extends BroadcastReceiver {
    /** TAG used for Debug-Logging */
    protected static final String LOG_TAG = "SMSReceiver";

    /** The Action fired by the Android-System when a SMS was received.
     * We are using the Default Package-Visibility */
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    private String remoteCommand = null;

    private static Map<String, String> msgs = null;
    private static long lastMsg = 0L;

    private static HashMap<String,Long> seen = null;

    // @Override
    public void onReceive(Context context, Intent intent) {

        String incoming = null;

        if(msgs == null) {
            msgs = new Hashtable<String, String>();
        }

        if(seen == null) {
            seen = new HashMap<String, Long>();
        }

        if ((intent.getAction().equals(ACTION) || intent.getAction().contains("SMS_RECEIVED")) && PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCEPT_REMOTE, false) == true) {
            StringBuilder sb = new StringBuilder();
            Bundle bundle = intent.getExtras();

            if (bundle != null) {

                Object[] pdusObj = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdusObj.length];
                for(int i = 0; i < pdusObj.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[])pdusObj[i]);
                }

                for(SmsMessage currentMessage : messages)	{
                    String msg = currentMessage.getDisplayMessageBody().trim().toLowerCase();
                    String[] s = msg.split("\\s+");
                    String incomingTelNo = currentMessage.getDisplayOriginatingAddress();

                    if(seen.get(incomingTelNo + msg) != null && seen.get(incomingTelNo + msg) != 0L && (System.currentTimeMillis() - seen.get(incomingTelNo + msg) < 3000)) {
                        continue;
                    }
                    else {
                        seen.put(incomingTelNo + msg, System.currentTimeMillis());
                    }

                    if(s.length == 3)	{

                        if(AccessFactory.getInstance(context).validateHash(PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCESS_HASH2, ""), AccessFactory.getInstance(context).getGUID(), new CharSequenceX(s[2]), AESUtil.DefaultPBKDF2Iterations)) {
                            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.TRUSTED_LOCK, false) == true)	{
                                String strTrustedNo = PrefsUtil.getInstance(context).getValue(PrefsUtil.ALERT_MOBILE_NO, "");
//                                Log.i("SMSReceiver", "using trusted no.:" + strTrustedNo.replaceAll("[^\\+0-9]", ""));
//                                Log.i("SMSReceiver", "incoming no.:" + incomingTelNo);
                                if(!incomingTelNo.equals(strTrustedNo.replaceAll("[^\\+0-9]", "")))	{
                                    continue;
                                }
                            }

                            doRemote(context, msg, incomingTelNo, s[2]);

                        }

                    }

                }
            }
        }
    }

    private void doRemote(final Context context, final String msg, final String incomingTelNo, final String pin)	{

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                // remote commands: [sw 'command' 'pin']
                if(msg.startsWith("sw ") && msg.contains(" wipe "))	{
                    AppUtil.getInstance(context).wipeApp();
                    abortBroadcast();
                    System.exit(0);
                }
                else if(msg.startsWith("sw ") && msg.contains(" seed "))	{

                    if(!HD_WalletFactory.getInstance(context).holding())	{

                        try {
                            PayloadUtil.getInstance(context).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + pin));
                        }
                        catch (MnemonicException.MnemonicLengthException mle) {
                            mle.printStackTrace();
                        } catch (DecoderException de) {
                            de.printStackTrace();
                        } finally {
                            ;
                        }

                    }

                    String seed = null;
                    try {
                        seed = HD_WalletFactory.getInstance(context).get().getSeedHex();
                    }
                    catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                    catch(MnemonicException.MnemonicLengthException mle) {
                        mle.printStackTrace();
                    }

//            Log.i("SMSReceiver", "sending to:" + incomingTelNo);
                    SMSSender.getInstance(context).send(seed, incomingTelNo);

                }
                else	{
                    ;
                }

                Looper.loop();

            }
        }).start();

    }

}
