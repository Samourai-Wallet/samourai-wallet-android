package com.samourai.wallet.access;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
//import android.util.Log;

import org.spongycastle.crypto.digests.RIPEMD160Digest;
import org.spongycastle.util.encoders.Hex;

public class FootprintUtil {

    private static Context context = null;
    private static FootprintUtil instance = null;

    private FootprintUtil() { ; }

    public static FootprintUtil getInstance(Context ctx)    {

        context = ctx;

        if(instance == null) {
            instance = new FootprintUtil();
        }

        return instance;
    }

    public String getFootprint() {

        TelephonyManager tManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        String strFootprint =
                Build.BOARD + "|" + Build.CPU_ABI + "," +
                        Build.MANUFACTURER + ":" + Build.BRAND + ";" + Build.MODEL + "/" + Build.DEVICE + "+" + Build.PRODUCT + "#" +
                        Build.SERIAL + "§"
                ;

        strFootprint += tManager.getDeviceId();

        return RIPEMD160(strFootprint);
    }

    public String getFootprintV3() {

        TelephonyManager tManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        String strFootprint = Build.BOARD +  Build.MANUFACTURER + Build.BRAND + Build.MODEL + Build.SERIAL;

        strFootprint += tManager.getDeviceId();

        return RIPEMD160(strFootprint);
    }

    public String getFootprintV4() {

        String strFootprint = Build.MANUFACTURER + Build.BRAND + Build.MODEL;

        return RIPEMD160(strFootprint);
    }

    public String RIPEMD160(String data)   {
        try {
            byte[] hash = data.getBytes("UTF-8");
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(hash, 0, hash.length);
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            if(out != null) {
                return new String(Hex.encode(out));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";

    }

}
