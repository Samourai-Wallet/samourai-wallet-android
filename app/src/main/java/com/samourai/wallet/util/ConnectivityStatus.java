package com.samourai.wallet.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectivityStatus {

    ConnectivityStatus() {
        ;
    }

    public static boolean hasConnectivity(Context ctx) {
        boolean ret = false;
        boolean userEnabledOffline = PrefsUtil.getInstance(ctx).getValue(PrefsUtil.OFFLINE, false);
        if (userEnabledOffline) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo neti = cm.getActiveNetworkInfo();
            if (neti != null && neti.isConnectedOrConnecting()) {
                ret = true;
            }
        }

        return ret;
	}

	public static boolean hasWiFi(Context ctx) {
		boolean ret = false;

 		ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null) {
    	    if(cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
    	        ret = true;
    	    }
    	}

        return ret;
	}

    public static boolean checkVPN(Context ctx) {
        boolean ret = false;

        ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null) {
            if(cm.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnected()) {
                ret = true;
            }
        }

        return ret;
    }

}
