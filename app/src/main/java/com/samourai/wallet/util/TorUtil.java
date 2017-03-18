package com.samourai.wallet.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class TorUtil {

    private static Context context = null;
    private static TorUtil instance = null;

    private static boolean statusFromBroadcast = false;

    private TorUtil()   { ; }

    public static TorUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new TorUtil();
        }

        return instance;

    }

    public boolean statusFromBroadcast() {
        return statusFromBroadcast;
    }

    public void setStatusFromBroadcast(boolean status) {
        statusFromBroadcast = status;
    }

    public boolean orbotIsRunning() {
        return (getPID() != -1);
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();

        try {

            jsonPayload.put("active", statusFromBroadcast);

        }
        catch(JSONException je) {
            ;
        }

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

        try {

            if(jsonPayload.has("active"))    {
                statusFromBroadcast = jsonPayload.getBoolean("active");
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

    private int getPID() {

        String dataPath = context.getFilesDir().getParentFile().getParentFile().getAbsolutePath();
        final String execPath = "/" + OrbotHelper.ORBOT_PACKAGE_NAME + "/app_bin/tor";
        String command = dataPath + execPath;

        int pid = -1;
        pid = getPIDCmd(command);
        if (pid == -1) {
            String defaultCommand = "/data/data" + execPath;
            pid = getPIDCmd(defaultCommand);
        }

        return pid;
    }

    private int getPIDCmd(String command) {

        int pid = -1;

        try {
            pid = getPIDviaPIDOf(command);

            if (pid == -1)   {
                pid = getPIDviaPS(command);
            }
        }
        catch (Exception e) {
            try {
                pid = getPIDviaPS(command);
            }
            catch (Exception e2) {
                Log.e("TorUtil", "Unable to get proc id for command: " + URLEncoder.encode(command), e2);
            }
        }

        return pid;
    }

    private int getPIDviaPIDOf(String command) throws Exception {

        int pid = -1;

        Process proc = null;
        Runtime runtime = Runtime.getRuntime();
        String baseName = new File(command).getName();
        proc = runtime.exec(new String[] {
                "pidof", baseName
        });

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null)  {

            try {
                pid = Integer.parseInt(line.trim());
                break;
            }
            catch (NumberFormatException e) {
                Log.e("TorUtil", "cannot parse process pid: " + line, e);
            }
        }

        return pid;

    }

   private int getPIDviaPS(String command) throws Exception    {

        int pid = -1;

        Process proc = null;
        Runtime runtime = Runtime.getRuntime();
        proc = runtime.exec("ps");

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null)  {
            if (line.indexOf(' ' + command) != -1)  {

                StringTokenizer st = new StringTokenizer(line, " ");
                st.nextToken();

                pid = Integer.parseInt(st.nextToken().trim());

                break;
            }
        }

        return pid;

    }

}