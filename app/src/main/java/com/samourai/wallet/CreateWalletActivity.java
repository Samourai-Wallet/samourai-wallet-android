package com.samourai.wallet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.fragments.PassphraseEntryFragment;
import com.samourai.wallet.fragments.PinEntryFragment;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.widgets.ViewPager;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;

import java.io.IOException;

import static com.samourai.wallet.R.id.dots;


public class CreateWalletActivity extends FragmentActivity implements
        PinEntryFragment.onPinEntryListener,
        PassphraseEntryFragment.onPassPhraseListener {
    private ViewPager wallet_create_viewpager;

    private PagerAdapter adapter;
    private LinearLayout pagerIndicatorContainer;
    private LinearLayout forwardButton;
    private ImageView[] indicators;
    private String passPhrase39 = null;
    private String passPhraseConfirm = "";
    private boolean checkedDisclaimer = false;
    private String pinCode = "", pinCodeConfirm = "";
    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);
        wallet_create_viewpager = (ViewPager) findViewById(R.id.wallet_create_viewpager);
        pagerIndicatorContainer = (LinearLayout) findViewById(dots);
        forwardButton = (LinearLayout) findViewById(R.id.wizard_forward);
        if (getActionBar() != null)
            getActionBar().hide();
        adapter = new PagerAdapter(getSupportFragmentManager());
        wallet_create_viewpager.enableSwipe(false);
        wallet_create_viewpager.setAdapter(adapter);
        wallet_create_viewpager.setCurrentItem(0);
        setPagerIndicators();
    }

    /**
     * Creates pager indicator dynamically using number of fragments present in the adapter
     */
    private void setPagerIndicators() {
        indicators = new ImageView[adapter.getCount()];
        //Creating circle dot ImageView based on adapter size
        for (int i = 0; i < adapter.getCount(); i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(getResources().getDrawable(R.drawable.pager_indicator_dot));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            pagerIndicatorContainer.addView(indicators[i], params);
        }
        //Setting first ImageView as active indicator
        indicators[0].setImageDrawable(getResources().getDrawable(R.drawable.pager_indicator_dot));
        indicators[0].getDrawable().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.ADD);
        // Viewpager listener is responsible for changing indicator color
        wallet_create_viewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    indicators[i].setImageDrawable(getResources().getDrawable(R.drawable.pager_indicator_dot));
                }
                // here we using PorterDuff mode to overlay color over ImageView to set Active indicator
                // we don't have to create multiple asset for showing active and inactive states of indicators
                indicators[position].getDrawable().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.ADD);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    /**
     * Accepts Forward navigation clicks
     *
     * @param view
     */
    public void wizardNavigationForward(View view) {
        int count = wallet_create_viewpager.getCurrentItem();
        switch (count) {
            case 0: {
                if (!(passPhraseConfirm.equals(passPhrase39))) {
                    Toast.makeText(this, R.string.bip39_unmatch, Toast.LENGTH_SHORT).show();
                    break;
                }
                if (passPhrase39.contains(" ")) {
                    Toast.makeText(this, R.string.bip39_invalid, Toast.LENGTH_SHORT).show();
                    break;
                }
                if (passPhrase39.length() == 0) {
                    Toast.makeText(this, R.string.bip39_empty, Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!checkedDisclaimer) {
                    Toast.makeText(this, R.string.accept_disclaimer_error, Toast.LENGTH_SHORT).show();
                    break;
                }
                wallet_create_viewpager.setCurrentItem(count + 1);
                if (pinCode.isEmpty()) {
                    setForwardButtonEnable(false);
                }
                if (this.getCurrentFocus() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                break;
            }
            case 1: {
                if (pinCode.length() >= 5) {
                    wallet_create_viewpager.setCurrentItem(count + 1);
                    setForwardButtonEnable(false);
                } else {
                    Toast.makeText(this, R.string.pin_5_8, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case 2: {
                initThread(true, pinCode, passPhrase39, null);
            }

        }
    }

    /**
     * Accepts backward navigation clicks
     *
     * @param view
     */
    public void wizardNavigationBackward(View view) {
        int count = wallet_create_viewpager.getCurrentItem();
        switch (count) {
            case 0: {
                finish();
                break;
            }
            case 1: {
                wallet_create_viewpager.setCurrentItem(count - 1);
            }
            case 2: {
                ((TextView) forwardButton.getChildAt(0)).setText(R.string.next);
                wallet_create_viewpager.setCurrentItem(count - 1);
            }
        }
    }

    /**
     * Helper method to enable and disable forward navigation button
     *
     * @param enable
     */
    private void setForwardButtonEnable(boolean enable) {
        forwardButton.setClickable(enable);
        forwardButton.setAlpha(enable ? 1 : 0.2f);
    }

    /**
     * @param passPhrase
     * @param confirm
     * @param checked
     */
    @Override
    public void passPhraseSet(String passPhrase, String confirm, boolean checked) {
        passPhrase39 = passPhrase;
        passPhraseConfirm = confirm;
        checkedDisclaimer = checked;
    }

    /**
     * Callback method for receiving pin code from child fragment
     * pin entry and confirm entry fragment invoke this method
     * based on current active viewpager item we will set pin code
     *
     * @param pin
     */
    @Override
    public void PinEntry(String pin) {
        if (wallet_create_viewpager.getCurrentItem() == 1) {
            pinCode = pin;
            if (pinCode.length() >= AccessFactory.MIN_PIN_LENGTH && pinCode.length() <= AccessFactory.MAX_PIN_LENGTH) {
                setForwardButtonEnable(true);
            } else {
                setForwardButtonEnable(false);
            }
        } else {
            pinCodeConfirm = pin;
            if (pinCodeConfirm.equals(pinCode)) {
                setForwardButtonEnable(true);
                ((TextView) forwardButton.getChildAt(0)).setText(R.string.finish);
            }
        }
    }

    /**
     * Pager adapter for viewpager
     */
    private class PagerAdapter extends FragmentPagerAdapter {

        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: {
                    return new PassphraseEntryFragment();
                }
                case 1: {
                    return new PinEntryFragment();
                }
                case 2: {
                    return PinEntryFragment
                            .newInstance(getString(R.string.pin_5_8_confirm), getString(R.string.re_enter_your_pin_code));
                }
                default: {
                    return null;
                }
            }

        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        super.onDetachedFromWindow();
    }

    private void toggleLoading() {
        if (progressDialog == null) {
            Activity activity = this;
            if ( activity.isFinishing() ) {
                return;
            }
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

    /**
     * Creates new wallet account
     *
     * @param create
     * @param pin
     * @param passphrase
     * @param seed
     */
    private void initThread(final boolean create, final String pin, final String passphrase, final String seed) {
        toggleLoading();
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                String guid = AccessFactory.getInstance(CreateWalletActivity.this).createGUID();
                String hash = AccessFactory.getInstance(CreateWalletActivity.this).getHash(guid, new CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations);
                PrefsUtil.getInstance(CreateWalletActivity.this).setValue(PrefsUtil.ACCESS_HASH, hash);
                PrefsUtil.getInstance(CreateWalletActivity.this).setValue(PrefsUtil.ACCESS_HASH2, hash);

                if (create) {

                    try {
                        HD_WalletFactory.getInstance(CreateWalletActivity.this).newWallet(12, passphrase, SamouraiWallet.NB_ACCOUNTS);
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
                        HD_WalletFactory.getInstance(CreateWalletActivity.this).restoreWallet(seed, passphrase, SamouraiWallet.NB_ACCOUNTS);
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

                PrefsUtil.getInstance(CreateWalletActivity.this).setValue(PrefsUtil.SCRAMBLE_PIN, true);

                try {

                    String msg = null;

                    if (HD_WalletFactory.getInstance(CreateWalletActivity.this).get() != null) {

                        if (create) {
                            msg = getString(R.string.wallet_created_ok);
                        } else {
                            msg = getString(R.string.wallet_restored_ok);
                        }

                        try {
                            AccessFactory.getInstance(CreateWalletActivity.this).setPIN(pin);
                            PayloadUtil.getInstance(CreateWalletActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(CreateWalletActivity.this).getGUID() + pin));

                            if (create) {
                                PrefsUtil.getInstance(CreateWalletActivity.this).setValue(PrefsUtil.WALLET_ORIGIN, "new");
                                PrefsUtil.getInstance(CreateWalletActivity.this).setValue(PrefsUtil.FIRST_RUN, true);
                            } else {
                                PrefsUtil.getInstance(CreateWalletActivity.this).setValue(PrefsUtil.WALLET_ORIGIN, "restored");
                                PrefsUtil.getInstance(CreateWalletActivity.this).setValue(PrefsUtil.FIRST_RUN, true);
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
                            AddressFactory.getInstance().account2xpub().put(i, HD_WalletFactory.getInstance(CreateWalletActivity.this).get().getAccount(i).xpubstr());
                            AddressFactory.getInstance().xpub2account().put(HD_WalletFactory.getInstance(CreateWalletActivity.this).get().getAccount(i).xpubstr(), i);
                        }

                        //
                        // backup wallet for alpha
                        //
                        if (create) {

                            String seed = null;
                            try {
                                seed = HD_WalletFactory.getInstance(CreateWalletActivity.this).get().getMnemonic();
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            } catch (MnemonicException.MnemonicLengthException mle) {
                                mle.printStackTrace();
                            }

                            Intent intent = new Intent(CreateWalletActivity.this,  RecoveryWordsActivity.class);
                            intent.putExtra("BIP39_WORD_LIST",seed);
                            startActivity(intent);
                            finish();

                        } else {
                            AccessFactory.getInstance(CreateWalletActivity.this).setIsLoggedIn(true);
                            TimeOutUtil.getInstance().updatePin();
                            AppUtil.getInstance(CreateWalletActivity.this).restartApp();
                        }

                    } else {
                        if (create) {
                            msg = getString(R.string.wallet_created_ko);
                        } else {
                            msg = getString(R.string.wallet_restored_ko);
                        }
                    }

                    Toast.makeText(CreateWalletActivity.this, msg, Toast.LENGTH_SHORT).show();

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } finally {
                    ;
                }
                toggleLoading();

                Looper.loop();

            }
        }).start();

    }


}


