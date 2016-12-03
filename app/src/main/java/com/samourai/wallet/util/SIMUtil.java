package com.samourai.wallet.util;

import android.content.Context;
import android.telephony.TelephonyManager;

public class SIMUtil {

    private static TelephonyManager tm = null;
    private static SIMUtil instance = null;
    private static Context context = null;

    private static String imsi = null;

    private SIMUtil() { ; }

    public static SIMUtil getInstance(Context ctx) {

        if(instance == null) {
            context = ctx;
            tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            instance = new SIMUtil();

            if(tm.getSubscriberId() != null && tm.getSubscriberId().length() > 0)	{
                imsi = tm.getSubscriberId();
//                Log.i("SIMUtil", "subscriber id:" + imsi);
            }
            else if(tm.getSimSerialNumber() != null && tm.getSimSerialNumber().length() > 0)	{
                imsi = tm.getSimSerialNumber();
//                Log.i("SIMUtil", "sim serial no.:" + imsi);
            }
            else	{
                imsi = "";
            }

        }

        return instance;
    }

    public void setStoredSIM()   	{
        PrefsUtil.getInstance(context).setValue(PrefsUtil.SIM_IMSI, imsi);
    }

    public String getStoredSIM()   	{
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.SIM_IMSI, "");
    }

    public String getSIM()   	{
        return imsi;
    }

    public boolean isSIMSwitch()   	{
//        Log.i("SIMUtil", "stored sim:" + getStoredSIM());
//        Log.i("SIMUtil", "subscriber id:" + imsi);
        return !getStoredSIM().equals(imsi);
    }

}
