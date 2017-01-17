package com.samourai.wallet;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
//import android.util.Log;

import com.samourai.wallet.R;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.TimeOutUtil;

public class SettingsActivity extends PreferenceActivity	{

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_root);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Preference prefsPref = (Preference) findPreference("prefs");
        prefsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "prefs");
                startActivity(intent);
                return true;
            }
        });

        Preference txsPref = (Preference) findPreference("txs");
        txsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "txs");
                startActivity(intent);
                return true;
            }
        });

        Preference stealthPref = (Preference) findPreference("stealth");
        stealthPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("branch", "stealth");
                startActivity(intent);
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
                    Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                    intent.putExtra("branch", "remote");
                    startActivity(intent);
                    return true;
                }
            });
        }

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
