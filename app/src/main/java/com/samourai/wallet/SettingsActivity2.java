package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.MnemonicException;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import com.samourai.wallet.JSONRPC.JSONRPC;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.cahoots.util.CahootsUtil;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.ReceiversUtil;
import com.samourai.wallet.util.SIMUtil;
import com.samourai.wallet.util.SendAddressUtil;

import com.yanzhenjie.zbar.Symbol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;

public class SettingsActivity2 extends PreferenceActivity	{

    private ProgressDialog progress = null;
    private boolean steathActivating = false;

    private final static int SCAN_HEX_TX = 2011;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("branch"))	{
            String strBranch = extras.getString("branch");
            if(strBranch.equals("txs"))   {
                addPreferencesFromResource(R.xml.settings_txs);

                final CheckBoxPreference cbPref0 = (CheckBoxPreference) findPreference("segwit");
                cbPref0.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref0.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_SEGWIT, false);
                        }
                        else    {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_SEGWIT, true);
                        }

                        return true;
                    }
                });

                final CheckBoxPreference cbPref15 = (CheckBoxPreference) findPreference("likeTypedChange");
                cbPref15.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref15.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, false);
                        }
                        else    {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true);
                        }

                        return true;
                    }
                });

                final CheckBoxPreference cbPref9 = (CheckBoxPreference) findPreference("rbf");
                cbPref9.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref9.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.RBF_OPT_IN, false);
                        }
                        else    {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.RBF_OPT_IN, true);
                        }

                        return true;
                    }
                });

                Preference trustedNodePref = (Preference) findPreference("trustedNode");
                trustedNodePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getTrustedNode();
                        return true;
                    }
                });

                final CheckBoxPreference cbPref8 = (CheckBoxPreference) findPreference("useTrustedNode");
                if(TrustedNodeUtil.getInstance().isSet() && TrustedNodeUtil.getInstance().isValidated())    {
                    cbPref8.setEnabled(true);
                }
                else    {
                    cbPref8.setEnabled(false);
                }
                cbPref8.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref8.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_TRUSTED_NODE, false);
                        }
                        else if(TrustedNodeUtil.getInstance().isSet() && TrustedNodeUtil.getInstance().isValidated())    {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_TRUSTED_NODE, true);
                        }
                        else    {
                            Toast.makeText(SettingsActivity2.this, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                            cbPref8.setEnabled(false);
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_TRUSTED_NODE, false);
                        }

                        return true;
                    }
                });

                Preference feeproviderPref = (Preference) findPreference("feeProvider");
                feeproviderPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getFeeProvider();
                        return true;
                    }
                });

                final CheckBoxPreference cbPref10 = (CheckBoxPreference) findPreference("broadcastTx");
                cbPref10.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref10.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.BROADCAST_TX, false);
                        }
                        else    {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.BROADCAST_TX, true);
                        }

                        return true;
                    }
                });

                Preference broadcastHexPref = (Preference) findPreference("broadcastHex");
                broadcastHexPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doBroadcastHex();
                        return true;
                    }
                });

                Preference cahootsPref = (Preference) findPreference("cahoots");
                cahootsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doCahoots();
                        return true;
                    }
                });

                Preference psbtPref = (Preference) findPreference("psbt");
                psbtPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doPSBT();
                        return true;
                    }
                });

                Preference whirlpoolGUIPref = (Preference) findPreference("whirlpool_gui");
                whirlpoolGUIPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doWhirlpoolGUIPairing();
                        return true;
                    }
                });

            }
            else if(strBranch.equals("stealth"))   {
                addPreferencesFromResource(R.xml.settings_stealth);

                final CheckBoxPreference cbPref1 = (CheckBoxPreference) findPreference("stealthDisplay");
                cbPref1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        final ComponentName component = new ComponentName(getApplicationContext().getPackageName(), "com.samourai.wallet.Launcher");

                        if (component != null) {

                            if (cbPref1.isChecked()) {
                                getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ICON_HIDDEN, false);

                                ReceiversUtil.getInstance(SettingsActivity2.this).initReceivers();

                                AppUtil.getInstance(SettingsActivity2.this).restartApp();
                            }
                            else {

                                String strMsg = SettingsActivity2.this.getString(R.string.options_stealth_display2);

                                new AlertDialog.Builder(SettingsActivity2.this)
                                        .setIcon(R.drawable.ic_launcher).setTitle(R.string.options_stealth_display)
                                        .setMessage(strMsg)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            //@Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                steathActivating = true;

                                                getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ICON_HIDDEN, true);

                                                ReceiversUtil.getInstance(SettingsActivity2.this).initReceivers();

                                                try {
                                                    PayloadUtil.getInstance(SettingsActivity2.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(SettingsActivity2.this).getGUID() + AccessFactory.getInstance(SettingsActivity2.this).getPIN()));
                                                } catch (Exception e) {
                                                    ;
                                                }

                                            }
                                        })
                                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                            //@Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                cbPref1.setChecked(false);
                                            }
                                        }).show();

                            }

                        }

                        return true;
                    }
                });

                final Preference remotePinPref = (Preference) findPreference("remote_pin");
                remotePinPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        if(steathActivating)    {
                            Toast.makeText(SettingsActivity2.this, R.string.alternative_pin_wait, Toast.LENGTH_SHORT).show();
                        }
                        else    {

                            new AlertDialog.Builder(SettingsActivity2.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.alternative_pin_create)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            final EditText pin = new EditText(SettingsActivity2.this);
                                            pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                                            new AlertDialog.Builder(SettingsActivity2.this)
                                                    .setTitle(R.string.app_name)
                                                    .setMessage(R.string.pin_5_8)
                                                    .setView(pin)
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int whichButton) {

                                                            final String _pin = pin.getText().toString();
                                                            if (_pin != null && _pin.length() >= AccessFactory.MIN_PIN_LENGTH && _pin.length() <= AccessFactory.MAX_PIN_LENGTH) {

                                                                final EditText pin2 = new EditText(SettingsActivity2.this);
                                                                pin2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                                                                new AlertDialog.Builder(SettingsActivity2.this)
                                                                        .setTitle(R.string.app_name)
                                                                        .setMessage(R.string.pin_5_8_confirm)
                                                                        .setView(pin2)
                                                                        .setCancelable(false)
                                                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                                                String _pin2 = pin2.getText().toString();
                                                                                if (_pin2 != null && _pin2.equals(_pin)) {

                                                                                    String hash = AccessFactory.getInstance(SettingsActivity2.this).getHash(AccessFactory.getInstance(SettingsActivity2.this).getGUID(), new CharSequenceX(_pin), AESUtil.DefaultPBKDF2Iterations);
                                                                                    PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ACCESS_HASH2, hash);
                                                                                    AccessFactory.getInstance(SettingsActivity2.this).setPIN2(_pin2);

                                                                                    try {
                                                                                        PayloadUtil.getInstance(SettingsActivity2.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(SettingsActivity2.this).getGUID() + AccessFactory.getInstance(SettingsActivity2.this).getPIN()));
                                                                                    }
                                                                                    catch (JSONException je) {
                                                                                        je.printStackTrace();
                                                                                    }
                                                                                    catch (IOException ioe) {
                                                                                        ioe.printStackTrace();
                                                                                    }
                                                                                    catch (MnemonicException.MnemonicLengthException mle) {
                                                                                        mle.printStackTrace();
                                                                                    }
                                                                                    catch (DecryptionException de) {
                                                                                        de.printStackTrace();
                                                                                    }
                                                                                    finally {
                                                                                        Toast.makeText(SettingsActivity2.this.getApplicationContext(), R.string.success_change_pin, Toast.LENGTH_SHORT).show();
                                                                                    }

                                                                                }

                                                                            }
                                                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                                        ;
                                                                    }
                                                                }).show();

                                                            } else {
                                                                AccessFactory.getInstance(SettingsActivity2.this).setPIN2("");
                                                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ACCESS_HASH2, PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.ACCESS_HASH, ""));
                                                                Toast.makeText(SettingsActivity2.this, R.string.alternative_pin_deleted, Toast.LENGTH_SHORT).show();
                                                            }

                                                        }
                                                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    ;
                                                }
                                            }).show();

                                        }
                                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            }).show();

                        }

                        return true;
                    }
                });

            }
            else if(strBranch.equals("remote"))   {
                addPreferencesFromResource(R.xml.settings_remote);

                final CheckBoxPreference cbPref2 = (CheckBoxPreference) findPreference("stealthRemote");
                final CheckBoxPreference cbPref3 = (CheckBoxPreference) findPreference("trustedLock");
                final CheckBoxPreference cbPref4 = (CheckBoxPreference) findPreference("sim_switch");
                final EditTextPreference textPref1 = (EditTextPreference) findPreference("alertSMSNo");

                cbPref2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref2.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ACCEPT_REMOTE, false);
                            ReceiversUtil.getInstance(SettingsActivity2.this).initReceivers();
                        }
                        else {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ACCEPT_REMOTE, true);
                            ReceiversUtil.getInstance(SettingsActivity2.this).initReceivers();
                        }

                        return true;
                    }
                });

                cbPref3.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref3.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.TRUSTED_LOCK, false);
                        }
                        else {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.TRUSTED_LOCK, true);
                        }

                        return true;
                    }
                });

                cbPref4.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref4.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.CHECK_SIM, false);
                        }
                        else {
                            SIMUtil.getInstance(SettingsActivity2.this).setStoredSIM();
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.CHECK_SIM, true);
                        }

                        return true;
                    }
                });

                textPref1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        String telno = newValue.toString();
                        if (telno != null && telno.length() > 0) {
                            String s = telno.replaceAll("[^\\+0-9]", "");
                            if (s.matches("^\\+[0-9]+$")) {
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ALERT_MOBILE_NO, s);
                                cbPref3.setEnabled(true);
                                cbPref4.setEnabled(true);
                            }
                            else {
                                Toast.makeText(SettingsActivity2.this, "Use international dialing format. Ex.:'+447385555555'", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            cbPref3.setEnabled(false);
                            cbPref3.setChecked(false);
                            cbPref4.setEnabled(false);
                            cbPref4.setChecked(false);
                        }

                        return true;
                    }
                });

            }
            else if(strBranch.equals("wallet"))   {
                addPreferencesFromResource(R.xml.settings_wallet);

                Preference mnemonicPref = (Preference) findPreference("mnemonic");
                mnemonicPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getHDSeed(true);
                        return true;
                    }
                });

                Preference xpubPref = (Preference) findPreference("xpub");
                xpubPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getXPUB(44);
                        return true;
                    }
                });

                Preference ypubPref = (Preference) findPreference("ypub");
                ypubPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getXPUB(49);
                        return true;
                    }
                });

                Preference zpubPref = (Preference) findPreference("zpub");
                zpubPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getXPUB(84);
                        return true;
                    }
                });

                Preference wipePref = (Preference) findPreference("wipe");
                wipePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        new AlertDialog.Builder(SettingsActivity2.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.sure_to_erase)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final ProgressDialog progress = new ProgressDialog(SettingsActivity2.this);
                                        progress.setTitle(R.string.app_name);
                                        progress.setMessage(SettingsActivity2.this.getResources().getString(R.string.securely_wiping_wait));
                                        progress.setCancelable(false);
                                        progress.show();

                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {

                                                Looper.prepare();

                                                AppUtil.getInstance(SettingsActivity2.this).wipeApp();

                                                Toast.makeText(SettingsActivity2.this, R.string.wallet_erased, Toast.LENGTH_SHORT).show();
                                                AppUtil.getInstance(SettingsActivity2.this).restartApp();

                                                if (progress != null && progress.isShowing()) {
                                                    progress.dismiss();
                                                }

                                                Looper.loop();

                                            }
                                        }).start();


                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }
                        }).show();

                        return true;
                    }
                });

                final CheckBoxPreference cbPref5 = (CheckBoxPreference) findPreference("scramblePin");
                cbPref5.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref5.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.SCRAMBLE_PIN, false);
                        }
                        else {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.SCRAMBLE_PIN, true);
                        }

                        return true;
                    }
                });

                final CheckBoxPreference cbPref11 = (CheckBoxPreference) findPreference("haptic");
                cbPref11.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref11.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.HAPTIC_PIN, false);
                        }
                        else {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.HAPTIC_PIN, true);
                        }

                        return true;
                    }
                });

                Preference changePinPref = (Preference) findPreference("change_pin");
                changePinPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        new AlertDialog.Builder(SettingsActivity2.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.confirm_change_pin)
                                .setCancelable(false)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final EditText pin = new EditText(SettingsActivity2.this);
                                        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                                        new AlertDialog.Builder(SettingsActivity2.this)
                                                .setTitle(R.string.app_name)
                                                .setMessage(R.string.pin_enter)
                                                .setView(pin)
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        String _pin = pin.getText().toString();
                                                        if(_pin != null && _pin.length() >= AccessFactory.MIN_PIN_LENGTH && _pin.length() <= AccessFactory.MAX_PIN_LENGTH)    {

                                                            String hash = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.ACCESS_HASH, "");
                                                            if(AccessFactory.getInstance(SettingsActivity2.this).validateHash(hash, AccessFactory.getInstance(SettingsActivity2.this).getGUID(), new CharSequenceX(_pin), AESUtil.DefaultPBKDF2Iterations)) {

                                                                final EditText pin = new EditText(SettingsActivity2.this);
                                                                pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                                                                new AlertDialog.Builder(SettingsActivity2.this)
                                                                        .setTitle(R.string.app_name)
                                                                        .setMessage(R.string.pin_5_8)
                                                                        .setView(pin)
                                                                        .setCancelable(false)
                                                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                                                final String _pin = pin.getText().toString();
                                                                                if(_pin != null && _pin.length() >= AccessFactory.MIN_PIN_LENGTH && _pin.length() <= AccessFactory.MAX_PIN_LENGTH)    {

                                                                                    final EditText pin2 = new EditText(SettingsActivity2.this);
                                                                                    pin2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                                                                                    new AlertDialog.Builder(SettingsActivity2.this)
                                                                                            .setTitle(R.string.app_name)
                                                                                            .setMessage(R.string.pin_5_8_confirm)
                                                                                            .setView(pin2)
                                                                                            .setCancelable(false)
                                                                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                                                                public void onClick(DialogInterface dialog, int whichButton) {

                                                                                                    String _pin2 = pin2.getText().toString();
                                                                                                    if(_pin2 != null && _pin2.equals(_pin))    {

                                                                                                        String accessHash = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.ACCESS_HASH, "");
                                                                                                        String accessHash2 = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.ACCESS_HASH2, "");

                                                                                                        String hash = AccessFactory.getInstance(SettingsActivity2.this).getHash(AccessFactory.getInstance(SettingsActivity2.this).getGUID(), new CharSequenceX(_pin), AESUtil.DefaultPBKDF2Iterations);
                                                                                                        PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ACCESS_HASH, hash);
                                                                                                        if(accessHash.equals(accessHash2))    {
                                                                                                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ACCESS_HASH2, hash);
                                                                                                        }
                                                                                                        AccessFactory.getInstance(SettingsActivity2.this).setPIN(_pin2);

                                                                                                        try {
                                                                                                            PayloadUtil.getInstance(SettingsActivity2.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(SettingsActivity2.this).getGUID() + _pin));
                                                                                                        }
                                                                                                        catch(JSONException je) {
                                                                                                            je.printStackTrace();
                                                                                                        }
                                                                                                        catch (IOException ioe) {
                                                                                                            ioe.printStackTrace();
                                                                                                        }
                                                                                                        catch (MnemonicException.MnemonicLengthException mle) {
                                                                                                            mle.printStackTrace();
                                                                                                        }
                                                                                                        catch (DecryptionException de) {
                                                                                                            de.printStackTrace();
                                                                                                        }
                                                                                                        finally {
                                                                                                            Toast.makeText(SettingsActivity2.this.getApplicationContext(), R.string.success_change_pin, Toast.LENGTH_SHORT).show();
                                                                                                        }

                                                                                                    }

                                                                                                }
                                                                                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                                                        public void onClick(DialogInterface dialog, int whichButton) {
                                                                                            ;
                                                                                        }
                                                                                    }).show();

                                                                                }

                                                                            }
                                                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                                        ;
                                                                    }
                                                                }).show();

                                                            }
                                                            else    {
                                                                Toast.makeText(SettingsActivity2.this.getApplicationContext(), R.string.pin_error, Toast.LENGTH_SHORT).show();
                                                            }

                                                        }
                                                        else    {
                                                            Toast.makeText(SettingsActivity2.this.getApplicationContext(), R.string.pin_error, Toast.LENGTH_SHORT).show();
                                                        }

                                                    }
                                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                ;
                                            }
                                        }).show();

                                    }
                                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }
                        }).show();

                        return true;
                    }
                });

                final CheckBoxPreference cbPref6 = (CheckBoxPreference) findPreference("autoBackup");
                if(!SamouraiWallet.getInstance().hasPassphrase(SettingsActivity2.this)) {
                    cbPref6.setChecked(false);
                    cbPref6.setEnabled(false);
                }
                else    {
                    cbPref6.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference preference, Object newValue) {

                            if (cbPref6.isChecked()) {
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.AUTO_BACKUP, false);
                            }
                            else {
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.AUTO_BACKUP, true);
                            }

                            return true;
                        }
                    });
                }

            }
            else if(strBranch.equals("networking"))   {
                addPreferencesFromResource(R.xml.settings_networking);

                Preference vpnPref = (Preference) findPreference("VPN");
                vpnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        PackageManager pm = SettingsActivity2.this.getPackageManager();
                        try	{
                            pm.getPackageInfo(AppUtil.OPENVPN_PACKAGE_ID, 0);
                            Intent intent = getPackageManager().getLaunchIntentForPackage(AppUtil.OPENVPN_PACKAGE_ID);
                            startActivity(intent);
                        }
                        catch(PackageManager.NameNotFoundException nnfe)	{
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + AppUtil.OPENVPN_PACKAGE_ID));
                            startActivity(intent);
                        }

                        return true;
                    }
                });

            }
            else if(strBranch.equals("troubleshoot"))   {
                addPreferencesFromResource(R.xml.settings_troubleshoot);

                Preference troubleshootPref = (Preference) findPreference("troubleshoot");
                troubleshootPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doTroubleshoot();
                        return true;
                    }
                });

                Preference sendBackupPref = (Preference) findPreference("send_backup_support");
                sendBackupPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        new AlertDialog.Builder(SettingsActivity2.this)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.prompt_send_backup_to_support)
                                .setCancelable(false)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        doSendBackup();

                                    }
                                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }
                        }).show();

                        return true;
                    }
                });

                Preference prunePref = (Preference) findPreference("prune");
                prunePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doPrune();
                        return true;
                    }
                });

                Preference idxPref = (Preference) findPreference("idx");
                idxPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doIndexes();
                        return true;
                    }
                });

                Preference addressCalcPref = (Preference) findPreference("acalc");
                addressCalcPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doAddressCalc();
                        return true;
                    }
                });

                Preference paynymCalcPref = (Preference) findPreference("pcalc");
                paynymCalcPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        doPayNymCalc();
                        return true;
                    }
                });

            }
            else if(strBranch.equals("other"))   {
                addPreferencesFromResource(R.xml.settings_other);

                Preference hashPref = (Preference) findPreference("hash");
                hashPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        try {
                            File apk = new File(SettingsActivity2.this.getPackageCodePath());
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            FileInputStream fis = new FileInputStream(apk);
                            byte[] dataBytes = new byte[1024 * 8];
                            int nread = 0;
                            while ((nread = fis.read(dataBytes)) != -1) {
                                md.update(dataBytes, 0, nread);
                            }
                            ;
                            byte[] hval = md.digest();
                            String hash = Hex.toHexString(hval);

                            TextView showText = new TextView(SettingsActivity2.this);
                            showText.setText(hash);
                            showText.setTextIsSelectable(true);
                            showText.setPadding(40, 10, 40, 10);
                            showText.setTextSize(18.0f);
                            new AlertDialog.Builder(SettingsActivity2.this)
                                    .setTitle(R.string.app_name)
                                    .setView(showText)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            ;
                                        }
                                    }).show();
                        } catch (Exception e) {
                            ;
                        }

                        return true;
                    }
                });

                Preference aboutPref = (Preference) findPreference("about");
                aboutPref.setSummary("Samourai," + " " + getResources().getString(R.string.version_name));
                aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(SettingsActivity2.this, AboutActivity.class);
                        startActivity(intent);
                        return true;
                    }
                });

            }
            else    {
                finish();
            }

        }
        else    {
            finish();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(SettingsActivity2.this).setIsInForeground(true);

        AppUtil.getInstance(SettingsActivity2.this).checkTimeOut();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_HEX_TX)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                doBroadcastHex(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_HEX_TX)	{
            ;
        }
        else {
            ;
        }

    }

    private void getHDSeed(boolean mnemonic)	{
        String seed = null;
        try {
            if(mnemonic)	{
                seed = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getMnemonic();
            }
            else	{
                seed = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getSeedHex();
            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
            Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        TextView showText = new TextView(SettingsActivity2.this);
        showText.setText(seed);
        showText.setTextIsSelectable(true);
        showText.setPadding(40, 10, 40, 10);
        showText.setTextSize(18.0f);
        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setView(showText)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

    }

    private void getXPUB(int purpose)	{

        String xpub = "";

        switch(purpose)    {
            case 49:
                xpub = BIP49Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).ypubstr();
                break;
            case 84:
                xpub = BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).zpubstr();
                break;
            default:
                try {
                    xpub = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getAccount(0).xpubstr();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                    Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
                }
                catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                    Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
                }
                break;
        }

        ImageView showQR = new ImageView(SettingsActivity2.this);
        Bitmap bitmap = null;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(xpub, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        }
        catch (WriterException e) {
            e.printStackTrace();
        }
        showQR.setImageBitmap(bitmap);

        TextView showText = new TextView(SettingsActivity2.this);
        showText.setText(xpub);
        showText.setTextIsSelectable(true);
        showText.setPadding(40, 10, 40, 10);
        showText.setTextSize(18.0f);

        LinearLayout xpubLayout = new LinearLayout(SettingsActivity2.this);
        xpubLayout.setOrientation(LinearLayout.VERTICAL);
        xpubLayout.addView(showQR);
        xpubLayout.addView(showText);

        final String _xpub = xpub;

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setView(xpubLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)SettingsActivity2.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = null;
                        clip = android.content.ClipData.newPlainText("XPUB", _xpub);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(SettingsActivity2.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                })
                .show();

    }

    private void getFeeProvider()	{

        final String[] providers;
        if(PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true)    {
            providers = new String[FeeUtil.getInstance().getProviders().length + 1];
            System.arraycopy(FeeUtil.getInstance().getProviders(), 0, providers, 0, FeeUtil.getInstance().getProviders().length);
            String[] trusted = new String[] { "Trusted node" };
            System.arraycopy(trusted, 0, providers, FeeUtil.getInstance().getProviders().length, 1);
        }
        else    {
            providers = FeeUtil.getInstance().getProviders();
        }

        final int sel;
        if(PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.FEE_PROVIDER_SEL, 0) >= providers.length)    {
            sel = 0;
        }
        else    {
            sel = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.FEE_PROVIDER_SEL, 0);
        }

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.options_fee_provider)
                .setSingleChoiceItems(providers, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.FEE_PROVIDER_SEL, which);

                                if(which != sel)    {

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                            APIFactory.getInstance(SettingsActivity2.this).getDynamicFees();

                                        }
                                    }).start();

                                }

                                dialog.dismiss();

                            }
                        }
                ).show();

    }

    private void getTrustedNode()	{

        final EditText edNode = new EditText(SettingsActivity2.this);
        edNode.setHint(R.string.trusted_node_ip_hint);
        edNode.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        edNode.setText(TrustedNodeUtil.getInstance().getNode() == null ? "" : TrustedNodeUtil.getInstance().getNode());
        final EditText edPort = new EditText(SettingsActivity2.this);
        edPort.setHint(R.string.trusted_node_port_hint);
        edPort.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_NUMBER);
        edPort.setText(TrustedNodeUtil.getInstance().getPort() == 0 ? Integer.toString(TrustedNodeUtil.DEFAULT_PORT) : Integer.toString(TrustedNodeUtil.getInstance().getPort()));
        final EditText edUser = new EditText(SettingsActivity2.this);
        edUser.setHint(R.string.trusted_node_user_hint);
        edUser.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        edUser.setText(TrustedNodeUtil.getInstance().getUser() == null ? "" : TrustedNodeUtil.getInstance().getUser());
        final EditText edPassword = new EditText(SettingsActivity2.this);
        edPassword.setHint(R.string.trusted_node_password_hint);
        edPassword.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edPassword.setSingleLine(true);
        edPassword.setText(TrustedNodeUtil.getInstance().getPassword() == null ? "" : TrustedNodeUtil.getInstance().getPassword());

        LinearLayout restoreLayout = new LinearLayout(SettingsActivity2.this);
        restoreLayout.setOrientation(LinearLayout.VERTICAL);
        restoreLayout.addView(edNode);
        restoreLayout.addView(edPort);
        restoreLayout.addView(edUser);
        restoreLayout.addView(edPassword);

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.trusted_node)
                .setView(restoreLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String node = edNode.getText().toString();
                        final String port = edPort.getText().toString().length() == 0 ? Integer.toString(TrustedNodeUtil.DEFAULT_PORT) : edPort.getText().toString();
                        final String user = edUser.getText().toString();
                        final String password = edPassword.getText().toString();

                        if(node != null && node.length() > 0 &&
                                port != null && port.length() > 0 &&
                                user != null && user.length() > 0 &&
                                password != null && password.length() > 0
                        )    {

                            TrustedNodeUtil.getInstance().setParams(user, new CharSequenceX(password), node, Integer.parseInt(port));

                            final Handler handler = new Handler();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    Looper.prepare();

                                    final CheckBoxPreference cbPref8 = (CheckBoxPreference)SettingsActivity2.this.findPreference("useTrustedNode");
                                    boolean isOK = false;

                                    JSONRPC jsonrpc = new JSONRPC(user, new CharSequenceX(password), node, Integer.parseInt(port));
                                    String result = jsonrpc.getNetworkInfoAsString();
                                    Log.d("TrustedNodeUtil", "getnetworkinfo:" + result);

                                    if(result != null)    {
                                        try {
                                            JSONObject obj = new JSONObject(result);
                                            if(obj != null && obj.has("version") && obj.has("subversion"))   {

                                                if(obj.getString("subversion").contains("Bitcoin XT") || obj.getString("subversion").contains("Classic") || obj.getString("subversion").contains("BitcoinUnlimited") ||
                                                        obj.getString("subversion").contains("SegWit2x") || obj.getString("subversion").contains("Segwit2x") ||
                                                        obj.getString("subversion").contains("Bitcoin ABC") ||
                                                        obj.getString("subversion").contains("Satoshi:1.14"))    {
                                                    Toast.makeText(SettingsActivity2.this, R.string.trusted_node_breaks_consensus, Toast.LENGTH_SHORT).show();
                                                }
                                                else if(obj.getInt("version") < 130100 || !obj.getString("subversion").contains("Satoshi"))   {
                                                    isOK = true;
                                                    Toast.makeText(SettingsActivity2.this, R.string.trusted_node_not_core_131, Toast.LENGTH_SHORT).show();
                                                }
                                                else    {
                                                    isOK = true;
                                                    Toast.makeText(SettingsActivity2.this, "Trusted node running:\n" + obj.getString("subversion") + ", " + obj.getInt("version"), Toast.LENGTH_SHORT).show();
                                                }

                                            }
                                            else    {
                                                Toast.makeText(SettingsActivity2.this, R.string.trusted_node_ko, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        catch(Exception e) {
                                            Toast.makeText(SettingsActivity2.this, e.getMessage() + "\n" + R.string.trusted_node_error, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else    {
                                        Toast.makeText(SettingsActivity2.this, R.string.trusted_node_not_responding, Toast.LENGTH_SHORT).show();
                                    }

                                    final boolean _isOK = isOK;

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            if(_isOK)    {
                                                cbPref8.setEnabled(true);
                                                TrustedNodeUtil.getInstance().setValidated(true);
                                            }
                                            else    {
                                                cbPref8.setChecked(false);
                                                cbPref8.setEnabled(false);
                                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.USE_TRUSTED_NODE, false);
                                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.FEE_PROVIDER_SEL, 0);
                                                TrustedNodeUtil.getInstance().setValidated(false);
                                            }

                                            SettingsActivity2.this.recreate();

                                        }
                                    });

                                    Looper.loop();

                                }
                            }).start();


                            dialog.dismiss();

                        }
                        else if((node == null || node.length() == 0) &&
                                (port == null || port.length() == 0) &&
                                (user == null || user.length() == 0) &&
                                (password == null || password.length() == 0))   {

                            TrustedNodeUtil.getInstance().reset();

                        }
                        else    {
                            Toast.makeText(SettingsActivity2.this, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                        }

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }
    }

    private void doTroubleshoot()   {

        try {
            final String strExpected = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getPassphrase();

            final EditText passphrase = new EditText(SettingsActivity2.this);
            passphrase.setSingleLine(true);
            passphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.wallet_passphrase)
                    .setView(passphrase)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            final String _passphrase39 = passphrase.getText().toString();

                            if(_passphrase39.equals(strExpected))    {

                                Toast.makeText(SettingsActivity2.this, R.string.bip39_match, Toast.LENGTH_SHORT).show();

                                final File file = PayloadUtil.getInstance(SettingsActivity2.this).getBackupFile();
                                if(file != null && file.exists())    {

                                    new AlertDialog.Builder(SettingsActivity2.this)
                                            .setTitle(R.string.app_name)
                                            .setMessage(R.string.bip39_decrypt_test)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {

                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            Looper.prepare();

                                                            StringBuilder sb = new StringBuilder();
                                                            try {
                                                                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
                                                                String str = null;
                                                                while((str = in.readLine()) != null) {
                                                                    sb.append(str);
                                                                }
                                                                in.close();
                                                                String data = sb.toString();

                                                                String decrypted = PayloadUtil.getInstance(SettingsActivity2.this).getDecryptedBackupPayload(data, new CharSequenceX(_passphrase39));
                                                                if(decrypted == null || decrypted.length() < 1)    {
                                                                    Toast.makeText(SettingsActivity2.this, R.string.backup_read_error, Toast.LENGTH_SHORT).show();
                                                                }
                                                                else    {
                                                                    Toast.makeText(SettingsActivity2.this, R.string.backup_read_ok, Toast.LENGTH_SHORT).show();
                                                                }

                                                            }
                                                            catch(FileNotFoundException fnfe) {
                                                                Toast.makeText(SettingsActivity2.this, R.string.backup_read_error, Toast.LENGTH_SHORT).show();
                                                            }
                                                            catch(IOException ioe) {
                                                                Toast.makeText(SettingsActivity2.this, R.string.backup_read_error, Toast.LENGTH_SHORT).show();
                                                            }

                                                            Looper.loop();

                                                        }
                                                    }).start();

                                                }
                                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            ;
                                        }
                                    }).show();

                                }

                            }
                            else {

                                Toast.makeText(SettingsActivity2.this, R.string.invalid_passphrase, Toast.LENGTH_SHORT).show();

                            }

                        }

                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            ;

                        }
                    });
            if(!isFinishing())    {
                dlg.show();
            }

        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

    }

    private void doPrune()   {

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.prune_backup)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        try {

//                            BIP47Meta.getInstance().pruneIncoming();
                            SendAddressUtil.getInstance().reset();
                            RicochetMeta.getInstance(SettingsActivity2.this).empty();
                            BatchSendUtil.getInstance().clear();
                            RBFUtil.getInstance().clear();

                            PayloadUtil.getInstance(SettingsActivity2.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(SettingsActivity2.this).getGUID() + AccessFactory.getInstance(SettingsActivity2.this).getPIN()));

                        }
                        catch(JSONException je) {
                            je.printStackTrace();
                            Toast.makeText(SettingsActivity2.this, R.string.error_reading_payload, Toast.LENGTH_SHORT).show();
                        }
                        catch(MnemonicException.MnemonicLengthException mle) {
                            ;
                        }
                        catch(IOException ioe) {
                            ;
                        }
                        catch(DecryptionException de) {
                            ;
                        }

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        ;

                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doSendBackup() {

        try {
            JSONObject jsonObject = PayloadUtil.getInstance(SettingsActivity2.this).getPayload();

            jsonObject.getJSONObject("wallet").remove("seed");
            jsonObject.getJSONObject("wallet").remove("passphrase");

            if(jsonObject.has("meta") && jsonObject.getJSONObject("meta").has("trusted_node"))    {
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("password");
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("node");
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("port");
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("user");
            }

            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[] { "support@samouraiwallet.com" } );
            email.putExtra(Intent.EXTRA_SUBJECT, "Samourai Wallet support backup");
            email.putExtra(Intent.EXTRA_TEXT, jsonObject.toString());
            email.setType("message/rfc822");
            startActivity(Intent.createChooser(email, SettingsActivity2.this.getText(R.string.choose_email_client)));

        }
        catch(JSONException je) {
            je.printStackTrace();
            Toast.makeText(SettingsActivity2.this, R.string.error_reading_payload, Toast.LENGTH_SHORT).show();
        }

    }

    private void doScanHexTx()   {
        Intent intent = new Intent(SettingsActivity2.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_HEX_TX);
    }

    private void doIndexes()	{

        final StringBuilder builder = new StringBuilder();

        int idxBIP84External = 0;
        int idxBIP84Internal = 0;
        int idxBIP49External = 0;
        int idxBIP49Internal = 0;
        int idxBIP44External = 0;
        int idxBIP44Internal = 0;

        idxBIP84External = BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).getReceive().getAddrIdx();
        idxBIP84Internal = BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).getChange().getAddrIdx();
        idxBIP49External = BIP49Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).getReceive().getAddrIdx();
        idxBIP49Internal = BIP49Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).getChange().getAddrIdx();

        try {
            idxBIP44External = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getAccount(0).getReceive().getAddrIdx();
            idxBIP44Internal = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getAccount(0).getChange().getAddrIdx();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        builder.append("44 receive: " + idxBIP44External + "\n");
        builder.append("44 change: " + idxBIP44Internal + "\n");
        builder.append("49 receive: " + idxBIP49External + "\n");
        builder.append("49 change: " + idxBIP49Internal + "\n");
        builder.append("84 receive :" + idxBIP84External + "\n");
        builder.append("84 change :" + idxBIP84Internal + "\n");
        builder.append("Ricochet :" + RicochetMeta.getInstance(SettingsActivity2.this).getIndex() + "\n");

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(builder.toString())
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                })
                .show();

    }

    private void doAddressCalc()    {
        Intent intent = new Intent(SettingsActivity2.this, AddressCalcActivity.class);
        startActivity(intent);
    }

    private void doPayNymCalc()    {
        Intent intent = new Intent(SettingsActivity2.this, PayNymCalcActivity.class);
        startActivity(intent);
    }

    private void doBroadcastHex()    {

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.tx_hex)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_tx_hex, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText edHexTx = new EditText(SettingsActivity2.this);
                        edHexTx.setSingleLine(false);
                        edHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        edHexTx.setLines(10);
                        edHexTx.setHint(R.string.tx_hex);
                        edHexTx.setGravity(Gravity.START);
                        TextWatcher textWatcher = new TextWatcher() {

                            public void afterTextChanged(Editable s) {
                                edHexTx.setSelection(0);
                            }
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                ;
                            }
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                ;
                            }
                        };
                        edHexTx.addTextChangedListener(textWatcher);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                                .setTitle(R.string.app_name)
                                .setView(edHexTx)
                                .setMessage(R.string.enter_tx_hex)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final String strHexTx = edHexTx.getText().toString().trim();

                                        doBroadcastHex(strHexTx);

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ;
                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
                        }

                    }

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        doScanHexTx();

                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doBroadcastHex(final String strHexTx)    {

        Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), Hex.decode(strHexTx));

        String msg = SettingsActivity2.this.getString(R.string.broadcast) + ":" + tx.getHashAsString() + " ?";

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }

                        progress = new ProgressDialog(SettingsActivity2.this);
                        progress.setCancelable(false);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(getString(R.string.please_wait));
                        progress.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.prepare();

                                PushTx.getInstance(SettingsActivity2.this).pushTx(strHexTx);

                                progress.dismiss();

                                Looper.loop();

                            }
                        }).start();

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doCahoots()    {

        final EditText edCahoots = new EditText(SettingsActivity2.this);
        edCahoots.setSingleLine(false);
        edCahoots.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edCahoots.setLines(10);
        edCahoots.setHint(R.string.cahoots);
        edCahoots.setGravity(Gravity.START);
        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                edCahoots.setSelection(0);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edCahoots.addTextChangedListener(textWatcher);

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setView(edCahoots)
                .setMessage(R.string.enter_cahoots)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final String strCahoots = edCahoots.getText().toString().trim();

                        CahootsUtil.getInstance(SettingsActivity2.this).processCahoots(strCahoots);

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doPSBT()    {

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.PSBT)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_psbt, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final EditText edPSBT = new EditText(SettingsActivity2.this);
                        edPSBT.setSingleLine(false);
                        edPSBT.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        edPSBT.setLines(10);
                        edPSBT.setHint(R.string.PSBT);
                        edPSBT.setGravity(Gravity.START);
                        TextWatcher textWatcher = new TextWatcher() {

                            public void afterTextChanged(Editable s) {
                                edPSBT.setSelection(0);
                            }
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                ;
                            }
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                ;
                            }
                        };
                        edPSBT.addTextChangedListener(textWatcher);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                                .setTitle(R.string.app_name)
                                .setView(edPSBT)
                                .setMessage(R.string.enter_psbt)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                        final String strPSBT = edPSBT.getText().toString().trim();

                                        CahootsUtil.getInstance(SettingsActivity2.this).doPSBT(strPSBT);

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
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

    private void doWhirlpoolGUIPairing()    {

        JSONObject pairingObj = null;
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("type", "whirlpool.gui");
            jsonObj.put("version", "1.0.0");
            jsonObj.put("network", SamouraiWallet.getInstance().isTestNet() ? "testnet" : "mainnet");

            String mnemonic = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getMnemonic();
            String encrypted = AESUtil.encrypt(mnemonic, new CharSequenceX(HD_WalletFactory.getInstance(SettingsActivity2.this).get().getPassphrase()), AESUtil.DefaultPBKDF2Iterations);
            jsonObj.put("mnemonic", encrypted);

            pairingObj = new JSONObject();
            pairingObj.put("pairing", jsonObj);
        }
        catch(Exception e) {
            Toast.makeText(SettingsActivity2.this, R.string.cannot_pair_whirlpool_gui, Toast.LENGTH_SHORT).show();
            return;
        }

        if(pairingObj == null)    {
            Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageView showQR = new ImageView(SettingsActivity2.this);
        Bitmap bitmap = null;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(pairingObj.toString(), null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        }
        catch (WriterException e) {
            e.printStackTrace();
        }
        showQR.setImageBitmap(bitmap);

        TextView showText = new TextView(SettingsActivity2.this);
        showText.setText(pairingObj.toString());
        showText.setTextIsSelectable(true);
        showText.setPadding(40, 10, 40, 10);
        showText.setTextSize(18.0f);

        LinearLayout pairingLayout = new LinearLayout(SettingsActivity2.this);
        pairingLayout.setOrientation(LinearLayout.VERTICAL);
        pairingLayout.addView(showQR);
        pairingLayout.addView(showText);

        final String _pairing = pairingObj.toString();

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setView(pairingLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)SettingsActivity2.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = null;
                        clip = android.content.ClipData.newPlainText("GUI pairing", _pairing);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(SettingsActivity2.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                })
                .show();

    }

}
