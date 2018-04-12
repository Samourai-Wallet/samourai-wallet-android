package com.samourai.wallet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.permissions.PermissionsUtil;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class LandingActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        Button createAccount = (Button) findViewById(R.id.button_create_new_wallet);
        FrameLayout snackBarView = (FrameLayout) findViewById(R.id.snackbar_landing);
        TextView textView = (TextView) findViewById(R.id.restore_textview);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RestoreWalletFromBackup();
            }
        });
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LandingActivity.this, CreateWalletActivity.class);
                startActivity(intent);
            }
        });
        setAppVersion();
        if (PayloadUtil.getInstance(this).getBackupFile().exists()) {
            snackBarView.setVisibility(View.VISIBLE);
        }
        if(!PermissionsUtil.getInstance(LandingActivity.this).hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || !PermissionsUtil.getInstance(LandingActivity.this).hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionsUtil.getInstance(LandingActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.READ_WRITE_EXTERNAL_PERMISSION_CODE);
        }
        if(!PermissionsUtil.getInstance(LandingActivity.this).hasPermission(Manifest.permission.CAMERA)) {
            PermissionsUtil.getInstance(LandingActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.CAMERA_PERMISSION_CODE);
        }
        if(!PermissionsUtil.getInstance(LandingActivity.this).hasPermission(Manifest.permission.SEND_SMS) || !PermissionsUtil.getInstance(LandingActivity.this).hasPermission(Manifest.permission.RECEIVE_SMS) || !PermissionsUtil.getInstance(LandingActivity.this).hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            PermissionsUtil.getInstance(LandingActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.SMS_PERMISSION_CODE);
        }

    }

    private void setAppVersion() {
        try {
            PackageInfo PInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = "v ".concat(PInfo.versionName);
            ((TextView) findViewById(R.id.version_text_view)).setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void RestoreWalletFromBackup() {
        ContextWrapper themeWrapper = new ContextThemeWrapper(this, R.style.restoreDialogStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(themeWrapper);
        builder.setTitle("Restore backup");
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.landing_restore_dialog, null);
        final EditText password = (EditText) view.findViewById(R.id.restore_dialog_password_edittext);
        builder.setView(view);
        builder.setPositiveButton("RESTORE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String backupData = readFromBackupFile();
                final String decrypted = PayloadUtil.getInstance(LandingActivity.this).getDecryptedBackupPayload(backupData, new CharSequenceX(password.getText()));
                if (decrypted == null || decrypted.length() < 1) {
                    Toast.makeText(LandingActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } else {
                    RestoreWalletFromSamouraiBackup(decrypted);
                }
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private String readFromBackupFile() {
        File file = PayloadUtil.getInstance(this).getBackupFile();
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            String str = null;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * our custom menu won't work with default menu inflator
     * so we listen to menu clicks using onKeyDown and shows the overflow menu
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showPopup(findViewById(R.id.menu_button));
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Shows overflow menu manually
     *
     * @param v
     */
    public void showPopup(View v) {
        Context wrapper = new ContextThemeWrapper(this, R.style.popMenuStyle);
        PopupMenu popup = new PopupMenu(wrapper, v, Gravity.LEFT);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.landing_activity_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.import_external_wallet_menu: {
                Intent intent = new Intent(LandingActivity.this, RestoreSeedWalletActivity.class);
                intent.putExtra("mode", "mnemonic");
                startActivity(intent);
                return false;
            }
            case R.id.import_samourai_backup_menu: {
                Intent intent = new Intent(LandingActivity.this, RestoreSeedWalletActivity.class);
                intent.putExtra("mode", "backup");
                startActivity(intent);
                return false;
            }
            case R.id.get_help_menu_create: {
                doSupportCreate();
                return false;
            }
            case R.id.get_help_menu_restore: {
                doSupportRestore();
                return false;
            }
            default: {
                return false;
            }
        }
    }

    private void toggleLoading() {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(R.string.app_name);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.show();
        } else {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            } else {
                progressDialog.show();
            }
        }
    }

    private void RestoreWalletFromSamouraiBackup(final String decrypted) {
        toggleLoading();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    JSONObject json = new JSONObject(decrypted);
                    HD_Wallet hdw = PayloadUtil.getInstance(LandingActivity.this).restoreWalletfromJSON(json);
                    HD_WalletFactory.getInstance(LandingActivity.this).set(hdw);
                    String guid = AccessFactory.getInstance(LandingActivity.this).createGUID();
                    String hash = AccessFactory.getInstance(LandingActivity.this).getHash(guid, new CharSequenceX(AccessFactory.getInstance(LandingActivity.this).getPIN()), AESUtil.DefaultPBKDF2Iterations);
                    PrefsUtil.getInstance(LandingActivity.this).setValue(PrefsUtil.ACCESS_HASH, hash);
                    PrefsUtil.getInstance(LandingActivity.this).setValue(PrefsUtil.ACCESS_HASH2, hash);
                    PayloadUtil.getInstance(LandingActivity.this).saveWalletToJSON(new CharSequenceX(guid + AccessFactory.getInstance().getPIN()));

                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                    Toast.makeText(LandingActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (DecoderException de) {
                    de.printStackTrace();
                    Toast.makeText(LandingActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (JSONException je) {
                    je.printStackTrace();
                    Toast.makeText(LandingActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    Toast.makeText(LandingActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (java.lang.NullPointerException npe) {
                    npe.printStackTrace();
                    Toast.makeText(LandingActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (DecryptionException de) {
                    de.printStackTrace();
                    Toast.makeText(LandingActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } finally {
                    AppUtil.getInstance(LandingActivity.this).restartApp();
                }

                Looper.loop();
                toggleLoading();

            }
        }).start();
    }

    private void doSupportCreate()	{
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://samourai.kayako.com/section/1-starting-a-new-wallet"));
        startActivity(intent);
    }

    private void doSupportRestore()	{
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://samourai.kayako.com/category/3-restore-recovery"));
        startActivity(intent);
    }

}

