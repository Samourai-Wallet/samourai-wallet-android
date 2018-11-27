package com.samourai.wallet.spend;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.fragments.PaynymSelectModalFragment;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.spend.widgets.EntropyBar;
import com.samourai.wallet.spend.widgets.SendTransactionDetailsView;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;


public class SendNewUIActivity extends AppCompatActivity {


    private final static int SCAN_QR = 2012;
    private final static int RICOCHET = 2013;


    private SendTransactionDetailsView sendTransactionDetailsView;
    private ViewSwitcher amountViewSwitcher;
    private EditText toAddressEditText, btcEditText, satEditText;
    private TextView tvMaxAmount, tvReviewSpendAmount, tvTotalFee, tvToAddress, tvEstimatedBlockWait, tvSelectedFeeRate, tvSelectedFeeRateLayman;
    private Button btnReview, btnSend;
    private ImageView selectPaynymBtn;
    private Switch ricochetHopsSwitch;
    private ConstraintLayout bottomSheet;
    private EntropyBar entropyBar;
//    BottomSheetBehavior sheetBehavior;

    private static final String TAG = "SendNewUIActivity";
    private long balance = 0L;
    private String strDestinationBTCAddress = null;
    private Boolean isOnReviewPage = false;

    private final static int FEE_LOW = 0;
    private final static int FEE_NORMAL = 1;
    private final static int FEE_PRIORITY = 2;
    private final static int FEE_CUSTOM = 3;
    private int FEE_TYPE = FEE_LOW;

    public final static int SPEND_SIMPLE = 0;
    public final static int SPEND_BOLTZMANN = 1;
    public final static int SPEND_RICOCHET = 2;
    private int SPEND_TYPE = SPEND_BOLTZMANN;

    private int selectedAccount = 0;

    private String strPCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_new_ui);
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
        selectPaynymBtn = findViewById(R.id.paynym_select_btn);
        tvReviewSpendAmount = findViewById(R.id.send_review_amount);
        tvMaxAmount = findViewById(R.id.totalBTC);


        //view elements from review segment and transaction segment can be access through respective
        //methods which returns root viewGroup
        entropyBar = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.entropyBar);
        btnReview = sendTransactionDetailsView.getTransactionView().findViewById(R.id.review_button);
        ricochetHopsSwitch = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_hops_switch);
        tvSelectedFeeRate = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.selected_fee_rate);
        tvSelectedFeeRateLayman = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.selected_fee_rate_in_layman);
        tvTotalFee = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.total_fee);
        btnSend = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.send_btn);

        btcEditText.addTextChangedListener(BTCWatcher);
        satEditText.addTextChangedListener(satWatcher);

        btnReview.setOnClickListener(v -> review());


        btnSend.setOnClickListener(v -> Toast.makeText(SendNewUIActivity.this, "Send Clicked", Toast.LENGTH_SHORT).show());

        if (SamouraiWallet.getInstance().getShowTotalBalance()) {
            if (SamouraiWallet.getInstance().getCurrentSelectedAccount() == 2) {
                selectedAccount = 1;
            } else {
                selectedAccount = 0;
            }
        } else {
            selectedAccount = 0;
        }

        SPEND_TYPE = PrefsUtil.getInstance(this).getValue(PrefsUtil.USE_BOLTZMANN, true) ? SPEND_BOLTZMANN : SPEND_SIMPLE;
        if (SPEND_TYPE > SPEND_BOLTZMANN) {
            SPEND_TYPE = SPEND_BOLTZMANN;
            PrefsUtil.getInstance(this).setValue(PrefsUtil.SPEND_TYPE, SPEND_BOLTZMANN);
        }

        setUpRicochet();

        setUpFee();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
//            bViaMenu = extras.getBoolean("via_menu", false);
            String strUri = extras.getString("uri");
            strPCode = extras.getString("pcode");
            if (strUri != null && strUri.length() > 0) {
                processScan(strUri);
            }
            if (strPCode != null && strPCode.length() > 0) {
                processPCode(strPCode, null);
            }
        }

        validateSpend();

        setBalance();

        setUpPaynym();
    }
//

    private void setUpPaynym() {
        selectPaynymBtn.setOnClickListener((view) -> {
            PaynymSelectModalFragment paynymSelectModalFragment =
                    PaynymSelectModalFragment.newInstance(code -> processPCode(code, null));
            paynymSelectModalFragment.show(getSupportFragmentManager(), "paynym_select");
        });
    }

    private void setUpFee() {

        FEE_TYPE = PrefsUtil.getInstance(this).getValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL);

        long lo = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
        long mi = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
        long hi = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

        if (lo == mi && mi == hi) {
            lo = (long) ((double) mi * 0.85);
            hi = (long) ((double) mi * 1.15);
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(lo * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(hi * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        } else if (lo == mi || mi == hi) {
            mi = (lo + hi) / 2L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(mi * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        } else {
            ;
        }

        if (lo < 1L) {
            lo = 1L;
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(lo * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
        }
        if (mi < 1L) {
            mi = 1L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(mi * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        if (hi < 1L) {
            hi = 1L;
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(hi * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }

        FeeUtil.getInstance().sanitizeFee();

        switch (FEE_TYPE) {
            case FEE_LOW:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getLowFee());
                FeeUtil.getInstance().sanitizeFee();

//                tvFeePrompt.setText(getText(R.string.fee_low_priority) + " " + getText(R.string.blocks_to_cf));
                break;
            case FEE_PRIORITY:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                FeeUtil.getInstance().sanitizeFee();

//                tvFeePrompt.setText(getText(R.string.fee_high_priority) + " " + getText(R.string.blocks_to_cf));
                break;
            default:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                FeeUtil.getInstance().sanitizeFee();

//                tvFeePrompt.setText(getText(R.string.fee_mid_priority) + " " + getText(R.string.blocks_to_cf));
                break;
        }

    }

    private void setUpRicochet() {
        ricochetHopsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                SPEND_TYPE = SPEND_RICOCHET;
                PrefsUtil.getInstance(this).setValue(PrefsUtil.USE_RICOCHET, true);
            } else {
                SPEND_TYPE = PrefsUtil.getInstance(this).getValue(PrefsUtil.SPEND_TYPE, SPEND_BOLTZMANN);
                PrefsUtil.getInstance(this).setValue(PrefsUtil.USE_RICOCHET, false);
            }

        });
        ricochetHopsSwitch.setChecked(PrefsUtil.getInstance(this).getValue(PrefsUtil.USE_RICOCHET, false));

    }

    private void setBalance() {

        try {
            balance = APIFactory.getInstance(this).getXpubAmounts().get(HD_WalletFactory.getInstance(this).get().getAccount(selectedAccount).xpubstr());
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
        Log.i(TAG, "setBalance: ------------> ".concat(strAmount));
        Log.i(TAG, "setBalance: ------------> ".concat(getDisplayUnits()));
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
                Float btc = Float.parseFloat(String.valueOf(editable));
                Double sats = getSatValue(Double.valueOf(btc));
                Log.i(TAG, "afterTextChanged: ....".concat(sats.toString()));
                satEditText.setText(formattedSatValue(sats));

                if (btc > 21000000.0) {
                    btcEditText.setText("0.00");
                    btcEditText.setSelection(btcEditText.getText().length());
                    satEditText.setText("0");
                    satEditText.setSelection(satEditText.getText().length());
                    Toast.makeText(SendNewUIActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }

//
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            satEditText.addTextChangedListener(satWatcher);
            btcEditText.addTextChangedListener(this);


        }
    };

    private String formattedSatValue(Object number) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
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
                Float btc = getBtcValue(sats);
                String formatted = formattedSatValue(sats);
                Log.i(TAG, "afterTextChanged: ....".concat(btc.toString()));


                satEditText.setText(formatted);
                satEditText.setSelection(formatted.length());
                btcEditText.setText(String.format(Locale.ENGLISH, "%.8f", btc));
                if (btc > 21000000.0) {
                    btcEditText.setText("0.00");
                    btcEditText.setSelection(btcEditText.getText().length());
                    satEditText.setText("0");
                    satEditText.setSelection(satEditText.getText().length());
                    Toast.makeText(SendNewUIActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();

            }
            satEditText.addTextChangedListener(this);
            btcEditText.addTextChangedListener(BTCWatcher);

        }
    };

    private void setTvToAddress(String string) {
        tvToAddress.setText(string);
        toAddressEditText.setText(string);
    }

    private String getTvToAddress() {
        if (toAddressEditText.getText().toString().trim().length() != 0) {
            return toAddressEditText.getText().toString();
        }
        if (tvToAddress.getText().toString().length() != 0) {
            return tvToAddress.getText().toString();
        }
        return "";
    }

    private Float getBtcValue(Double sats) {
        return (float) (sats / 100000000);
    }

    private Double getSatValue(Double btc) {
        if (btc == 0) {
            return (double) 0;
        }
        return btc * 100000000;
    }

    private void review() {

        if (validateSpend()) {
            tvReviewSpendAmount.setText(btcEditText.getText());
            amountViewSwitcher.showNext();
            sendTransactionDetailsView.showReview(ricochetHopsSwitch.isChecked());
        }

    }

    private void backToTransactionView() {
        amountViewSwitcher.showPrevious();
        sendTransactionDetailsView.showTransaction();
        isOnReviewPage = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isOnReviewPage) {
            backToTransactionView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
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

        if (FormatsUtil.getInstance().isValidPaymentCode(data)) {
            processPCode(data, null);
            return;
        }

        if (FormatsUtil.getInstance().isBitcoinUri(data)) {
            String address = FormatsUtil.getInstance().getBitcoinAddress(data);
            String amount = FormatsUtil.getInstance().getBitcoinAmount(data);

            toAddressEditText.setText(address);

            if (amount != null) {
                try {
                    NumberFormat btcFormat = NumberFormat.getInstance(Locale.US);
                    btcFormat.setMaximumFractionDigits(8);
                    btcFormat.setMinimumFractionDigits(1);
                    setTvToAddress(btcFormat.format(Double.parseDouble(amount) / 1e8));
                } catch (NumberFormatException nfe) {
                    setTvToAddress("0.0");
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
                    toAddressEditText.setEnabled(false);
//                    Toast.makeText(this, R.string.no_edit_BIP21_scan, Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException nfe) {
                toAddressEditText.setText("0.0");
            }

        } else if (FormatsUtil.getInstance().isValidBitcoinAddress(data)) {

            if (FormatsUtil.getInstance().isValidBech32(data)) {
                setTvToAddress(data.toLowerCase());
            } else {
                setTvToAddress(data);
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
                    setTvToAddress(BIP47Meta.getInstance().getDisplayLabel(strPCode));
                    toAddressEditText.setEnabled(false);
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

        String strBTCAddress = getTvToAddress();
        if (strBTCAddress.startsWith("bitcoin:")) {
            setTvToAddress(strBTCAddress.substring(8));
        }

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
//        Log.i("SendFragment", "amount entered (converted to long):" + amount);
//        Log.i("SendFragment", "balance:" + balance);
        if (amount > balance) {
            insufficientFunds = true;
        }

//        Log.i("SendFragment", "insufficient funds:" + insufficientFunds);

        if (btc_amount > 0.00 && FormatsUtil.getInstance().isValidBitcoinAddress(getTvToAddress())) {
            isValid = true;
        } else if (btc_amount > 0.00 && strDestinationBTCAddress != null && FormatsUtil.getInstance().isValidBitcoinAddress(strDestinationBTCAddress)) {
            isValid = true;
        } else {
            isValid = false;
        }

        if (!isValid || insufficientFunds) {
            return false;
        } else {
            return true;
        }

    }


    private void disableSend(boolean enable) {
        btnSend.setEnabled(!enable);
        btnSend.setAlpha(!enable ? 1f : .8f);
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


}
