package com.samourai.wallet.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalReceiveIndexes {

    private static LocalReceiveIndexes instance = null;

    private static Context context = null;

    private LocalReceiveIndexes() { ; }

    public static LocalReceiveIndexes getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new LocalReceiveIndexes();
        }

        return instance;
    }

    public JSONObject toJSON() {

        JSONObject indexes = new JSONObject();

        try {
            indexes.put("local44idx", AddressFactory.getInstance(context).getLocalBIP44ReceiveIdx());
            indexes.put("local49idx", AddressFactory.getInstance(context).getLocalBIP49ReceiveIdx());
            indexes.put("local84idx", AddressFactory.getInstance(context).getLocalBIP84ReceiveIdx());
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

        return indexes;
    }

    public void fromJSON(JSONObject obj) {

        try {
            if(obj.has("local44idx")) {
                AddressFactory.getInstance(context).setLocalBIP44ReceiveIdx(obj.getInt("local44idx"));
            }
            if(obj.has("local49idx")) {
                AddressFactory.getInstance(context).setLocalBIP49ReceiveIdx(obj.getInt("local49idx"));
            }
            if(obj.has("local84idx")) {
                AddressFactory.getInstance(context).setLocalBIP84ReceiveIdx(obj.getInt("local84idx"));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
