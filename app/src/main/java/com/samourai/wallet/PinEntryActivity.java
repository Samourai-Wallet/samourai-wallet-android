package com.samourai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.text.InputType;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.ExternalBackupManager;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.widgets.PinEntryView;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PinEntryActivity extends AppCompatActivity {


    private ImageButton tsend = null;
    private ImageButton tback = null;


    private StringBuilder userInput = null;

    private boolean create = false;             // create PIN
    private boolean confirm = false;            // confirm PIN
    private String strConfirm = null;
    private String strSeed = null;
    private String strPassphrase = "";
    private boolean isOpenDime = false;


    private String strUri = null;

    private int failures = 0;
    private PinEntryView pinEntryView;
    private LinearLayout restoreLayout;
    private LinearLayout pinEntryMaskLayout;
    private TextView walletStatusTextView;
    private ProgressBar progressBar;
    private static final String TAG = "PinEntryActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinentry);
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        if (!BuildConfig.DEBUG)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        userInput = new StringBuilder();
        pinEntryView = findViewById(R.id.pinentry_view);
        walletStatusTextView = findViewById(R.id.pin_entry_wallet_status);
        restoreLayout = findViewById(R.id.pin_entry_restore);
        MaterialButton restoreBtn = findViewById(R.id.pin_entry_restore_btn);
        pinEntryMaskLayout = findViewById(R.id.pin_entry_mask_layout);
        progressBar = findViewById(R.id.progress_pin_entry);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.window));
        restoreLayout.setVisibility(View.GONE);
        pinEntryView.setEntryListener((key, view) -> {
            if (userInput.length() <= (AccessFactory.MAX_PIN_LENGTH - 1)) {
                userInput = userInput.append(key);
                if (userInput.length() >= AccessFactory.MIN_PIN_LENGTH) {
                    pinEntryView.showCheckButton();
                } else {
                    pinEntryView.hideCheckButton();
                }
                setPinMaskView();
            }
        });
        restoreBtn.setOnClickListener(v -> doBackupRestore());
        pinEntryView.setClearListener(clearType -> {
            if (clearType == PinEntryView.KeyClearTypes.CLEAR) {
                if (userInput.length() != 0)
                    userInput = new StringBuilder(userInput.substring(0, (userInput.length() - 1)));
                if (userInput.length() >= AccessFactory.MIN_PIN_LENGTH) {
                    pinEntryView.showCheckButton();
                } else {
                    pinEntryView.hideCheckButton();
                }
            } else {
                strPassphrase = "";
                userInput = new StringBuilder();
                pinEntryMaskLayout.removeAllViews();
                pinEntryView.hideCheckButton();
            }
            setPinMaskView();
        });


        boolean scramble = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.SCRAMBLE_PIN, false);

        strUri = PrefsUtil.getInstance(PinEntryActivity.this).getValue("SCHEMED_URI", "");
        if (strUri.length() > 0) {
            PrefsUtil.getInstance(PinEntryActivity.this).setValue("SCHEMED_URI", "");
        } else {
            strUri = null;
        }
        if (scramble) {
            pinEntryView.setScramble(true);
        }


        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("create") && extras.getBoolean("create")) {
//            tvPrompt.setText(R.string.create_pin);
            scramble = false;
            create = true;
            confirm = false;
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8, Toast.LENGTH_LONG).show();
        } else if (extras != null && extras.containsKey("confirm") && extras.getBoolean("confirm")) {
//            tvPrompt.setText(R.string.confirm_pin);
            scramble = false;
            create = false;
            confirm = true;
            strConfirm = extras.getString("first");
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8_confirm, Toast.LENGTH_LONG).show();
        } else if (extras != null && extras.containsKey("opendime") && extras.getBoolean("opendime")) {
            isOpenDime = true;
        } else {
            if(isLocked()){
                startCountDownTimer();
            }
        }

        if (strSeed != null && strSeed.length() < 1) {
            strSeed = null;
        }

        if (strPassphrase == null) {
            strPassphrase = "";
        }
        if (!PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, true)) {
            pinEntryView.disableHapticFeedBack();
        }
        pinEntryView.setConfirmClickListener(view -> {

            if (create && strPassphrase.length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("confirm", true);
                intent.putExtra("create", false);
                intent.putExtra("first", userInput.toString());
                intent.putExtra("seed", strSeed);
                intent.putExtra("passphrase", strPassphrase);
                startActivity(intent);
                finish();
            } else if (confirm && strPassphrase.length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {

                if (userInput.toString().equals(strConfirm)) {

                    progressBar.setVisibility(View.VISIBLE);

                    initThread(strSeed == null, userInput.toString(), strPassphrase, strSeed == null ? null : strSeed);

                } else {
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("create", true);
                    intent.putExtra("seed", strSeed);
                    intent.putExtra("passphrase", strPassphrase);
                    startActivity(intent);
                    finish();
                }

            } else {
                if (userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                    validateThread(userInput.toString(), strUri);
                }
            }
        });

//

    }

    private void setPinMaskView() {

        pinEntryMaskLayout.post(() -> {
            if (userInput.length() == 0) {
                pinEntryMaskLayout.removeAllViews();
                return;
            }
            if (userInput.length() > pinEntryMaskLayout.getChildCount() && userInput.length() != 0) {
                ImageView image = new ImageView(getApplicationContext());
                image.setImageDrawable(getResources().getDrawable(R.drawable.circle_dot_white));
                image.getDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.ADD);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 0, 8, 0);
                TransitionManager.beginDelayedTransition(pinEntryMaskLayout, new ChangeBounds().setDuration(50));
                pinEntryMaskLayout.addView(image, params);
            } else {
                if (pinEntryMaskLayout.getChildCount() != 0) {
                    TransitionManager.beginDelayedTransition(pinEntryMaskLayout, new ChangeBounds().setDuration(200));
                    pinEntryMaskLayout.removeViewAt(pinEntryMaskLayout.getChildCount() - 1);
                }
            }
        });

    }

    public void OnNumberPadClick(View view) {
        if (PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, true)) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
        userInput.append(((Button) view).getText().toString());
        displayUserInput();
    }

    private void displayUserInput() {

//        tvUserInput.setText("");

        for (int i = 0; i < userInput.toString().length(); i++) {
//            tvUserInput.append("*");
        }

        if (userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
            tsend.setVisibility(View.VISIBLE);
        } else {
            tsend.setVisibility(View.INVISIBLE);
        }

    }

    private void validateThread(final String pin, final String uri) {

        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            Looper.prepare();

            if (pin.length() < AccessFactory.MIN_PIN_LENGTH || pin.length() > AccessFactory.MAX_PIN_LENGTH) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.INVISIBLE);

                });
                Toast.makeText(PinEntryActivity.this, R.string.pin_error, Toast.LENGTH_SHORT).show();
                AppUtil.getInstance(PinEntryActivity.this).restartApp(getIntent().getExtras());
                finish();
            }

            String randomKey = AccessFactory.getInstance(PinEntryActivity.this).getGUID();
            if (randomKey.length() < 1) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.INVISIBLE);

                });
                Toast.makeText(PinEntryActivity.this, R.string.random_key_error, Toast.LENGTH_SHORT).show();
                AppUtil.getInstance(PinEntryActivity.this).restartApp(getIntent().getExtras());
                finish();
            }

            String hash = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.ACCESS_HASH, "");
            if (AccessFactory.getInstance(PinEntryActivity.this).validateHash(hash, randomKey, new CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations)) {

                AccessFactory.getInstance(PinEntryActivity.this).setPIN(pin);

                try {
                    HD_Wallet hdw = PayloadUtil.getInstance(PinEntryActivity.this).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(PinEntryActivity.this).getGUID() + pin));

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.INVISIBLE);
                    });

                    if (hdw == null) {

                        runOnUiThread(() -> {
                            failures++;
                            userInput = new StringBuilder();
                            pinEntryMaskLayout.removeAllViews();
                            pinEntryView.hideCheckButton();
                            setPinMaskView();
                            if(failures<=2){
                                walletStatusTextView.setText( this.getText(R.string.login_error) + ": " + failures + "/3");
                            }
                            if (failures == 3) {
                                failures = 0;
                                startCountDownTimer();
                            }
                        });


                    }

                    AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                    TimeOutUtil.getInstance().updatePin();
                    if (isOpenDime) {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(PinEntryActivity.this, OpenDimeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });

                    } else {
                        AppUtil.getInstance(PinEntryActivity.this).restartApp(getIntent().getExtras());
                        finish();
                    }

                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } catch (DecoderException de) {
                    de.printStackTrace();
                } finally {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                }

            } else {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.INVISIBLE);
                     failures++;
                    userInput = new StringBuilder();
                    pinEntryMaskLayout.removeAllViews();
                    pinEntryView.hideCheckButton();
                    if(failures<=2){
                        walletStatusTextView.setText(this.getText(R.string.login_error) + ": " + failures + "/3");
                    }
                    if (failures == 3) {
                        failures = 0;
                        startCountDownTimer();
                    }
                });

            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.INVISIBLE);

            });


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

                            String seed = HD_WalletFactory.getInstance(PinEntryActivity.this).get().getMnemonic();

                            Intent intent = new Intent(PinEntryActivity.this, RecoveryWordsActivity.class);
                            intent.putExtra(RecoveryWordsActivity.WORD_LIST, seed);
                            intent.putExtra(RecoveryWordsActivity.PASSPHRASE, passphrase);
                            startActivity(intent);
                            finish();

                        } else {
                            AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                            TimeOutUtil.getInstance().updatePin();
                            AppUtil.getInstance(PinEntryActivity.this).restartApp(getIntent().getExtras());
                            finish();
                        }

                    } else {
                        if (create) {
                            msg = getString(R.string.wallet_created_ko);
                        } else {
                            msg = getString(R.string.wallet_restored_ko);
                        }
                    }

                    Toast.makeText(PinEntryActivity.this, msg, Toast.LENGTH_SHORT).show();

                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                }

                progressBar.setVisibility(View.INVISIBLE);

                Looper.loop();

            }
        }).start();

    }

    void doBackupRestore() {
        if (ExternalBackupManager.backupAvailable()) {
            Disposable disposable = Single.fromCallable(ExternalBackupManager::read)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((data, throwable) -> {
                        if (throwable != null) {
                            Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (data != null && data.length() > 0) {

                            final EditText passphrase = new EditText(PinEntryActivity.this);
                            passphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                            passphrase.setHint(R.string.passphrase);
                            final FrameLayout layout = new FrameLayout(this);
                            layout.addView(passphrase);

                            MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(PinEntryActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setView(layout)
                                    .setMessage(R.string.restore_wallet_from_backup)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            final String pw = passphrase.getText().toString();
                                            if (pw == null || pw.length() < 1) {
                                                Toast.makeText(PinEntryActivity.this, R.string.invalid_passphrase, Toast.LENGTH_SHORT).show();
                                                AppUtil.getInstance(PinEntryActivity.this).restartApp(getIntent().getExtras());
                                                finish();
                                            }

                                            String decrypted = null;
                                            try {
                                                decrypted = PayloadUtil.getInstance(PinEntryActivity.this).getDecryptedBackupPayload(data, new CharSequenceX(pw));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            if (decrypted == null || decrypted.length() < 1) {
                                                Toast.makeText(PinEntryActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                AppUtil.getInstance(PinEntryActivity.this).restartApp(getIntent().getExtras());
                                                finish();
                                            }


                                            progressBar.setVisibility(View.VISIBLE);
                                            final String _decrypted = decrypted;
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Looper.prepare();

                                                    try {

                                                        JSONObject json = new JSONObject(_decrypted);
                                                        HD_Wallet hdw = PayloadUtil.getInstance(PinEntryActivity.this).restoreWalletfromJSON(json, false);
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
                                                        runOnUiThread(() -> {
                                                            progressBar.setVisibility(View.INVISIBLE);
                                                        });

                                                        new MaterialAlertDialogBuilder(PinEntryActivity.this)
                                                                .setTitle(R.string.app_name)
                                                                .setMessage(getString(R.string.pin_reminder) + "\n\n" + AccessFactory.getInstance(PinEntryActivity.this).getPIN())
                                                                .setCancelable(false)
                                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                                        dialog.dismiss();
                                                                        AppUtil.getInstance(PinEntryActivity.this).restartApp(getIntent().getExtras());
                                                                        finish();

                                                                    }
                                                                }).show();

                                                    }

                                                    Looper.loop();

                                                }
                                            }).start();

                                        }
                                    }).setNegativeButton(R.string.cancel, (dialog, whichButton) -> {

                                    });
                            dlg.show();

                        }

                    });
        } else {
            Toast.makeText(this,"Backup file not available ",Toast.LENGTH_SHORT).show();
            if (!ExternalBackupManager.hasPermissions()) {
                ExternalBackupManager.askPermission(this);
            }
        }
    }

    boolean isLocked() {
        return PrefsUtil.getInstance(getApplication()).getValue(PrefsUtil.PIN_TIMEOUT,30000L) != 0L;
    }

    void startCountDownTimer() {
        TransitionManager.beginDelayedTransition((ViewGroup ) restoreLayout.getRootView() );
        pinEntryView.disable(true);
        restoreLayout.setVisibility(View.VISIBLE);
        long timeoutPref = PrefsUtil.getInstance(getApplication()).getValue(PrefsUtil.PIN_TIMEOUT,0L);
        new CountDownTimer(  timeoutPref == 0L ? 30000L : timeoutPref, 1000) {
            public void onTick(long duration) {
                long secs = (duration / 1000) % 60;
                PrefsUtil.getInstance(getApplication()).setValue(PrefsUtil.PIN_TIMEOUT,duration);
                walletStatusTextView.setText(getString(R.string.please_try_again_in).concat(" ").concat(String.valueOf(secs)).concat(" ").concat(getString(R.string.seconds)));
            }

            public void onFinish() {
                PrefsUtil.getInstance(getApplication()).setValue(PrefsUtil.PIN_TIMEOUT,0L);
                pinEntryView.disable(false);
                TransitionManager.beginDelayedTransition((ViewGroup ) restoreLayout.getRootView() );
                restoreLayout.setVisibility(View.GONE);
                walletStatusTextView.setText(R.string.wallet_locked);
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ExternalBackupManager.onActivityResult(requestCode, resultCode, data,getApplication());
    }
}
