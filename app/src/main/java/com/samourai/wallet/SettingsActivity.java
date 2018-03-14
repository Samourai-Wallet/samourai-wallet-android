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

import com.samourai.wallet.util.AppUtil;

public class SettingsActivity extends PreferenceActivity	{

    private static final int SMS_PERMISSION_CODE = 0;
    private static final int OUTGOING_CALL_PERMISSION_CODE = 1;

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

                if(!hasProcessOutgoingCallPermission()) {
                    showRequestPermissionsInfoAlertDialog(false);
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
                    if(!hasSendSmsPermission() || !hasReceiveSmsPermission() || !hasReadPhoneStatePermission()) {
                        showRequestPermissionsInfoAlertDialog(true);
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

    private void showRequestPermissionsInfoAlertDialog(final boolean smsPermission) {

        String title = null;
        String message = null;

        if(smsPermission)    {
            title = getResources().getString(R.string.permission_alert_dialog_title_sms);
            message = getResources().getString(R.string.permission_dialog_message_sms);
        }
        else    {
            title = getResources().getString(R.string.permission_alert_dialog_title_outgoing);
            message = getResources().getString(R.string.permission_dialog_message_outgoing);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(smsPermission)    {
                    requestSmsPermission();
                }
                else    {
                    requestProcessOutgoingCallPermission();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();

    }

    private boolean hasSendSmsPermission() {
        return ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasReceiveSmsPermission() {
        return ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasProcessOutgoingCallPermission() {
        return ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasReadPhoneStatePermission() {
        return ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(SettingsActivity.this, Manifest.permission.SEND_SMS) &&
            ActivityCompat.shouldShowRequestPermissionRationale(SettingsActivity.this, Manifest.permission.RECEIVE_SMS) &&
            ActivityCompat.shouldShowRequestPermissionRationale(SettingsActivity.this, Manifest.permission.READ_PHONE_STATE))
            {
            Log.d("SettingsActivity", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE}, SMS_PERMISSION_CODE);

    }

    private void requestProcessOutgoingCallPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(SettingsActivity.this, Manifest.permission.PROCESS_OUTGOING_CALLS)) {
            Log.d("SettingsActivity", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS}, OUTGOING_CALL_PERMISSION_CODE);

    }

}
