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
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
//import android.util.Log;

import org.bitcoinj.crypto.MnemonicException;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.prng.PRNGFixes;
import com.samourai.wallet.service.BroadcastReceiverService;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.util.WebUtil;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

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
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        loadedBalanceFragment = false;

//        doAccountSelection();

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

        if(!ConnectivityStatus.hasConnectivity(MainActivity2.this))  {

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

            if(PrefsUtil.getInstance(MainActivity2.this).getValue("popup_" + getResources().getString(R.string.version_name), false) == true)	{
                doAppInit(isDial, strUri, strPCode);
            }
            else	{

                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity2.this);

                WebView wv = new WebView(MainActivity2.this);
                wv.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });
                wv.loadUrl("http://samouraiwallet.com/changelog/alpha3.html");
                alert.setView(wv);
                alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        PrefsUtil.getInstance(MainActivity2.this).setValue("popup_" + getResources().getString(R.string.version_name), true);
                        AppUtil.getInstance(MainActivity2.this).restartApp();
                    }
                });
                if(!isFinishing())    {
                    alert.show();
                }
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(true);

        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

        if(TimeOutUtil.getInstance().isTimedOut()) {
            if(AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists()) {
                AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
                initDialog();
            }
            else {
                AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
                validatePIN(null);
            }
        }
        else {
            TimeOutUtil.getInstance().updatePin();

//            SSLVerifierThreadUtil.getInstance(MainActivity2.this).validateSSLThread();
//            APIFactory.getInstance(MainActivity2.this).validateAPIThread();
        }

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

        super.onDestroy();
    }

    private void initDialog()	{

        AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.please_select)
                .setCancelable(false)
                .setPositiveButton(R.string.create_wallet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText passphrase = new EditText(MainActivity2.this);
                        passphrase.setSingleLine(true);
                        passphrase.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity2.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.bip39_safe)
                                .setView(passphrase)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final String passphrase39 = passphrase.getText().toString();

                                        if (passphrase39 != null && passphrase39.length() > 0) {

                                            Intent intent = new Intent(MainActivity2.this, PinEntryActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.putExtra("create", true);
                                            intent.putExtra("passphrase", passphrase39 == null ? "" : passphrase39);
                                            startActivity(intent);

                                        } else {

                                            Toast.makeText(MainActivity2.this, R.string.bip39_must, Toast.LENGTH_SHORT).show();
                                            AppUtil.getInstance(MainActivity2.this).restartApp();

                                        }

                                    }

                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        Toast.makeText(MainActivity2.this, R.string.bip39_must, Toast.LENGTH_SHORT).show();
                                        AppUtil.getInstance(MainActivity2.this).restartApp();

                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
                        }

                    }
                })
                .setNegativeButton(R.string.restore_wallet, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity2.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.restore_wallet)
                                .setCancelable(false)
                                .setPositiveButton(R.string.import_backup, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final EditText passphrase = new EditText(MainActivity2.this);
                                        passphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                        passphrase.setHint(R.string.passphrase);

                                        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity2.this)
                                                .setTitle(R.string.app_name)
                                                .setView(passphrase)
                                                .setMessage(R.string.restore_wallet_from_backup)
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        final String pw = passphrase.getText().toString();
                                                        if (pw == null || pw.length() < 1) {
                                                            Toast.makeText(MainActivity2.this, R.string.invalid_passphrase, Toast.LENGTH_SHORT).show();
                                                            AppUtil.getInstance(MainActivity2.this).restartApp();
                                                        }

                                                        String directory = Environment.DIRECTORY_DOCUMENTS;
                                                        File dir = Environment.getExternalStoragePublicDirectory(directory + "/samourai");
                                                        File file = new File(dir, "samourai.txt");
                                                        String encrypted = null;
                                                        if(file.exists())    {

                                                            StringBuilder sb = new StringBuilder();
                                                            try {
                                                                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
                                                                String str = null;
                                                                while((str = in.readLine()) != null) {
                                                                    sb.append(str);
                                                                }

                                                                in.close();
                                                            }
                                                            catch(FileNotFoundException fnfe) {

                                                            }
                                                            catch(IOException ioe) {

                                                            }

                                                            encrypted = sb.toString();

                                                        }

                                                        final EditText edBackup = new EditText(MainActivity2.this);
                                                        edBackup.setSingleLine(false);
                                                        edBackup.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                                                        edBackup.setLines(10);
                                                        edBackup.setHint(R.string.encrypted_backup);
                                                        edBackup.setGravity(Gravity.START);
                                                        TextWatcher textWatcher = new TextWatcher() {

                                                            public void afterTextChanged(Editable s) {
                                                                edBackup.setSelection(0);
                                                            }
                                                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                                ;
                                                            }
                                                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                                ;
                                                            }
                                                        };
                                                        edBackup.addTextChangedListener(textWatcher);
                                                        String message = null;
                                                        if(encrypted != null)   {
                                                            edBackup.setText(encrypted);
                                                            message = getText(R.string.restore_wallet_from_existing_backup).toString();
                                                        }
                                                        else    {
                                                            message = getText(R.string.restore_wallet_from_backup).toString();
                                                        }

                                                        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity2.this)
                                                                .setTitle(R.string.app_name)
                                                                .setView(edBackup)
                                                                .setMessage(message)
                                                                .setCancelable(false)
                                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                                        String encrypted = edBackup.getText().toString();
                                                                        if (encrypted == null || encrypted.length() < 1) {
                                                                            Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                            AppUtil.getInstance(MainActivity2.this).restartApp();
                                                                        }

                                                                        String decrypted = null;
                                                                        try {
                                                                            decrypted = AESUtil.decrypt(encrypted, new CharSequenceX(pw), AESUtil.DefaultPBKDF2Iterations);
                                                                        } catch (Exception e) {
                                                                            Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                        } finally {
                                                                            if (decrypted == null || decrypted.length() < 1) {
                                                                                Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                                AppUtil.getInstance(MainActivity2.this).restartApp();
                                                                            }
                                                                        }

                                                                        final String decryptedPayload = decrypted;
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

                                                                                    JSONObject json = new JSONObject(decryptedPayload);
                                                                                    HD_Wallet hdw = PayloadUtil.getInstance(MainActivity2.this).restoreWalletfromJSON(json);
                                                                                    HD_WalletFactory.getInstance(MainActivity2.this).set(hdw);
                                                                                    String guid = AccessFactory.getInstance(MainActivity2.this).createGUID();
                                                                                    String hash = AccessFactory.getInstance(MainActivity2.this).getHash(guid, new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getPIN()), AESUtil.DefaultPBKDF2Iterations);
                                                                                    PrefsUtil.getInstance(MainActivity2.this).setValue(PrefsUtil.ACCESS_HASH, hash);
                                                                                    PrefsUtil.getInstance(MainActivity2.this).setValue(PrefsUtil.ACCESS_HASH2, hash);
                                                                                    PayloadUtil.getInstance(MainActivity2.this).saveWalletToJSON(new CharSequenceX(guid + AccessFactory.getInstance().getPIN()));

                                                                                }
                                                                                catch(MnemonicException.MnemonicLengthException mle) {
                                                                                    mle.printStackTrace();
                                                                                    Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                                }
                                                                                catch(DecoderException de) {
                                                                                    de.printStackTrace();
                                                                                    Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                                }
                                                                                catch(JSONException je) {
                                                                                    je.printStackTrace();
                                                                                    Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                                }
                                                                                catch(IOException ioe) {
                                                                                    ioe.printStackTrace();
                                                                                    Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                                }
                                                                                catch(java.lang.NullPointerException npe) {
                                                                                    npe.printStackTrace();
                                                                                    Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                                }
                                                                                catch(DecryptionException de) {
                                                                                    de.printStackTrace();
                                                                                    Toast.makeText(MainActivity2.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                                }
                                                                                finally {
                                                                                    if (progress != null && progress.isShowing()) {
                                                                                        progress.dismiss();
                                                                                        progress = null;
                                                                                    }
                                                                                    AppUtil.getInstance(MainActivity2.this).restartApp();
                                                                                }

                                                                                Looper.loop();

                                                                            }
                                                                        }).start();

                                                                    }
                                                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                                        AppUtil.getInstance(MainActivity2.this).restartApp();

                                                                    }
                                                                });
                                                        if(!isFinishing())    {
                                                            dlg.show();
                                                        }

                                                    }
                                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        AppUtil.getInstance(MainActivity2.this).restartApp();

                                                    }
                                                });
                                        if(!isFinishing())    {
                                            dlg.show();
                                        }

                                    }
                                })
                                .setNegativeButton(R.string.import_mnemonic, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final EditText mnemonic = new EditText(MainActivity2.this);
                                        mnemonic.setHint(R.string.mnemonic_hex);
                                        mnemonic.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                        final EditText passphrase = new EditText(MainActivity2.this);
                                        passphrase.setHint(R.string.bip39_passphrase);
                                        passphrase.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                        passphrase.setSingleLine(true);

                                        LinearLayout restoreLayout = new LinearLayout(MainActivity2.this);
                                        restoreLayout.setOrientation(LinearLayout.VERTICAL);
                                        restoreLayout.addView(mnemonic);
                                        restoreLayout.addView(passphrase);

                                        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity2.this)
                                                .setTitle(R.string.app_name)
                                                .setMessage(R.string.bip39_safe)
                                                .setView(restoreLayout)
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        final String seed39 = mnemonic.getText().toString();
                                                        final String passphrase39 = passphrase.getText().toString();

                                                        if (seed39 != null && seed39.length() > 0) {
                                                            Intent intent = new Intent(MainActivity2.this, PinEntryActivity.class);
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            intent.putExtra("create", true);
                                                            intent.putExtra("seed", seed39);
                                                            intent.putExtra("passphrase", passphrase39 == null ? "" : passphrase39);
                                                            startActivity(intent);
                                                        } else {

                                                            AppUtil.getInstance(MainActivity2.this).restartApp();

                                                        }

                                                    }
                                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        AppUtil.getInstance(MainActivity2.this).restartApp();

                                                    }
                                                });
                                        if(!isFinishing())    {
                                            dlg.show();
                                        }

                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
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
                catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                }
                catch (DecoderException de) {
                    de.printStackTrace();
                }
                finally {
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
                    response = WebUtil.getInstance(null).getURL(WebUtil.BTCe_EXCHANGE_URL + "btc_eur");
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
                    response = WebUtil.getInstance(null).getURL(WebUtil.AVG_EXCHANGE_URL);
                    ExchangeRateFactory.getInstance(MainActivity2.this).setDataBTCAvg(response);
                    ExchangeRateFactory.getInstance(MainActivity2.this).parseBTCAvg();
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
            initDialog();
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

    public void doAccountSelection() {

        if(!PayloadUtil.getInstance(MainActivity2.this).walletFileExists())    {
            return;
        }

        /*
        if(AddressFactory.getInstance(MainActivity2.this).getHighestTxReceiveIdx(SamouraiWallet.MIXING_ACCOUNT) < 1)    {
            account_selections = new String[] {
                    getString(R.string.account_samourai),
            };
        }
        else    {
            account_selections = new String[] {
                    getString(R.string.total),
                    getString(R.string.account_samourai),
                    getString(R.string.account_shuffling),
            };
        }
        */

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

}
