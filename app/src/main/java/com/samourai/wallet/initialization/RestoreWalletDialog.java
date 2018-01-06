package com.samourai.wallet.initialization;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.samourai.wallet.PinEntryActivity;
import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.LogUtil;
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

public class RestoreWalletDialog {
    private static final String TAG = LogUtil.getTag();

    public static AlertDialog.Builder makeDialog(final Context context) {

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.restore_wallet)
                .setCancelable(false)
                .setPositiveButton(R.string.import_backup, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText passphrase = new EditText(context);
                        passphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                        passphrase.setHint(R.string.passphrase);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                                .setTitle(R.string.app_name)
                                .setView(passphrase)
                                .setMessage(R.string.restore_wallet_from_backup)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        final String pw = passphrase.getText().toString();
                                        if (pw == null || pw.length() < 1) {
                                            Toast.makeText(context, R.string.invalid_passphrase, Toast.LENGTH_SHORT).show();
                                            AppUtil.getInstance(context).restartApp();
                                        }

                                        String data = null;
                                        File file = PayloadUtil.getInstance(context).getBackupFile();
                                        if(file != null && file.exists())    {

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

                                            data = sb.toString();

                                        }

                                        final EditText edBackup = new EditText(context);
                                        edBackup.setSingleLine(false);
                                        edBackup.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                                        edBackup.setLines(7);
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
                                        if(data != null)   {
                                            edBackup.setText(data);
                                            message = context.getText(R.string.restore_wallet_from_existing_backup).toString();
                                        }
                                        else    {
                                            message = context.getText(R.string.restore_wallet_from_backup).toString();
                                        }

                                        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                                                .setTitle(R.string.app_name)
                                                .setView(edBackup)
                                                .setMessage(message)
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        String data = edBackup.getText().toString();
                                                        if (data == null || data.length() < 1) {
                                                            Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                            AppUtil.getInstance(context).restartApp();
                                                        }

                                                        final String decrypted = PayloadUtil.getInstance(context).getDecryptedBackupPayload(data, new CharSequenceX(pw));
                                                        if(decrypted == null || decrypted.length() < 1)    {
                                                            Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                            AppUtil.getInstance(context).restartApp();
                                                        }

                                                        //TODO: progress variable was shared with launchFromDialer()
//                                                        if (progress != null && progress.isShowing()) {
//                                                            progress.dismiss();
//                                                            progress = null;
//                                                        }

                                                        final ProgressDialog progress = new ProgressDialog(context);
                                                        progress.setCancelable(false);
                                                        progress.setTitle(R.string.app_name);
                                                        progress.setMessage(context.getString(R.string.please_wait));
                                                        progress.show();

                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Looper.prepare();

                                                                try {

                                                                    JSONObject json = new JSONObject(decrypted);
                                                                    HD_Wallet hdw = PayloadUtil.getInstance(context).restoreWalletfromJSON(json);
                                                                    HD_WalletFactory.getInstance(context).set(hdw);
                                                                    String guid = AccessFactory.getInstance(context).createGUID();
                                                                    String hash = AccessFactory.getInstance(context).getHash(guid, new CharSequenceX(AccessFactory.getInstance(context).getPIN()), AESUtil.DefaultPBKDF2Iterations);
                                                                    PrefsUtil.getInstance(context).setValue(PrefsUtil.ACCESS_HASH, hash);
                                                                    PrefsUtil.getInstance(context).setValue(PrefsUtil.ACCESS_HASH2, hash);
                                                                    PayloadUtil.getInstance(context).saveWalletToJSON(new CharSequenceX(guid + AccessFactory.getInstance().getPIN()));

                                                                }
                                                                catch(MnemonicException.MnemonicLengthException mle) {
                                                                    mle.printStackTrace();
                                                                    Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                }
                                                                catch(DecoderException de) {
                                                                    de.printStackTrace();
                                                                    Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                }
                                                                catch(JSONException je) {
                                                                    je.printStackTrace();
                                                                    Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                }
                                                                catch(IOException ioe) {
                                                                    ioe.printStackTrace();
                                                                    Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                }
                                                                catch(NullPointerException npe) {
                                                                    npe.printStackTrace();
                                                                    Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                }
                                                                catch(DecryptionException de) {
                                                                    de.printStackTrace();
                                                                    Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                                }
                                                                finally {
                                                                    if (progress.isShowing()) {
                                                                        progress.dismiss();
                                                                    }
                                                                    AppUtil.getInstance(context).restartApp();
                                                                }

                                                                Looper.loop();

                                                            }
                                                        }).start();

                                                    }
                                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        AppUtil.getInstance(context).restartApp();

                                                    }
                                                });
                                        if(!((Activity) context).isFinishing())    {
                                            dlg.show();
                                        }

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        AppUtil.getInstance(context).restartApp();

                                    }
                                });
                        if(!((Activity) context).isFinishing())    {
                            dlg.show();
                        }

                    }
                })
                .setNegativeButton(R.string.import_mnemonic, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText mnemonic = new EditText(context);
                        mnemonic.setHint(R.string.mnemonic_hex);
                        mnemonic.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                        final EditText passphrase = new EditText(context);
                        passphrase.setHint(R.string.bip39_passphrase);
                        passphrase.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                        passphrase.setSingleLine(true);

                        LinearLayout restoreLayout = new LinearLayout(context);
                        restoreLayout.setOrientation(LinearLayout.VERTICAL);
                        restoreLayout.addView(mnemonic);
                        restoreLayout.addView(passphrase);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.bip39_restore)
                                .setView(restoreLayout)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        PrefsUtil.getInstance(context).setValue(PrefsUtil.IS_RESTORE, true);

                                        final String seed39 = mnemonic.getText().toString();
                                        final String passphrase39 = passphrase.getText().toString();

                                        if (seed39 != null && seed39.length() > 0) {
                                            Intent intent = new Intent(context, PinEntryActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.putExtra("create", true);
                                            intent.putExtra("seed", seed39);
                                            intent.putExtra("passphrase", passphrase39 == null ? "" : passphrase39);
                                            context.startActivity(intent);
                                        } else {

                                            AppUtil.getInstance(context).restartApp();

                                        }

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        AppUtil.getInstance(context).restartApp();

                                    }
                                });
                        if(!((Activity) context).isFinishing())    {
                            dlg.show();
                        }

                    }
                });

        return dlg;
    }
}
