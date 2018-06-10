package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ArrayAdapter;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.prng.PRNGFixes;
import com.samourai.wallet.service.BackgroundManager;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.ReceiversUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.util.WebUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

public class MainActivity2 extends Activity {

    private ProgressDialog progress = null;

    /** An array of strings to populate dropdown list */
    private static String[] account_selections = null;
    private static ArrayAdapter<String> adapter = null;

    private static boolean loadedBalanceFragment = false;

    public static final String ACTION_RESTART = "com.samourai.wallet.MainActivity2.RESTART_SERVICE";

    protected BroadcastReceiver receiver_restart = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(ACTION_RESTART.equals(intent.getAction())) {

                ReceiversUtil.getInstance(MainActivity2.this).initReceivers();

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if(AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                        stopService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                    }
                    startService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                }

            }

        }
    };

    protected BackgroundManager.Listener bgListener = new BackgroundManager.Listener()  {

        public void onBecameForeground()    {

            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
            intent.putExtra("notifTx", false);
            LocalBroadcastManager.getInstance(MainActivity2.this.getApplicationContext()).sendBroadcast(intent);

            Intent _intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
            LocalBroadcastManager.getInstance(MainActivity2.this.getApplicationContext()).sendBroadcast(_intent);

        }

        public void onBecameBackground()    {

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                    stopService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                }
            }

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            PayloadUtil.getInstance(MainActivity2.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getGUID() + AccessFactory.getInstance(MainActivity2.this).getPIN()));
                        }
                        catch(Exception e) {
                            ;
                        }

                    }
                }).start();
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        loadedBalanceFragment = false;

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            BackgroundManager.get(MainActivity2.this).addListener(bgListener);
//        }

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {

                if(itemPosition == 2 && PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.FIRST_USE_SHUFFLE, true) == true)    {

                    new AlertDialog.Builder(MainActivity2.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.first_use_shuffle)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    PrefsUtil.getInstance(MainActivity2.this).setValue(PrefsUtil.FIRST_USE_SHUFFLE, false);
                                }
                            }).show();

                }

                SamouraiWallet.getInstance().setCurrentSelectedAccount(itemPosition);
                if(account_selections.length > 1)    {
                    SamouraiWallet.getInstance().setShowTotalBalance(true);
                }
                else    {
                    SamouraiWallet.getInstance().setShowTotalBalance(false);
                }
                if(loadedBalanceFragment)    {
                    Intent intent = new Intent(MainActivity2.this, BalanceActivity.class);
                    intent.putExtra("notifTx", false);
                    intent.putExtra("fetch", false);
                    startActivity(intent);
                }

                return false;
            }
        };

        getActionBar().setListNavigationCallbacks(adapter, navigationListener);
        getActionBar().setSelectedNavigationItem(1);

        // Apply PRNG fixes for Android 4.1
        if(!AppUtil.getInstance(MainActivity2.this).isPRNG_FIXED())    {
            PRNGFixes.apply();
            AppUtil.getInstance(MainActivity2.this).setPRNG_FIXED(true);
        }

        if(!ConnectivityStatus.hasConnectivity(MainActivity2.this) &&
        !(AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists())) {

            new AlertDialog.Builder(MainActivity2.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.no_internet)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            AppUtil.getInstance(MainActivity2.this).restartApp();
                        }
                    }).show();

        }
        else  {
//            SSLVerifierThreadUtil.getInstance(MainActivity2.this).validateSSLThread();
//            APIFactory.getInstance(MainActivity2.this).validateAPIThread();
            exchangeRateThread();

            boolean isDial = false;
            String strUri = null;
            String strPCode = null;
            Bundle extras = getIntent().getExtras();
            if(extras != null && extras.containsKey("dialed"))	{
                isDial = extras.getBoolean("dialed");
            }
            if(extras != null && extras.containsKey("uri"))	{
                strUri = extras.getString("uri");
            }
            if(extras != null && extras.containsKey("pcode"))	{
                strPCode = extras.getString("pcode");
            }

            doAppInit(isDial, strUri, strPCode);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(true);

        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

        IntentFilter filter_restart = new IntentFilter(ACTION_RESTART);
        LocalBroadcastManager.getInstance(MainActivity2.this).registerReceiver(receiver_restart, filter_restart);

        doAccountSelection();

    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(MainActivity2.this).unregisterReceiver(receiver_restart);

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(false);
    }

    @Override
    protected void onDestroy() {

        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            BackgroundManager.get(this).removeListener(bgListener);
//        }

        super.onDestroy();
    }

    private void initDialog() {
        Intent intent = new Intent(MainActivity2.this, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void validatePIN(String strUri) {

        if (AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {
            return;
        }

        AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);

        Intent intent = new Intent(MainActivity2.this, PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (strUri != null) {
            intent.putExtra("uri", strUri);
            PrefsUtil.getInstance(MainActivity2.this).setValue("SCHEMED_URI", strUri);
        }
        startActivity(intent);

    }

    private void launchFromDialer(final String pin) {

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        progress = new ProgressDialog(MainActivity2.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    PayloadUtil.getInstance(MainActivity2.this).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getGUID() + pin));

                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(true);
                    TimeOutUtil.getInstance().updatePin();
                    AppUtil.getInstance(MainActivity2.this).restartApp();
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } catch (DecoderException de) {
                    de.printStackTrace();
                } finally {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }

                Looper.loop();

            }
        }).start();

    }

    private void exchangeRateThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                String response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.LBC_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataLBC(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseLBC();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_usd");
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_rur");
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_eur");
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBTCe(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBTCe();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                response = null;
                try {
                    response = WebUtil.getInstance(null).getURL(WebUtil.BFX_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBFX(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBFX();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();

            }
        }).start();
    }

    private void doAppInit(boolean isDial, final String strUri, final String strPCode) {

        if((strUri != null || strPCode != null) && AccessFactory.getInstance(MainActivity2.this).isLoggedIn())    {

            progress = new ProgressDialog(MainActivity2.this);
            progress.setCancelable(false);
            progress.setTitle(R.string.app_name);
            progress.setMessage(getText(R.string.please_wait));
            progress.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();

                    APIFactory.getInstance(MainActivity2.this).initWallet();

                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    Intent intent = new Intent(MainActivity2.this, SendActivity.class);
                    intent.putExtra("uri", strUri);
                    intent.putExtra("pcode", strPCode);
                    startActivity(intent);

                    Looper.loop();

                }
            }).start();

        }
        else if(AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists()) {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            if(AppUtil.getInstance(MainActivity2.this).isSideLoaded())    {
                doSelectNet();
            }
            else    {
                initDialog();
            }
        }
        else if(isDial && AccessFactory.getInstance(MainActivity2.this).validateHash(PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.ACCESS_HASH, ""), AccessFactory.getInstance(MainActivity2.this).getGUID(), new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getPIN()), AESUtil.DefaultPBKDF2Iterations)) {
            TimeOutUtil.getInstance().updatePin();
            launchFromDialer(AccessFactory.getInstance(MainActivity2.this).getPIN());
        }
        else if(TimeOutUtil.getInstance().isTimedOut()) {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri == null ? null : strUri);
        }
        else if(AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {

            TimeOutUtil.getInstance().updatePin();
            loadedBalanceFragment = true;

            Intent intent = new Intent(MainActivity2.this, BalanceActivity.class);
            intent.putExtra("notifTx", true);
            intent.putExtra("fetch", true);
            startActivity(intent);
        }
        else {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri == null ? null : strUri);
        }

    }

    private void doAccountSelection() {

        if(!PayloadUtil.getInstance(MainActivity2.this).walletFileExists())    {
            return;
        }

        account_selections = new String[] {
                getString(R.string.total),
                getString(R.string.account_Samourai),
                getString(R.string.account_shuffling),
        };

        adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, account_selections);

        if(account_selections.length > 1)    {
            SamouraiWallet.getInstance().setShowTotalBalance(true);
        }
        else    {
            SamouraiWallet.getInstance().setShowTotalBalance(false);
        }

    }

    private void doSelectNet()  {

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.select_network)
                .setCancelable(false)
                .setPositiveButton(R.string.MainNet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        PrefsUtil.getInstance(MainActivity2.this).removeValue(PrefsUtil.TESTNET);
                        SamouraiWallet.getInstance().setCurrentNetworkParams(MainNetParams.get());
                        initDialog();

                    }
                })
                .setNegativeButton(R.string.TestNet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        PrefsUtil.getInstance(MainActivity2.this).setValue(PrefsUtil.TESTNET, true);
                        SamouraiWallet.getInstance().setCurrentNetworkParams(TestNet3Params.get());
                        initDialog();

                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

}
