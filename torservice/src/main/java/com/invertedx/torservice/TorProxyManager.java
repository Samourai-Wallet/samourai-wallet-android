package com.invertedx.torservice;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.invertedx.torservice.util.CustomShell;
import com.invertedx.torservice.util.CustomTorResourceInstaller;
import com.invertedx.torservice.util.Prefs;
import com.invertedx.torservice.util.TorServiceUtils;
import com.invertedx.torservice.util.Utils;
import com.jaredrummler.android.shell.CommandResult;

import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.TorControlConnection;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import info.pluggabletransports.dispatch.util.TransportListener;
import info.pluggabletransports.dispatch.util.TransportManager;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class TorProxyManager implements TorServiceConstants, TorPrefernceConstants {

    public final static String BINARY_TOR_VERSION = org.torproject.android.binary.TorServiceConstants.BINARY_TOR_VERSION;

    private final static int CONTROL_SOCKET_TIMEOUT = 60000;

    private TorControlConnection conn = null;
    private int mLastProcessId = -1;

    public static int mPortSOCKS = -1;
    public static int mPortHTTP = -1;
    //    public static int mPortDns = TOR_DNS_PORT_DEFAULT;
    public static int mPortTrans = TOR_TRANSPROXY_PORT_DEFAULT;

    private static final int NOTIFY_ID = 1;
    private static final int ERROR_NOTIFY_ID = 3;
    private static final int HS_NOTIFY_ID = 4;

    private ArrayList<String> configBuffer = null;
    private ArrayList<String> resetBuffer = null;

    private File fileControlPort, filePid;

    private boolean bootstrapComplete = false;
    private boolean mConnectivity = true;

    private Context mAndroidContext;


    private ExecutorService mExecutor = Executors.newFixedThreadPool(3);

    public enum ConnectionStatus {
        IDLE,
        CONNECTED,
        CONNECTING,
        DISCONNECTING,
        DISCONNECTED,
    }

    TorEventHandler mEventHandler;

    private ConnectionStatus mCurrentStatus = ConnectionStatus.DISCONNECTED;

    public static File appBinHome;
    public static File appCacheHome;

    public static File fileTor;
    public static File fileObfsclient;
    public static File fileTorRc;
    private File mHSBasePath;

    public BehaviorSubject<ConnectionStatus> torStatus = BehaviorSubject.create();
    public Subject<String> torLogs = PublishSubject.create();
    public Subject<String> torCircuitStatus = PublishSubject.create();
    public Subject<Map<String, Long>> bandWidthStatus = PublishSubject.create();

    private ArrayList<Bridge> alBridges = null;

    private static final Uri HS_CONTENT_URI = Uri.parse("content://org.torproject.android.ui.hiddenservices.providers/hs");
    private static final Uri COOKIE_CONTENT_URI = Uri.parse("content://org.torproject.android.ui.hiddenservices.providers.cookie/cookie");

    public static final class HiddenService implements BaseColumns {
        public static final String NAME = "name";
        public static final String PORT = "port";
        public static final String ONION_PORT = "onion_port";
        public static final String DOMAIN = "domain";
        public static final String AUTH_COOKIE = "auth_cookie";
        public static final String AUTH_COOKIE_VALUE = "auth_cookie_value";
        public static final String CREATED_BY_USER = "created_by_user";
        public static final String ENABLED = "enabled";

        private HiddenService() {
        }
    }

    public static final class ClientCookie implements BaseColumns {
        public static final String DOMAIN = "domain";
        public static final String AUTH_COOKIE_VALUE = "auth_cookie_value";
        public static final String ENABLED = "enabled";

        private ClientCookie() {
        }
    }

    private String[] hsProjection = new String[]{
            HiddenService._ID,
            HiddenService.NAME,
            HiddenService.DOMAIN,
            HiddenService.PORT,
            HiddenService.AUTH_COOKIE,
            HiddenService.AUTH_COOKIE_VALUE,
            HiddenService.ONION_PORT,
            HiddenService.ENABLED};

    private String[] cookieProjection = new String[]{
            ClientCookie._ID,
            ClientCookie.DOMAIN,
            ClientCookie.AUTH_COOKIE_VALUE,
            ClientCookie.ENABLED};

    public void debug(String msg) {
        if (Prefs.useDebugLogging()) {
            Log.d(TorPrefernceConstants.TAG, msg);
            sendCallbackLogMessage(msg);

        }
    }

    protected void bandWidthUpdate(String toString, long read, long written) {

    }


    protected void logException(String msg, Exception e) {
        if (Prefs.useDebugLogging()) {
            Log.e(TorPrefernceConstants.TAG, msg, e);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));

            sendCallbackLogMessage(msg + '\n' + new String(baos.toByteArray()));

        } else
            sendCallbackLogMessage(msg);

    }


    public boolean findExistingTorDaemon() {
        try {
            mLastProcessId = initControlConnection(3, true);

            if (mLastProcessId != -1 && conn != null) {
                setStatus(ConnectionStatus.CONNECTED);
                sendCallbackLogMessage("found existing Tor process&#8230;");
                sendLogs("Connected to the Tor network");
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    protected void setStatus(ConnectionStatus status) {
        if (torStatus.hasObservers()) {
            torStatus.onNext(status);
        }
        mCurrentStatus = status;
    }

    protected void sendLogs(String log) {
        if (torLogs.hasObservers()) {
            torLogs.onNext(log);
        }
    }


    public Completable stopTor() {
        return Completable.fromCallable(() -> {
            stopTorAsync();
            return true;
        }).subscribeOn(Schedulers.io());
    }


    private void stopTorAsync() throws Exception {
        setStatus(ConnectionStatus.DISCONNECTING);
        killAllDaemons();
        setStatus(ConnectionStatus.DISCONNECTED);

    }


    private void killAllDaemons() throws Exception {

        if (conn != null) {
            logNotice("Using control port to shutdown Tor");

            try {
                logNotice("sending HALT signal to Tor process");
                conn.shutdownTor("HALT");

            } catch (IOException e) {
                Log.d(TorPrefernceConstants.TAG, "error shutting down Tor via connection", e);
            }

            conn = null;
        }

        /**
         if (mLastProcessId != -1)
         killProcess(mLastProcessId + "", "-9");

         if (filePid != null && filePid.exists())
         {
         List<String> lines = IOUtils.readLines(new FileReader(filePid));
         String torPid = lines.get(0);
         killProcess(torPid,"-9");

         }


         // if that fails, try again using native utils
         try {
         killProcess(fileTor, "-9"); // this is -HUP
         } catch (Exception e) {
         e.printStackTrace();
         }**/

    }

    protected void setCircuitStatus(String status) {

        if(status.contains("LAUNCHED") || status.contains("BUILT")){
            setStatus(ConnectionStatus.CONNECTED);
        }
        if (status.contains("NEWNYM")) {
            if (torLogs.hasObservers())
                torLogs.onNext(status);
         }
        if (torCircuitStatus.hasObservers())
            torCircuitStatus.onNext(status);

    }


    public Completable requestTorRereadConfig() {
        return Completable.fromCallable(() -> {
            try {
                if (conn != null)
                    conn.signal("HUP");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }).subscribeOn(Schedulers.io());
        /**
         // if that fails, try again using native utils
         try {
         killProcess(fileTor, "-1"); // this is -HUP
         } catch (Exception e) {
         e.printStackTrace();
         }**/
    }

    public void logNotice(String msg) {
        if (msg != null && msg.trim().length() > 0) {
            Log.d(TorPrefernceConstants.TAG, msg);
            sendCallbackLogMessage(msg);
        }
    }

    public TorProxyManager(Context mAndroidContext) {
        this.mAndroidContext = mAndroidContext;
        Prefs.setContext(mAndroidContext);
        this.setStatus(ConnectionStatus.IDLE);

        try {
            appBinHome = this.mAndroidContext.getFilesDir();//getDir(TorServiceConstants.DIRECTORY_TOR_BINARY, Application.MODE_PRIVATE);
            if (!appBinHome.exists())
                appBinHome.mkdirs();

            appCacheHome = this.mAndroidContext.getDir(DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);
            if (!appCacheHome.exists())
                appCacheHome.mkdirs();

            fileTorRc = new File(appBinHome, TORRC_ASSET_KEY);
            fileControlPort = new File(this.mAndroidContext.getFilesDir(), TOR_CONTROL_PORT_FILE);
            filePid = new File(this.mAndroidContext.getFilesDir(), TOR_PID_FILE);

            mHSBasePath = new File(
                    this.mAndroidContext.getFilesDir().getAbsolutePath(),
                    TorServiceConstants.HIDDEN_SERVICES_DIR
            );

            if (!mHSBasePath.isDirectory())
                mHSBasePath.mkdirs();

            mEventHandler = new TorEventHandler(this);


            //    IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            //  registerReceiver(mNetworkStateReceiver , mNetworkStateFilter);

            torUpgradeAndConfig();

            pluggableTransportInstall();

            new Thread(() -> {
                try {
                    findExistingTorDaemon();
                } catch (Exception e) {
                    Log.e(TorPrefernceConstants.TAG, "error onBind", e);
                    logNotice("error finding exiting process: " + e.toString());
                }

            }).start();

        } catch (Exception e) {
            //what error here
            Log.e(TorPrefernceConstants.TAG, "Error installing Orbot binaries", e);
            logNotice("There was an error installing Orbot binaries");
        }

    }


    public ConnectionStatus getCurrentStatus() {
        return mCurrentStatus;
    }

    private boolean pluggableTransportInstall() {

        fileObfsclient = new TransportManager() {
            @Override
            public void startTransportSync(TransportListener transportListener) {

            }
        }.installTransport(this.mAndroidContext, OBFSCLIENT_ASSET_KEY);

        if (fileObfsclient != null && fileObfsclient.exists()) {

            fileObfsclient.setReadable(true);
            fileObfsclient.setExecutable(true);
            fileObfsclient.setWritable(false);
            fileObfsclient.setWritable(true, true);

            return fileObfsclient.canExecute();
        }

        return false;
    }

    private boolean torUpgradeAndConfig() throws IOException, TimeoutException {

        SharedPreferences prefs = Prefs.getSharedPrefs(this.mAndroidContext);
        String version = prefs.getString(PREF_BINARY_TOR_VERSION_INSTALLED, null);

        logNotice("checking binary version: " + version);

        CustomTorResourceInstaller installer = new CustomTorResourceInstaller(this.mAndroidContext, appBinHome);
        logNotice("upgrading binaries to latest version: " + BINARY_TOR_VERSION);


        fileTor = installer.installResources();

        if (fileTor != null && fileTor.canExecute()) {
            prefs.edit().putString(PREF_BINARY_TOR_VERSION_INSTALLED, BINARY_TOR_VERSION).apply();

            fileTorRc = new File(appBinHome, "torrc");//installer.getTorrcFile();
            if (!fileTorRc.exists())
                return false;

            return true;
        }


        return false;
    }

    public File updateTorrcCustomFile() throws IOException, TimeoutException {
        SharedPreferences prefs = Prefs.getSharedPrefs(this.mAndroidContext);

        StringBuffer extraLines = new StringBuffer();

        extraLines.append("\n");
        extraLines.append("ControlPortWriteToFile").append(' ').append(fileControlPort.getCanonicalPath()).append('\n');

        extraLines.append("PidFile").append(' ').append(filePid.getCanonicalPath()).append('\n');

        //       extraLines.append("RunAsDaemon 1").append('\n');
        //       extraLines.append("AvoidDiskWrites 1").append('\n');

        String socksPortPref = prefs.getString(TorPrefernceConstants.PREF_SOCKS, (TorServiceConstants.SOCKS_PROXY_PORT_DEFAULT));

        if (socksPortPref.indexOf(':') != -1)
            socksPortPref = socksPortPref.split(":")[1];

        socksPortPref = checkPortOrAuto(socksPortPref);

      /*
            ** DISABLED HTTP PROXY ***

      String httpPortPref = prefs.getString(TorPrefernceConstants.PREF_HTTP, (TorServiceConstants.HTTP_PROXY_PORT_DEFAULT));

        if (httpPortPref.indexOf(':')!=-1)
            httpPortPref = httpPortPref.split(":")[1];

        httpPortPref = checkPortOrAuto(httpPortPref);
*/
        String isolate = "";
        if (prefs.getBoolean(TorPrefernceConstants.PREF_ISOLATE_DEST, false)) {
            isolate += " IsolateDestAddr ";
        }

        String ipv6Pref = "";

        if (prefs.getBoolean(TorPrefernceConstants.PREF_PREFER_IPV6, true)) {
            ipv6Pref += " IPv6Traffic PreferIPv6 ";
        }

        if (prefs.getBoolean(TorPrefernceConstants.PREF_DISABLE_IPV4, false)) {
            ipv6Pref += " IPv6Traffic NoIPv4Traffic ";
        }

        extraLines.append("SOCKSPort ").append(socksPortPref).append(isolate).append(ipv6Pref).append('\n');
        extraLines.append("SafeSocks 0").append('\n');
        extraLines.append("TestSocks 0").append('\n');

        if (Prefs.openProxyOnAllInterfaces())
            extraLines.append("SocksListenAddress 0.0.0.0").append('\n');


        /*
                    ** DISABLED HTTP PROXY ***

        extraLines.append("HTTPTunnelPort ").append(httpPortPref).append('\n');

         */

        if (prefs.getBoolean(TorPrefernceConstants.PREF_CONNECTION_PADDING, false)) {
            extraLines.append("ConnectionPadding 1").append('\n');
        }

        if (prefs.getBoolean(TorPrefernceConstants.PREF_REDUCED_CONNECTION_PADDING, true)) {
            extraLines.append("ReducedConnectionPadding 1").append('\n');
        }

        if (prefs.getBoolean(TorPrefernceConstants.PREF_CIRCUIT_PADDING, true)) {
            extraLines.append("CircuitPadding 1").append('\n');
        } else {
            extraLines.append("CircuitPadding 0").append('\n');
        }

        if (prefs.getBoolean(TorPrefernceConstants.PREF_REDUCED_CIRCUIT_PADDING, true)) {
            extraLines.append("ReducedCircuitPadding 1").append('\n');
        }

        String transPort = prefs.getString("pref_transport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT + "");
//        String dnsPort = prefs.getString("pref_dnsport", TorServiceConstants.TOR_DNS_PORT_DEFAULT+"");

        extraLines.append("TransPort ").append(checkPortOrAuto(transPort)).append('\n');
//    	extraLines.append("DNSPort ").append(checkPortOrAuto(dnsPort)).append('\n');

        extraLines.append("VirtualAddrNetwork 10.192.0.0/10").append('\n');
        extraLines.append("AutomapHostsOnResolve 1").append('\n');

        extraLines.append("DormantClientTimeout 10 minutes").append('\n');
        extraLines.append("DormantOnFirstStartup 0").append('\n');

        extraLines.append("DisableNetwork 0").append('\n');

        if (Prefs.useDebugLogging()) {
            extraLines.append("Log debug syslog").append('\n');
            extraLines.append("Log info syslog").append('\n');
            extraLines.append("SafeLogging 0").append('\n');
        }

        extraLines = processSettingsImpl(extraLines);

        if (extraLines == null)
            return null;

        extraLines.append('\n');
        extraLines.append(prefs.getString("pref_custom_torrc", "")).append('\n');

        logNotice("updating torrc custom configuration...");

        debug("torrc.custom=" + extraLines.toString());

        File fileTorRcCustom = new File(fileTorRc.getAbsolutePath() + ".custom");
        boolean success = updateTorConfigCustom(fileTorRcCustom, extraLines.toString());

        if (success && fileTorRcCustom.exists()) {
            logNotice("success.");
            return fileTorRcCustom;
        } else
            return null;

    }

    private String checkPortOrAuto(String portString) {
        if (!portString.equalsIgnoreCase("auto")) {
            boolean isPortUsed = true;
            int port = Integer.parseInt(portString);

            while (isPortUsed) {
                isPortUsed = TorServiceUtils.isPortOpen("127.0.0.1", port, 500);

                if (isPortUsed) //the specified port is not available, so let Tor find one instead
                    port++;
            }


            return port + "";
        }

        return portString;

    }

    public boolean updateTorConfigCustom(File fileTorRcCustom, String extraLines) throws IOException, FileNotFoundException, TimeoutException {
        FileWriter fos = new FileWriter(fileTorRcCustom, false);
        PrintWriter ps = new PrintWriter(fos);
        ps.print(extraLines);
        ps.flush();
        ps.close();
        return true;
    }


    /**
     * The entire process for starting tor and related services is run from this method.
     */
    public void startTor() {

        String torProcId = null;

        try {
            if (conn != null) torProcId = conn.getInfo("process/pid");
        } catch (Exception e) {
        }

        try {

            // STATUS_STARTING is set in onCreate()
            if (mCurrentStatus == ConnectionStatus.DISCONNECTING) {
                // these states should probably be handled better
                sendCallbackLogMessage("Ignoring start request, currently " + mCurrentStatus);
                return;
            } else if (mCurrentStatus == ConnectionStatus.CONNECTED && (torProcId != null)) {

                sendCallbackLogMessage("Ignoring start request, already started.");
                setTorNetworkEnabled(true);

                return;
            }


            // make sure there are no stray daemons running
            killAllDaemons();

            SharedPreferences prefs = Prefs.getSharedPrefs(this.mAndroidContext);
            String version = prefs.getString(PREF_BINARY_TOR_VERSION_INSTALLED, null);
            logNotice("checking binary version: " + version);

            setStatus(ConnectionStatus.CONNECTING);

            ArrayList<String> customEnv = new ArrayList<String>();


            boolean success = runTorShellCmd();


        } catch (Exception e) {
            logException("Unable to start Tor: " + e.toString(), e);
            stopTor();
            sendLogs(this.mAndroidContext.getString(R.string.unable_to_start_tor));
        }
    }

    private void updateOnionNames() throws SecurityException {
        // Tor is running, update new .onion names at db
        ContentResolver mCR = mAndroidContext.getContentResolver();
        Cursor hidden_services = mCR.query(HS_CONTENT_URI, hsProjection, null, null, null);
        if (hidden_services != null) {
            try {
                while (hidden_services.moveToNext()) {
                    String HSDomain = hidden_services.getString(hidden_services.getColumnIndex(HiddenService.DOMAIN));
                    Integer HSLocalPort = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.PORT));
                    Integer HSAuthCookie = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.AUTH_COOKIE));
                    String HSAuthCookieValue = hidden_services.getString(hidden_services.getColumnIndex(HiddenService.AUTH_COOKIE_VALUE));

                    // Update only new domains or restored from backup with auth cookie
                    if ((HSDomain == null || HSDomain.length() < 1) || (HSAuthCookie == 1 && (HSAuthCookieValue == null || HSAuthCookieValue.length() < 1))) {
                        String hsDirPath = new File(mHSBasePath.getAbsolutePath(), "hs" + HSLocalPort).getCanonicalPath();
                        File file = new File(hsDirPath, "hostname");

                        if (file.exists()) {
                            ContentValues fields = new ContentValues();

                            try {
                                String onionHostname = Utils.readString(new FileInputStream(file)).trim();
                                if (HSAuthCookie == 1) {
                                    String[] aux = onionHostname.split(" ");
                                    onionHostname = aux[0];
                                    fields.put(HiddenService.AUTH_COOKIE_VALUE, aux[1]);
                                }
                                fields.put(HiddenService.DOMAIN, onionHostname);
                                mCR.update(HS_CONTENT_URI, fields, "port=" + HSLocalPort, null);
                            } catch (FileNotFoundException e) {
                                logException("unable to read onion hostname file", e);
                                sendLogs(mAndroidContext.getString(R.string.unable_to_read_hidden_service_name));
                            }
                        } else {
                            sendLogs(this.mAndroidContext.getString(R.string.unable_to_read_hidden_service_name));
                        }
                    }
                }

            } catch (NumberFormatException e) {
                Log.e(TorPrefernceConstants.TAG, "error parsing hsport", e);
            } catch (Exception e) {
                Log.e(TorPrefernceConstants.TAG, "error starting share server", e);
            }

            hidden_services.close();
        }
    }

    private boolean runTorShellCmd() throws Exception {
        boolean result = true;

        File fileTorrcCustom = updateTorrcCustomFile();

        //make sure Tor exists and we can execute it
        if (fileTor == null || (!fileTor.exists()) || (!fileTor.canExecute()))
            return false;

        if ((!fileTorRc.exists()) || (!fileTorRc.canRead()))
            return false;

        if ((!fileTorrcCustom.exists()) || (!fileTorrcCustom.canRead()))
            return false;

        sendCallbackLogMessage(mAndroidContext.getString(R.string.status_starting_up));

        String torCmdString = fileTor.getCanonicalPath()
                + " DataDirectory " + appCacheHome.getCanonicalPath()
                + " --defaults-torrc " + fileTorRc.getCanonicalPath()
                + " -f " + fileTorrcCustom.getCanonicalPath();

        int exitCode = -1;

        try {
            exitCode = exec(torCmdString + " --verify-config", true);
        } catch (Exception e) {
            logNotice("Tor configuration did not verify: " + e.getMessage());
            return false;
        }

        if (exitCode == 0) {
            logNotice("Tor configuration VERIFIED.");
            try {
                exitCode = exec(torCmdString, false);
            } catch (Exception e) {
                logNotice("Tor was unable to start: " + e.getMessage());
                result = false;

                throw new Exception("Tor was unable to start: " + e.getMessage());

            }

            if (exitCode != 0) {
                logNotice("Tor did not start. Exit:" + exitCode);
                return false;
            }

            //now try to connect
            mLastProcessId = initControlConnection(10, false);

            if (mLastProcessId == -1) {
                logNotice(mAndroidContext.getString(R.string.couldn_t_start_tor_process_) + "; exit=" + exitCode);
                result = false;
                throw new Exception(mAndroidContext.getString(R.string.couldn_t_start_tor_process_) + "; exit=" + exitCode);
            } else {

                logNotice("Tor started; process id=" + mLastProcessId);
                result = true;
            }
        }

        return result;
    }

    public void exec(Runnable runn) {
        mExecutor.execute(runn);
    }

    private int exec(String cmd, boolean wait) throws Exception {
        HashMap<String, String> mapEnv = new HashMap();
        mapEnv.put("HOME", appBinHome.getAbsolutePath());

        CommandResult result = CustomShell.run("sh", wait, mapEnv, cmd);
        debug("executing: " + cmd);
        debug("stdout: " + result.getStdout());
        debug("stderr: " + result.getStderr());

        return result.exitCode;


    }

    private int initControlConnection(int maxTries, boolean isReconnect) throws Exception {
        int controlPort = -1;
        int attempt = 0;


        while (conn == null && attempt++ < maxTries && (mCurrentStatus != ConnectionStatus.IDLE && mCurrentStatus != ConnectionStatus.DISCONNECTED)) {
            try {

                controlPort = getControlPort();
                if (controlPort != -1) {
                    logNotice("Connecting to control port: " + controlPort);

                    Socket torConnSocket = new Socket(IP_LOCALHOST, controlPort);
                    torConnSocket.setSoTimeout(CONTROL_SOCKET_TIMEOUT);

                    conn = new TorControlConnection(torConnSocket);
                    conn.launchThread(true);//is daemon

                    break;
                }

            } catch (Exception ce) {
                conn = null;
                logException("Error connecting to Tor local control port: " + ce.getMessage(), ce);

            }


            try {
                logNotice("waiting...");
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (conn != null) {
            logNotice("SUCCESS connected to Tor control port.");

            File fileCookie = new File(appCacheHome, TOR_CONTROL_COOKIE);

            if (fileCookie.exists()) {
                byte[] cookie = new byte[(int) fileCookie.length()];
                DataInputStream fis = new DataInputStream(new FileInputStream(fileCookie));
                fis.read(cookie);
                fis.close();
                conn.authenticate(cookie);

                logNotice("SUCCESS - authenticated to control port.");

                sendCallbackLogMessage(mAndroidContext.getString(R.string.tor_process_starting) + ' ' + mAndroidContext.getString(R.string.tor_process_complete));

                String torProcId = conn.getInfo("process/pid");

                String confSocks = conn.getInfo("net/listeners/socks");
                StringTokenizer st = new StringTokenizer(confSocks, " ");

                confSocks = st.nextToken().split(":")[1];
                confSocks = confSocks.substring(0, confSocks.length() - 1);
                mPortSOCKS = Integer.parseInt(confSocks);

                String confHttp = conn.getInfo("net/listeners/httptunnel");
                st = new StringTokenizer(confHttp, " ");
//
//                        confHttp = st.nextToken().split(":")[1];
//                        confHttp = confHttp.substring(0,confHttp.length()-1);
//                        mPortHTTP = Integer.parseInt(confHttp);

                String confDns = conn.getInfo("net/listeners/dns");
                st = new StringTokenizer(confDns, " ");
                if (st.hasMoreTokens()) {
                    confDns = st.nextToken().split(":")[1];
                    confDns = confDns.substring(0, confDns.length() - 1);
//                    mPortDns = Integer.parseInt(confDns);
//                    getSharedPrefs(mAndroidContext).edit().putInt(VpnPrefs.PREFS_DNS_PORT, mPortDns).apply();
                }

                String confTrans = conn.getInfo("net/listeners/trans");
                st = new StringTokenizer(confTrans, " ");
                if (st.hasMoreTokens()) {
                    confTrans = st.nextToken().split(":")[1];
                    confTrans = confTrans.substring(0, confTrans.length() - 1);
                    mPortTrans = Integer.parseInt(confTrans);
                }


                addEventHandler();

                return Integer.parseInt(torProcId);

            } else {
                logNotice("Tor authentication cookie does not exist yet");
                conn = null;

            }
        }

        throw new Exception("Tor control port could not be found");


    }

    private int getControlPort() {
        int result = -1;

        try {
            if (fileControlPort.exists()) {
                debug("Reading control port config file: " + fileControlPort.getCanonicalPath());
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileControlPort));
                String line = bufferedReader.readLine();

                if (line != null) {
                    String[] lineParts = line.split(":");
                    result = Integer.parseInt(lineParts[1]);
                }


                bufferedReader.close();

                //store last valid control port
                SharedPreferences prefs = Prefs.getSharedPrefs(this.mAndroidContext);
                prefs.edit().putInt("controlport", result).commit();

            } else {
                debug("Control Port config file does not yet exist (waiting for tor): " + fileControlPort.getCanonicalPath());

            }


        } catch (FileNotFoundException e) {
            debug("unable to get control port; file not found");
        } catch (Exception e) {
            debug("unable to read control port config file");
        }

        return result;
    }


    public void bandwidthUsed(long read, long written) {
        if (bandWidthStatus.hasObservers()) {
            Map<String, Long> bandWidth = new HashMap<>();

            bandWidth.put("read", read);
            bandWidth.put("written", written);

            bandWidthStatus.onNext(bandWidth);
        }
    }


    public void addEventHandler() throws Exception {
        // We extend NullEventHandler so that we don't need to provide empty
        // implementations for all the events we don't care about.
        // ...
        logNotice("adding control port event handler");

        conn.setEventHandler(mEventHandler);

        conn.setEvents(Arrays.asList(new String[]{
                "ORCONN", "CIRC", "NOTICE", "WARN", "ERR", "BW"}));


        logNotice("SUCCESS added control port event handler");

    }

    public synchronized void enableNetwork(boolean enable) throws IOException {
        if (conn == null) {
            throw new RuntimeException("Tor is not running!");
        }
        conn.setConf("DisableNetwork", enable ? "0" : "1");
    }

    /**
     * Returns the port number that the HTTP proxy is running on
     */
    public int getHTTPPort() throws RemoteException {
        return mPortHTTP;
    }


    /**
     * Returns the port number that the HTTP proxy is running on
     */
    public int getSOCKSPort() throws RemoteException {
        return mPortSOCKS;
    }


    public String getInfo(String key) {
        try {
            if (conn != null) {
                String m = conn.getInfo(key);
                return m;
            }
        } catch (Exception ioe) {
            //    Log.e(TAG,"Unable to get Tor information",ioe);
            logNotice("Unable to get Tor information" + ioe.getMessage());
        }
        return null;
    }

    public String getConfiguration(String name) {
        try {
            if (conn != null) {
                StringBuffer result = new StringBuffer();

                List<ConfigEntry> listCe = conn.getConf(name);

                Iterator<ConfigEntry> itCe = listCe.iterator();
                ConfigEntry ce = null;


                while (itCe.hasNext()) {
                    ce = itCe.next();

                    result.append(ce.key);
                    result.append(' ');
                    result.append(ce.value);
                    result.append('\n');
                }

                return result.toString();
            }
        } catch (Exception ioe) {

            logException("Unable to get Tor configuration: " + ioe.getMessage(), ioe);
        }

        return null;
    }

    private final static String RESET_STRING = "=\"\"";

    /**
     * Set configuration
     **/
    public boolean updateConfiguration(String name, String value, boolean saveToDisk) {


        if (configBuffer == null)
            configBuffer = new ArrayList<String>();

        if (resetBuffer == null)
            resetBuffer = new ArrayList<String>();

        if (value == null || value.length() == 0) {
            resetBuffer.add(name + RESET_STRING);

        } else {
            StringBuffer sbConf = new StringBuffer();
            sbConf.append(name);
            sbConf.append(' ');
            sbConf.append(value);

            configBuffer.add(sbConf.toString());
        }

        return false;
    }


    public void setTorNetworkEnabled(final boolean isEnabled) throws IOException {

        //it is possible to not have a connection yet, and someone might try to newnym
        if (conn != null) {
            new Thread() {
                public void run() {
                    try {

                        final String newValue = isEnabled ? "0" : "1";
                        conn.setConf("DisableNetwork", newValue);
                    } catch (Exception ioe) {
                        debug("error requesting : " + ioe.getLocalizedMessage());
                    }
                }
            }.start();
        }

    }

    public Completable newIdentity() {
        //it is possible to not have a connection yet, and someone might try to newnym
        if (conn != null) {
            return Completable.fromCallable(() -> {

                try {

                    if (hasConnectivity()) {
                        sendLogs(mAndroidContext.getString(R.string.newnym));
                        conn.signal("NEWNYM");

                    }
                } catch (Exception ioe) {

                    debug("error requesting newnym: " + ioe.getLocalizedMessage());
                    throw new Exception("error requesting newnym: " + ioe.getLocalizedMessage());

                }
                return true;
            });
        }
        return null;
    }

    public boolean saveConfiguration() {
        try {
            if (conn != null) {

                if (resetBuffer != null && resetBuffer.size() > 0) {
                    for (String value : configBuffer) {

                        //   debug("removing torrc conf: " + value);


                    }

                    // conn.resetConf(resetBuffer);
                    resetBuffer = null;
                }

                if (configBuffer != null && configBuffer.size() > 0) {

                    for (String value : configBuffer) {

                        debug("Setting torrc conf: " + value);


                    }

                    conn.setConf(configBuffer);

                    configBuffer = null;
                }

                // Flush the configuration to disk.
                //this is doing bad things right now NF 22/07/10
                //conn.saveConf();

                return true;
            }
        } catch (Exception ioe) {
            logException("Unable to update Tor configuration: " + ioe.getMessage(), ioe);
        }

        return false;
    }


    private void sendCallbackLogMessage(String logMessage) {

    }


    /*
     *  Another way to do this would be to use the Observer pattern by defining the
     *  BroadcastReciever in the Android manifest.
     */

    /**
     * private final BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
     *
     * @Override public void onReceive(Context context, Intent intent) {
     * <p>
     * if (mCurrentStatus == STATUS_OFF)
     * return;
     * <p>
     * SharedPreferences prefs = Prefs.getSharedPrefs(getApplicationContext());
     * <p>
     * boolean doNetworKSleep = prefs.getBoolean(TorPrefernceConstants.PREF_DISABLE_NETWORK, true);
     * <p>
     * final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
     * final NetworkInfo netInfo = cm.getActiveNetworkInfo();
     * <p>
     * boolean newConnectivityState = false;
     * int newNetType = -1;
     * <p>
     * if (netInfo!=null)
     * newNetType = netInfo.getType();
     * <p>
     * if(netInfo != null && netInfo.isConnected()) {
     * // WE ARE CONNECTED: DO SOMETHING
     * newConnectivityState = true;
     * }
     * else {
     * // WE ARE NOT: DO SOMETHING ELSE
     * newConnectivityState = false;
     * }
     * <p>
     * if (newConnectivityState != mConnectivity) {
     * mConnectivity = newConnectivityState;
     * <p>
     * //if (mConnectivity)
     * //  newIdentity();
     * }
     * <p>
     * }
     * };
     **/

    private StringBuffer processSettingsImpl(StringBuffer extraLines) throws IOException {
        logNotice(this.mAndroidContext.getString(R.string.updating_settings_in_tor_service));

        SharedPreferences prefs = Prefs.getSharedPrefs(this.mAndroidContext);

        boolean useBridges = Prefs.bridgesEnabled();

        boolean becomeRelay = prefs.getBoolean(TorPrefernceConstants.PREF_OR, false);
        boolean ReachableAddresses = prefs.getBoolean(TorPrefernceConstants.PREF_REACHABLE_ADDRESSES, false);

        boolean enableStrictNodes = prefs.getBoolean("pref_strict_nodes", false);
        String entranceNodes = prefs.getString("pref_entrance_nodes", "");
        String exitNodes = prefs.getString("pref_exit_nodes", "");
        String excludeNodes = prefs.getString("pref_exclude_nodes", "");

        if (!useBridges) {

            extraLines.append("UseBridges 0").append('\n');

            if (Prefs.useVpn()) //set the proxy here if we aren't using a bridge
            {

//                if (!mIsLollipop) {
//                    String proxyType = "socks5";
//                    extraLines.append(proxyType + "Proxy" + ' ' + OrbotVpnManager.sSocksProxyLocalhost + ':' + OrbotVpnManager.sSocksProxyServerPort).append('\n');
//                }
//                ;

            } else {
                String proxyType = prefs.getString("pref_proxy_type", null);
                if (proxyType != null && proxyType.length() > 0) {
                    String proxyHost = prefs.getString("pref_proxy_host", null);
                    String proxyPort = prefs.getString("pref_proxy_port", null);
                    String proxyUser = prefs.getString("pref_proxy_username", null);
                    String proxyPass = prefs.getString("pref_proxy_password", null);

                    if ((proxyHost != null && proxyHost.length() > 0) && (proxyPort != null && proxyPort.length() > 0)) {
                        extraLines.append(proxyType + "Proxy" + ' ' + proxyHost + ':' + proxyPort).append('\n');

                        if (proxyUser != null && proxyPass != null) {
                            if (proxyType.equalsIgnoreCase("socks5")) {
                                extraLines.append("Socks5ProxyUsername" + ' ' + proxyUser).append('\n');
                                extraLines.append("Socks5ProxyPassword" + ' ' + proxyPass).append('\n');
                            } else
                                extraLines.append(proxyType + "ProxyAuthenticator" + ' ' + proxyUser + ':' + proxyPort).append('\n');

                        } else if (proxyPass != null)
                            extraLines.append(proxyType + "ProxyAuthenticator" + ' ' + proxyUser + ':' + proxyPort).append('\n');


                    }
                }
            }
        } else {
            if (fileObfsclient != null
                    && fileObfsclient.exists()
                    && fileObfsclient.canExecute()) {

                loadBridgeDefaults();

                extraLines.append("UseBridges 1").append('\n');
                //    extraLines.append("UpdateBridgesFromAuthority 1").append('\n');

                String bridgeList = Prefs.getBridgesList();
                boolean obfs3Bridges = bridgeList.contains("obfs3");
                boolean obfs4Bridges = bridgeList.contains("obfs4");
                boolean meekBridges = bridgeList.contains("meek");

                //check if any PT bridges are needed
                if (obfs3Bridges)
                    extraLines.append("ClientTransportPlugin obfs3 exec ")
                            .append(fileObfsclient.getAbsolutePath()).append('\n');

                if (obfs4Bridges)
                    extraLines.append("ClientTransportPlugin obfs4 exec ")
                            .append(fileObfsclient.getAbsolutePath()).append('\n');

                if (meekBridges)
                    extraLines.append("ClientTransportPlugin meek_lite exec " + fileObfsclient.getCanonicalPath()).append('\n');

                if (bridgeList != null && bridgeList.length() > 5) //longer then 1 = some real values here
                {
                    String[] bridgeListLines = bridgeList.trim().split("\\n");


                    int bridgeIdx = (int) Math.round(Math.random() * ((double) bridgeListLines.length));
                    String bridgeLine = bridgeListLines[bridgeIdx];
                    extraLines.append("Bridge ");
                    extraLines.append(bridgeLine);
                    extraLines.append("\n");
                    /**
                     for (String bridgeConfigLine : bridgeListLines) {
                     if (!TextUtils.isEmpty(bridgeConfigLine)) {
                     extraLines.append("Bridge ");
                     extraLines.append(bridgeConfigLine.trim());
                     extraLines.append("\n");
                     }

                     }**/

                } else {

                    String type = "obfs4";

                    if (meekBridges)
                        type = "meek_lite";

                    getBridges(type, extraLines);

                }
            } else {
                throw new IOException("Bridge binary does not exist: " + fileObfsclient.getCanonicalPath());
            }
        }


        //only apply GeoIP if you need it
        File fileGeoIP = new File(appBinHome, GEOIP_ASSET_KEY);
        File fileGeoIP6 = new File(appBinHome, GEOIP6_ASSET_KEY);

        if (fileGeoIP.exists()) {
            extraLines.append("GeoIPFile" + ' ' + fileGeoIP.getCanonicalPath()).append('\n');
            extraLines.append("GeoIPv6File" + ' ' + fileGeoIP6.getCanonicalPath()).append('\n');
        }

        if (!TextUtils.isEmpty(entranceNodes))
            extraLines.append("EntryNodes" + ' ' + entranceNodes).append('\n');

        if (!TextUtils.isEmpty(exitNodes))
            extraLines.append("ExitNodes" + ' ' + exitNodes).append('\n');

        if (!TextUtils.isEmpty(excludeNodes))
            extraLines.append("ExcludeNodes" + ' ' + excludeNodes).append('\n');

        extraLines.append("StrictNodes" + ' ' + (enableStrictNodes ? "1" : "0")).append('\n');

        try {
            if (ReachableAddresses) {
                String ReachableAddressesPorts =
                        prefs.getString(TorPrefernceConstants.PREF_REACHABLE_ADDRESSES_PORTS, "*:80,*:443");

                extraLines.append("ReachableAddresses" + ' ' + ReachableAddressesPorts).append('\n');

            }

        } catch (Exception e) {
            sendLogs(mAndroidContext.getString(R.string.your_reachableaddresses_settings_caused_an_exception_));

            return null;
        }

        try {
            if (becomeRelay && (!useBridges) && (!ReachableAddresses)) {
                int ORPort = Integer.parseInt(prefs.getString(TorPrefernceConstants.PREF_OR_PORT, "9001"));
                String nickname = prefs.getString(TorPrefernceConstants.PREF_OR_NICKNAME, "Orbot");

                String dnsFile = writeDNSFile();

                extraLines.append("ServerDNSResolvConfFile" + ' ' + dnsFile).append('\n');
                extraLines.append("ORPort" + ' ' + ORPort).append('\n');
                extraLines.append("Nickname" + ' ' + nickname).append('\n');
                extraLines.append("ExitPolicy" + ' ' + "reject *:*").append('\n');

            }
        } catch (Exception e) {
            this.sendLogs(mAndroidContext.getString(R.string.your_relay_settings_caused_an_exception_));

            return null;
        }

        ContentResolver mCR = this.mAndroidContext.getContentResolver();

        try {
            /* ---- Hidden Services ---- */
            Cursor hidden_services = mCR.query(HS_CONTENT_URI, hsProjection, HiddenService.ENABLED + "=1", null, null);
            if (hidden_services != null) {
                try {
                    while (hidden_services.moveToNext()) {
                        String HSname = hidden_services.getString(hidden_services.getColumnIndex(HiddenService.NAME));
                        Integer HSLocalPort = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.PORT));
                        Integer HSOnionPort = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.ONION_PORT));
                        Integer HSAuthCookie = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.AUTH_COOKIE));
                        String hsDirPath = new File(mHSBasePath.getAbsolutePath(), "hs" + HSLocalPort).getCanonicalPath();

                        debug("Adding hidden service on port: " + HSLocalPort);

                        extraLines.append("HiddenServiceDir" + ' ' + hsDirPath).append('\n');
                        extraLines.append("HiddenServicePort" + ' ' + HSOnionPort + " 127.0.0.1:" + HSLocalPort).append('\n');
                        extraLines.append("HiddenServiceVersion 2").append('\n');

                        if (HSAuthCookie == 1)
                            extraLines.append("HiddenServiceAuthorizeClient stealth " + HSname).append('\n');
                    }
                } catch (NumberFormatException e) {
                    Log.e(TorPrefernceConstants.TAG, "error parsing hsport", e);
                } catch (Exception e) {
                    Log.e(TorPrefernceConstants.TAG, "error starting share server", e);
                }

                hidden_services.close();
            }
        } catch (SecurityException se) {
        }

        try {

            /* ---- Client Cookies ---- */
            Cursor client_cookies = mCR.query(COOKIE_CONTENT_URI, cookieProjection, ClientCookie.ENABLED + "=1", null, null);
            if (client_cookies != null) {
                try {
                    while (client_cookies.moveToNext()) {
                        String domain = client_cookies.getString(client_cookies.getColumnIndex(ClientCookie.DOMAIN));
                        String cookie = client_cookies.getString(client_cookies.getColumnIndex(ClientCookie.AUTH_COOKIE_VALUE));
                        extraLines.append("HidServAuth" + ' ' + domain + ' ' + cookie).append('\n');
                    }
                } catch (Exception e) {
                    Log.e(TorPrefernceConstants.TAG, "error starting share server", e);
                }

                client_cookies.close();
            }
        } catch (SecurityException se) {
        }

        return extraLines;
    }

    public static String flattenToAscii(String string) {
        char[] out = new char[string.length()];
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        int j = 0;
        for (int i = 0, n = string.length(); i < n; ++i) {
            char c = string.charAt(i);
            if (c <= '\u007F') out[j++] = c;
        }
        return new String(out);
    }

    //using Google DNS for now as the public DNS server
    private String writeDNSFile() throws IOException {
        File file = new File(appBinHome, "resolv.conf");

        PrintWriter bw = new PrintWriter(new FileWriter(file));
        bw.println("nameserver 8.8.8.8");
        bw.println("nameserver 8.8.4.4");
        bw.close();

        return file.getCanonicalPath();
    }


    private void handleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Log.e(TAG, intent.getAction().toString());
        }
    }


    private void setExitNode(String newExits) {
        SharedPreferences prefs = Prefs.getSharedPrefs(this.mAndroidContext);

        if (TextUtils.isEmpty(newExits)) {
            prefs.edit().remove("pref_exit_nodes").apply();

            if (conn != null) {
                try {
                    ArrayList<String> resetBuffer = new ArrayList<String>();
                    resetBuffer.add("ExitNodes");
                    resetBuffer.add("StrictNodes");
                    conn.resetConf(resetBuffer);
                    conn.setConf("DisableNetwork", "1");
                    conn.setConf("DisableNetwork", "0");

                } catch (Exception ioe) {
                    Log.e(TorPrefernceConstants.TAG, "Connection exception occured resetting exits", ioe);
                }
            }
        } else {
            prefs.edit().putString("pref_exit_nodes", newExits).apply();

            if (conn != null) {
                try {
                    File fileGeoIP = new File(appBinHome, GEOIP_ASSET_KEY);
                    File fileGeoIP6 = new File(appBinHome, GEOIP6_ASSET_KEY);

                    conn.setConf("GeoIPFile", fileGeoIP.getCanonicalPath());
                    conn.setConf("GeoIPv6File", fileGeoIP6.getCanonicalPath());

                    conn.setConf("ExitNodes", newExits);
                    conn.setConf("StrictNodes", "1");

                    conn.setConf("DisableNetwork", "1");
                    conn.setConf("DisableNetwork", "0");

                } catch (Exception ioe) {
                    Log.e(TorPrefernceConstants.TAG, "Connection exception occured resetting exits", ioe);
                }
            }
        }

    }

    public boolean hasConnectivity() {
        return mConnectivity;
    }

    // for bridge loading from the assets default bridges.txt file
    class Bridge {
        String type;
        String config;
    }

    @Nullable
    public Proxy getProxy() {
        try {
            return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(
                    "127.0.0.1", getSOCKSPort()));
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }


    private void loadBridgeDefaults() {
        if (alBridges == null) {
            alBridges = new ArrayList<Bridge>();

            try {
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(this.mAndroidContext.getResources().openRawResource(R.raw.bridges), "UTF-8"));
                String str;

                while ((str = in.readLine()) != null) {

                    StringTokenizer st = new StringTokenizer(str, " ");
                    Bridge b = new Bridge();
                    b.type = st.nextToken();

                    StringBuffer sbConfig = new StringBuffer();

                    while (st.hasMoreTokens())
                        sbConfig.append(st.nextToken()).append(' ');

                    b.config = sbConfig.toString().trim();

                    alBridges.add(b);

                }

                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //we should randomly sort alBridges so we don't have the same bridge order each time
    Random bridgeSelectRandom = new Random(System.nanoTime());

    private void getBridges(String type, StringBuffer extraLines) {

        Collections.shuffle(alBridges, bridgeSelectRandom);

        //let's just pull up to 2 bridges from the defaults at time
        int maxBridges = 2;
        int bridgeCount = 0;

        //now go through the list to find the bridges we want
        for (Bridge b : alBridges) {
            if (b.type.equals(type)) {
                extraLines.append("Bridge ");
                extraLines.append(b.type);
                extraLines.append(' ');
                extraLines.append(b.config);
                extraLines.append('\n');

                bridgeCount++;

                if (bridgeCount > maxBridges)
                    break;
            }
        }

    }

}
