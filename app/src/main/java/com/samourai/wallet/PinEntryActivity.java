package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;

import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.access.ScrambledPin;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;

import java.io.IOException;

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

    private TextView tvPrompt = null;
    private TextView tvUserInput = null;

    private ScrambledPin keypad = null;
    
    private StringBuilder userInput = null;

    private boolean create = false;             // create PIN
    private boolean confirm = false;            // confirm PIN
    private String strConfirm = null;
    private String strSeed = null;
    private String strPassphrase = null;

    private ProgressDialog progress = null;

    private String strUri = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        userInput = new StringBuilder();
        keypad = new ScrambledPin();

        tvUserInput = (TextView)findViewById(R.id.userInput);
        tvUserInput.setText("");

        tvPrompt = (TextView)findViewById(R.id.prompt2);

        boolean scramble = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.SCRAMBLE_PIN, false);

        strUri = PrefsUtil.getInstance(PinEntryActivity.this).getValue("SCHEMED_URI", "");
        if(strUri.length() > 0)    {
            PrefsUtil.getInstance(PinEntryActivity.this).setValue("SCHEMED_URI", "");
        }
        else{
            strUri = null;
        }

        Bundle extras = getIntent().getExtras();

        if(extras != null && extras.containsKey("create") && extras.getBoolean("create") == true)	{
            tvPrompt.setText(R.string.create_pin);
            scramble = false;
            create = true;
            confirm = false;
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8, Toast.LENGTH_LONG).show();
        }
        else if(extras != null && extras.containsKey("confirm") && extras.getBoolean("confirm") == true)	{
            tvPrompt.setText(R.string.confirm_pin);
            scramble = false;
            create = false;
            confirm = true;
            strConfirm = extras.getString("first");
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8_confirm, Toast.LENGTH_LONG).show();
        }
        else	{
            ;
        }

        if(strSeed != null && strSeed.length() < 1)	{
            strSeed = null;
        }

        if(strPassphrase == null)	{
            strPassphrase = "";
        }

        ta = (Button)findViewById(R.id.ta);
        ta.setText(scramble ? Integer.toString(keypad.getMatrix().get(0).getValue()) : "1");
        tb = (Button)findViewById(R.id.tb);
        tb.setText(scramble ? Integer.toString(keypad.getMatrix().get(1).getValue()) : "2");
        tc = (Button)findViewById(R.id.tc);
        tc.setText(scramble ? Integer.toString(keypad.getMatrix().get(2).getValue()) : "3");
        td = (Button)findViewById(R.id.td);
        td.setText(scramble ? Integer.toString(keypad.getMatrix().get(3).getValue()) : "4");
        te = (Button)findViewById(R.id.te);
        te.setText(scramble ? Integer.toString(keypad.getMatrix().get(4).getValue()) : "5");
        tf = (Button)findViewById(R.id.tf);
        tf.setText(scramble ? Integer.toString(keypad.getMatrix().get(5).getValue()) : "6");
        tg = (Button)findViewById(R.id.tg);
        tg.setText(scramble ? Integer.toString(keypad.getMatrix().get(6).getValue()) : "7");
        th = (Button)findViewById(R.id.th);
        th.setText(scramble ? Integer.toString(keypad.getMatrix().get(7).getValue()) : "8");
        ti = (Button)findViewById(R.id.ti);
        ti.setText(scramble ? Integer.toString(keypad.getMatrix().get(8).getValue()) : "9");
        tj = (Button)findViewById(R.id.tj);
        tj.setText(scramble ? Integer.toString(keypad.getMatrix().get(9).getValue()) : "0");
        tsend = (ImageButton)findViewById(R.id.tsend);
        tback = (ImageButton)findViewById(R.id.tback);

        ta.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(ta.getText().toString());
                displayUserInput();
            }
        });

        tb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(tb.getText().toString());
                displayUserInput();
            }
        });

        tc.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(tc.getText().toString());
                displayUserInput();
            }
        });

        td.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(td.getText().toString());
                displayUserInput();
            }
        });

        te.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(te.getText().toString());
                displayUserInput();
            }
        });

        tf.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(tf.getText().toString());
                displayUserInput();
            }
        });

        tg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(tg.getText().toString());
                displayUserInput();
            }
        });

        th.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(th.getText().toString());
                displayUserInput();
            }
        });

        ti.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(ti.getText().toString());
                displayUserInput();
            }
        });

        tj.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                userInput.append(tj.getText().toString());
                displayUserInput();
            }
        });

        tsend.setVisibility(View.INVISIBLE);
        tsend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(create && userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("confirm", true);
                    intent.putExtra("create", false);
                    intent.putExtra("first", userInput.toString());
                    intent.putExtra("seed", strSeed);
                    intent.putExtra("passphrase", strPassphrase);
                    startActivity(intent);
                }
                else if(confirm && userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {

                    if(userInput.toString().equals(strConfirm)) {

                        progress = new ProgressDialog(PinEntryActivity.this);
                        progress.setCancelable(false);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(strSeed == null ? getString(R.string.creating_wallet) :  getString(R.string.restoring_wallet));
                        progress.show();

                        initThread(strSeed == null ? true : false, userInput.toString(), strPassphrase, strSeed == null ? null : strSeed);

                    }
                    else {
                        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("create", true);
                        intent.putExtra("seed", strSeed);
                        intent.putExtra("passphrase", strPassphrase);
                        startActivity(intent);
                    }

                }
                else {
                    if(userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                        validateThread(userInput.toString(), strUri);
                    }
                }

            }
        });

        tback.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                
                if(userInput.toString().length() > 0) {
                    userInput.deleteCharAt(userInput.length() - 1);
                }
                displayUserInput();

            }
        });

    }

    private void displayUserInput() {

        tvUserInput.setText("");

        for(int i = 0; i < userInput.toString().length(); i++) {
            tvUserInput.append("*");
        }

        if(userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
            tsend.setVisibility(View.VISIBLE);
        }
        else {
            tsend.setVisibility(View.INVISIBLE);
        }

    }

    private void validateThread(final String pin, final String uri)	{

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
                        PayloadUtil.getInstance(PinEntryActivity.this).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(PinEntryActivity.this).getGUID() + pin));

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                        }

                        AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                        TimeOutUtil.getInstance().updatePin();
                        if(uri != null)    {
                            Log.i("PinEntryActivity", "uri to restartApp()");
                            AppUtil.getInstance(PinEntryActivity.this).restartApp("uri", uri);
                        }
                        else    {
                            AppUtil.getInstance(PinEntryActivity.this).restartApp();
                        }

                    }
                    catch (MnemonicException.MnemonicLengthException mle) {
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
                    Toast.makeText(PinEntryActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
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

                if(create)	{

                    try {
                        HD_WalletFactory.getInstance(PinEntryActivity.this).newWallet(12, passphrase, SamouraiWallet.NB_ACCOUNTS);
                    }
                    catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                    catch(MnemonicException.MnemonicLengthException mle) {
                        mle.printStackTrace();
                    }
                    finally {
                        ;
                    }

                }
                else if(seed == null)	{
                    ;
                }
                else	{

                    try {
                        HD_WalletFactory.getInstance(PinEntryActivity.this).restoreWallet(seed, passphrase, SamouraiWallet.NB_ACCOUNTS);
                    }
                    catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                    catch(DecoderException de) {
                        de.printStackTrace();
                    }
                    catch(AddressFormatException afe) {
                        afe.printStackTrace();
                    }
                    catch(MnemonicException.MnemonicLengthException mle) {
                        mle.printStackTrace();
                    }
                    catch(MnemonicException.MnemonicChecksumException mce) {
                        mce.printStackTrace();
                    }
                    catch(MnemonicException.MnemonicWordException mwe) {
                        mwe.printStackTrace();
                    }
                    finally {
                        ;
                    }

                }

                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.SCRAMBLE_PIN, true);

                try {

                    String msg = null;

                    if(HD_WalletFactory.getInstance(PinEntryActivity.this).get() != null) {

                        if(create) {
                            msg = getString(R.string.wallet_created_ok);
                        }
                        else {
                            msg = getString(R.string.wallet_restored_ok);
                        }

                        try {
                            PayloadUtil.getInstance(PinEntryActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(PinEntryActivity.this).getGUID() + pin));
                            AccessFactory.getInstance(PinEntryActivity.this).setPIN(pin);

                            if(create) {
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.WALLET_ORIGIN, "new");
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.FIRST_RUN, true);
                            }
                            else {
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.WALLET_ORIGIN, "restored");
                                PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.FIRST_RUN, true);
                            }

                        }
                        catch(JSONException je) {
                            je.printStackTrace();
                        }
                        catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        catch (DecryptionException de) {
                            de.printStackTrace();
                        }
                        finally {
                            ;
                        }

                        for(int i = 0; i < 2; i++) {
                            AddressFactory.getInstance().account2xpub().put(i, HD_WalletFactory.getInstance(PinEntryActivity.this).get().getAccount(i).xpubstr());
                            AddressFactory.getInstance().xpub2account().put(HD_WalletFactory.getInstance(PinEntryActivity.this).get().getAccount(i).xpubstr(), i);
                        }

                        //
                        // backup wallet for alpha
                        //
                        if(create) {

                            String seed = null;
                            try {
                                seed = HD_WalletFactory.getInstance(PinEntryActivity.this).get().getMnemonic();
                            }
                            catch(IOException ioe) {
                                ioe.printStackTrace();
                            }
                            catch(MnemonicException.MnemonicLengthException mle) {
                                mle.printStackTrace();
                            }

                            new AlertDialog.Builder(PinEntryActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(getString(R.string.alpha_create_wallet) + "\n\n" + seed)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.alpha_create_confirm_backup, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                                            TimeOutUtil.getInstance().updatePin();
                                            AppUtil.getInstance(PinEntryActivity.this).restartApp();

                                        }
                                    }).show();

                        }
                        else {
                            AccessFactory.getInstance(PinEntryActivity.this).setIsLoggedIn(true);
                            TimeOutUtil.getInstance().updatePin();
                            AppUtil.getInstance(PinEntryActivity.this).restartApp();
                        }

                    }
                    else {
                        if(create) {
                            msg = getString(R.string.wallet_created_ko);
                        }
                        else {
                            msg = getString(R.string.wallet_restored_ko);
                        }
                    }

                    Toast.makeText(PinEntryActivity.this, msg, Toast.LENGTH_SHORT).show();

                }
                catch(IOException ioe) {
                    ioe.printStackTrace();
                }
                catch(MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                }
                finally {
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

}
