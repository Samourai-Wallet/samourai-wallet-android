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
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.initialization.ExchangeRateThread;
import com.samourai.wallet.initialization.NewWalletDialog;
import com.samourai.wallet.initialization.RestoreWalletDialog;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.prng.PRNGFixes;
import com.samourai.wallet.service.BroadcastReceiverService;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;


public class MainActivity2 extends Activity {
    private static final String TAG = LogUtil.getTag();

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

                if(AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(BroadcastReceiverService.class)) {
                    stopService(new Intent(MainActivity2.this.getApplicationContext(), BroadcastReceiverService.class));
                }
                startService(new Intent(MainActivity2.this.getApplicationContext(), BroadcastReceiverService.class));

                if(AppUtil.getInstance(MainActivity2.this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                    stopService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));
                }
                startService(new Intent(MainActivity2.this.getApplicationContext(), WebSocketService.class));

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LogUtil.DEBUG) Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        loadedBalanceFragment = false;

        if (LogUtil.DEBUG) Log.d(TAG, "setup action bar");
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

        //check if no internetz
        if(!ConnectivityStatus.hasConnectivity(MainActivity2.this)
                && !(AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1
                || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists())) {

            if (LogUtil.DEBUG) Log.d(TAG, "no internet connection");
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
        //there is internetz
        else  {
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
    }

    @Override
    protected void onDestroy() {

        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

        super.onDestroy();
    }

    private void initDialog()	{
        if (LogUtil.DEBUG) Log.d(TAG, "initDialog");
        AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);

        //Chose create new or restore old wallet
        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.please_select)
                .setCancelable(false)
                .setPositiveButton(R.string.create_wallet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (LogUtil.DEBUG) Log.d(TAG, "createWallet");

                        if(!isFinishing())    {
                            NewWalletDialog.makeDialog(MainActivity2.this).show();
                        }
                    }
                })
                .setNegativeButton(R.string.restore_wallet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (LogUtil.DEBUG) Log.d(TAG, "restoreWallet");

                        if(!isFinishing())    {
                            RestoreWalletDialog.makeDialog(MainActivity2.this).show();
                        }
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void validatePIN(String strUri)	{

        if(AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut())	{
            return;
        }

        AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);

        Intent intent = new Intent(MainActivity2.this, PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if(strUri != null)    {
            intent.putExtra("uri", strUri);
            PrefsUtil.getInstance(MainActivity2.this).setValue("SCHEMED_URI", strUri);
        }
        startActivity(intent);

    }

    private void launchFromDialer(final String pin)	{

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
                }
                catch (MnemonicException.MnemonicLengthException | DecoderException mle) {
                    mle.printStackTrace();
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
        ExchangeRateThread exchangeThread = new ExchangeRateThread(MainActivity2.this);
        exchangeThread.startExchangeRateCheck();
    }

    private void doAppInit(boolean isDial, final String strUri, final String strPCode) {
        if (LogUtil.DEBUG) Log.d(TAG, "doAppInit -"
                + " isDial: " + isDial
                + " strUri: " + strUri
                + " strPCode: " + strPCode);

        //IF 1 -
        if((strUri != null || strPCode != null) && AccessFactory.getInstance(MainActivity2.this).isLoggedIn()) {
            if (LogUtil.DEBUG) Log.d(TAG, "IF 1");
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
        //IF 2 -
        else if(AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists()) {
            if (LogUtil.DEBUG) Log.d(TAG, "IF 2");

            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            if(AppUtil.getInstance(MainActivity2.this).isSideLoaded())    {
                doSelectNet();
            }
            else    {
                initDialog();
            }
        }
        //IF 3 -
        else if(isDial && AccessFactory.getInstance(MainActivity2.this).validateHash(PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.ACCESS_HASH, ""), AccessFactory.getInstance(MainActivity2.this).getGUID(), new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getPIN()), AESUtil.DefaultPBKDF2Iterations)) {
            if (LogUtil.DEBUG) Log.d(TAG, "IF 3");
            TimeOutUtil.getInstance().updatePin();
            launchFromDialer(AccessFactory.getInstance(MainActivity2.this).getPIN());
        }
        //IF 4 -
        else if(TimeOutUtil.getInstance().isTimedOut()) {
            if (LogUtil.DEBUG) Log.d(TAG, "IF 4");
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri);
        }
        //IF 5 -
        else if(AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {
            if (LogUtil.DEBUG) Log.d(TAG, "IF 5");
            TimeOutUtil.getInstance().updatePin();
            loadedBalanceFragment = true;

            Intent intent = new Intent(MainActivity2.this, BalanceActivity.class);
            intent.putExtra("notifTx", true);
            intent.putExtra("fetch", true);
            startActivity(intent);
        }
        //ELSE
        else {
            if (LogUtil.DEBUG) Log.d(TAG, "ELSE");
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri);
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

        adapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, account_selections);

        if(account_selections.length > 1)    {
            SamouraiWallet.getInstance().setShowTotalBalance(true);
        }
        else    {
            SamouraiWallet.getInstance().setShowTotalBalance(false);
        }

    }

    private void doSelectNet()  {
        if (LogUtil.DEBUG) Log.d(TAG, "doSelectNet");
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
