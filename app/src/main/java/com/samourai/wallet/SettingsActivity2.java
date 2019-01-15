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
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;

import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
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
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsFactory;
import com.samourai.wallet.cahoots.Stowaway;
import com.samourai.wallet.cahoots._TransactionOutPoint;
import com.samourai.wallet.cahoots._TransactionOutput;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.cahoots.psbt.PSBTEntry;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.ReceiversUtil;
import com.samourai.wallet.util.SIMUtil;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.util.TorUtil;

import com.yanzhenjie.zbar.Symbol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class SettingsActivity2 extends PreferenceActivity	{

    private ProgressDialog progress = null;
    private boolean steathActivating = false;

    private final static int SCAN_HEX_TX = 2011;
    private final static int SCAN_CAHOOTS = 2012;
    private final static int SCAN_PSBT = 2013;

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

                if(SamouraiWallet.getInstance().isTestNet())    {

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

                }
                else    {
                    PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("txPrefs");
                    PreferenceGroup xcategory = (PreferenceGroup) findPreference(getResources().getString(R.string.experimental));
                    preferenceScreen.removePreference(xcategory);
                }

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

                final Preference torPref = (Preference) findPreference("Tor");
                if(!OrbotHelper.isOrbotInstalled(SettingsActivity2.this))    {
                    torPref.setSummary(R.string.tor_install);
                }
                else if(TorUtil.getInstance(SettingsActivity2.this).statusFromBroadcast())    {
                    torPref.setSummary(R.string.tor_routing_on);
                }
                else    {
                    torPref.setSummary(R.string.tor_routing_off);
                }
                torPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        if(!OrbotHelper.isOrbotInstalled(SettingsActivity2.this))    {

                            new AlertDialog.Builder(SettingsActivity2.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.you_must_have_orbot)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            Intent intent = OrbotHelper.getOrbotInstallIntent(SettingsActivity2.this);
                                            startActivity(intent);

                                        }
                                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            }).show();

                        }
                        else if(TorUtil.getInstance(SettingsActivity2.this).statusFromBroadcast())    {
                            TorUtil.getInstance(SettingsActivity2.this).setStatusFromBroadcast(false);
                            torPref.setSummary(R.string.tor_routing_off);
                        }
                        else    {
                            OrbotHelper.requestStartTor(SettingsActivity2.this);
                            TorUtil.getInstance(SettingsActivity2.this).setStatusFromBroadcast(true);
                            torPref.setSummary(R.string.tor_routing_on);
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
        else if(resultCode == Activity.RESULT_OK && requestCode == SCAN_CAHOOTS)	{
            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                processCahoots(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == SCAN_PSBT)	{
            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                doPSBT(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_HEX_TX)	{
            ;
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_CAHOOTS)	{
            ;
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_PSBT)	{
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

    private void doScanCahoots()   {
        Intent intent = new Intent(SettingsActivity2.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_CAHOOTS);
    }

    private void doScanPSBT()   {
        Intent intent = new Intent(SettingsActivity2.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_PSBT);
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

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.cahoots)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_cahoots, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

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

                                        processCahoots(strCahoots);

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

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        doScanCahoots();

                    }
                }).setNeutralButton(R.string.start_stowaway, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final EditText edAmount = new EditText(SettingsActivity2.this);
                        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
                        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                                .setTitle(R.string.app_name)
                                .setView(edAmount)
                                .setMessage(R.string.amount_sats)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                        final String strAmount = edAmount.getText().toString().trim();
                                        try {
                                            long amount = Long.parseLong(strAmount);
                                            doStowaway0(amount);
                                        }
                                        catch(NumberFormatException nfe) {
                                            Toast.makeText(SettingsActivity2.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
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
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void processCahoots(String strCahoots)    {

        Stowaway stowaway = null;

        try {
            JSONObject obj = new JSONObject(strCahoots);
            Log.d("SettingsActivity2", "incoming st:" + strCahoots);
            Log.d("SettingsActivity2", "object json:" + obj.toString());
            if(obj.has("cahoots") && obj.getJSONObject("cahoots").has("type"))    {

                int type = obj.getJSONObject("cahoots").getInt("type");
                switch(type)    {
                    case Cahoots.CAHOOTS_STOWAWAY:
                        stowaway = new Stowaway(obj);
                        Log.d("SettingsActivity2", "stowaway st:" + stowaway.toJSON().toString());
//                        Log.d("SettingsActivity2", "tx:" + stowaway.getTransaction().toString());
                        break;
                    case Cahoots.CAHOOTS_STONEWALLx2:
                        Toast.makeText(SettingsActivity2.this, "STONEWALLx2 coming soon", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(SettingsActivity2.this, R.string.unrecognized_cahoots, Toast.LENGTH_SHORT).show();
                        return;
                }

            }
            else    {
                Toast.makeText(SettingsActivity2.this, R.string.not_cahoots, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch(JSONException je) {
            Toast.makeText(SettingsActivity2.this, R.string.not_valid_json, Toast.LENGTH_SHORT).show();
            return;
        }

        //            doCahoots(stowaway.toJSON().toString());

        if(stowaway != null)    {

            int step = stowaway.getStep();

            try {
                switch(step)    {
                    case 0:
                        Log.d("SettingsActivity2", "calling doStowaway1");
                        doStowaway1(stowaway);
                        break;
                    case 1:
                        doStowaway2(stowaway);
                        break;
                    case 2:
                        doStowaway3(stowaway);
                        break;
                    case 3:
                        doStowaway4(stowaway);
                        break;
                    default:
                        Toast.makeText(SettingsActivity2.this, "unrecognized step", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            catch(Exception e) {
                Log.d("SettingsActivity2", e.getMessage());
                e.printStackTrace();
            }

        }
        else    {
            Toast.makeText(SettingsActivity2.this, "error processing #Cahoots", Toast.LENGTH_SHORT).show();
        }

    }

    private void doCahoots(final String strCahoots) {

        Cahoots cahoots = null;
        Transaction transaction = null;
        int step = 0;
        try {
            JSONObject jsonObject = new JSONObject(strCahoots);
            if(jsonObject != null && jsonObject.has("cahoots") && jsonObject.getJSONObject("cahoots").has("step"))    {
                step = jsonObject.getJSONObject("cahoots").getInt("step");
                if(step == 4) {
                    cahoots = new Stowaway(jsonObject);
                    transaction = cahoots.getPSBT().getTransaction();
                }
            }
        }
        catch(JSONException je) {
            ;
        }

        final int _step = step;
        final Transaction _transaction = transaction;

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        TextView showTx = new TextView(SettingsActivity2.this);
        showTx.setText(step != 4 ? strCahoots : Hex.toHexString(transaction.bitcoinSerialize()));
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);

        LinearLayout hexLayout = new LinearLayout(SettingsActivity2.this);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(showTx);

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.cahoots)
                .setView(hexLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }
                })
                .setNeutralButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)SettingsActivity2.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = null;
                        clip = android.content.ClipData.newPlainText("Cahoots", _step != 4 ? strCahoots : Hex.toHexString(_transaction.bitcoinSerialize()));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(SettingsActivity2.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton(R.string.show_qr, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {


                        if(strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT)    {

                            final ImageView ivQR = new ImageView(SettingsActivity2.this);

                            Display display = (SettingsActivity2.this).getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int imgWidth = Math.max(size.x - 240, 150);

                            Bitmap bitmap = null;

                            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strCahoots, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

                            try {
                                bitmap = qrCodeEncoder.encodeAsBitmap();
                            } catch (WriterException e) {
                                e.printStackTrace();
                            }

                            ivQR.setImageBitmap(bitmap);

                            LinearLayout qrLayout = new LinearLayout(SettingsActivity2.this);
                            qrLayout.setOrientation(LinearLayout.VERTICAL);
                            qrLayout.addView(ivQR);

                            new AlertDialog.Builder(SettingsActivity2.this)
                                    .setTitle(R.string.cahoots)
                                    .setView(qrLayout)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            dialog.dismiss();

                                        }
                                    })
                                    .setNegativeButton(R.string.share_qr, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String strFileName = AppUtil.getInstance(SettingsActivity2.this).getReceiveQRFilename();
                                            File file = new File(strFileName);
                                            if(!file.exists()) {
                                                try {
                                                    file.createNewFile();
                                                }
                                                catch(Exception e) {
                                                    Toast.makeText(SettingsActivity2.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            file.setReadable(true, false);

                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(file);
                                            }
                                            catch(FileNotFoundException fnfe) {
                                                ;
                                            }

                                            if(file != null && fos != null) {
                                                Bitmap bitmap = ((BitmapDrawable)ivQR.getDrawable()).getBitmap();
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                                try {
                                                    fos.close();
                                                }
                                                catch(IOException ioe) {
                                                    ;
                                                }

                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.setType("image/png");
                                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                                    //From API 24 sending FIle on intent ,require custom file provider
                                                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                            SettingsActivity2.this,
                                                            getApplicationContext()
                                                                    .getPackageName() + ".provider", file));
                                                } else {
                                                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                                }
                                                startActivity(Intent.createChooser(intent, SettingsActivity2.this.getText(R.string.send_tx)));
                                            }

                                        }
                                    }).show();
                        }
                        else    {

                            Toast.makeText(SettingsActivity2.this, R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();

                        }

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

                                        doPSBT(strPSBT);

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

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        doScanPSBT();

                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doPSBT(final String strPSBT)    {

        String msg = null;
        PSBT psbt = new PSBT(strPSBT, SamouraiWallet.getInstance().getCurrentNetworkParams());
        try {
            psbt.read();
            msg = psbt.dump();
        }
        catch(Exception e) {
            msg = e.getMessage();
        }

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
        edPSBT.setText(msg);

        AlertDialog.Builder dlg = new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.PSBT)
                .setView(edPSBT)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }

                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doStowaway0(long spendAmount)    {
        // Bob -> Alice, spendAmount in sats
        NetworkParameters params = TestNet3Params.get();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        Stowaway stowaway0 = new Stowaway(spendAmount, params);
        System.out.println(stowaway0.toJSON().toString());

        doCahoots(stowaway0.toJSON().toString());
    }

    private void doStowaway1(Stowaway stowaway0) throws Exception    {

        List<UTXO> utxos = new ArrayList<UTXO>();
        List<UTXO> _utxos = APIFactory.getInstance(SettingsActivity2.this).getUtxos(true);
        for(UTXO utxo : _utxos)   {
            String script = Hex.toHexString(utxo.getOutpoints().get(0).getScriptBytes());
            if(script.startsWith("0014"))   {
                utxos.add(utxo);
            }
        }
        // sort in descending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());

        Log.d("SettingsActivity2", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        for(UTXO utxo : utxos)   {
            selectedUTXO.add(utxo);
            totalContributedAmount += utxo.getValue();
            Log.d("SettingsActivity2", "BIP84 selected utxo:" + utxo.getValue());
            if(totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue())    {
                break;
            }
        }
        if(!(totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue()))    {
            Toast.makeText(SettingsActivity2.this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SettingsActivity2", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stowaway0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        // A provides 5750000
        //
        //

        String zpub = BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();
//        inputsA.put(outpoint_A0, Triple.of(Hex.decode("0221b719bc26fb49971c7dd328a6c7e4d17dfbf4e2217bee33a65c53ed3daf041e"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/4"));
//        inputsA.put(outpoint_A1, Triple.of(Hex.decode("020ab261e1a3cf986ecb3cd02299de36295e804fd799934dc5c99dde0d25e71b93"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/2"));

        for(UTXO utxo : selectedUTXO)  {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress());
                String path = APIFactory.getInstance(SettingsActivity2.this).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // destination output
        int idx = BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).getReceive().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(SettingsActivity2.this).getAddressAt(0, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
//        byte[] scriptPubKey_A = getScriptPubKey("tb1qewwlc2dksuez3zauf38d82m7uqd4ewkf2avdl8", params);
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_A = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stowaway0.getSpendAmount()), scriptPubKey_A);
        outputsA.put(output_A0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).zpubstr()), "M/0/" + idx));

        Stowaway stowaway1 = new Stowaway(stowaway0);
        stowaway1.inc(inputsA, outputsA, null);

        doCahoots(stowaway1.toJSON().toString());
    }

    private void doStowaway2(Stowaway stowaway1) throws Exception    {

        Transaction transaction = stowaway1.getTransaction();
        Log.d("SettingsActivity2", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
//        Log.d("SettingsActivity2", "input value:" + transaction.getInputs().get(0).getValue().longValue());
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = new ArrayList<UTXO>();
        List<UTXO> _utxos = APIFactory.getInstance(SettingsActivity2.this).getUtxos(true);
        for(UTXO utxo : _utxos)   {
            String script = Hex.toHexString(utxo.getOutpoints().get(0).getScriptBytes());
            if(script.startsWith("0014"))   {
                utxos.add(utxo);
            }
        }
        // sort in ascending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        Collections.reverse(utxos);

        Log.d("SettingsActivity2", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        int nbTotalSelectedOutPoints = 0;
        long totalSelectedAmount = 0L;
        for(UTXO utxo : utxos)   {
            selectedUTXO.add(utxo);
            totalSelectedAmount += utxo.getValue();
            Log.d("BIP84 selected utxo:", "" + utxo.getValue());
            nbTotalSelectedOutPoints += utxo.getOutpoints().size();
            if(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())    {

                // discard "extra" utxo, if any
                List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                Collections.reverse(selectedUTXO);
                int _nbTotalSelectedOutPoints = 0;
                long _totalSelectedAmount = 0L;
                for(UTXO utxoSel : selectedUTXO)   {
                    _selectedUTXO.add(utxoSel);
                    _totalSelectedAmount += utxoSel.getValue();
                    Log.d("SettingsActivity2", "BIP84 post selected utxo:" + utxoSel.getValue());
                    _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                    if(_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())    {
                        selectedUTXO.clear();
                        selectedUTXO.addAll(_selectedUTXO);
                        totalSelectedAmount = _totalSelectedAmount;
                        nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                        break;
                    }
                }

                break;
            }
        }
        if(!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()))    {
            Toast.makeText(SettingsActivity2.this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SettingsActivity2", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue();
        Log.d("SettingsActivity2", "fee:" + fee);

        NetworkParameters params = stowaway1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        // B provides 1000000, 1500000 (250000 change, A input larger)
        //
        //

        String zpub = BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for(UTXO utxo : selectedUTXO)  {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress());
                String path = APIFactory.getInstance(SettingsActivity2.this).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        Log.d("SettingsActivity2", "inputsB:" + inputsB.size());

        // change output
        int idx = BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).getChange().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(SettingsActivity2.this).getAddressAt(1, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_B = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stowaway1.getSpendAmount()) - fee), scriptPubKey_B);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(SettingsActivity2.this).getWallet().getAccount(0).zpubstr()), "M/1/" + idx));

        Log.d("SettingsActivity2", "outputsB:" + outputsB.size());

        Stowaway stowaway2 = new Stowaway(stowaway1);
        stowaway2.inc(inputsB, outputsB, null);
        System.out.println("step 2:" + stowaway2.toJSON().toString());

        doCahoots(stowaway2.toJSON().toString());
    }

    private void doStowaway3(Stowaway stowaway2) throws Exception    {

        HashMap<String,String> utxo2Address = new HashMap<String,String>();
        List<UTXO> utxos = APIFactory.getInstance(SettingsActivity2.this).getUtxos(true);
        for(UTXO utxo : utxos)   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                Log.d("SettingsActivity2", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway2.getPSBT().getTransaction();
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        for(TransactionInput input : transaction.getInputs())   {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if(utxo2Address.containsKey(key))    {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address);
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step3: A verif, BIP69, sig
        //
        //
        /*
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        keyBag_A.put(outpoint_A0.toString(), ecKey_A0);
        keyBag_A.put(outpoint_A1.toString(), ecKey_A1);
        */

        Stowaway stowaway3 = new Stowaway(stowaway2);
        stowaway3.inc(null, null, keyBag_A);
        System.out.println("step 3:" + stowaway3.toJSON().toString());

        doCahoots(stowaway3.toJSON().toString());
    }

    private void doStowaway4(Stowaway stowaway3) throws Exception    {

        HashMap<String,String> utxo2Address = new HashMap<String,String>();
        List<UTXO> utxos = APIFactory.getInstance(SettingsActivity2.this).getUtxos(true);
        for(UTXO utxo : utxos)   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                Log.d("SettingsActivity2", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway3.getPSBT().getTransaction();
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        for(TransactionInput input : transaction.getInputs())   {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if(utxo2Address.containsKey(key))    {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address);
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step4: B verif, sig, broadcast
        //
        //

        /*
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        keyBag_B.put(outpoint_B0.toString(), ecKey_B0);
        keyBag_B.put(outpoint_B1.toString(), ecKey_B1);
        */

        Stowaway stowaway4 = new Stowaway(stowaway3);
        stowaway4.inc(null, null, keyBag_B);
        System.out.println("step 4:" + stowaway4.toJSON().toString());
        System.out.println("step 4 psbt:" + stowaway4.getPSBT().toString());
        System.out.println("step 4 tx:" + stowaway4.getTransaction().toString());
        System.out.println("step 4 tx hex:" + Hex.toHexString(stowaway4.getTransaction().bitcoinSerialize()));

//        Stowaway s = new Stowaway(stowaway4.toJSON());
//        System.out.println(s.toJSON().toString());

        // broadcast ???
        doCahoots(stowaway4.toJSON().toString());
    }

}
