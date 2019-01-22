package com.samourai.wallet;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.MenuItem;
//import android.util.Log;

import com.samourai.wallet.permissions.PermissionsUtil;
import com.samourai.wallet.util.AppUtil;

public class SettingsActivity extends PreferenceActivity	{

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_root);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Preference txsPref = (Preference) findPreference("txs");
        txsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "txs");
                startActivity(intent);
                return true;
            }
        });
/*
        Preference stealthPref = (Preference) findPreference("stealth");
        stealthPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                if(!PermissionsUtil.getInstance(SettingsActivity.this).hasPermission(Manifest.permission.PROCESS_OUTGOING_CALLS)) {
                    PermissionsUtil.getInstance(SettingsActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.OUTGOING_CALL_PERMISSION_CODE);
                }
                else    {
                    Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                    intent.putExtra("branch", "stealth");
                    startActivity(intent);
                }
                return true;
            }
        });

        Preference remotePref = (Preference) findPreference("remote");
        if(!SamouraiWallet.getInstance().hasPassphrase(SettingsActivity.this)) {
            remotePref.setEnabled(false);
        }
        else    {
            remotePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if(!PermissionsUtil.getInstance(SettingsActivity.this).hasPermission(Manifest.permission.SEND_SMS) ||
                            !PermissionsUtil.getInstance(SettingsActivity.this).hasPermission(Manifest.permission.RECEIVE_SMS) ||
                            !PermissionsUtil.getInstance(SettingsActivity.this).hasPermission(Manifest.permission.READ_PHONE_STATE)
                            ) {
                        PermissionsUtil.getInstance(SettingsActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.SMS_PERMISSION_CODE);
                    }
                    else    {
                        Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                        intent.putExtra("branch", "remote");
                        startActivity(intent);
                    }
                    return true;
                }
            });
        }
*/
        Preference walletPref = (Preference) findPreference("wallet");
        walletPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "wallet");
                startActivity(intent);
                return true;
            }
        });

        Preference networkingPref = (Preference) findPreference("networking");
        networkingPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "networking");
                startActivity(intent);
                return true;
            }
        });

        Preference troubleshootPref = (Preference) findPreference("troubleshoot");
        troubleshootPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "troubleshoot");
                startActivity(intent);
                return true;
            }
        });

        Preference otherPref = (Preference) findPreference("other");
        otherPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "other");
                startActivity(intent);
                return true;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(SettingsActivity.this).setIsInForeground(true);

        AppUtil.getInstance(SettingsActivity.this).checkTimeOut();

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

}
