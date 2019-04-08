package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.Group;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.boltzmann.beans.BoltzmannSettings;
import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.linker.TxosLinkerOptionEnum;
import com.samourai.boltzmann.processor.TxProcessor;
import com.samourai.boltzmann.processor.TxProcessorResult;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.util.CahootsUtil;
import com.samourai.wallet.fragments.PaynymSelectModalFragment;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.ricochet.RicochetActivity;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.SpendUtil;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.widgets.EntropyBar;
import com.samourai.wallet.widgets.SendTransactionDetailsView;
import com.samourai.wallet.widgets.EntropyBar;
import com.samourai.wallet.widgets.SendTransactionDetailsView;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.tx.TxDetailsActivity;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.util.WebUtil;
import com.yanzhenjie.zbar.Symbol;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SendActivity extends AppCompatActivity {

    private final static int SCAN_QR = 2012;
    private final static int RICOCHET = 2013;
    private static final String TAG = "SendActivity";

    private SendTransactionDetailsView sendTransactionDetailsView;
    private ViewSwitcher amountViewSwitcher;
    private EditText toAddressEditText, btcEditText, satEditText;
    private TextView tvMaxAmount, tvReviewSpendAmount, tvTotalFee, tvToAddress, tvEstimatedBlockWait, tvSelectedFeeRate, tvSelectedFeeRateLayman, stoneWallDesc, stonewallOptionText, ricochetTitle, ricochetDesc, entropyValue;
    private Button btnReview, btnSend;
    private Switch ricochetHopsSwitch, ricochetStaggeredDelivery, stoneWallSwitch;
    private SeekBar feeSeekBar;
    private EntropyBar entropyBar;
    private Group ricochetStaggeredOptionGroup;

    private long balance = 0L;
    private String strDestinationBTCAddress = null;

    private final static int FEE_LOW = 0;
    private final static int FEE_NORMAL = 1;
    private final static int FEE_PRIORITY = 2;
    private final static int FEE_CUSTOM = 3;
    private int FEE_TYPE = FEE_LOW;

    public final static int SPEND_SIMPLE = 0;
    public final static int SPEND_BOLTZMANN = 1;
    public final static int SPEND_RICOCHET = 2;
    private int SPEND_TYPE = SPEND_BOLTZMANN;

    private String strPCode = null;
    private long feeLow, feeMed, feeHigh;
    private String strPrivacyWarning;
    private ArrayList<UTXO> selectedUTXO;
    private long _change;
    private HashMap<String, BigInteger> receivers;
    private int changeType;
    private String address;
    private String message;
    private long amount;
    private int change_index;
    private String ricochetMessage;
    private JSONObject ricochetJsonObj = null;

    private int idxBIP44Internal = 0;
    private int idxBIP49Internal = 0;
    private int idxBIP84Internal = 0;

    //stub address for entropy calculation
    private String[] stubAddress = {"1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", "12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX", "1HLoD9E4SDFFPDiYfNYnkBLQ85Y51J3Zb1", "1FvzCLoTPGANNjWoUo6jUGuAG3wg1w4YjR", "15ubicBBWFnvoZLT7GiU2qxjRaKJPdkDMG", "1JfbZRwdDHKZmuiZgYArJZhcuuzuw2HuMu", "1GkQmKAmHtNfnD3LHhTkewJxKHVSta4m2a", "16LoW7y83wtawMg5XmT4M3Q7EdjjUmenjM", "1J6PYEzr4CUoGbnXrELyHszoTSz3wCsCaj", "12cbQLTFMXRnSzktFkuoG3eHoMeFtpTu3S", "15yN7NPEpu82sHhB6TzCW5z5aXoamiKeGy ", "1dyoBoF5vDmPCxwSsUZbbYhA5qjAfBTx9", "1PYELM7jXHy5HhatbXGXfRpGrgMMxmpobu", "17abzUBJr7cnqfnxnmznn8W38s9f9EoXiq", "1DMGtVnRrgZaji7C9noZS3a1QtoaAN2uRG", "1CYG7y3fukVLdobqgUtbknwWKUZ5p1HVmV", "16kktFTqsruEfPPphW4YgjktRF28iT8Dby", "1LPBetDzQ3cYwqQepg4teFwR7FnR1TkMCM", "1DJkjSqW9cX9XWdU71WX3Aw6s6Mk4C3TtN", "1P9VmZogiic8d5ZUVZofrdtzXgtpbG9fop", "15ubjFzmWVvj3TqcpJ1bSsb8joJ6gF6dZa"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        setSupportActionBar(findViewById(R.id.toolbar_send));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle("");
        //CustomView for showing and hiding body of th UI
        sendTransactionDetailsView = findViewById(R.id.sendTransactionDetailsView);

        //ViewSwitcher Element for toolbar section of the UI.
        //we can switch between Form and review screen with this element
        amountViewSwitcher = findViewById(R.id.toolbar_view_switcher);

        //Input elements from toolbar section of the UI
        toAddressEditText = findViewById(R.id.edt_send_to);
        btcEditText = findViewById(R.id.amountBTC);
        satEditText = findViewById(R.id.amountSat);
        tvToAddress = findViewById(R.id.to_address_review);
        tvReviewSpendAmount = findViewById(R.id.send_review_amount);
        tvMaxAmount = findViewById(R.id.totalBTC);


        //view elements from review segment and transaction segment can be access through respective
        //methods which returns root viewGroup
        entropyBar = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.entropyBar);
        btnReview = sendTransactionDetailsView.getTransactionView().findViewById(R.id.review_button);
        ricochetHopsSwitch = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_hops_switch);
        ricochetTitle = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_desc);
        ricochetDesc = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_title);
        ricochetStaggeredDelivery = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_staggered_option);
        ricochetStaggeredOptionGroup = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_staggered_option_group);
        tvSelectedFeeRate = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.selected_fee_rate);
        tvSelectedFeeRateLayman = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.selected_fee_rate_in_layman);
        tvTotalFee = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.total_fee);
        btnSend = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.send_btn);
        feeSeekBar = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.fee_seekbar);
        tvEstimatedBlockWait = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.est_block_time);
        feeSeekBar = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.fee_seekbar);
        stoneWallSwitch = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.stone_wall_radio_btn);
        stonewallOptionText = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.textView_stonewall);
        stoneWallDesc = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.stonewall_desc);
        entropyValue = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.entropy_value);
        entropyBar = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.entropy_bar);
        entropyBar.setMaxBars(4);

        btcEditText.addTextChangedListener(BTCWatcher);
        satEditText.addTextChangedListener(satWatcher);
        toAddressEditText.addTextChangedListener(AddressWatcher);

        btnReview.setOnClickListener(v -> review());
        btnSend.setOnClickListener(v -> initiateSpend());

        View.OnClickListener clipboardCopy = view -> {
            ClipboardManager cm = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = android.content.ClipData
                    .newPlainText("Miner fee", tvTotalFee.getText());
            if (cm != null) {
                cm.setPrimaryClip(clipData);
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
            }
        };

        tvTotalFee.setOnClickListener(clipboardCopy);
        tvSelectedFeeRate.setOnClickListener(clipboardCopy);

        SPEND_TYPE = SPEND_BOLTZMANN;

        saveChangeIndexes();

        setUpRicochet();

        setUpFee();

        setBalance();

        enableReviewButton(false);

        setUpBoltzman();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
//            bViaMenu = extras.getBoolean("via_menu", false);
            String strUri = extras.getString("uri");
            strPCode = extras.getString("pcode");

            if (strUri != null && strUri.length() > 0) {
                processScan(strUri);
            }
            if (extras.containsKey("amount")) {
                btcEditText.setText(String.valueOf(getBtcValue(extras.getDouble("amount"))));
            }
            if (strPCode != null && strPCode.length() > 0) {
                processPCode(strPCode, null);
            }
        }
        validateSpend();

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(SendActivity.this).setIsInForeground(true);

        AppUtil.getInstance(SendActivity.this).checkTimeOut();

    }

    private void setUpBoltzman() {
        stonewallOptionText.setAlpha(1f);
        stoneWallSwitch.setAlpha(1f);
        stoneWallDesc.setAlpha(1f);
        stoneWallSwitch.setChecked(true);
        stoneWallSwitch.setEnabled(true);
        stoneWallSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            SPEND_TYPE = checked ? SPEND_BOLTZMANN : SPEND_SIMPLE;
            //small delay for storing prefs.
            new Handler().postDelayed(() -> prepareSpend(), 100);
        });
    }

    private void checkRicochetPossibility() {
        double btc_amount = 0.0;

        try {
            btc_amount = NumberFormat.getInstance(Locale.US).parse(btcEditText.getText().toString().trim()).doubleValue();
//                    Log.i("SendFragment", "amount entered:" + btc_amount);
        } catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        } catch (ParseException pe) {
            btc_amount = 0.0;
            return;
        }

        double dAmount = btc_amount;

        amount = Math.round(dAmount * 1e8);
        if (amount < (balance - (RicochetMeta.samouraiFeeAmountV2.add(BigInteger.valueOf(50000L))).longValue())) {
            ricochetDesc.setAlpha(1f);
            ricochetTitle.setAlpha(1f);
            ricochetHopsSwitch.setAlpha(1f);
            ricochetHopsSwitch.setEnabled(true);
            if (ricochetHopsSwitch.isChecked()) {
                ricochetStaggeredOptionGroup.setVisibility(View.VISIBLE);
            }
        } else {
            ricochetStaggeredOptionGroup.setVisibility(View.GONE);
            ricochetDesc.setAlpha(.6f);
            ricochetTitle.setAlpha(.6f);
            ricochetHopsSwitch.setAlpha(.6f);
            ricochetHopsSwitch.setEnabled(false);

        }
    }

    private void enableReviewButton(boolean enable) {
        btnReview.setEnabled(enable);
        if (enable) {
            btnReview.setBackground(getDrawable(R.drawable.button_blue));
        } else {
            btnReview.setBackground(getDrawable(R.drawable.disabled_grey_button));
        }
    }

    private void setUpFee() {


        int multiplier = 10000;

        FEE_TYPE = PrefsUtil.getInstance(this).getValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL);


        feeLow = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
        feeMed = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
        feeHigh = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

        float high = ((float) feeHigh / 2) + (float) feeHigh;
        int feeHighSliderValue = (int) (high * multiplier);
        int feeMedSliderValue = (int) (feeMed * multiplier);


        feeSeekBar.setMax(feeHighSliderValue - multiplier);

        if (feeLow == feeMed && feeMed == feeHigh) {
            feeLow = (long) ((double) feeMed * 0.85);
            feeHigh = (long) ((double) feeMed * 1.15);
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        } else if (feeLow == feeMed || feeMed == feeMed) {
            feeMed = (feeLow + feeHigh) / 2L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        } else {
            ;
        }

        if (feeLow < 1L) {
            feeLow = 1L;
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
        }
        if (feeMed < 1L) {
            feeMed = 1L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        if (feeHigh < 1L) {
            feeHigh = 1L;
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }
//        tvEstimatedBlockWait.setText("6 blocks");
        tvSelectedFeeRateLayman.setText(getString(R.string.normal));

        FeeUtil.getInstance().sanitizeFee();

        tvSelectedFeeRate.setText((String.valueOf((int) feeMed).concat(" sats/b")));

        feeSeekBar.setProgress((feeMedSliderValue - multiplier) + 1);
        DecimalFormat decimalFormat = new DecimalFormat("##.00");
        setFeeLabels();
        feeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                double value = ((double) i + multiplier) / (double) multiplier;

                tvSelectedFeeRate.setText(String.valueOf(decimalFormat.format(value)).concat(" sats/b"));
                if (value == 0.0) {
                    value = 1.0;
                }
                double pct = 0.0;
                int nbBlocks = 6;
                if (value <= (double) feeLow) {
                    pct = ((double) feeLow / value);
                    nbBlocks = ((Double) Math.ceil(pct * 24.0)).intValue();
                } else if (value >= (double) feeHigh) {
                    pct = ((double) feeHigh / value);
                    nbBlocks = ((Double) Math.ceil(pct * 2.0)).intValue();
                    if (nbBlocks < 1) {
                        nbBlocks = 1;
                    }
                } else {
                    pct = ((double) feeMed / value);
                    nbBlocks = ((Double) Math.ceil(pct * 6.0)).intValue();
                }
                tvEstimatedBlockWait.setText(nbBlocks + " blocks");
                setFee(value);
                setFeeLabels();

                restoreChangeIndexes();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                restoreChangeIndexes();
            }
        });


        switch (FEE_TYPE) {
            case FEE_LOW:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getLowFee());
                FeeUtil.getInstance().sanitizeFee();
                break;
            case FEE_PRIORITY:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                FeeUtil.getInstance().sanitizeFee();
                break;
            default:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                FeeUtil.getInstance().sanitizeFee();
                break;
        }

    }

    private void setFeeLabels() {
        float sliderValue = (((float) feeSeekBar.getProgress()) / feeSeekBar.getMax());

        float sliderInPercentage = sliderValue * 100;

        if (sliderInPercentage < 33) {
            tvSelectedFeeRateLayman.setText(R.string.low);
        } else if (sliderInPercentage > 33 && sliderInPercentage < 66) {
            tvSelectedFeeRateLayman.setText(R.string.normal);
        } else if (sliderInPercentage > 66) {
            tvSelectedFeeRateLayman.setText(R.string.urgent);

        }
    }

    private void setFee(double fee) {

        double sanitySat = FeeUtil.getInstance().getHighFee().getDefaultPerKB().doubleValue() / 1000.0;
        final long sanityValue;
        if (sanitySat < 10.0) {
            sanityValue = 15L;
        } else {
            sanityValue = (long) (sanitySat * 1.5);
        }

        //        String val  = null;
        double d = FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().doubleValue() / 1000.0;
        NumberFormat decFormat = NumberFormat.getInstance(Locale.US);
        decFormat.setMaximumFractionDigits(3);
        decFormat.setMinimumFractionDigits(0);
        double customValue = 0.0;

        if (PrefsUtil.getInstance(this).getValue(PrefsUtil.USE_TRUSTED_NODE, false)) {
            customValue = 0.0;
        } else {

            try {
                customValue = (double) fee;
            } catch (Exception e) {
                Toast.makeText(this, R.string.custom_fee_too_low, Toast.LENGTH_SHORT).show();
                return;
            }

        }
        SuggestedFee suggestedFee = new SuggestedFee();
        suggestedFee.setStressed(false);
        suggestedFee.setOK(true);
        suggestedFee.setDefaultPerKB(BigInteger.valueOf((long) (customValue * 1000.0)));
        FeeUtil.getInstance().setSuggestedFee(suggestedFee);
        prepareSpend();

    }

    private void setUpRicochet() {
        ricochetHopsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ricochetStaggeredOptionGroup.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                SPEND_TYPE = SPEND_RICOCHET;
                PrefsUtil.getInstance(this).setValue(PrefsUtil.USE_RICOCHET, true);
            } else {
                SPEND_TYPE = stoneWallSwitch.isChecked() ? SPEND_BOLTZMANN : SPEND_SIMPLE;
                PrefsUtil.getInstance(this).setValue(PrefsUtil.USE_RICOCHET, false);
            }

            if (isChecked) {
                ricochetStaggeredOptionGroup.setVisibility(View.VISIBLE);
            } else {
                ricochetStaggeredOptionGroup.setVisibility(View.GONE);
            }
        });
        ricochetHopsSwitch.setChecked(PrefsUtil.getInstance(this).getValue(PrefsUtil.USE_RICOCHET, false));

        if (ricochetHopsSwitch.isChecked()) {
            ricochetStaggeredOptionGroup.setVisibility(View.VISIBLE);
        } else {
            ricochetStaggeredOptionGroup.setVisibility(View.GONE);

        }
        ricochetStaggeredDelivery.setChecked(PrefsUtil.getInstance(this).getValue(PrefsUtil.RICOCHET_STAGGERED, false));

        ricochetStaggeredDelivery.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            PrefsUtil.getInstance(this).setValue(PrefsUtil.RICOCHET_STAGGERED, isChecked);

            // Handle staggered delivery option

        });
    }

    private void setBalance() {

        try {
            balance = APIFactory.getInstance(SendActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).xpubstr());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            balance = 0L;
        } catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            balance = 0L;
        } catch (java.lang.NullPointerException npe) {
            npe.printStackTrace();
            balance = 0L;
        }
        final String strAmount;
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
        nf.setMinimumIntegerDigits(1);

        strAmount = nf.format(balance / 1e8);

        tvMaxAmount.setOnClickListener(view -> {
            btcEditText.setText(strAmount);
        });

        tvMaxAmount.setText(strAmount + " " + getDisplayUnits());

    }

    private TextWatcher BTCWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            satEditText.removeTextChangedListener(satWatcher);
            btcEditText.removeTextChangedListener(this);

            try {
                if (editable.toString().length() == 0) {
                    satEditText.setText("0");
                    btcEditText.setText("");
                    satEditText.setSelection(satEditText.getText().length());
                    satEditText.addTextChangedListener(satWatcher);
                    btcEditText.addTextChangedListener(this);
                    return;
                }

                Double btc = Double.parseDouble(String.valueOf(editable));

                if (btc > 21000000.0) {
                    btcEditText.setText("0.00");
                    btcEditText.setSelection(btcEditText.getText().length());
                    satEditText.setText("0");
                    satEditText.setSelection(satEditText.getText().length());
                    Toast.makeText(SendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                } else {
                    DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
                    DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
                    String defaultSeparator = Character.toString(symbols.getDecimalSeparator());
                    int max_len = 8;
                    NumberFormat btcFormat = NumberFormat.getInstance(Locale.US);
                    btcFormat.setMaximumFractionDigits(max_len + 1);

                    try {
                        double d = NumberFormat.getInstance(Locale.US).parse(editable.toString()).doubleValue();
                        String s1 = btcFormat.format(d);
                        if (s1.indexOf(defaultSeparator) != -1) {
                            String dec = s1.substring(s1.indexOf(defaultSeparator));
                            if (dec.length() > 0) {
                                dec = dec.substring(1);
                                if (dec.length() > max_len) {
                                    btcEditText.setText(s1.substring(0, s1.length() - 1));
                                    btcEditText.setSelection(btcEditText.getText().length());
                                    editable = btcEditText.getEditableText();
                                    btc = Double.parseDouble(btcEditText.getText().toString());
                                }
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        ;
                    } catch (ParseException pe) {
                        ;
                    }

                    Double sats = getSatValue(Double.valueOf(btc));
                    satEditText.setText(formattedSatValue(sats));
                    checkRicochetPossibility();
                }

//
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            satEditText.addTextChangedListener(satWatcher);
            btcEditText.addTextChangedListener(this);
            validateSpend();


        }
    };

    private TextWatcher AddressWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {

            if(editable.toString().equalsIgnoreCase("Stowaway"))    {
                doStowaway();
            }
            else if(editable.toString().equalsIgnoreCase("STONEWALLx2"))    {
                doSTONEWALLx2();
            }
            else    {
                validateSpend();
            }

        }
    };

    private String formattedSatValue(Object number) {
        NumberFormat nformat = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat decimalFormat = (DecimalFormat) nformat;
        decimalFormat.applyPattern("#,###");
        return decimalFormat.format(number).replace(",", " ");
    }

    private TextWatcher satWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            satEditText.removeTextChangedListener(this);
            btcEditText.removeTextChangedListener(BTCWatcher);

            try {
                if (editable.toString().length() == 0) {
                    btcEditText.setText("0.00");
                    satEditText.setText("");
                    satEditText.addTextChangedListener(this);
                    btcEditText.addTextChangedListener(BTCWatcher);
                    return;
                }
                String cleared_space = editable.toString().replace(" ", "");

                Double sats = Double.parseDouble(cleared_space);
                Double btc = getBtcValue(sats);
                String formatted = formattedSatValue(sats);


                satEditText.setText(formatted);
                satEditText.setSelection(formatted.length());
                btcEditText.setText(String.format(Locale.ENGLISH, "%.8f", btc));
                if (btc > 21000000.0) {
                    btcEditText.setText("0.00");
                    btcEditText.setSelection(btcEditText.getText().length());
                    satEditText.setText("0");
                    satEditText.setSelection(satEditText.getText().length());
                    Toast.makeText(SendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();

            }
            satEditText.addTextChangedListener(this);
            btcEditText.addTextChangedListener(BTCWatcher);
            checkRicochetPossibility();
            validateSpend();

        }
    };

    private void setToAddress(String string) {
        tvToAddress.setText(string);
        toAddressEditText.removeTextChangedListener(AddressWatcher);
        toAddressEditText.setText(string);
        toAddressEditText.setSelection(toAddressEditText.getText().length());
        toAddressEditText.addTextChangedListener(AddressWatcher);
    }

    private String getToAddress() {
        if (toAddressEditText.getText().toString().trim().length() != 0) {
            return toAddressEditText.getText().toString();
        }
        if (tvToAddress.getText().toString().length() != 0) {
            return tvToAddress.getText().toString();
        }
        return "";
    }

    private Double getBtcValue(Double sats) {
        return (double) (sats / 1e8);
    }

    private Double getSatValue(Double btc) {
        if (btc == 0) {
            return (double) 0;
        }
        return btc * 1e8;
    }

    private void review() {

        setUpBoltzman();
        if (validateSpend() && prepareSpend()) {
            tvReviewSpendAmount.setText(btcEditText.getText().toString().concat(" BTC"));
            amountViewSwitcher.showNext();
            entropyBar.setRange(3);
            hideKeyboard();
            sendTransactionDetailsView.showReview(ricochetHopsSwitch.isChecked());

        }

    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(amountViewSwitcher.getWindowToken(), 0);
        }
    }

    private boolean prepareSpend() {

        restoreChangeIndexes();

        double btc_amount = 0.0;

        try {
            btc_amount = NumberFormat.getInstance(Locale.US).parse(btcEditText.getText().toString().trim()).doubleValue();
//                    Log.i("SendFragment", "amount entered:" + btc_amount);
        } catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        } catch (ParseException pe) {
            btc_amount = 0.0;
        }

        double dAmount = btc_amount;

        amount = (long) (Math.round(dAmount * 1e8));
        //                Log.i("SendActivity", "amount:" + amount);

        address = strDestinationBTCAddress == null ? toAddressEditText.getText().toString().trim() : strDestinationBTCAddress;

        if(PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false) {
            changeType = 84;
        }
        else if (FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            changeType = FormatsUtil.getInstance().isValidBech32(address) ? 84 : 49;
        }
        else {
            changeType = 44;
        }

        receivers = new HashMap<String, BigInteger>();
        receivers.put(address, BigInteger.valueOf(amount));

        if (changeType == 84) {
            change_index = idxBIP84Internal;
        } else if (changeType == 49) {
            change_index = idxBIP49Internal;
        } else {
            change_index = idxBIP44Internal;
        }

        // if possible, get UTXO by input 'type': p2pkh, p2sh-p2wpkh or p2wpkh, else get all UTXO
        long neededAmount = 0L;
        if (FormatsUtil.getInstance().isValidBech32(address)) {
            neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(0, 0, UTXOFactory.getInstance().getCountP2WPKH(), 4).longValue();
//                    Log.d("SendActivity", "segwit:" + neededAmount);
        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(0, UTXOFactory.getInstance().getCountP2SH_P2WPKH(), 0, 4).longValue();
//                    Log.d("SendActivity", "segwit:" + neededAmount);
        } else {
            neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(UTXOFactory.getInstance().getCountP2PKH(), 0, 4).longValue();
//                    Log.d("SendActivity", "p2pkh:" + neededAmount);
        }
        neededAmount += amount;
        neededAmount += SamouraiWallet.bDust.longValue();

        // get all UTXO
        List<UTXO> utxos = SpendUtil.getUTXOS(SendActivity.this, address, neededAmount);

        List<UTXO> utxosP2WPKH = new ArrayList<UTXO>(UTXOFactory.getInstance().getP2WPKH().values());
        List<UTXO> utxosP2SH_P2WPKH = new ArrayList<UTXO>(UTXOFactory.getInstance().getP2SH_P2WPKH().values());
        List<UTXO> utxosP2PKH = new ArrayList<UTXO>(UTXOFactory.getInstance().getP2PKH().values());

        selectedUTXO = new ArrayList<UTXO>();
        long totalValueSelected = 0L;
        long change = 0L;
        BigInteger fee = null;
        boolean canDoBoltzmann = true;

//                Log.d("SendActivity", "amount:" + amount);
//                Log.d("SendActivity", "balance:" + balance);

        // insufficient funds
        if (amount > balance) {
            Toast.makeText(SendActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();

        }
        // entire balance (can only be simple spend)
        else if (amount == balance) {
            // make sure we are using simple spend
            SPEND_TYPE = SPEND_SIMPLE;
            canDoBoltzmann = false;

//                    Log.d("SendActivity", "amount == balance");
            // take all utxos, deduct fee
            selectedUTXO.addAll(utxos);

            for (UTXO u : selectedUTXO) {
                totalValueSelected += u.getValue();
            }

//                    Log.d("SendActivity", "balance:" + balance);
//                    Log.d("SendActivity", "total value selected:" + totalValueSelected);

        } else {
            ;
        }

        org.apache.commons.lang3.tuple.Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> pair = null;
        if (SPEND_TYPE == SPEND_RICOCHET) {

            boolean samouraiFeeViaBIP47 = false;
            if (BIP47Meta.getInstance().getOutgoingStatus(BIP47Meta.strSamouraiDonationPCode) == BIP47Meta.STATUS_SENT_CFM) {
                samouraiFeeViaBIP47 = true;
            }

            ricochetJsonObj = RicochetMeta.getInstance(SendActivity.this).script(amount, FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue(), address, 4, strPCode, samouraiFeeViaBIP47, ricochetStaggeredDelivery.isChecked());
            if (ricochetJsonObj != null) {

                try {
                    long totalAmount = ricochetJsonObj.getLong("total_spend");
                    if (totalAmount > balance) {
                        Toast.makeText(SendActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                        ricochetHopsSwitch.setChecked(false);
                        return false;
                    }
                    long hop0Fee = ricochetJsonObj.getJSONArray("hops").getJSONObject(0).getLong("fee");
                    long perHopFee = ricochetJsonObj.getJSONArray("hops").getJSONObject(0).getLong("fee_per_hop");

                    long ricochetFee = hop0Fee + (RicochetMeta.defaultNbHops * perHopFee);

                    tvTotalFee.setText(Coin.valueOf(ricochetFee).toPlainString().concat(" BTC"));

                    ricochetMessage = getText(R.string.ricochet_spend1) + " " + address + " " + getText(R.string.ricochet_spend2) + " " + Coin.valueOf(totalAmount).toPlainString() + " " + getText(R.string.ricochet_spend3);

                    btnSend.setText("send ".concat(String.format(Locale.ENGLISH, "%.8f", getBtcValue((double) totalAmount))).concat(" BTC"));

                    return true;

                } catch (JSONException je) {
                    return false;
                }

            }

            return true;
        } else if (SPEND_TYPE == SPEND_BOLTZMANN) {

            Log.d("SendActivity", "needed amount:" + neededAmount);

            List<UTXO> _utxos1 = null;
            List<UTXO> _utxos2 = null;

            long valueP2WPKH = UTXOFactory.getInstance().getTotalP2WPKH();
            long valueP2SH_P2WPKH = UTXOFactory.getInstance().getTotalP2SH_P2WPKH();
            long valueP2PKH = UTXOFactory.getInstance().getTotalP2PKH();

            Log.d("SendActivity", "value P2WPKH:" + valueP2WPKH);
            Log.d("SendActivity", "value P2SH_P2WPKH:" + valueP2SH_P2WPKH);
            Log.d("SendActivity", "value P2PKH:" + valueP2PKH);

            boolean selectedP2WPKH = false;
            boolean selectedP2SH_P2WPKH = false;
            boolean selectedP2PKH = false;

            if ((valueP2WPKH > (neededAmount * 2)) && FormatsUtil.getInstance().isValidBech32(address)) {
                Log.d("SendActivity", "set 1 P2WPKH 2x");
                _utxos1 = utxosP2WPKH;
                selectedP2WPKH = true;
            } else if (!FormatsUtil.getInstance().isValidBech32(address) && (valueP2SH_P2WPKH > (neededAmount * 2)) && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
                Log.d("SendActivity", "set 1 P2SH_P2WPKH 2x");
                _utxos1 = utxosP2SH_P2WPKH;
                selectedP2SH_P2WPKH = true;
            } else if (!FormatsUtil.getInstance().isValidBech32(address) && (valueP2PKH > (neededAmount * 2)) && !Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
                Log.d("SendActivity", "set 1 P2PKH 2x");
                _utxos1 = utxosP2PKH;
                selectedP2PKH = true;
            } else if (valueP2WPKH > (neededAmount * 2)) {
                Log.d("SendActivity", "set 1 P2WPKH 2x");
                _utxos1 = utxosP2WPKH;
                selectedP2WPKH = true;
            } else if (valueP2SH_P2WPKH > (neededAmount * 2)) {
                Log.d("SendActivity", "set 1 P2SH_P2WPKH 2x");
                _utxos1 = utxosP2SH_P2WPKH;
                selectedP2SH_P2WPKH = true;
            } else if (valueP2PKH > (neededAmount * 2)) {
                Log.d("SendActivity", "set 1 P2PKH 2x");
                _utxos1 = utxosP2PKH;
                selectedP2PKH = true;
            } else {
                ;
            }

            if (_utxos1 == null) {
                if (valueP2SH_P2WPKH > neededAmount) {
                    Log.d("SendActivity", "set 1 P2SH_P2WPKH");
                    _utxos1 = utxosP2SH_P2WPKH;
                    selectedP2SH_P2WPKH = true;
                } else if (valueP2WPKH > neededAmount) {
                    Log.d("SendActivity", "set 1 P2WPKH");
                    _utxos1 = utxosP2WPKH;
                    selectedP2WPKH = true;
                } else if (valueP2PKH > neededAmount) {
                    Log.d("SendActivity", "set 1 P2PKH");
                    _utxos1 = utxosP2PKH;
                    selectedP2PKH = true;
                } else {
                    ;
                }

            }

            if (_utxos1 != null && _utxos2 == null) {

                if (!selectedP2SH_P2WPKH && valueP2SH_P2WPKH > neededAmount) {
                    Log.d("SendActivity", "set 2 P2SH_P2WPKH");
                    _utxos2 = utxosP2SH_P2WPKH;
                } else if (!selectedP2WPKH && valueP2WPKH > neededAmount) {
                    Log.d("SendActivity", "set 2 P2WPKH");
                    _utxos2 = utxosP2WPKH;
                } else if (!selectedP2PKH && valueP2PKH > neededAmount) {
                    Log.d("SendActivity", "set 2 P2PKH");
                    _utxos2 = utxosP2PKH;
                } else {
                    ;
                }
            }

            if (_utxos1 == null && _utxos2 == null) {
                // can't do boltzmann, revert to SPEND_SIMPLE
                canDoBoltzmann = false;
                SPEND_TYPE = SPEND_SIMPLE;
            } else {

                Log.d("SendActivity", "boltzmann spend");

                Collections.shuffle(_utxos1);
                if (_utxos2 != null) {
                    Collections.shuffle(_utxos2);
                }

                // boltzmann spend (STONEWALL)
                pair = SendFactory.getInstance(SendActivity.this).boltzmann(_utxos1, _utxos2, BigInteger.valueOf(amount), address);

                if (pair == null) {
                    // can't do boltzmann, revert to SPEND_SIMPLE
                    canDoBoltzmann = false;
                    restoreChangeIndexes();
                    SPEND_TYPE = SPEND_SIMPLE;
                } else {
                    canDoBoltzmann = true;
                }
            }

        } else {
            ;
        }

        if (SPEND_TYPE == SPEND_SIMPLE && amount == balance) {
            // do nothing, utxo selection handles above
            ;
        }
        // simple spend (less than balance)
        else if (SPEND_TYPE == SPEND_SIMPLE) {
            List<UTXO> _utxos = utxos;

            // sort in ascending order by value
            Collections.sort(_utxos, new UTXO.UTXOComparator());
            Collections.reverse(_utxos);

            // get smallest 1 UTXO > than spend + fee + dust
            for (UTXO u : _utxos) {
                Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(u.getOutpoints()));
                if (u.getValue() >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), 2).longValue())) {
                    selectedUTXO.add(u);
                    totalValueSelected += u.getValue();
                    Log.d("SendActivity", "spend type:" + SPEND_TYPE);
                    Log.d("SendActivity", "single output");
                    Log.d("SendActivity", "amount:" + amount);
                    Log.d("SendActivity", "value selected:" + u.getValue());
                    Log.d("SendActivity", "total value selected:" + totalValueSelected);
                    Log.d("SendActivity", "nb inputs:" + u.getOutpoints().size());
                    break;
                }
            }

            if (selectedUTXO.size() == 0) {
                // sort in descending order by value
                Collections.sort(_utxos, new UTXO.UTXOComparator());
                int selected = 0;
                int p2pkh = 0;
                int p2sh_p2wpkh = 0;
                int p2wpkh = 0;

                // get largest UTXOs > than spend + fee + dust
                for (UTXO u : _utxos) {

                    selectedUTXO.add(u);
                    totalValueSelected += u.getValue();
                    selected += u.getOutpoints().size();

//                            Log.d("SendActivity", "value selected:" + u.getValue());
//                            Log.d("SendActivity", "total value selected/threshold:" + totalValueSelected + "/" + (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(selected, 2).longValue()));

                    Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector<MyTransactionOutPoint>(u.getOutpoints()));
                    p2pkh += outpointTypes.getLeft();
                    p2sh_p2wpkh += outpointTypes.getMiddle();
                    p2wpkh += outpointTypes.getRight();
                    if (totalValueSelected >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, 2).longValue())) {
                        Log.d("SendActivity", "spend type:" + SPEND_TYPE);
                        Log.d("SendActivity", "multiple outputs");
                        Log.d("SendActivity", "amount:" + amount);
                        Log.d("SendActivity", "total value selected:" + totalValueSelected);
                        Log.d("SendActivity", "nb inputs:" + selected);
                        break;
                    }
                }
            }

        } else if (pair != null) {

            selectedUTXO.clear();
            receivers.clear();

            long inputAmount = 0L;
            long outputAmount = 0L;

            for (MyTransactionOutPoint outpoint : pair.getLeft()) {
                UTXO u = new UTXO();
                List<MyTransactionOutPoint> outs = new ArrayList<MyTransactionOutPoint>();
                outs.add(outpoint);
                u.setOutpoints(outs);
                totalValueSelected += u.getValue();
                selectedUTXO.add(u);
                inputAmount += u.getValue();
            }

            for (TransactionOutput output : pair.getRight()) {
                try {
                    Script script = new Script(output.getScriptBytes());
                    receivers.put(script.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), BigInteger.valueOf(output.getValue().longValue()));
                    outputAmount += output.getValue().longValue();
                } catch (Exception e) {
                    Toast.makeText(SendActivity.this, R.string.error_bip126_output, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            change = outputAmount - amount;
            fee = BigInteger.valueOf(inputAmount - outputAmount);

        } else {
            Toast.makeText(SendActivity.this, R.string.cannot_select_utxo, Toast.LENGTH_SHORT).show();
            return false;
        }

//         do spend here
        if (selectedUTXO.size() > 0) {

            // estimate fee for simple spend, already done if boltzmann
            if (SPEND_TYPE == SPEND_SIMPLE) {
                List<MyTransactionOutPoint> outpoints = new ArrayList<MyTransactionOutPoint>();
                for (UTXO utxo : selectedUTXO) {
                    outpoints.addAll(utxo.getOutpoints());
                }
                Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(outpoints));
                if (amount == balance) {
                    fee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), 1);
                    amount -= fee.longValue();
                    receivers.clear();
                    receivers.put(address, BigInteger.valueOf(amount));

                    //
                    // fee sanity check
                    //
                    restoreChangeIndexes();
                    Transaction tx = SendFactory.getInstance(SendActivity.this).makeTransaction(0, outpoints, receivers);
                    tx = SendFactory.getInstance(SendActivity.this).signTransaction(tx);
                    byte[] serialized = tx.bitcoinSerialize();
                    Log.d("SendActivity", "size:" + serialized.length);
                    Log.d("SendActivity", "vsize:" + tx.getVirtualTransactionSize());
                    Log.d("SendActivity", "fee:" + fee.longValue());
                    if ((tx.hasWitness() && (fee.longValue() < tx.getVirtualTransactionSize())) || (!tx.hasWitness() && (fee.longValue() < serialized.length))) {
                        Toast.makeText(SendActivity.this, R.string.insufficient_fee, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    //
                    //
                    //

                } else {
                    fee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), 2);
                }
            }

            Log.d("SendActivity", "spend type:" + SPEND_TYPE);
            Log.d("SendActivity", "amount:" + amount);
            Log.d("SendActivity", "total value selected:" + totalValueSelected);
            Log.d("SendActivity", "fee:" + fee.longValue());
            Log.d("SendActivity", "nb inputs:" + selectedUTXO.size());

            change = totalValueSelected - (amount + fee.longValue());
//                    Log.d("SendActivity", "change:" + change);

            if (change > 0L && change < SamouraiWallet.bDust.longValue() && SPEND_TYPE == SPEND_SIMPLE) {

                AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.change_is_dust)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();

                            }
                        });
                if (!isFinishing()) {
                    dlg.show();
                }

                return false;
            }

            _change = change;
            final BigInteger _fee = fee;

            String dest = null;
            if (strPCode != null && strPCode.length() > 0) {
                dest = BIP47Meta.getInstance().getDisplayLabel(strPCode);
            } else {
                dest = address;
            }

            if (SendAddressUtil.getInstance().get(address) == 1) {
                strPrivacyWarning = getString(R.string.send_privacy_warning) + "\n\n";
            } else {
                strPrivacyWarning = "";
            }

            if (!canDoBoltzmann) {
                restoreChangeIndexes();
                stonewallOptionText.setAlpha(.6f);
                stoneWallSwitch.setAlpha(.6f);
                stoneWallDesc.setAlpha(.6f);
                stoneWallSwitch.setChecked(false);
                stoneWallSwitch.setEnabled(false);
//                strCannotDoBoltzmann = getString(R.string.boltzmann_cannot) + "\n\n";
            }

            /*
                    String strNoLikedTypeBoltzmann = null;
                    if(canDoBoltzmann && PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_BOLTZMANN, true) == true && PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false)    {
                        strNoLikedTypeBoltzmann = getString(R.string.boltzmann_like_typed) + "\n\n";
                    }
                    else    {
                        strNoLikedTypeBoltzmann = "";
                    }
                    */

//                    String message = strCannotDoBoltzmann + strNoLikedTypeBoltzmann + strPrivacyWarning + "Send " + Coin.valueOf(amount).toPlainString() + " to " + dest + " (fee:" + Coin.valueOf(_fee.longValue()).toPlainString() + ")?\n";
            message = strPrivacyWarning + "Send " + Coin.valueOf(amount).toPlainString() + " to " + dest + " (fee:" + Coin.valueOf(_fee.longValue()).toPlainString() + ")?\n";

            tvTotalFee.setText(Coin.valueOf(_fee.longValue()).toPlainString().concat(" BTC"));

            double value = Double.parseDouble(String.valueOf(_fee.add(BigInteger.valueOf(amount))));

            btnSend.setText("send ".concat(String.format(Locale.ENGLISH, "%.8f", getBtcValue(value))).concat(" BTC"));
            CalculateEntropy(selectedUTXO, receivers);
            return true;
        }
        return false;
    }

    private void initiateSpend() {

        if (SPEND_TYPE == SPEND_RICOCHET) {
            ricochetSpend(ricochetStaggeredDelivery.isChecked());
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(message);
        final CheckBox cbShowAgain;
        if (strPrivacyWarning.length() > 0) {
            cbShowAgain = new CheckBox(SendActivity.this);
            cbShowAgain.setText(R.string.do_not_repeat_sent_to);
            cbShowAgain.setChecked(false);
            builder.setView(cbShowAgain);
        } else {
            cbShowAgain = null;
        }
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {

                final List<MyTransactionOutPoint> outPoints = new ArrayList<MyTransactionOutPoint>();
                for (UTXO u : selectedUTXO) {
                    outPoints.addAll(u.getOutpoints());
                }

                // add change
                if (_change > 0L) {
                    if (SPEND_TYPE == SPEND_SIMPLE) {
                        if (changeType == 84) {
                            String change_address = BIP84Util.getInstance(SendActivity.this).getAddressAt(AddressFactory.CHANGE_CHAIN, BIP84Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx()).getBech32AsString();
                            receivers.put(change_address, BigInteger.valueOf(_change));
                        } else if (changeType == 49) {
                            String change_address = BIP49Util.getInstance(SendActivity.this).getAddressAt(AddressFactory.CHANGE_CHAIN, BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx()).getAddressAsString();
                            receivers.put(change_address, BigInteger.valueOf(_change));
                        } else {
                            try {
                                String change_address = HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().getAddressAt(HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().getAddrIdx()).getAddressString();
                                receivers.put(change_address, BigInteger.valueOf(_change));
                            } catch (IOException ioe) {
                                Toast.makeText(SendActivity.this, R.string.error_change_output, Toast.LENGTH_SHORT).show();
                                return;
                            } catch (MnemonicException.MnemonicLengthException mle) {
                                Toast.makeText(SendActivity.this, R.string.error_change_output, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                    } else if (SPEND_TYPE == SPEND_BOLTZMANN) {
                        // do nothing, change addresses included
                        ;
                    } else {
                        ;
                    }
                }

                SendParams.getInstance().setParams(outPoints,
                        receivers,
                        strPCode,
                        SPEND_TYPE,
                        _change,
                        changeType,
                        address,
                        strPrivacyWarning.length() > 0,
                        cbShowAgain != null ? cbShowAgain.isChecked() : false,
                        amount,
                        change_index
                );
                Intent _intent = new Intent(SendActivity.this, TxAnimUIActivity.class);
                startActivity(_intent);

            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {

                SendActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                            btSend.setActivated(true);
//                            btSend.setClickable(true);
//                                        dialog.dismiss();
                    }
                });

            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    private void ricochetSpend(boolean staggered) {

        AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(ricochetMessage)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        if (staggered) {

//                            Log.d("SendActivity", "Ricochet staggered:" + ricochetJsonObj.toString());

                            try {
                                if (ricochetJsonObj.has("hops")) {
                                    JSONArray hops = ricochetJsonObj.getJSONArray("hops");
                                    if (hops.getJSONObject(0).has("nTimeLock")) {

                                        JSONArray nLockTimeScript = new JSONArray();
                                        for (int i = 0; i < hops.length(); i++) {
                                            JSONObject hopObj = hops.getJSONObject(i);
                                            int seq = i;
                                            long locktime = hopObj.getLong("nTimeLock");
                                            String hex = hopObj.getString("tx");
                                            JSONObject scriptObj = new JSONObject();
                                            scriptObj.put("hop", i);
                                            scriptObj.put("nlocktime", locktime);
                                            scriptObj.put("tx", hex);
                                            nLockTimeScript.put(scriptObj);
                                        }

                                        JSONObject nLockTimeObj = new JSONObject();
                                        nLockTimeObj.put("script", nLockTimeScript);

//                                        Log.d("SendActivity", "Ricochet nLockTime:" + nLockTimeObj.toString());

                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {

                                                Looper.prepare();

                                                String url = WebUtil.getAPIUrl(SendActivity.this);
                                                url += "pushtx/schedule";
                                                try {
                                                    String result = "";
                                                    if (TorManager.getInstance(getApplicationContext()).isConnected()) {
                                                        result = WebUtil.getInstance(SendActivity.this).tor_postURL(url, nLockTimeObj);

                                                    } else {
                                                        result = WebUtil.getInstance(SendActivity.this).postURL("application/json", url, nLockTimeObj.toString());

                                                    }
//                                                    Log.d("SendActivity", "Ricochet staggered result:" + result);
                                                    JSONObject resultObj = new JSONObject(result);
                                                    if (resultObj.has("status") && resultObj.getString("status").equalsIgnoreCase("ok")) {
                                                        Toast.makeText(SendActivity.this, R.string.ricochet_nlocktime_ok, Toast.LENGTH_LONG).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(SendActivity.this, R.string.ricochet_nlocktime_ko, Toast.LENGTH_LONG).show();
                                                        finish();
                                                    }
                                                } catch (Exception e) {
                                                    Log.d("SendActivity", e.getMessage());
                                                    Toast.makeText(SendActivity.this, R.string.ricochet_nlocktime_ko, Toast.LENGTH_LONG).show();
                                                    finish();
                                                }

                                                Looper.loop();

                                            }
                                        }).start();

                                    }
                                }
                            } catch (JSONException je) {
                                Log.d("SendActivity", je.getMessage());
                            }

                        } else {
                            RicochetMeta.getInstance(SendActivity.this).add(ricochetJsonObj);

                            Intent intent = new Intent(SendActivity.this, RicochetActivity.class);
                            startActivityForResult(intent, RICOCHET);
                        }

                    }

                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }
                });
        if (!isFinishing()) {
            dlg.show();
        }

    }

    private void backToTransactionView() {
        amountViewSwitcher.showPrevious();
        sendTransactionDetailsView.showTransaction();

    }

    @Override
    public void onBackPressed() {
        if (sendTransactionDetailsView.isReview()) {
            backToTransactionView();
        } else {
            super.onBackPressed();
        }
    }

    private void enableAmount(boolean enable) {
        btcEditText.setEnabled(enable);
        satEditText.setEnabled(enable);
    }

    private void processScan(String data) {

        if (data.contains("https://bitpay.com")) {

            AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.no_bitpay)
                    .setCancelable(false)
                    .setPositiveButton(R.string.learn_more, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://blog.samouraiwallet.com/post/169222582782/bitpay-qr-codes-are-no-longer-valid-important"));
                            startActivity(intent);

                        }
                    }).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            dialog.dismiss();

                        }
                    });
            if (!isFinishing()) {
                dlg.show();
            }

            return;
        }

        if(Cahoots.isCahoots(data.trim())) {
            CahootsUtil.getInstance(SendActivity.this).processCahoots(data.trim());
            return;
        }
        if (FormatsUtil.getInstance().isPSBT(data.trim())) {
            CahootsUtil.getInstance(SendActivity.this).doPSBT(data.trim());
            return;
        }

        if (FormatsUtil.getInstance().isValidPaymentCode(data)) {
            processPCode(data, null);
            return;
        }

        if (FormatsUtil.getInstance().isBitcoinUri(data)) {
            String address = FormatsUtil.getInstance().getBitcoinAddress(data);
            String amount = FormatsUtil.getInstance().getBitcoinAmount(data);

            setToAddress(address);
            if (amount != null) {
                try {
                    NumberFormat btcFormat = NumberFormat.getInstance(Locale.US);
                    btcFormat.setMaximumFractionDigits(8);
                    btcFormat.setMinimumFractionDigits(1);
//                    setToAddress(btcFormat.format(Double.parseDouble(amount) / 1e8));
//                    Log.i(TAG, "------->: ".concat();
                    btcEditText.setText(btcFormat.format(Double.parseDouble(amount) / 1e8));
                } catch (NumberFormatException nfe) {
//                    setToAddress("0.0");
                }
            }

            final String strAmount;
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            nf.setMinimumIntegerDigits(1);
            nf.setMinimumFractionDigits(1);
            nf.setMaximumFractionDigits(8);
            strAmount = nf.format(balance / 1e8);
            tvMaxAmount.setText(strAmount + " " + getDisplayUnits());

            try {
                if (amount != null && Double.parseDouble(amount) != 0.0) {
                    toAddressEditText.setEnabled(false);
//                    selectPaynymBtn.setEnabled(false);
//                    selectPaynymBtn.setAlpha(0.5f);
                    //                    Toast.makeText(this, R.string.no_edit_BIP21_scan, Toast.LENGTH_SHORT).show();
                    enableAmount(false);

                }
            } catch (NumberFormatException nfe) {
                enableAmount(true);
            }

        } else if (FormatsUtil.getInstance().isValidBitcoinAddress(data)) {

            if (FormatsUtil.getInstance().isValidBech32(data)) {
                setToAddress(data.toLowerCase());
            } else {
                setToAddress(data);
            }

        } else if (data.contains("?")) {

            String pcode = data.substring(0, data.indexOf("?"));
            // not valid BIP21 but seen often enough
            if (pcode.startsWith("bitcoin://")) {
                pcode = pcode.substring(10);
            }
            if (pcode.startsWith("bitcoin:")) {
                pcode = pcode.substring(8);
            }
            if (FormatsUtil.getInstance().isValidPaymentCode(pcode)) {
                processPCode(pcode, data.substring(data.indexOf("?")));
            }
        } else {
            Toast.makeText(this, R.string.scan_error, Toast.LENGTH_SHORT).show();
        }

        validateSpend();
    }

    public String getDisplayUnits() {

        return MonetaryUtil.getInstance().getBTCUnits();

    }

    private void processPCode(String pcode, String meta) {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setBalance();
            }
        }, 2000);

        if (FormatsUtil.getInstance().isValidPaymentCode(pcode)) {

            if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_CFM) {
                try {
                    PaymentCode _pcode = new PaymentCode(pcode);
                    PaymentAddress paymentAddress = BIP47Util.getInstance(this).getSendAddress(_pcode, BIP47Meta.getInstance().getOutgoingIdx(pcode));

                    if (BIP47Meta.getInstance().getSegwit(pcode)) {
                        SegwitAddress segwitAddress = new SegwitAddress(paymentAddress.getSendECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                        strDestinationBTCAddress = segwitAddress.getBech32AsString();
                    } else {
                        strDestinationBTCAddress = paymentAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    }

                    strPCode = _pcode.toString();
                    setToAddress(BIP47Meta.getInstance().getDisplayLabel(strPCode));
                    toAddressEditText.setEnabled(false);
                    validateSpend();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.error_payment_code, Toast.LENGTH_SHORT).show();
                }
            } else {
//                Toast.makeText(SendActivity.this, "Payment must be added and notification tx sent", Toast.LENGTH_SHORT).show();

                if (meta != null && meta.startsWith("?") && meta.length() > 1) {
                    meta = meta.substring(1);
                }

                Intent intent = new Intent(this, BIP47Activity.class);
                intent.putExtra("pcode", pcode);
                if (meta != null && meta.length() > 0) {
                    intent.putExtra("meta", meta);
                }
                startActivity(intent);
            }

        } else {
            Toast.makeText(this, R.string.invalid_payment_code, Toast.LENGTH_SHORT).show();
        }

    }

    private boolean validateSpend() {

        boolean isValid = false;
        boolean insufficientFunds = false;

        double btc_amount = 0.0;

        String strBTCAddress = getToAddress();
        if (strBTCAddress.startsWith("bitcoin:")) {
            setToAddress(strBTCAddress.substring(8));
        }
        setToAddress(strBTCAddress);

        try {
            btc_amount = NumberFormat.getInstance(Locale.US).parse(btcEditText.getText().toString()).doubleValue();
//            Log.i("SendFragment", "amount entered:" + btc_amount);
        } catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        } catch (ParseException pe) {
            btc_amount = 0.0;
        }

        final double dAmount = btc_amount;

        //        Log.i("SendFragment", "amount entered (converted):" + dAmount);

        final long amount = (long) (Math.round(dAmount * 1e8));
        Log.i("SendFragment", "amount entered (converted to long):" + amount);
        Log.i("SendFragment", "balance:" + balance);
        if (amount > balance) {
            insufficientFunds = true;
        }

//        Log.i("SendFragment", "insufficient funds:" + insufficientFunds);

        if (amount >= SamouraiWallet.bDust.longValue() && FormatsUtil.getInstance().isValidBitcoinAddress(getToAddress())) {
            isValid = true;
        } else if (amount >= SamouraiWallet.bDust.longValue() && strDestinationBTCAddress != null && FormatsUtil.getInstance().isValidBitcoinAddress(strDestinationBTCAddress)) {
            isValid = true;
        } else {
            isValid = false;
        }

        if (insufficientFunds) {
            Toast.makeText(this, getString(R.string.insufficient_funds), Toast.LENGTH_SHORT).show();
        }
        if (!isValid || insufficientFunds) {
            enableReviewButton(false);
            return false;
        } else {
            enableReviewButton(true);
            return true;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_QR) {

            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {

                strPCode = null;
                strDestinationBTCAddress = null;

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                processScan(strResult);

            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_QR) {
            ;
        } else if (resultCode == Activity.RESULT_OK && requestCode == RICOCHET) {
            ;
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == RICOCHET) {
            ;
        } else {
            ;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.select_paynym) {
            PaynymSelectModalFragment paynymSelectModalFragment =
                    PaynymSelectModalFragment.newInstance(code -> processPCode(code, null));
            paynymSelectModalFragment.show(getSupportFragmentManager(), "paynym_select");
            return true;
        }
        // noinspection SimplifiableIfStatement
        if (id == R.id.action_scan_qr) {
            doScan();
        } else if (id == R.id.action_ricochet) {
            Intent intent = new Intent(SendActivity.this, RicochetActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_empty_ricochet) {
            emptyRicochetQueue();
        } else if (id == R.id.action_utxo) {
            doUTXO();
        } else if (id == R.id.action_fees) {
            doFees();
        } else if (id == R.id.action_batch) {
            doBatchSpend();
        } else if (id == R.id.action_support) {
            doSupport();
        } else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void emptyRicochetQueue() {

        RicochetMeta.getInstance(this).setLastRicochet(null);
        RicochetMeta.getInstance(this).empty();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    PayloadUtil.getInstance(SendActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(SendActivity.this).getGUID() + AccessFactory.getInstance(SendActivity.this).getPIN()));
                } catch (Exception e) {
                    ;
                }

            }
        }).start();

    }

    private void doScan() {
        Intent intent = new Intent(SendActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
        startActivityForResult(intent, SCAN_QR);
    }

    private void doSupport() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/section/8-sending-bitcoin"));
        startActivity(intent);
    }

    private void doUTXO() {
        Intent intent = new Intent(SendActivity.this, UTXOActivity.class);
        startActivity(intent);
    }

    private void doBatchSpend() {
        Intent intent = new Intent(SendActivity.this, BatchSendActivity.class);
        startActivity(intent);
    }

    private void doStowaway() {

        long amountCahoots = CahootsUtil.getInstance(SendActivity.this).getCahootsValue();
        String strCahootsAmount = SendActivity.this.getText(R.string.amount_sats).toString();
        strCahootsAmount += "\n(" + Coin.valueOf(amountCahoots).toPlainString() + " BTC available)";

        final EditText edAmount = new EditText(SendActivity.this);
        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                .setTitle(R.string.app_name)
                .setView(edAmount)
                .setMessage(strCahootsAmount)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final String strAmount = edAmount.getText().toString().trim();
                        try {
                            long amount = Long.parseLong(strAmount);
                            if(amount < CahootsUtil.getInstance(SendActivity.this).getCahootsValue())    {
                                CahootsUtil.getInstance(SendActivity.this).doStowaway0(amount);
                            }
                            else    {
                                Toast.makeText(SendActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                            }
                        }
                        catch(NumberFormatException nfe) {
                            Toast.makeText(SendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                        }

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doSTONEWALLx2() {

        long amountCahoots = CahootsUtil.getInstance(SendActivity.this).getCahootsValue();
        String strCahootsAmount = SendActivity.this.getText(R.string.amount_sats).toString();
        strCahootsAmount += "\n(" + Coin.valueOf(amountCahoots).toPlainString() + " BTC available)";

        final EditText edAmount = new EditText(SendActivity.this);
        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                .setTitle(R.string.app_name)
                .setView(edAmount)
                .setMessage(strCahootsAmount)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final String strAmount = edAmount.getText().toString().trim();
                        try {
                            long amount = Long.parseLong(strAmount);

                            if(amount < CahootsUtil.getInstance(SendActivity.this).getCahootsValue())    {
                                CahootsUtil.getInstance(SendActivity.this).doStowaway0(amount);

                                final EditText edAddress = new EditText(SendActivity.this);
                                AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                                        .setTitle(R.string.app_name)
                                        .setView(edAddress)
                                        .setMessage(R.string.segwit_address)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                dialog.dismiss();

                                                final String strAddress = edAddress.getText().toString().trim();
                                                if(FormatsUtil.getInstance().isValidBitcoinAddress(strAddress, SamouraiWallet.getInstance().getCurrentNetworkParams()) && FormatsUtil.getInstance().isValidBech32(strAddress))    {
                                                    try {
                                                        CahootsUtil.getInstance(SendActivity.this).doSTONEWALLx2_0(amount, strAddress);
                                                    }
                                                    catch(NumberFormatException nfe) {
                                                        Toast.makeText(SendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                                else    {
                                                    Toast.makeText(SendActivity.this, R.string.invalid_address, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                dialog.dismiss();
                                            }
                                        });
                                if(!isFinishing())    {
                                    dlg.show();
                                }
                            }
                            else    {
                                Toast.makeText(SendActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                            }

                        }
                        catch(NumberFormatException nfe) {
                            Toast.makeText(SendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void doFees() {

        SuggestedFee highFee = FeeUtil.getInstance().getHighFee();
        SuggestedFee normalFee = FeeUtil.getInstance().getNormalFee();
        SuggestedFee lowFee = FeeUtil.getInstance().getLowFee();

        String message = getText(R.string.current_fee_selection) + " " + (FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() / 1000L) + " " + getText(R.string.slash_sat);
        message += "\n";
        message += getText(R.string.current_hi_fee_value) + " " + (highFee.getDefaultPerKB().longValue() / 1000L) + " " + getText(R.string.slash_sat);
        message += "\n";
        message += getText(R.string.current_mid_fee_value) + " " + (normalFee.getDefaultPerKB().longValue() / 1000L) + " " + getText(R.string.slash_sat);
        message += "\n";
        message += getText(R.string.current_lo_fee_value) + " " + (lowFee.getDefaultPerKB().longValue() / 1000L) + " " + getText(R.string.slash_sat);

        AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> dialog.dismiss());
        if (!isFinishing()) {
            dlg.show();
        }

    }

    private void saveChangeIndexes() {

        idxBIP84Internal = BIP84Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx();
        idxBIP49Internal = BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx();
        try {
            idxBIP44Internal = HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().getAddrIdx();
        } catch (IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

    }

    private void restoreChangeIndexes() {

        BIP84Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(idxBIP84Internal);
        BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(idxBIP49Internal);
        try {
            HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().setAddrIdx(idxBIP44Internal);
        } catch (IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

    }

    private void CalculateEntropy(ArrayList<UTXO> selectedUTXO, HashMap<String, BigInteger> receivers) {

        // for ricochet entropy will be 0 always
        if (SPEND_TYPE == SPEND_RICOCHET) {
            return;
        }

        Map<String, Long> inputs = new HashMap<>();
        Map<String, Long> outputs = new HashMap<>();

        if (receivers.size() <= 1) {
            entropyValue.setText(R.string.zero_bits);
            entropyBar.disable();
            return;
        }
        if (receivers.size() > 8) {
            entropyValue.setText(R.string.not_available);
            entropyBar.disable();
            return;
        }

        for (Map.Entry<String, BigInteger> mapEntry : receivers.entrySet()) {
            String toAddress = mapEntry.getKey();
            BigInteger value = mapEntry.getValue();
            outputs.put(toAddress, value.longValue());
        }

        for (int i = 0; i < selectedUTXO.size(); i++) {
            inputs.put(stubAddress[i], selectedUTXO.get(i).getValue());
        }

        Observable.create((ObservableOnSubscribe<TxProcessorResult>) emitter -> {

            TxProcessor txProcessor = new TxProcessor(BoltzmannSettings.MAX_DURATION_DEFAULT, BoltzmannSettings.MAX_TXOS_DEFAULT);
            Txos txos = new Txos(inputs, outputs);
            TxProcessorResult result = txProcessor.processTx(txos, 0.005f, TxosLinkerOptionEnum.PRECHECK, TxosLinkerOptionEnum.LINKABILITY, TxosLinkerOptionEnum.MERGE_INPUTS);
            emitter.onNext(result);
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TxProcessorResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(TxProcessorResult entropyResult) {

                        DecimalFormat decimalFormat = new DecimalFormat("##.00");
                        entropyValue.setText(decimalFormat.format(entropyResult.getEntropy()).concat(" bits"));
                        entropyBar.setRange(entropyResult);
                    }

                    @Override
                    public void onError(Throwable e) {
                        entropyValue.setText(R.string.not_available);
                        entropyBar.disable();
                    }

                    @Override
                    public void onComplete() {
                    }
                });

    }


}

