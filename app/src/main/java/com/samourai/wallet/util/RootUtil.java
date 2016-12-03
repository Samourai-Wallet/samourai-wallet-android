package com.samourai.wallet.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class RootUtil {

    private static RootUtil instance = null;

    private RootUtil()  { ; }

    public static RootUtil getInstance()    {

        if(instance == null)    {
            instance = new RootUtil();
        }

        return instance;
    }

    public boolean isDeviceRooted() {
        return buildTags() || checkPaths() || checkSu();
    }

    private boolean buildTags() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private boolean checkPaths() {

        String[] paths = {
                "/data/local/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/sbin/su",
                "/system/app/Superuser.apk",
                "/system/bin/failsafe/su",
                "/system/bin/su",
                "/system/sd/xbin/su",
                "/system/xbin/su"
        };

        for(String path : paths) {
            if(new File(path).exists()) {
                return true;
            }
        }

        return false;
    }

    private boolean checkSu() {

        Process process = null;

        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if(in.readLine() != null) {
                return true;
            }
            return false;
        }
        catch (Throwable t) {
            return false;
        }
        finally {
            if(process != null) {
                process.destroy();
            }
        }

    }

}
