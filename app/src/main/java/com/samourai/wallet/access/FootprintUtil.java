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

//        Log.i("FootprintUtil", strFootprint);

        try {
            byte[] data = strFootprint.getBytes("UTF-8");
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(data, 0, data.length);
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            if(out != null) {
//                Log.i("FootprintUtil", "RIPEMD160:" + new String(Hex.encode(out)));
                return new String(Hex.encode(out));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return strFootprint;

    }

}
