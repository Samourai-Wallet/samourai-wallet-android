package com.samourai.wallet.network;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.msopentech.thali.android.toronionproxy.NetworkManager;
import com.samourai.wallet.R;
import com.samourai.wallet.api.APIFactory;

import com.samourai.wallet.fragments.CameraFragmentBottomSheet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.network.dojo.DojoConfigureBottomSheet;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.network.dojo.DojoUtil;

import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.tor.TorService;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.ConnectionChangeReceiver;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class NetworkDashboard extends AppCompatActivity {

    private final static int SCAN_PAIRING = 2012;

    enum CONNECTION_STATUS {ENABLED, DISABLED, CONFIGURE, WAITING}

    Button torButton;
    Button dataButton;
    Button dojoBtn;
    TextView dataConnectionStatus;
    TextView torRenewBtn;
    TextView torConnectionStatus;
    TextView dojoConnectionStatus;
    ImageView dataConnectionIcon;
    ImageView torConnectionIcon;
    ImageView dojoConnectionIcon;
    LinearLayout offlineMessage;
    int activeColor, disabledColor, waiting;
    CompositeDisposable disposables = new CompositeDisposable();

    private boolean waitingForPairing = false;
    private String strPairingParams = null;
    private LinearLayout dojoLayout = null;
    private RegisterTask registerTask = null;

    private static final String TAG = "NetworkDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_dashboard);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        activeColor = ContextCompat.getColor(this, R.color.green_ui_2);
        disabledColor = ContextCompat.getColor(this, R.color.disabledRed);
        waiting = ContextCompat.getColor(this, R.color.warning_yellow);

        offlineMessage = findViewById(R.id.offline_message);

        dataButton = findViewById(R.id.networking_data_btn);
        torButton = findViewById(R.id.networking_tor_btn);
        dojoBtn = findViewById(R.id.networking_dojo_btn);
        torRenewBtn = findViewById(R.id.networking_tor_renew);

        dataConnectionStatus = findViewById(R.id.network_data_status);
        torConnectionStatus = findViewById(R.id.network_tor_status);
        dojoConnectionStatus = findViewById(R.id.network_dojo_status);

        dataConnectionIcon = findViewById(R.id.network_data_status_icon);
        torConnectionIcon = findViewById(R.id.network_tor_status_icon);
        dojoConnectionIcon = findViewById(R.id.network_dojo_status_icon);

        setDojoConnectionState(CONNECTION_STATUS.CONFIGURE);
        listenToTorStatus();

        dataButton.setOnClickListener(view -> toggleNetwork());

        torRenewBtn.setOnClickListener(view -> {
            if(TorManager.getInstance(getApplicationContext()).isConnected()){
                startService(new Intent(this,TorService.class).setAction(TorService.RENEW_IDENTITY));
            }
        });
        dojoBtn.setOnClickListener(view -> {

            Toast.makeText(this, getString(R.string.temporary_dojo_disable),Toast.LENGTH_LONG).show();
//            DojoConfigureBottomSheet dojoConfigureBottomSheet = new DojoConfigureBottomSheet();
//            dojoConfigureBottomSheet.show(getSupportFragmentManager(), dojoConfigureBottomSheet.getTag());
//
//            if(DojoUtil.getInstance(NetworkDashboard.this).getDojoParams() != null)    {
//                resetAPI();
//                setDojoConnectionState(CONNECTION_STATUS.ENABLED);
//            }
//            else    {
//                resetAPI();
//                setDojoConnectionState(CONNECTION_STATUS.DISABLED);
////                doScan();
//            }
//            stopTor();
//            resetAPI();
//            setDojoConnectionState(CONNECTION_STATUS.DISABLED);
        });

        torButton.setOnClickListener(view -> {

            if (TorManager.getInstance(getApplicationContext()).isRequired()) {
                if(DojoUtil.getInstance(NetworkDashboard.this).getDojoParams() !=null ){
                    Toast.makeText(this,R.string.cannot_disable_tor_dojo,Toast.LENGTH_LONG).show();
                    return;
                }

                stopTor();
                PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.ENABLE_TOR, false);
            }
            else {
                if (AppUtil.getInstance(NetworkDashboard.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                    stopService(new Intent(NetworkDashboard.this.getApplicationContext(), WebSocketService.class));
                }
                startTor();
                PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.ENABLE_TOR, true);
            }
        });

        setDataState();

        Disposable onlineSubscription = NetworkManager.getInstance().onlineSignal()
                .debounce(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state->{
                    new Handler().postDelayed(this::setDataState,300);
                }, error -> {
                    Log.i(TAG, "onCreate: ".concat(error.getMessage()));
                });
        disposables.add(onlineSubscription);

        dojoLayout = findViewById(R.id.network_dojo_layout);
        dojoLayout.setVisibility(View.GONE);
        if(DojoUtil.getInstance(NetworkDashboard.this).getDojoParams() != null)    {
            dojoLayout.setVisibility(View.VISIBLE);
            setDojoConnectionState(CONNECTION_STATUS.ENABLED);
        }
        else    {
            resetAPI();
            dojoLayout.setVisibility(View.GONE);
            setDojoConnectionState(CONNECTION_STATUS.DISABLED);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("params")) {
            DojoUtil.getInstance(NetworkDashboard.this).clear();
            Log.d("NetworkDashboard", "getting extras");
            strPairingParams = extras.getString("params");
            enableDojoConfigure(strPairingParams);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_PAIRING) {
            final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT).trim();
            if(DojoUtil.getInstance(NetworkDashboard.this).isValidPairingPayload(strResult))    {
                DojoUtil.getInstance(NetworkDashboard.this).clear();
                strPairingParams = strResult;
                enableDojoConfigure(strPairingParams);
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_PAIRING) {
            ;
        }
        else {
            ;
        }

    }

    private void doScan() {

        CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
        cameraFragmentBottomSheet.show(getSupportFragmentManager(),cameraFragmentBottomSheet.getTag());

        cameraFragmentBottomSheet.setQrCodeScanLisenter(code -> {
            cameraFragmentBottomSheet.dismissAllowingStateLoss();
            try {
                if (DojoUtil.getInstance(NetworkDashboard.this).isValidPairingPayload(code.trim())) {
                    DojoUtil.getInstance(NetworkDashboard.this).clear();
                    strPairingParams = code.trim();
                    enableDojoConfigure(strPairingParams);
                }
                else {
                    ;
                }
            } catch (Exception e) {
            }
        });
    }

    private void toggleNetwork() {
        boolean pref = PrefsUtil.getInstance(getApplicationContext()).getValue(PrefsUtil.OFFLINE, false);
        PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.OFFLINE, !pref);
        this.setDataState();
    }

    private void setDataState() {
        if (ConnectivityStatus.hasConnectivity(getApplicationContext())) {
            setDataConnectionState(CONNECTION_STATUS.ENABLED);
            if (TorManager.getInstance(getApplicationContext()).isRequired() && !TorManager.getInstance(getApplicationContext()).isConnected()) {
                startTor();
             }
        } else {
            setDataConnectionState(CONNECTION_STATUS.DISABLED);
            if (TorManager.getInstance(getApplicationContext()).isConnected()) {
                stopTor();
            }
            if(!ConnectionChangeReceiver.isConnected(getApplicationContext())){
               Snackbar.make(torButton.getRootView(),"No data connection",Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }

    private void listenToTorStatus() {

        Disposable disposable = TorManager.getInstance(getApplicationContext())
                .torStatus
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( this::setTorConnectionState);

        disposables.add(disposable);

        NetworkDashboard.this.runOnUiThread(new Runnable() {
            public void run() {
                    setTorConnectionState(TorManager.getInstance(getApplicationContext()).isConnected() ? TorManager.CONNECTION_STATES.CONNECTED : TorManager.CONNECTION_STATES.DISCONNECTED);
            }
        });

    }

    private void setDataConnectionState(CONNECTION_STATUS enabled) {

        NetworkDashboard.this.runOnUiThread(new Runnable() {
            public void run() {
                if (enabled == CONNECTION_STATUS.ENABLED) {
                    showOfflineMessage(false);
                    dataButton.setText("Disable");
                    dataConnectionIcon.setColorFilter(activeColor);
                    dataConnectionStatus.setText("Enabled");
                } else {
                    dataButton.setText("Enable");
                    showOfflineMessage(true);
                    dataConnectionIcon.setColorFilter(disabledColor);
                    dataConnectionStatus.setText("Disabled");
                }
            }
        });

    }

    private void setDojoConnectionState(CONNECTION_STATUS enabled) {

        NetworkDashboard.this.runOnUiThread(new Runnable() {
            public void run() {
                if (enabled == CONNECTION_STATUS.ENABLED) {
                    dojoBtn.setText("Disable");
                    dojoConnectionIcon.setColorFilter(activeColor);
                    dojoConnectionStatus.setText("Enabled");
                } else if (enabled == CONNECTION_STATUS.CONFIGURE) {
                    dojoBtn.setText("configure");
                    dojoConnectionIcon.setColorFilter(waiting);
                    dojoConnectionStatus.setText("Not configured");
                } else {
                    dojoBtn.setText("Enable");
                    dojoConnectionIcon.setColorFilter(disabledColor);
                    dojoConnectionStatus.setText("Disabled");
                }
            }
        });

    }

    private void setTorConnectionState(TorManager.CONNECTION_STATES enabled) {
        Log.i(TAG, "setTorConnectionState: ".concat(String.valueOf(enabled)));
        NetworkDashboard.this.runOnUiThread(() -> {
            if (enabled == TorManager.CONNECTION_STATES.CONNECTED) {
                torButton.setText("Disable");
                torButton.setEnabled(true);
                torConnectionIcon.setColorFilter(activeColor);
                torConnectionStatus.setText("Enabled");
                torRenewBtn.setVisibility(View.VISIBLE);
                if(waitingForPairing)    {
                    waitingForPairing = false;

                    if (strPairingParams != null) {
                        DojoUtil.getInstance(NetworkDashboard.this).setDojoParams(strPairingParams);
                        Toast.makeText(NetworkDashboard.this, "Tor enabled for Dojo pairing:" + DojoUtil.getInstance(NetworkDashboard.this).getDojoParams(), Toast.LENGTH_SHORT).show();
                        initDojo();
                    }

                }

            }
            else if (enabled == TorManager.CONNECTION_STATES.CONNECTING) {
                torRenewBtn.setVisibility(View.INVISIBLE);
                torButton.setText("loading...");
                torButton.setEnabled(false);
                torConnectionIcon.setColorFilter(waiting);
                torConnectionStatus.setText("Tor initializing");
            }
            else  {
                torRenewBtn.setVisibility(View.INVISIBLE);
                torButton.setText("Enable");
                torButton.setEnabled(true);
                torConnectionIcon.setColorFilter(disabledColor);
                torConnectionStatus.setText("Disabled");

                /*
                if (strPairingParams != null) {
                    DojoUtil.getInstance(NetworkDashboard.this).removeDojoParams();
                }
                */

            }
        });

    }

    private void showOfflineMessage(boolean show) {
        TransitionManager.beginDelayedTransition((ViewGroup) offlineMessage.getRootView());
        offlineMessage.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void startTor() {
        if (ConnectivityStatus.hasConnectivity(getApplicationContext())) {

            Intent startIntent = new Intent(getApplicationContext(), TorService.class);
            startIntent.setAction(TorService.START_SERVICE);
            startService(startIntent);

        } else {
            Snackbar.make(torButton.getRootView(), R.string.in_offline_mode, Snackbar.LENGTH_LONG)
                    .setAction("Turn off", view -> toggleNetwork())
                    .show();
        }
    }

    private void stopTor() {
        Intent startIntent = new Intent(getApplicationContext(), TorService.class);
        startIntent.setAction(TorService.STOP_SERVICE);
        startService(startIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return true;
    }

    private void enableDojoConfigure(String params)  {

        Log.d("NetworkDashboard", "enableDojoConfigure()");

        dojoLayout.setVisibility(View.VISIBLE);
        waitingForPairing = true;
        startTor();
        PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.ENABLE_TOR, true);
    }

    private void initDojo() {

        Log.d("NetworkDashboard", "initDojo()");

        if (registerTask == null || registerTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            registerTask = new RegisterTask();

            Log.d("NetworkDashboard", "registerTask launched");

            registerTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }

    }

    private class RegisterTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            Log.d("NetworkDashboard", "registerTask: query Dojo");
            Log.d("NetworkDashboard", WebUtil.SAMOURAI_API2_TESTNET_TOR);

            resetAPI();

            PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.IS_RESTORE, false);

            APIFactory.getInstance(NetworkDashboard.this).initWallet();

            setDojoConnectionState(CONNECTION_STATUS.ENABLED);

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            ;
        }

        @Override
        protected void onPreExecute() {
            ;
        }

    }

    private void resetAPI() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                //        PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUB44REG, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUB49REG, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUB84REG, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUBPREREG, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUBPOSTREG, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUB44LOCK, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUB49LOCK, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUB84LOCK, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUBPRELOCK, false);
                PrefsUtil.getInstance(NetworkDashboard.this).setValue(PrefsUtil.XPUBPOSTLOCK, false);

                DojoUtil.getInstance(NetworkDashboard.this).clear();
                APIFactory.getInstance(NetworkDashboard.this).setAccessToken(null);
                APIFactory.getInstance(NetworkDashboard.this).setAppToken(null);
                APIFactory.getInstance(NetworkDashboard.this).getToken(true);

                Looper.loop();

            }
        }).start();

    }

}
