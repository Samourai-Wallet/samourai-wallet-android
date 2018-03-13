package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.access.ScrambledPin;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class PinEntryActivity extends Activity {

    private Button ta = null;
    private Button tb = null;
    private Button tc = null;
    private Button td = null;
    private Button te = null;
    private Button tf = null;
    private Button tg = null;
    private Button th = null;
    private Button ti = null;
    private Button tj = null;
    private ImageButton tsend = null;
    private ImageButton tback = null;
    private Vibrator vibrator;

    private TextView tvPrompt = null;
    private TextView tvUserInput = null;

    private ScrambledPin keypad = null;

    private StringBuilder userInput = null;

    private boolean create = false;             // create PIN
    private boolean confirm = false;            // confirm PIN
    private String strConfirm = null;
    private String strSeed = null;
    private String strPassphrase = null;
    private boolean isOpenDime = false;

    private ProgressDialog progress = null;

    private String strUri = null;

    private static int failures = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        userInput = new StringBuilder();
        keypad = new ScrambledPin();

        tvUserInput = (TextView) findViewById(R.id.userInput);
        tvUserInput.setText("");

        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        tvPrompt = (TextView) findViewById(R.id.prompt2);

        boolean scramble = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.SCRAMBLE_PIN, false);

        strUri = PrefsUtil.getInstance(PinEntryActivity.this).getValue("SCHEMED_URI", "");
        if (strUri.length() > 0) {
            PrefsUtil.getInstance(PinEntryActivity.this).setValue("SCHEMED_URI", "");
        } else {
            strUri = null;
        }

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("create") && extras.getBoolean("create") == true) {
            tvPrompt.setText(R.string.create_pin);
            scramble = false;
            create = true;
            confirm = false;
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8, Toast.LENGTH_LONG).show();
        } else if (extras != null && extras.containsKey("confirm") && extras.getBoolean("confirm") == true) {
            tvPrompt.setText(R.string.confirm_pin);
            scramble = false;
            create = false;
            confirm = true;
            strConfirm = extras.getString("first");
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8_confirm, Toast.LENGTH_LONG).show();
        } else if (extras != null && extras.containsKey("opendime") && extras.getBoolean("opendime") == true) {
            isOpenDime = true;
        } else {
            ;
        }

        if (strSeed != null && strSeed.length() < 1) {
            strSeed = null;
        }

        if (strPassphrase == null) {
            strPassphrase = "";
        }

        ta = (Button) findViewById(R.id.ta);
        ta.setText(scramble ? Integer.toString(keypad.getMatrix().get(0).getValue()) : "1");
        tb = (Button) findViewById(R.id.tb);
        tb.setText(scramble ? Integer.toString(keypad.getMatrix().get(1).getValue()) : "2");
        tc = (Button) findViewById(R.id.tc);
        tc.setText(scramble ? Integer.toString(keypad.getMatrix().get(2).getValue()) : "3");
        td = (Button) findViewById(R.id.td);
        td.setText(scramble ? Integer.toString(keypad.getMatrix().get(3).getValue()) : "4");
        te = (Button) findViewById(R.id.te);
        te.setText(scramble ? Integer.toString(keypad.getMatrix().get(4).getValue()) : "5");
        tf = (Button) findViewById(R.id.tf);
        tf.setText(scramble ? Integer.toString(keypad.getMatrix().get(5).getValue()) : "6");
        tg = (Button) findViewById(R.id.tg);
        tg.setText(scramble ? Integer.toString(keypad.getMatrix().get(6).getValue()) : "7");
        th = (Button) findViewById(R.id.th);
        th.setText(scramble ? Integer.toString(keypad.getMatrix().get(7).getValue()) : "8");
        ti = (Button) findViewById(R.id.ti);
        ti.setText(scramble ? Integer.toString(keypad.getMatrix().get(8).getValue()) : "9");
        tj = (Button) findViewById(R.id.tj);
        tj.setText(scramble ? Integer.toString(keypad.getMatrix().get(9).getValue()) : "0");
        tsend = (ImageButton) findViewById(R.id.tsend);
        tback = (ImageButton) findViewById(R.id.tback);

        tsend.setVisibility(View.INVISIBLE);
        tsend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (create && userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("confirm", true);
                    intent.putExtra("create", false);
                    intent.putExtra("first", userInput.toString());
                    intent.putExtra("seed", strSeed);
                    intent.putExtra("passphrase", strPassphrase);
                    startActivity(intent);
                } else if (confirm && userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {

                    if (userInput.toString().equals(strConfirm)) {

                        progress = new ProgressDialog(PinEntryActivity.this);
                        progress.setCancelable(false);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(strSeed == null ? getString(R.string.creating_wallet) : getString(R.string.restoring_wallet));
                        progress.show();

                        initThread(strSeed == null ? true : false, userInput.toString(), strPassphrase, strSeed == null ? null : strSeed);

                    } else {
                        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("create", true);
                        intent.putExtra("seed", strSeed);
                        intent.putExtra("passphrase", strPassphrase);
                        startActivity(intent);
                    }

                } else {
                    if (userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                        validateThread(userInput.toString(), strUri);
                    }
                }

            }
        });

        tback.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (userInput.toString().length() > 0) {
                    userInput.deleteCharAt(userInput.length() - 1);
                    if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, true) == true)    {
                        vibrator.vibrate(55);
                    }
                }
                displayUserInput();

            }
        });

        tback.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (userInput.toString().length() > 0) {
                    userInput.setLength(0);
                    if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, true) == true)    {
                        vibrator.vibrate(55);
                    }
                }
                displayUserInput();
                return false;
            }
        });

    }

    public void OnNumberPadClick(View view) {
        if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, true) == true)    {
            vibrator.vibrate(55);
        }
        userInput.append(((Button) view).getText().toString());
        displayUserInput();
    }

    private void displayUserInput() {

        tvUserInput.setText("");

        for (int i = 0; i < userInput.toString().length(); i++) {
            tvUserInput.append("*");
        }

        if (userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
            tsend.setVisibility(View.VISIBLE);
        } else {
            tsend.setVisibility(View.INVISIBLE);
        }

    }

    private void validateThread(final String pin, final String uri) {

        final ProgressDialog progress = new ProgressDialog(PinEntryActivity.this);

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }

        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if (pin.length() < AccessFactory.MIN_PIN_LENGTH || pin.length() > AccessFactory.MAX_PIN_LENGTH) {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                    }
                    Toast.makeText(PinEntryActivity.this, R.string.pin_error, Toast.LENGTH_SHORT).show();
                    AppUtil.getInstance(PinEntryActivity.this).restartApp();
                }

                String randomKey = AccessFactory.getInstance(PinEntryActivity.this).getGUID();
                if (randomKey.length() < 1) {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                    }
                    Toast.makeText(PinEntryActivity.this, R.string.random_key_error, Toast.LENGTH_SHORT).show();
                    AppUtil.getInstance(PinEntryActivity.this).restartApp();
                }

                String hash = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.ACCESS_HASH, "");
                if (AccessFactory.getInstance(PinEntryActivity.this).validateHash(hash, randomKey, new CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations)) {

                    AccessFactory.getInstance(PinEntryActivity.this).setPIN(pin);

                    try {
                        HD_Wallet hdw = PayloadUtil.getInstance(PinEntryActivity.this).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(PinEntryActivity.this).getGUID() + pin));

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                        }

                        if (hdw == null) {

                            failures++;
                            Toast.makeText(PinEntryActivity.this, PinEntryActivity.this.getText(R.string.login_error) + ":" + failures + "/3", Toast.LENGTH_SHORT).show();

                            if (failures == 3) {
                                failures = 0;
                                doBackupRestore();
                            } else {
                                Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }

                        }

                        AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                        TimeOutUtil.getInstance().updatePin();
                        if (isOpenDime) {
                            Intent intent = new Intent(PinEntryActivity.this, OpenDimeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else if (uri != null) {
                            Log.i("PinEntryActivity", "uri to restartApp()");
                            AppUtil.getInstance(PinEntryActivity.this).restartApp("uri", uri);
                        } else {
                            AppUtil.getInstance(PinEntryActivity.this).restartApp();
                        }

                    } catch (MnemonicException.MnemonicLengthException mle) {
                        mle.printStackTrace();
                    } catch (DecoderException de) {
                        de.printStackTrace();
                    } finally {
                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                        }
                    }

                } else {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                    }

                    failures++;
                    Toast.makeText(PinEntryActivity.this, PinEntryActivity.this.getText(R.string.login_error) + ":" + failures + "/3", Toast.LENGTH_SHORT).show();

                    if (failures == 3) {
                        failures = 0;
                        doBackupRestore();
                    } else {
                        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }

                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }

                Looper.loop();

            }
        }).start();

    }

    private void initThread(final boolean create, final String pin, final String passphrase, final String seed) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                String guid = AccessFactory.getInstance(PinEntryActivity.this).createGUID();
                String hash = AccessFactory.getInstance(PinEntryActivity.this).getHash(guid, new CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations);
                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.ACCESS_HASH, hash);
                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.ACCESS_HASH2, hash);

                if (create) {

                    try {
                        HD_WalletFactory.getInstance(PinEntryActivity.this).newWallet(12, passphrase, SamouraiWallet.NB_ACCOUNTS);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    } catch (MnemonicException.MnemonicLengthException mle) {
                        mle.printStackTrace();
                    } finally {
                        ;
                    }

                } else if (seed == null) {
                    ;
                } else {

                    try {
                        HD_WalletFactory.getInstance(PinEntryActivity.this).restoreWallet(seed, passphrase, SamouraiWallet.NB_ACCOUNTS);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    } catch (DecoderException de) {
                        de.printStackTrace();
                    } catch (AddressFormatException afe) {
                        afe.printStackTrace();
                    } catch (MnemonicException.MnemonicLengthException mle) {
                        mle.printStackTrace();
                    } catch (MnemonicException.MnemonicChecksumException mce) {
                        mce.printStackTrace();
                    } catch (MnemonicException.MnemonicWordException mwe) {
                        mwe.printStackTrace();
                    } finally {
                        ;
                    }

                }

                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.SCRAMBLE_PIN, true);

                try {

                    String msg = null;

                    if (HD_WalletFactory.getInstance(PinEntryActivity.this).get() != null) {

                        if (create) {
                            msg = getString(R.string.wallet_created_ok);
                        } else {
                            msg = getString(R.string.wallet_restored_ok);
                        }

                        try {
                            AccessFactory.getInstance(PinEntryActivity.this).setPIN(pin);
                            PayloadUtil.getInstance(PinEntryActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(PinEntryActivity.this).getGUID() + pin));

                            if (create) {
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.WALLET_ORIGIN, "new");
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.FIRST_RUN, true);
                            } else {
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.WALLET_ORIGIN, "restored");
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.FIRST_RUN, true);
                            }

                        } catch (JSONException je) {
                            je.printStackTrace();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        } catch (DecryptionException de) {
                            de.printStackTrace();
                        } finally {
                            ;
                        }

                        for (int i = 0; i < 2; i++) {
                            AddressFactory.getInstance().account2xpub().put(i, HD_WalletFactory.getInstance(PinEntryActivity.this).get().getAccount(i).xpubstr());
                            AddressFactory.getInstance().xpub2account().put(HD_WalletFactory.getInstance(PinEntryActivity.this).get().getAccount(i).xpubstr(), i);
                        }

                        //
                        // backup wallet for alpha
                        //
                        if (create) {

                            String seed = null;
                            try {
                                seed = HD_WalletFactory.getInstance(PinEntryActivity.this).get().getMnemonic();
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            } catch (MnemonicException.MnemonicLengthException mle) {
                                mle.printStackTrace();
                            }

                            Intent intent = new Intent(PinEntryActivity.this,  RecoveryWordsActivity.class);
                            intent.putExtra("BIP39_WORD_LIST",seed);
                            startActivity(intent);
                            finish();

                        } else {
                            AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                            TimeOutUtil.getInstance().updatePin();
                            AppUtil.getInstance(PinEntryActivity.this).restartApp();
                        }

                    } else {
                        if (create) {
                            msg = getString(R.string.wallet_created_ko);
                        } else {
                            msg = getString(R.string.wallet_restored_ko);
                        }
                    }

                    Toast.makeText(PinEntryActivity.this, msg, Toast.LENGTH_SHORT).show();

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } finally {
                    ;
                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }

                Looper.loop();

            }
        }).start();

    }

    void doBackupRestore() {

        File file = PayloadUtil.getInstance(PinEntryActivity.this).getBackupFile();
        if (file != null && file.exists()) {

            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
                String str = null;
                while ((str = in.readLine()) != null) {
                    sb.append(str);
                }
                in.close();
            } catch (FileNotFoundException fnfe) {
                ;
            } catch (IOException ioe) {
                ;
            }

            final String data = sb.toString();
            if (data != null && data.length() > 0) {
                final EditText passphrase = new EditText(PinEntryActivity.this);
                passphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                passphrase.setHint(R.string.passphrase);

                AlertDialog.Builder dlg = new AlertDialog.Builder(PinEntryActivity.this)
                        .setTitle(R.string.app_name)
                        .setView(passphrase)
                        .setMessage(R.string.restore_wallet_from_backup)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                final String pw = passphrase.getText().toString();
                                if (pw == null || pw.length() < 1) {
                                    Toast.makeText(PinEntryActivity.this, R.string.invalid_passphrase, Toast.LENGTH_SHORT).show();
                                    AppUtil.getInstance(PinEntryActivity.this).restartApp();
                                }

                                final String decrypted = PayloadUtil.getInstance(PinEntryActivity.this).getDecryptedBackupPayload(data, new CharSequenceX(pw));
                                if (decrypted == null || decrypted.length() < 1) {
                                    Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                    AppUtil.getInstance(PinEntryActivity.this).restartApp();
                                }

                                progress = new ProgressDialog(PinEntryActivity.this);
                                progress.setCancelable(false);
                                progress.setTitle(R.string.app_name);
                                progress.setMessage(getString(R.string.please_wait));
                                progress.show();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Looper.prepare();

                                        try {

                                            JSONObject json = new JSONObject(decrypted);
                                            HD_Wallet hdw = PayloadUtil.getInstance(PinEntryActivity.this).restoreWalletfromJSON(json);
                                            HD_WalletFactory.getInstance(PinEntryActivity.this).set(hdw);
                                            String guid = AccessFactory.getInstance(PinEntryActivity.this).createGUID();
                                            String hash = AccessFactory.getInstance(PinEntryActivity.this).getHash(guid, new CharSequenceX(AccessFactory.getInstance(PinEntryActivity.this).getPIN()), AESUtil.DefaultPBKDF2Iterations);
                                            PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.ACCESS_HASH, hash);
                                            PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.ACCESS_HASH2, hash);
                                            PayloadUtil.getInstance(PinEntryActivity.this).saveWalletToJSON(new CharSequenceX(guid + AccessFactory.getInstance().getPIN()));

                                        } catch (MnemonicException.MnemonicLengthException mle) {
                                            mle.printStackTrace();
                                            Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                        } catch (DecoderException de) {
                                            de.printStackTrace();
                                            Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                        } catch (JSONException je) {
                                            je.printStackTrace();
                                            Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                        } catch (IOException ioe) {
                                            ioe.printStackTrace();
                                            Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                        } catch (java.lang.NullPointerException npe) {
                                            npe.printStackTrace();
                                            Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                        } catch (DecryptionException de) {
                                            de.printStackTrace();
                                            Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                        } finally {
                                            if (progress != null && progress.isShowing()) {
                                                progress.dismiss();
                                                progress = null;
                                            }

                                            new AlertDialog.Builder(PinEntryActivity.this)
                                                    .setTitle(R.string.app_name)
                                                    .setMessage(getString(R.string.pin_reminder) + "\n\n" + AccessFactory.getInstance(PinEntryActivity.this).getPIN())
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int whichButton) {

                                                            dialog.dismiss();
                                                            AppUtil.getInstance(PinEntryActivity.this).restartApp();

                                                        }
                                                    }).show();

                                        }

                                        Looper.loop();

                                    }
                                }).start();

                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                AppUtil.getInstance(PinEntryActivity.this).restartApp();

                            }
                        });
                if (!isFinishing()) {
                    dlg.show();
                }

            }

        } else {
            Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }


}
