package com.samourai.wallet.initialization;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.samourai.wallet.PinEntryActivity;
import com.samourai.wallet.R;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.LogUtil;

public class NewWalletDialog {
    private static final String TAG = LogUtil.getTag();
    
    public static AlertDialog.Builder makeDialog(final Context context) {
        
        final EditText passphrase = new EditText(context);
        passphrase.setSingleLine(true);
        passphrase.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        if (LogUtil.DEBUG) Log.d(TAG, "build passphrase dialog");
        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip39_safe)
                .setView(passphrase)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (LogUtil.DEBUG) Log.d(TAG, "passphrase OK btn");
                        final String passphrase39 = passphrase.getText().toString();

                        if(passphrase39 != null && passphrase39.length() > 0 && passphrase39.contains(" "))    {
                            if (LogUtil.DEBUG) Log.d(TAG, "passphrase invalid toast");
                            Toast.makeText(context, R.string.bip39_invalid, Toast.LENGTH_SHORT).show();
                            AppUtil.getInstance(context).restartApp();

                        }
                        else if (passphrase39 != null && passphrase39.length() > 0) {

                            final EditText passphrase2 = new EditText(context);
                            passphrase2.setSingleLine(true);
                            passphrase2.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                            if (LogUtil.DEBUG) Log.d(TAG, "build second passphrase dialog");
                            AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.bip39_safe2)
                                    .setView(passphrase2)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            if (LogUtil.DEBUG) Log.d(TAG, "second passphrase OK btn");

                                            final String _passphrase39 = passphrase2.getText().toString();

                                            if(_passphrase39.equals(passphrase39))    {
                                                if (LogUtil.DEBUG) Log.d(TAG, "start PinEntryActivity");
                                                Intent intent = new Intent(context, PinEntryActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                intent.putExtra("create", true);
                                                intent.putExtra("passphrase", _passphrase39);
                                                context.startActivity(intent);

                                            }
                                            else {
                                                if (LogUtil.DEBUG) Log.d(TAG, "second passphrase does not match");
                                                Toast.makeText(context, R.string.bip39_unmatch, Toast.LENGTH_SHORT).show();
                                                AppUtil.getInstance(context).restartApp();

                                            }

                                        }

                                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            Toast.makeText(context, R.string.bip39_must, Toast.LENGTH_SHORT).show();
                                            AppUtil.getInstance(context).restartApp();

                                        }
                                    });
                            //TODO: do we need this check?
                            if(!((Activity) context).isFinishing())    {
                                dlg.show();
                            }

                        }
                        else {

                            Toast.makeText(context, R.string.bip39_must, Toast.LENGTH_SHORT).show();
                            AppUtil.getInstance(context).restartApp();

                        }

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Toast.makeText(context, R.string.bip39_must, Toast.LENGTH_SHORT).show();
                        AppUtil.getInstance(context).restartApp();

                    }
                });

        return dlg;
    }
}
