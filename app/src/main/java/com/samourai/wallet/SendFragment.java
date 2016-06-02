package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.MnemonicException;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.ChangeMaker;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UnspentOutputsBundle;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.text.DecimalFormatSymbols;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;

import net.sourceforge.zbar.Symbol;

import org.bitcoinj.core.Coin;
import org.bitcoinj.params.MainNetParams;

public class SendFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private static final int SCAN_URI = 2077;

    private TextView tvMaxPrompt = null;
    private TextView tvMax = null;
    private long balance = 0L;

    private EditText edAddress = null;
    private String strDestinationBTCAddress = null;
    private TextWatcher textWatcherAddress = null;

    private EditText edAmountBTC = null;
    private EditText edAmountFiat = null;
    private TextWatcher textWatcherBTC = null;
    private TextWatcher textWatcherFiat = null;

    private String defaultSeparator = null;

    private Button btFee = null;
    private TextView tvFeeAmount = null;

    private EditText edCustomFee = null;

    private final static int FEE_AUTO = 0;
    private final static int FEE_PRIORITY = 1;
    private final static int FEE_CUSTOM = 2;
    private int FEE_TYPE = 0;
    private boolean networkIsStressed = false;

    private TextView tvSpendType = null;
    private TextView tvSpendTypeDesc = null;
    private SeekBar spendType = null;

    private String strFiat = null;
    private double btc_fx = 286.0;
    private TextView tvFiatSymbol = null;

    private Button btSend = null;

    private int selectedAccount = 0;

    private String strPCode = null;

    public static SendFragment newInstance(int sectionNumber) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public SendFragment() {
        ;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rootView = inflater.inflate(R.layout.fragment_send, container, false);
        }
        else {
            rootView = inflater.inflate(R.layout.fragment_send_compat, container, false);
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            rootView.setBackgroundColor(getActivity().getResources().getColor(R.color.divider));
        }

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        if(SamouraiWallet.getInstance().getShowTotalBalance())    {
            if(SamouraiWallet.getInstance().getCurrentSelectedAccount() == 2)    {
                selectedAccount = 1;
            }
            else    {
                selectedAccount = 0;
            }
        }
        else    {
            selectedAccount = 0;
        }

        tvMaxPrompt = (TextView)rootView.findViewById(R.id.max_prompt);
        tvMax = (TextView)rootView.findViewById(R.id.max);
        try    {
            balance = APIFactory.getInstance(getActivity()).getXpubAmounts().get(HD_WalletFactory.getInstance(getActivity()).get().getAccount(selectedAccount).xpubstr());
        }
        catch(IOException ioe)    {
            ;
        }
        catch(MnemonicException.MnemonicLengthException mle)    {
            ;
        }

        final String strAmount;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format(((double)(balance * 1000000L)) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format(((double)(balance * 1000L)) / 1e8);
                break;
            default:
                strAmount = Coin.valueOf(balance).toPlainString();
                break;
        }

        tvMax.setText(strAmount + " " + getDisplayUnits());
        tvMaxPrompt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                edAmountBTC.setText(strAmount);
                return false;
            }
        });
        tvMax.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                edAmountBTC.setText(strAmount);
                return false;
            }
        });

        DecimalFormat format = (DecimalFormat)DecimalFormat.getInstance(new Locale("en", "US"));
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        defaultSeparator = Character.toString(symbols.getDecimalSeparator());

        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getAvgPrice(strFiat);
        tvFiatSymbol = (TextView)rootView.findViewById(R.id.fiatSymbol);
        tvFiatSymbol.setText(getDisplayUnits() + "-" + strFiat);

        edAddress = (EditText)rootView.findViewById(R.id.destination);

        textWatcherAddress = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                validateSpend();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edAddress.addTextChangedListener(textWatcherAddress);
        edAddress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //final int DRAWABLE_LEFT = 0;
                //final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                //final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (edAddress.getRight() - edAddress.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        doScan();
                        return true;
                    }
                }

                return false;
            }
        });

        edAmountBTC = (EditText)rootView.findViewById(R.id.amountBTC);
        edAmountFiat = (EditText)rootView.findViewById(R.id.amountFiat);

        textWatcherBTC = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                edAmountBTC.removeTextChangedListener(this);
                edAmountFiat.removeTextChangedListener(textWatcherFiat);

                int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
                int max_len = 8;
                NumberFormat btcFormat = NumberFormat.getInstance(new Locale("en", "US"));
                switch (unit) {
                    case MonetaryUtil.MICRO_BTC:
                        max_len = 2;
                        break;
                    case MonetaryUtil.MILLI_BTC:
                        max_len = 4;
                        break;
                    default:
                        max_len = 8;
                        break;
                }
                btcFormat.setMaximumFractionDigits(max_len + 1);
                btcFormat.setMinimumFractionDigits(0);

                double d = 0.0;
                try {
                    d = NumberFormat.getInstance(new Locale("en", "US")).parse(s.toString()).doubleValue();
                    String s1 = btcFormat.format(d);
                    if (s1.indexOf(defaultSeparator) != -1) {
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmountBTC.setText(s1.substring(0, s1.length() - 1));
                                edAmountBTC.setSelection(edAmountBTC.getText().length());
                                s = edAmountBTC.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                } catch (ParseException pe) {
                    ;
                }

                switch (unit) {
                    case MonetaryUtil.MICRO_BTC:
                        d = d / 1000000.0;
                        break;
                    case MonetaryUtil.MILLI_BTC:
                        d = d / 1000.0;
                        break;
                    default:
                        break;
                }

                if(d > 21000000.0)    {
                    edAmountFiat.setText("0.00");
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                    edAmountBTC.setText("0");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    Toast.makeText(getActivity(), R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
                else    {
                    edAmountFiat.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(d * btc_fx));
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                }

                edAmountFiat.addTextChangedListener(textWatcherFiat);
                edAmountBTC.addTextChangedListener(this);

                validateSpend();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edAmountBTC.addTextChangedListener(textWatcherBTC);

        textWatcherFiat = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                edAmountFiat.removeTextChangedListener(this);
                edAmountBTC.removeTextChangedListener(textWatcherBTC);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(new Locale("en", "US"));
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                double d = 0.0;
                try	{
                    d = NumberFormat.getInstance(new Locale("en", "US")).parse(s.toString()).doubleValue();
                    String s1 = fiatFormat.format(d);
                    if(s1.indexOf(defaultSeparator) != -1)	{
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
                        if(dec.length() > 0)	{
                            dec = dec.substring(1);
                            if(dec.length() > max_len)	{
                                edAmountFiat.setText(s1.substring(0, s1.length() - 1));
                                edAmountFiat.setSelection(edAmountFiat.getText().length());
                            }
                        }
                    }
                }
                catch(NumberFormatException nfe)	{
                    ;
                }
                catch(ParseException pe) {
                    ;
                }

                int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
                switch (unit) {
                    case MonetaryUtil.MICRO_BTC:
                        d = d * 1000000.0;
                        break;
                    case MonetaryUtil.MILLI_BTC:
                        d = d * 1000.0;
                        break;
                    default:
                        break;
                }

                if((d / btc_fx) > 21000000.0)    {
                    edAmountFiat.setText("0.00");
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                    edAmountBTC.setText("0");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    Toast.makeText(getActivity(), R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
                else    {
                    edAmountBTC.setText(MonetaryUtil.getInstance().getBTCFormat().format(d / btc_fx));
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                }

                edAmountBTC.addTextChangedListener(textWatcherBTC);
                edAmountFiat.addTextChangedListener(this);

                validateSpend();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edAmountFiat.addTextChangedListener(textWatcherFiat);

        tvSpendType = (TextView)rootView.findViewById(R.id.spendType);
        tvSpendTypeDesc = (TextView)rootView.findViewById(R.id.spendTypeDesc);
        spendType = (SeekBar)rootView.findViewById(R.id.seekBar);
        spendType.setMax(2);
        spendType.setProgress(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.SPEND_TYPE, 1));
        spendType.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress;
                PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.SPEND_TYPE, spendType.getProgress());
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                ;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                String[] s1 = new String[3];
                s1[0] = getActivity().getString(R.string.spend_type_1);
                s1[1] = getActivity().getString(R.string.spend_type_2);
                s1[2] = getActivity().getString(R.string.spend_type_3);

                String[] s2 = new String[3];
                s2[0] = getActivity().getString(R.string.spend_type_1_desc);
                s2[1] = getActivity().getString(R.string.spend_type_2_desc);
                s2[2] = getActivity().getString(R.string.spend_type_3_desc);

                tvSpendType.setText(s1[progressChanged]);
                tvSpendTypeDesc.setText(s2[progressChanged]);

            }
        });

        tvFeeAmount = (TextView)rootView.findViewById(R.id.feeAmount);
        edCustomFee = (EditText)rootView.findViewById(R.id.customFeeAmount);
        edCustomFee.setText("0.0001");
        edCustomFee.setVisibility(View.GONE);

        btFee = (Button)rootView.findViewById(R.id.fee);
        btFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(FEE_TYPE == FEE_AUTO) {
                    FEE_TYPE = FEE_PRIORITY;
                    btFee.setText(getString(R.string.priority_fee));
                    edCustomFee.setVisibility(View.GONE);
                    tvFeeAmount.setVisibility(View.VISIBLE);
                    tvFeeAmount.setText("");
                }
                else if(FEE_TYPE == FEE_PRIORITY) {
                    FEE_TYPE = FEE_CUSTOM;
                    btFee.setText(getString(R.string.custom_fee));
                    tvFeeAmount.setVisibility(View.GONE);
                    edCustomFee.setVisibility(View.VISIBLE);
                    edCustomFee.setSelection(edCustomFee.getText().length());
                }
                else {
                    FEE_TYPE = FEE_AUTO;
                    btFee.setText(getString(R.string.auto_fee));
                    edCustomFee.setVisibility(View.GONE);
                    tvFeeAmount.setVisibility(View.VISIBLE);
                    tvFeeAmount.setText("");
                }
            }
        });

        tvFeeAmount.setText("");

        btSend = (Button)rootView.findViewById(R.id.send);
        btSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                btSend.setClickable(false);
                btSend.setActivated(false);

                double btc_amount = 0.0;

                try {
                    btc_amount = NumberFormat.getInstance(new Locale("en", "US")).parse(edAmountBTC.getText().toString().trim()).doubleValue();
//                    Log.i("SendFragment", "amount entered:" + btc_amount);
                }
                catch (NumberFormatException nfe) {
                    btc_amount = 0.0;
                }
                catch (ParseException pe) {
                    btc_amount = 0.0;
                }

                final double dAmount;
                int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
                switch (unit) {
                    case MonetaryUtil.MICRO_BTC:
                        dAmount = btc_amount / 1000000.0;
                        break;
                    case MonetaryUtil.MILLI_BTC:
                        dAmount = btc_amount / 1000.0;
                        break;
                    default:
                        dAmount = btc_amount;
                        break;
                }

                final long amount = (long)(Math.round(dAmount * 1e8));

//                Log.i("SendActivity", "amount:" + amount);
                final String address = strDestinationBTCAddress == null ? edAddress.getText().toString() : strDestinationBTCAddress;
                final int accountIdx = selectedAccount;

                final String strPrivacyWarning;
                if(SendAddressUtil.getInstance().get(address) == 1) {
                    strPrivacyWarning = getActivity().getResources().getString(R.string.send_privacy_warning) + "\n\n";
                }
                else {
                    strPrivacyWarning = "";
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();

                        //
                        // pass zero fee as fee is calculated afterwards
                        //
                        UnspentOutputsBundle unspentCoinsBundle = SendFactory.getInstance(getActivity()).phase1(accountIdx, BigInteger.valueOf(amount), BigInteger.valueOf(0L));

                        if(unspentCoinsBundle != null) {

//                            Log.i("SendActivity", "unspentCoinsBundle not null");
//                            Log.i("SendActivity", "unspentCoinsBundle outputs size:" + unspentCoinsBundle.getOutputs().size());
//                            Log.i("SendActivity", "amount:" + amount);

                            HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                            receivers.put(address, BigInteger.valueOf(amount));

                            int nb_outputs = 0;
                            switch(spendType.getProgress())    {
                                case ChangeMaker.CHANGE_SAMOURAI:
                                    nb_outputs = receivers.size() + 2;
                                    break;
                                case ChangeMaker.CHANGE_AGGRESSIVE:
                                    nb_outputs = receivers.size() + 10;
                                    break;
                                default:
                                    nb_outputs = receivers.size() + 1;
                                    break;
                            }

                            BigInteger _fee = BigInteger.ZERO;
                            if(FEE_TYPE == FEE_AUTO)    {
                                _fee = FeeUtil.getInstance().estimatedFee(unspentCoinsBundle.getOutputs().size(), nb_outputs);
//                                Log.i("SendActivity", "estimatedFee:" + _fee.toString());
                            }
                            else if(FEE_TYPE == FEE_PRIORITY)   {
//                                _fee = _fee.add(SamouraiWallet.bAddPriority);
                                if(networkIsStressed)    {
                                    _fee = FeeUtil.getInstance().getStressFee();
                                }
                                else    {
                                    _fee = FeeUtil.getInstance().getPriorityFee();
                                }
//                                Log.i("SendActivity", "priorityFee:" + _fee.toString());
                            }
                            else    {

                                double customFee = 0.0;
                                try {
                                    customFee = NumberFormat.getInstance(new Locale("en", "US")).parse(edCustomFee.getText().toString().trim()).doubleValue();
                                }
                                catch (NumberFormatException | ParseException e) {
                                    customFee = 0.0001;
                                }

                                _fee = BigInteger.valueOf((long)(Math.round(customFee * 1e8)));
//                                Log.i("SendActivity", "customFee:" + _fee.toString());

                            }

                            boolean spendAll = false;

                            if(amount == APIFactory.getInstance(getActivity()).getXpubAmounts().get(AddressFactory.getInstance().account2xpub().get(accountIdx)))	{
                                if(FEE_TYPE != FEE_CUSTOM)    {
                                    _fee = FeeUtil.getInstance().estimatedFee(unspentCoinsBundle.getOutputs().size(), nb_outputs);
//                                    Log.i("SendActivity", "estimatedFee:" + _fee.toString());
                                }
                                BigInteger revisedValue = receivers.get(address);
                                revisedValue = revisedValue.subtract(_fee);
                                receivers.put(address, revisedValue);
//                                Log.i("SendActivity", "revised amount:" + revisedValue.toString());
                                spendAll = true;
                            }
                            else if((amount + _fee.longValue()) > APIFactory.getInstance(getActivity()).getXpubAmounts().get(AddressFactory.getInstance().account2xpub().get(accountIdx)))    {
//                                Log.i("SendActivity", "insufficient funds");
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btSend.setActivated(true);
                                        btSend.setClickable(true);
                                        Toast.makeText(getActivity(), R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return;

                            }
                            else if((amount + _fee.longValue()) > unspentCoinsBundle.getTotalAmount().longValue())	{
                                UnspentOutputsBundle refreshUnspents = SendFactory.getInstance(getActivity()).supplementRandomizedUnspentOutputPoints(unspentCoinsBundle, BigInteger.valueOf((amount + _fee.longValue() + SamouraiWallet.bDust.longValue()) - unspentCoinsBundle.getTotalAmount().longValue()));

                                if(refreshUnspents == null)    {
//                                    Log.i("SendActivity", "refreshUnspents == null");
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btSend.setActivated(true);
                                            btSend.setClickable(true);
                                            Toast.makeText(getActivity(), R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return;
                                }

                                unspentCoinsBundle = refreshUnspents;

                            }
                            else    {
                                ;
                            }

                            final BigInteger fee = _fee;

                            final Pair<Transaction, Long> pair = SendFactory.getInstance(getActivity()).phase2(accountIdx, unspentCoinsBundle.getOutputs(), receivers, fee, spendType.getProgress());
                            if(pair == null) {
//                                Log.i("SendActivity", "pair == null");
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btSend.setActivated(true);
                                        btSend.setClickable(true);
                                        Toast.makeText(getActivity(), R.string.error_creating_tx, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return;
                            }

//                            Log.i("SendActivity", "pair not null");

                            final long _amount;
                            if(spendAll)    {
                                _amount = amount - _fee.longValue();
                            }
                            else    {
                                _amount = amount;
                            }

                            // use estimated fee
                            BigInteger totalSpend = BigInteger.valueOf(_amount).add(fee);
                            BigInteger totalChange = unspentCoinsBundle.getTotalAmount().subtract(totalSpend);

                            String strUnitDisplay = "";
                            if(!getDisplayUnits().equals((String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC)]))    {
                                strUnitDisplay = "Unit amount:" + edAmountBTC.getText().toString().trim() + " " + getDisplayUnits() + "\n\n";
                            }
//                            String strSpend = MonetaryUtil.getInstance().getBTCFormat().format(BigInteger.valueOf(_amount).doubleValue() / 1e8) + " BTC" + ", " + "Fee:" + MonetaryUtil.getInstance().getBTCFormat().format(fee.doubleValue() / 1e8) + " BTC" + "\n\n";
                            String strSpend = "Miners' fee:" + MonetaryUtil.getInstance().getBTCFormat().format(fee.doubleValue() / 1e8) + " BTC" + "\n\n";
//                            String strCJSimulate = "Inputs:" + ror.getOutputs().size() + ", " + "Change:" + MonetaryUtil.getInstance().getBTCFormat().format(totalChange.doubleValue() / 1e8) + "\n\n";
//                            String strNbAddresses = "Addresses associated with this spend:" + " " + ror.getNbAddress() + "\n\n";
                            String strChangeOutputSafe = "";
                            if(!unspentCoinsBundle.isChangeSafe()) {
                                strChangeOutputSafe = "Privacy Alert - This transaction contains addresses that have been seen together in a previous transaction." + "\n\n";
                            }
                            else {
                                strChangeOutputSafe = "";
                            }
//                            final String msg = strPrivacyWarning + strChangeOutputSafe + strNbAddresses + strUnitDisplay + strSpend + strCJSimulate + getString(R.string.send) + " " + MonetaryUtil.getInstance().getBTCFormat().format(((double)_amount) / 1e8) + " " + getString(R.string.to) + " " + address + " ?";

                            String dest = null;
                            if(strPCode != null && strPCode.length() > 0)    {
                                dest = BIP47Meta.getInstance().getDisplayLabel(strPCode);
                            }
                            else    {
                                dest = address;
                            }

                            final String msg = strPrivacyWarning + strChangeOutputSafe + strUnitDisplay + strSpend + getString(R.string.send) + " " + MonetaryUtil.getInstance().getBTCFormat().format(((double)_amount) / 1e8) + " BTC " + getString(R.string.to) + " " + dest;

                            final UnspentOutputsBundle _unspentCoinsBundle = unspentCoinsBundle;

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.app_name);
                            builder.setMessage(msg);
                            builder.setCancelable(false);
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, int whichButton) {

                                    final ProgressDialog progress = new ProgressDialog(getActivity());
                                    progress.setCancelable(false);
                                    progress.setTitle(R.string.app_name);
                                    progress.setMessage(getString(R.string.please_wait));
                                    progress.show();

                                    HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                                    receivers.put(address, BigInteger.valueOf(amount));

                                    SendFactory.getInstance(getActivity()).phase3(pair, accountIdx, _unspentCoinsBundle.getOutputs(), receivers, fee, new OpCallback() {

                                        public void onSuccess() {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(getActivity(), R.string.tx_ok, Toast.LENGTH_SHORT).show();
                                                    APIFactory.getInstance(getActivity()).setXpubBalance(APIFactory.getInstance(getActivity()).getXpubBalance() - (_amount + fee.longValue()));

                                                    btSend.setActivated(true);
                                                    btSend.setClickable(true);
                                                    edAddress.setText("");
                                                    edAmountBTC.setText("");

                                                }
                                            });
                                            if (progress != null && progress.isShowing()) {
                                                progress.dismiss();
                                            }

                                            // increment counter if BIP47 spend
                                            if(strPCode != null && strPCode.length() > 0)    {
                                                BIP47Meta.getInstance().getPCode4AddrLookup().put(address, strPCode);
                                                BIP47Meta.getInstance().inc(strPCode);

                                                SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                                                String strTS = sd.format(System.currentTimeMillis());
                                                String event = strTS + " " + getActivity().getString(R.string.sent) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) _amount / 1e8) + " BTC";
                                                BIP47Meta.getInstance().setLatestEvent(strPCode, event);
                                            }

                                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                                            intent.putExtra("notifTx", false);
                                            intent.putExtra("fetch", true);
                                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

                                            View view = getActivity().getCurrentFocus();
                                            if (view != null) {
                                                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                                            }

                                            if(strPCode != null && strPCode.length() > 0)    {
                                                AppUtil.getInstance(getActivity()).restartApp();
                                            }
                                            else    {
                                                getActivity().getFragmentManager().beginTransaction().remove(SendFragment.this).commit();
                                            }

                                        }

                                        public void onFail() {

                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    btSend.setActivated(true);
                                                    btSend.setClickable(true);
                                                    Toast.makeText(getActivity(), R.string.tx_ko, Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                            if (progress != null && progress.isShowing()) {
                                                progress.dismiss();
                                            }

                                        }

                                    });

                                }
                            });
                            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, int whichButton) {

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btSend.setActivated(true);
                                            btSend.setClickable(true);
                                            dialog.dismiss();
                                        }
                                    });

                                }
                            });

                            AlertDialog alert = builder.create();
                            alert.show();

                        }
                        else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btSend.setActivated(true);
                                    btSend.setClickable(true);
                                    Toast.makeText(getActivity(), R.string.no_confirmed_outputs_available, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        Looper.loop();

                    }
                }).start();

            }
        });

        if(getArguments() != null)    {
            String strUri = getArguments().getString("uri");
            strPCode = getArguments().getString("pcode");
            if(strUri != null && strUri.length() > 0)    {
                processScan(strUri);
            }
            if(strPCode != null && strPCode.length() > 0)    {
                processPCode(strPCode);
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                FeeUtil.getInstance().update();
                networkIsStressed = FeeUtil.getInstance().isStressed();
//                Log.i("SendFragment", "networkIsStressed:" + networkIsStressed);

                if(networkIsStressed)    {
                    Toast.makeText(getActivity(), R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                }

            }
        }).start();

        validateSpend();

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity2) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_URI)	{
            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{
                String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
                processScan(strResult);
            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_URI)	{
            ;
        }
        else {
            ;
        }

    }

    private void doScan() {
        Intent intent = new Intent(getActivity(), ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_URI);
    }

    private void processScan(String data) {

        if(FormatsUtil.getInstance().isValidPaymentCode(data))	{
            processPCode(data);
            return;
        }

        if(data.indexOf("?") != -1) {
            String pcode = data.substring(0, data.indexOf("?"));
            if(FormatsUtil.getInstance().isValidPaymentCode(pcode)) {
                processPCode(data);
                return;
            }
        }

        if(FormatsUtil.getInstance().isBitcoinUri(data))	{
            String address = FormatsUtil.getInstance().getBitcoinAddress(data);
            String amount = FormatsUtil.getInstance().getBitcoinAmount(data);

            edAddress.setText(address);

            if(amount != null) {
                try {
                    NumberFormat btcFormat = NumberFormat.getInstance(new Locale("en", "US"));
                    btcFormat.setMaximumFractionDigits(8);
                    btcFormat.setMinimumFractionDigits(1);
                    edAmountBTC.setText(btcFormat.format(Double.parseDouble(amount) / 1e8));
                }
                catch (NumberFormatException nfe) {
                    edAmountBTC.setText("0.0");
                }
            }

            PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
            tvFiatSymbol.setText(getDisplayUnits() + "-" + strFiat);

            final String strAmount;
            DecimalFormat df = new DecimalFormat("#");
            df.setMinimumIntegerDigits(1);
            df.setMinimumFractionDigits(1);
            df.setMaximumFractionDigits(8);
            strAmount = Coin.valueOf(balance).toPlainString();
            tvMax.setText(strAmount + " " + getDisplayUnits());

            try {
                if(amount != null && Double.parseDouble(amount) != 0.0)    {
                    edAddress.setEnabled(false);
                    edAmountBTC.setEnabled(false);
                    edAmountFiat.setEnabled(false);
//                    Toast.makeText(getActivity(), R.string.no_edit_BIP21_scan, Toast.LENGTH_SHORT).show();
                }
            }
            catch (NumberFormatException nfe) {
                edAmountBTC.setText("0.0");
            }

        }
        else if(FormatsUtil.getInstance().isValidBitcoinAddress(data))	{
            edAddress.setText(data);
        }
        else	{
            Toast.makeText(getActivity(), R.string.scan_error, Toast.LENGTH_SHORT).show();
        }

        validateSpend();
    }

    private void processPCode(String data) {

        if(FormatsUtil.getInstance().isValidPaymentCode(data))	{

            if(BIP47Meta.getInstance().getOutgoingStatus(data) == BIP47Meta.STATUS_SENT_CFM)    {
                try {
                    PaymentCode pcode = new PaymentCode(data);
                    PaymentAddress paymentAddress = BIP47Util.getInstance(getActivity()).getSendAddress(pcode, BIP47Meta.getInstance().getOutgoingIdx(data));

//                    edAddress.setText(paymentAddress.getSendECKey().toAddress(MainNetParams.get()).toString());

                    strDestinationBTCAddress = paymentAddress.getSendECKey().toAddress(MainNetParams.get()).toString();
                    edAddress.setText(BIP47Meta.getInstance().getDisplayLabel(pcode.toString()));
                    edAddress.setEnabled(false);
//                    Toast.makeText(getActivity(), R.string.no_edit_BIP47_address, Toast.LENGTH_SHORT).show();
                }
                catch(Exception e) {
                    Toast.makeText(getActivity(), R.string.error_payment_code, Toast.LENGTH_SHORT).show();
                }
            }
            else    {
                Toast.makeText(getActivity(), "Payment must be added and notification tx sent", Toast.LENGTH_SHORT).show();
            }

        }
        else	{
            Toast.makeText(getActivity(), R.string.invalid_payment_code, Toast.LENGTH_SHORT).show();
        }

    }

    private void validateSpend() {

        boolean isValid = false;
        boolean insufficientFunds = false;

        double btc_amount = 0.0;

        try {
            btc_amount = NumberFormat.getInstance(new Locale("en", "US")).parse(edAmountBTC.getText().toString().trim()).doubleValue();
//            Log.i("SendFragment", "amount entered:" + btc_amount);
        }
        catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        }
        catch (ParseException pe) {
            btc_amount = 0.0;
        }

        final double dAmount;
        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                dAmount = btc_amount / 1000000.0;
                break;
            case MonetaryUtil.MILLI_BTC:
                dAmount = btc_amount / 1000.0;
                break;
            default:
                dAmount = btc_amount;
                break;
        }
//        Log.i("SendFragment", "amount entered (converted):" + dAmount);

        final long amount = (long)(Math.round(dAmount * 1e8));
//        Log.i("SendFragment", "amount entered (converted to long):" + amount);
//        Log.i("SendFragment", "balance:" + balance);
        if(amount > balance)    {
            insufficientFunds = true;
        }

//        Log.i("SendFragment", "insufficient funds:" + insufficientFunds);

        if(btc_amount > 0.00 && FormatsUtil.getInstance().isValidBitcoinAddress(edAddress.getText().toString())) {
            isValid = true;
        }
        else if(btc_amount > 0.00 && strDestinationBTCAddress != null && FormatsUtil.getInstance().isValidBitcoinAddress(strDestinationBTCAddress)) {
            isValid = true;
        }
        else    {
            isValid = false;
        }

        if(!isValid || insufficientFunds) {
            btSend.setVisibility(View.INVISIBLE);
        }
        else {
            btSend.setVisibility(View.VISIBLE);
        }

    }

    public String getDisplayUnits() {

        return (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

}
