package com.samourai.wallet.util;
 
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.prng.PRNGFixes;
import com.samourai.wallet.R;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

public class AppUtil {

    public static final int MIN_BACKUP_PW_LENGTH = 6;
    public static final int MAX_BACKUP_PW_LENGTH = 255;

    public static final String TOR_PACKAGE_ID = "org.torproject.android";
    public static final String OPENVPN_PACKAGE_ID = "de.blinkt.openvpn";

    private boolean isInForeground = false;
	
	private static AppUtil instance = null;
	private static Context context = null;

    private static String strReceiveQRFilename = null;
    private static String strBackupFilename = null;

    private static boolean PRNG_FIXES = false;

    private static boolean CLIPBOARD_SEEN = false;

    private static boolean isOfflineMode = false;
    private static boolean isUserOfflineMode = false;

    private AppUtil() { ; }

	public static AppUtil getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
            strReceiveQRFilename = context.getExternalCacheDir() + File.separator + "qr.png";
            strBackupFilename = context.getCacheDir() + File.separator + "backup.asc";
			instance = new AppUtil();
		}
		
		return instance;
	}

    public boolean isOfflineMode() {

        isOfflineMode = (isUserOfflineMode() || !ConnectivityStatus.hasConnectivity(context)) ? true : false;

        return isOfflineMode;
    }

    public void setOfflineMode(boolean offline) {
        isOfflineMode = offline;
    }

    public boolean isUserOfflineMode() {
        return isUserOfflineMode;
    }

    public void setUserOfflineMode(boolean offline) {
        isUserOfflineMode = offline;
    }

    public void wipeApp() {

        try  {
            HD_Wallet bip84w = BIP84Util.getInstance(context).getWallet();
            String strIdentifier84 = WhirlpoolUtils.getInstance().computeWalletIdentifier(bip84w);
            File whirlpoolUtxos = WhirlpoolUtils.getInstance().computeUtxosFile(strIdentifier84, context);
            whirlpoolUtxos.delete();
            File whirlpoolIndexes = WhirlpoolUtils.getInstance().computeIndexFile(strIdentifier84, context);
            whirlpoolIndexes.delete();
        }
        catch(Exception e) {
            e.printStackTrace();
            ;
        }
/*
        try {
            HD_Wallet hdw = HD_WalletFactory.getInstance(context).get();
            String[] s = hdw.getXPUBs();
            for(int i = 0; i < s.length; i++)   {
//                APIFactory.getInstance(context).deleteXPUB(s[i], false);
            }
            String _s = BIP49Util.getInstance(context).getWallet().getAccount(0).ypubstr();
//            APIFactory.getInstance(context).deleteXPUB(_s, true);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
*/

        try {
            PayloadUtil.getInstance(context).wipe();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }

        BIP49Util.getInstance(context).reset();
        BIP84Util.getInstance(context).reset();
        BIP47Util.getInstance(context).reset();

        deleteBackup();
        deleteQR();

        final ComponentName component = new ComponentName(context.getApplicationContext().getPackageName(), "com.samourai.wallet.MainActivity");
        try {
            context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            PrefsUtil.getInstance(context).setValue(PrefsUtil.ICON_HIDDEN, false);
        }
        catch(IllegalArgumentException iae) {
            ;
        }

        APIFactory.getInstance(context).setXpubBalance(0L);
        APIFactory.getInstance(context).reset();
		PrefsUtil.getInstance(context).clear();
        BlockedUTXO.getInstance().clear();
        BlockedUTXO.getInstance().clearPostMix();
        RicochetMeta.getInstance(context).empty();
        SendAddressUtil.getInstance().reset();
        SentToFromBIP47Util.getInstance().reset();
        BatchSendUtil.getInstance().clear();
        AccessFactory.getInstance(context).setIsLoggedIn(false);

        try {
            clearApplicationData();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
	}

	public void restartApp() {
		Intent intent = new Intent(context, MainActivity2.class);
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.ICON_HIDDEN, false) == true) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
		context.startActivity(intent);
	}

	public void restartApp(Bundle extras) {

		Intent intent = new Intent(context, MainActivity2.class);
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.ICON_HIDDEN, false) == true) {
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        else {
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        if(extras!=null){
            intent.putExtras(extras);
        }
		context.startActivity(intent);
	}

	public void restartApp(String name, boolean value) {
        Intent intent = new Intent(context, MainActivity2.class);
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.ICON_HIDDEN, false) == true) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
		if(name != null) {
    		intent.putExtra(name, value);
		}
		context.startActivity(intent);
	}

    public void restartApp(String name, String value) {
        Intent intent = new Intent(context, MainActivity2.class);
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.ICON_HIDDEN, false) == true) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        if(name != null && value != null) {
            intent.putExtra(name, value);
        }
        context.startActivity(intent);
    }

    public boolean isServiceRunning(Class<?> serviceClass) {

        ActivityManager manager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d("AppUtil", "service class name:" + serviceClass.getName() + " is running");
                return true;
            }
        }

        Log.d("AppUtil", "service class name:" + serviceClass.getName() + " is not running");
        return false;
    }

    public boolean isInForeground() {
        return isInForeground;
    }

    public void setIsInForeground(boolean foreground) {
        isInForeground = foreground;
    }

    public String getReceiveQRFilename(){
        return strReceiveQRFilename;
    }

    public String getBackupFilename(){
        return strBackupFilename;
    }

    public void deleteQR(){
        String strFileName = strReceiveQRFilename;
        File file = new File(strFileName);
        if(file.exists()) {
            file.delete();
        }
    }

    public void deleteBackup(){
        String strFileName = strBackupFilename;
        File file = new File(strFileName);
        if(file.exists()) {
            file.delete();
        }
    }

    public boolean isPRNG_FIXED() {
        return PRNG_FIXES;
    }

    public void setPRNG_FIXED(boolean prng) {
        PRNG_FIXES = prng;
    }

    public void applyPRNGFixes()    {
        try {
            PRNGFixes.apply();
        }
        catch(Exception e0) {
            //
            // some Android 4.0 devices throw an exception when PRNGFixes is re-applied
            // removing provider before apply() is a workaround
            //
            Security.removeProvider("LinuxPRNG");
            try {
                PRNGFixes.apply();
            }
            catch(Exception e1) {
                Toast.makeText(context, R.string.cannot_launch_app, Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
        }
    }

    public void checkTimeOut()   {
        if(TimeOutUtil.getInstance().isTimedOut())    {
            AppUtil.getInstance(context).restartApp();
        }
        else    {
            TimeOutUtil.getInstance().updatePin();
        }
    }

    public boolean isSideLoaded() {
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return installer == null || !validInstallers.contains(installer);
    }

    public boolean isClipboardSeen() {
        return CLIPBOARD_SEEN;
    }

    public void setClipboardSeen(boolean seen) {
        CLIPBOARD_SEEN = seen;
    }

    private void clearApplicationData() throws IOException {
        File cacheDirectory = context.getCacheDir();
        File applicationDirectory = new File(cacheDirectory.getParent());
        if (applicationDirectory.exists()) {
            String[] fileNames = applicationDirectory.list();
            for (String fileName : fileNames) {
                deleteFiles(new File(applicationDirectory, fileName));
            }
        }
    }

    private synchronized boolean deleteFiles(File file) throws IOException {
        boolean deletedAll = true;
        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (int i = 0; i < children.length; i++) {
                    deletedAll = deleteFiles(new File(file, children[i])) && deletedAll;
                }
            }
            else {
                deletedAll = file.delete();
            }
        }

        return deletedAll;
    }

}
