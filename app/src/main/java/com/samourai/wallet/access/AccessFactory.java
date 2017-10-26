package com.samourai.wallet.access;

import android.content.Context;

import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;

import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.util.UUID;

//import android.util.Log;

public class AccessFactory	{

    public static final int MIN_PIN_LENGTH = 5;
    public static final int MAX_PIN_LENGTH = 8;

    private static String _key = null;
    private static String _value = null;
    private static String _pin = "";
    private static String _pin2 = "";

    private static boolean isLoggedIn = false;

    private static Context context = null;
    private static AccessFactory instance = null;

    private AccessFactory()	{ ; }

    public static AccessFactory getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new AccessFactory();
        }

        return instance;
    }

    public static AccessFactory getInstance() {

        if (instance == null) {
            instance = new AccessFactory();
        }

        return instance;
    }

    public boolean writeAccess() {
        return true;
    }

    public String getKey() {
        return _key;
    }

    public String getValue() {
        return _value;
    }

    public String getPIN() {
        return _pin;
    }

    public void setPIN(String pin) {
        _pin = pin;
    }

    public String getPIN2() {
        return _pin2;
    }

    public void setPIN2(String pin2) {
        _pin2 = pin2;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean logged) {
        isLoggedIn = logged;
    }

    public String getHash(String randomKey, CharSequenceX fixedKey, int iterations)	 {

        byte[] data = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            {
                // n rounds of SHA256
                data = md.digest((randomKey + fixedKey.toString()).getBytes("UTF-8"));
                // first hash already done above
                for(int i = 1; i < iterations; i++) {
                    data = md.digest(data);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(data != null) {
            return new String(Hex.encode(data));
        }
        else {
            return null;
        }

    }

	public boolean validateHash(String hash, String randomKey, CharSequenceX fixedKey, int iterations) {

        String _hash = null;
        _hash = getHash(randomKey, fixedKey, iterations);
        return hash.equals(_hash);

	}

    public String getGUID()    {
        if(PrefsUtil.getInstance(context).has(PrefsUtil.GUID_V) && PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID_V, 0) == 4)    {
            return PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID, "") + FootprintUtil.getInstance(context).getFootprintV4();
        }
        else if(PrefsUtil.getInstance(context).has(PrefsUtil.GUID_V) && PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID_V, 0) == 3)    {
            return PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID, "") + FootprintUtil.getInstance(context).getFootprintV3();
        }
        else if(PrefsUtil.getInstance(context).has(PrefsUtil.GUID_V) && PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID_V, 0) == 2)    {
            return PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID, "") + FootprintUtil.getInstance(context).getFootprint();
        }
        else    {
            return PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID, "");
        }
    }

    public String createGUID()    {
        String guid = UUID.randomUUID().toString();
        PrefsUtil.getInstance(context).setValue(PrefsUtil.GUID, guid);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.GUID_V, 4);
//        Log.i("AccessFactory", "create guid:" + guid);

        return guid + FootprintUtil.getInstance(context).getFootprintV4();
    }

}
