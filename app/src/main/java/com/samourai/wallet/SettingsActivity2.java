package com.samourai.wallet;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import org.bitcoinj.crypto.MnemonicException;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.service.BroadcastReceiverService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.Hash;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SIMUtil;
import com.samourai.wallet.util.TorUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class SettingsActivity2 extends PreferenceActivity	{

    private boolean steathActivating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("branch"))	{
            String strBranch = extras.getString("branch");

            if(strBranch.equals("prefs"))    {
                addPreferencesFromResource(R.xml.settings_prefs);

                Preference unitsPref = (Preference) findPreference("units");
                unitsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getUnits();
                        return true;
                    }
                });

                Preference fiatPref = (Preference) findPreference("fiat");
                fiatPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getExchange();
                        return true;
                    }
                });

                Preference explorersPref = (Preference) findPreference("explorer");
                explorersPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        getBlockExplorer();
                        return true;
                    }
                });

            }
            else if(strBranch.equals("txs"))   {
                addPreferencesFromResource(R.xml.settings_txs);

                final CheckBoxPreference cbPref7 = (CheckBoxPreference) findPreference("bip126");
                cbPref7.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        if (cbPref7.isChecked()) {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.SPEND_TYPE, SendActivity.SPEND_SIMPLE);
                        }
                        else    {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.SPEND_TYPE, SendActivity.SPEND_BIP126);
                        }

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

                                stopService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));
                                startService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));

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

                                                stopService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));
                                                startService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));

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

                            stopService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));
                            startService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));
                        }
                        else {
                            PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.ACCEPT_REMOTE, true);

                            stopService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));
                            startService(new Intent(SettingsActivity2.this, BroadcastReceiverService.class));
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
                        getXPUB();
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
                            String hash = new Hash(hval).toString();

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
/*
                Preference certifPref = (Preference) findPreference("certif");
                certifPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        try {

                            StringBuilder sb = new StringBuilder();

                            final PackageManager packageManager = SettingsActivity2.this.getPackageManager();

                            PackageInfo pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            final String strName = pkgInfo.applicationInfo.loadLabel(packageManager).toString();
                            final String strVendor = pkgInfo.packageName;

                            sb.append(strName + " / " + strVendor + "\n");

                            final Signature[] arrSignatures = pkgInfo.signatures;
                            if(arrSignatures != null)    {
                                for (final Signature sig : arrSignatures) {

                                    final byte[] rawCert = sig.toByteArray();
                                    InputStream certStream = new ByteArrayInputStream(rawCert);

                                    try {
                                        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                                        X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);

//                                        sb.append("Certificate subject: " + x509Cert.getSubjectDN() + "\n");
//                                        sb.append("Certificate issuer: " + x509Cert.getIssuerDN() + "\n");
//                                        sb.append("Certificate serial number: " + x509Cert.getSerialNumber());

                                        sb.append("Certificate signature: " + Hex.toHexString(x509Cert.getSignature()));

                                    }
                                    catch (CertificateException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            TextView showText = new TextView(SettingsActivity2.this);
                            showText.setText(sb.toString());
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
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        return true;
                    }
                });
*/
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

    private void getXPUB()	{

        final String[] accounts;
        if(AddressFactory.getInstance(SettingsActivity2.this).getHighestTxReceiveIdx(SamouraiWallet.MIXING_ACCOUNT) == 0)    {
            accounts = new String[] {
                    getString(R.string.account_Samourai),
            };
        }
        else    {
            accounts = new String[] {
                    getString(R.string.account_Samourai),
                    getString(R.string.account_shuffling),
            };
        }

        final int sel = 0;

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.select_account)
                .setSingleChoiceItems(accounts, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                                String xpub = null;
                                try {
                                    xpub = HD_WalletFactory.getInstance(SettingsActivity2.this).get().getAccount(which).xpubstr();
                                }
                                catch (IOException ioe) {
                                    ioe.printStackTrace();
                                    Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
                                }
                                catch (MnemonicException.MnemonicLengthException mle) {
                                    mle.printStackTrace();
                                    Toast.makeText(SettingsActivity2.this, "HD wallet error", Toast.LENGTH_SHORT).show();
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

                                new AlertDialog.Builder(SettingsActivity2.this)
                                        .setTitle(R.string.app_name)
                                        .setView(xpubLayout)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                ;
                                            }
                                        }).show();

                            }
                        }
                ).show();

    }

    private void getUnits()	{

        final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        final int sel = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.BTC_UNITS, 0);

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.options_units)
                .setSingleChoiceItems(units, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.BTC_UNITS, which);
                                dialog.dismiss();
                            }
                        }
                ).show();

    }

    private void getExchange()	{

        final String[] exchanges = ExchangeRateFactory.getInstance(this).getExchangeLabels();
        final int sel = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0);

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.options_currency)
                .setSingleChoiceItems(exchanges, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.CURRENT_EXCHANGE, exchanges[which].substring(exchanges[which].length() - 3));
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.CURRENT_EXCHANGE_SEL, which);
                                dialog.dismiss();
                                getFiat();

                            }
                        }
                ).show();

    }

    private void getFiat()	{

        final int fxSel = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0);

        final String[] currencies;
        if(fxSel == 1)	{
            currencies = ExchangeRateFactory.getInstance(this).getCurrencyLabelsBTCe();
        }
        else	{
            currencies = ExchangeRateFactory.getInstance(this).getCurrencyLabels();
        }

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.options_currency)
                .setSingleChoiceItems(currencies, 0, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                String selectedCurrency = null;
                                if (currencies[which].substring(currencies[which].length() - 3).equals("RUR")) {
                                    selectedCurrency = "RUB";
                                }
                                else {
                                    selectedCurrency = currencies[which].substring(currencies[which].length() - 3);
                                }

                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.CURRENT_FIAT, selectedCurrency);
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.CURRENT_FIAT_SEL, which);
                                dialog.dismiss();
                            }
                        }
                ).show();

    }

    private void getBlockExplorer()	{

        final CharSequence[] explorers = BlockExplorerUtil.getInstance().getBlockExplorers();
        final int sel = PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);

        new AlertDialog.Builder(SettingsActivity2.this)
                .setTitle(R.string.options_blockexplorer)
                .setSingleChoiceItems(explorers, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.BLOCK_EXPLORER, which);
                                dialog.dismiss();
                            }
                        }
                ).show();

    }

}
