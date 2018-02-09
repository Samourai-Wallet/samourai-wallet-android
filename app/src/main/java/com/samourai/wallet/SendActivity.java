package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.ricochet.RicochetActivity;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.util.SendAddressUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.text.DecimalFormatSymbols;

import net.sourceforge.zbar.Symbol;

import org.bitcoinj.core.Coin;
import org.bitcoinj.script.Script;
import org.json.JSONException;
import org.json.JSONObject;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

import static java.lang.System.currentTimeMillis;

public class SendActivity extends Activity {

    private final static int SCAN_QR = 2012;
    private final static int RICOCHET = 2013;

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

    private Button btLowFee = null;
    private Button btAutoFee = null;
    private Button btPriorityFee = null;
    private Button btCustomFee = null;
    private TextView tvFeePrompt = null;

    private final static int FEE_LOW = 0;
    private final static int FEE_NORMAL = 1;
    private final static int FEE_PRIORITY = 2;
    private final static int FEE_CUSTOM = 3;
    private int FEE_TYPE = FEE_LOW;

    public final static int SPEND_SIMPLE = 0;
    public final static int SPEND_BIP126 = 1;
    public final static int SPEND_RICOCHET = 2;
    private int SPEND_TYPE = SPEND_BIP126;
    //    private CheckBox cbSpendType = null;
    private Switch swRicochet = null;

    private String strFiat = null;

    private double btc_fx = 286.0;
    private TextView tvFiatSymbol = null;

    private Button btSend = null;

    private int selectedAccount = 0;

    private String strPCode = null;

    private boolean bViaMenu = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        SendActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

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

        tvMaxPrompt = (TextView)findViewById(R.id.max_prompt);
        tvMax = (TextView)findViewById(R.id.max);
        try    {
            balance = APIFactory.getInstance(SendActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(selectedAccount).xpubstr());
        }
        catch(IOException ioe)    {
            balance = 0L;
        }
        catch(MnemonicException.MnemonicLengthException mle)    {
            balance = 0L;
        }
        catch(java.lang.NullPointerException npe)    {
            balance = 0L;
        }

        final String strAmount;
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
        nf.setMinimumIntegerDigits(1);

        strAmount = nf.format(balance / 1e8);

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

        DecimalFormat format = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        defaultSeparator = Character.toString(symbols.getDecimalSeparator());

        strFiat = PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        btc_fx = ExchangeRateFactory.getInstance(SendActivity.this).getAvgPrice(strFiat);
        tvFiatSymbol = (TextView)findViewById(R.id.fiatSymbol);
        tvFiatSymbol.setText(getDisplayUnits() + "-" + strFiat);

        edAddress = (EditText)findViewById(R.id.destination);

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

                if(event.getAction() == MotionEvent.ACTION_UP && event.getRawX() >= (edAddress.getRight() - edAddress.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    final List<String> entries = new ArrayList<String>();
                    entries.addAll(BIP47Meta.getInstance().getSortedByLabels(false));

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(SendActivity.this, android.R.layout.select_dialog_singlechoice);
                    for(int i = 0; i < entries.size(); i++)   {
                        arrayAdapter.add(BIP47Meta.getInstance().getDisplayLabel(entries.get(i)));
                    }

                    AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this);
                    dlg.setIcon(R.drawable.ic_launcher);
                    dlg.setTitle(R.string.app_name);

                    dlg.setAdapter(arrayAdapter,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

//                                    Toast.makeText(SendActivity.this, BIP47Meta.getInstance().getDisplayLabel(entries.get(which)), Toast.LENGTH_SHORT).show();
//                                    Toast.makeText(SendActivity.this, entries.get(which), Toast.LENGTH_SHORT).show();

                                    processPCode(entries.get(which), null);

                                }
                            });

                    dlg.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    dlg.show();

                    return true;
                }

                return false;
            }
        });

        edAmountBTC = (EditText)findViewById(R.id.amountBTC);
        edAmountFiat = (EditText)findViewById(R.id.amountFiat);

        textWatcherBTC = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                edAmountBTC.removeTextChangedListener(this);
                edAmountFiat.removeTextChangedListener(textWatcherFiat);

                int max_len = 8;
                NumberFormat btcFormat = NumberFormat.getInstance(Locale.US);
                btcFormat.setMaximumFractionDigits(max_len + 1);
                btcFormat.setMinimumFractionDigits(0);

                double d = 0.0;
                try {
                    d = NumberFormat.getInstance(Locale.US).parse(s.toString()).doubleValue();
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

                if(d > 21000000.0)    {
                    edAmountFiat.setText("0.00");
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                    edAmountBTC.setText("0");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    Toast.makeText(SendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
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
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.US);
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                double d = 0.0;
                try	{
                    d = NumberFormat.getInstance(Locale.US).parse(s.toString()).doubleValue();
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

                if((d / btc_fx) > 21000000.0)    {
                    edAmountFiat.setText("0.00");
                    edAmountFiat.setSelection(edAmountFiat.getText().length());
                    edAmountBTC.setText("0");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    Toast.makeText(SendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
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

        SPEND_TYPE = PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_BIP126, true) ? SPEND_BIP126 : SPEND_SIMPLE;
        if(SPEND_TYPE > SPEND_BIP126)    {
            SPEND_TYPE = SPEND_BIP126;
            PrefsUtil.getInstance(SendActivity.this).setValue(PrefsUtil.SPEND_TYPE, SPEND_BIP126);
        }

        swRicochet = (Switch)findViewById(R.id.ricochet);
        swRicochet.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked)    {
                    SPEND_TYPE = SPEND_RICOCHET;
                }
                else    {
                    SPEND_TYPE = PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.SPEND_TYPE, SPEND_BIP126);
                }

            }
        });

        btLowFee = (Button)findViewById(R.id.low_fee);
        btAutoFee = (Button)findViewById(R.id.auto_fee);
        btPriorityFee = (Button)findViewById(R.id.priority_fee);
        btCustomFee = (Button)findViewById(R.id.custom_fee);
        tvFeePrompt = (TextView)findViewById(R.id.current_fee_prompt);

        FEE_TYPE = PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL);

        long lo = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
        long mi = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
        long hi = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

        if(lo == mi && mi == hi) {
            lo = (long) ((double) mi * 0.85);
            hi = (long) ((double) mi * 1.15);
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(lo * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(hi * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }
        else if(lo == mi || mi == hi)    {
            mi = (lo + hi) / 2L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(mi * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        else    {
            ;
        }

        switch(FEE_TYPE)    {
            case FEE_LOW:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getLowFee());
                btLowFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.blue));
                btAutoFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.BOLD);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                tvFeePrompt.setText(getText(R.string.fee_low_priority) + " " + getText(R.string.blocks_to_cf));
                break;
            case FEE_PRIORITY:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                btLowFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.blue));
                btCustomFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.BOLD);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                tvFeePrompt.setText(getText(R.string.fee_high_priority) + " " + getText(R.string.blocks_to_cf));
                break;
            default:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                btLowFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.blue));
                btPriorityFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.BOLD);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                tvFeePrompt.setText(getText(R.string.fee_mid_priority) + " " + getText(R.string.blocks_to_cf));
                break;
        }

        btLowFee.setText((FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L) + "\n" + getString(R.string.sat_b));
        btPriorityFee.setText((FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L) + "\n" + getString(R.string.sat_b));
        btAutoFee.setText((FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L) + "\n" + getString(R.string.sat_b));

        btLowFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getLowFee());
                PrefsUtil.getInstance(SendActivity.this).setValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_LOW);
                btLowFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.blue));
                btAutoFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.BOLD);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setText(R.string.custom_fee);
                tvFeePrompt.setText(getText(R.string.fee_low_priority) + " " + getText(R.string.blocks_to_cf));
            }
        });

        btAutoFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                PrefsUtil.getInstance(SendActivity.this).setValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL);
                btLowFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.blue));
                btPriorityFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.BOLD);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setText(R.string.custom_fee);
                tvFeePrompt.setText(getText(R.string.fee_mid_priority) + " " + getText(R.string.blocks_to_cf));
            }
        });

        btPriorityFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                PrefsUtil.getInstance(SendActivity.this).setValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_PRIORITY);
                btLowFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.blue));
                btCustomFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.BOLD);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setText(R.string.custom_fee);
                tvFeePrompt.setText(getText(R.string.fee_high_priority) + " " + getText(R.string.blocks_to_cf));
            }
        });

        btCustomFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doCustomFee();
            }
        });

        btSend = (Button)findViewById(R.id.send);
        btSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                btSend.setClickable(false);
                btSend.setActivated(false);

                double btc_amount = 0.0;

                try {
                    btc_amount = NumberFormat.getInstance(Locale.US).parse(edAmountBTC.getText().toString().trim()).doubleValue();
//                    Log.i("SendFragment", "amount entered:" + btc_amount);
                } catch (NumberFormatException nfe) {
                    btc_amount = 0.0;
                } catch (ParseException pe) {
                    btc_amount = 0.0;
                }

                double dAmount = btc_amount;

                long amount = (long)(Math.round(dAmount * 1e8));;

//                Log.i("SendActivity", "amount:" + amount);
                final String address = strDestinationBTCAddress == null ? edAddress.getText().toString() : strDestinationBTCAddress;
                final int accountIdx = selectedAccount;

                final boolean isSegwitChange = (FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) || PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false;

                final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                receivers.put(address, BigInteger.valueOf(amount));

                // store current change index to restore value in case of sending fail
                int change_index = 0;
                if(isSegwitChange)    {
                    change_index = BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx();
                }
                else    {
                    try {
                        change_index = HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().getAddrIdx();
//                    Log.d("SendActivity", "storing change index:" + change_index);
                    }
                    catch(IOException ioe) {
                        ;
                    }
                    catch(MnemonicException.MnemonicLengthException mle) {
                        ;
                    }
                }

                // get all UTXO
                List<UTXO> utxos = null;
                // if possible, get UTXO by input 'type': p2pkh or p2sh-p2wpkh, else get all UTXO
                long neededAmount = 0L;
                if(FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                    neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(0, UTXOFactory.getInstance().getCountP2SH_P2WPKH(), 4).longValue();
//                    Log.d("SendActivity", "segwit:" + neededAmount);
                }
                else    {
                    neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(UTXOFactory.getInstance().getCountP2PKH(), 0, 4).longValue();
//                    Log.d("SendActivity", "p2pkh:" + neededAmount);
                }
                neededAmount += amount;
                neededAmount += SamouraiWallet.bDust.longValue();

                if((FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) && (UTXOFactory.getInstance().getP2SH_P2WPKH().size() > 0 && UTXOFactory.getInstance().getTotalP2SH_P2WPKH() > neededAmount))    {
                    utxos = new ArrayList<UTXO>(UTXOFactory.getInstance().getP2SH_P2WPKH().values());
//                    Log.d("SendActivity", "segwit utxos:" + utxos.size());
                }
                else if((UTXOFactory.getInstance().getP2PKH().size() > 0) && (UTXOFactory.getInstance().getTotalP2PKH() > neededAmount))   {
                    utxos = new ArrayList<UTXO>(UTXOFactory.getInstance().getP2PKH().values());
//                    Log.d("SendActivity", "p2pkh utxos:" + utxos.size());
                }
                else    {
                    utxos = APIFactory.getInstance(SendActivity.this).getUtxos(true);
//                    Log.d("SendActivity", "all filtered utxos:" + utxos.size());
                }

                final List<UTXO> selectedUTXO = new ArrayList<UTXO>();
                long totalValueSelected = 0L;
                long change = 0L;
                BigInteger fee = null;

//                Log.d("SendActivity", "amount:" + amount);
//                Log.d("SendActivity", "balance:" + balance);

                // insufficient funds
                if(amount > balance)    {
                    Toast.makeText(SendActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                }
                // entire balance (can only be simple spend)
                else if(amount == balance)    {
                    // make sure we are using simple spend
                    SPEND_TYPE = SPEND_SIMPLE;

//                    Log.d("SendActivity", "amount == balance");
                    // take all utxos, deduct fee
                    selectedUTXO.addAll(utxos);

                    for(UTXO u : selectedUTXO)   {
                        totalValueSelected += u.getValue();
                    }

//                    Log.d("SendActivity", "balance:" + balance);
//                    Log.d("SendActivity", "total value selected:" + totalValueSelected);

                }
                else    {
                    ;
                }

                org.apache.commons.lang3.tuple.Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> pair = null;
                if(SPEND_TYPE == SPEND_RICOCHET)    {

                    boolean samouraiFeeViaBIP47 = false;
                    if(BIP47Meta.getInstance().getOutgoingStatus(BIP47Meta.strSamouraiDonationPCode) == BIP47Meta.STATUS_SENT_CFM)    {
                        samouraiFeeViaBIP47 = true;
                    }

                    final JSONObject jObj = RicochetMeta.getInstance(SendActivity.this).script(amount, FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue(), address, 4, strPCode, samouraiFeeViaBIP47);
                    if(jObj != null)    {

                        try {
                            long totalAmount = jObj.getLong("total_spend");
                            if(totalAmount > balance)    {
                                Toast.makeText(SendActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String msg = getText(R.string.ricochet_spend1) + " " + address + " " + getText(R.string.ricochet_spend2) + " " + Coin.valueOf(totalAmount).toPlainString() + " " + getText(R.string.ricochet_spend3);

                            AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(msg)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            RicochetMeta.getInstance(SendActivity.this).add(jObj);

                                            dialog.dismiss();

                                            Intent intent = new Intent(SendActivity.this, RicochetActivity.class);
                                            startActivityForResult(intent, RICOCHET);

                                        }

                                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            dialog.dismiss();

                                        }
                                    });
                            if(!isFinishing())    {
                                dlg.show();
                            }

                            return;

                        }
                        catch(JSONException je) {
                            return;
                        }

                    }

                    return;
                }
                // if BIP126 try both hetero/alt, if fails change type to SPEND_SIMPLE
                else if(SPEND_TYPE == SPEND_BIP126)   {

                    List<UTXO> _utxos = utxos;

                    //Collections.shuffle(_utxos);
                    // sort in descending order by value
                    Collections.sort(_utxos, new UTXO.UTXOComparator());
                    // hetero
                    pair = SendFactory.getInstance(SendActivity.this).heterogeneous(_utxos, BigInteger.valueOf(amount), address);
                    if(pair == null)    {
                        //Collections.sort(_utxos, new UTXO.UTXOComparator());
                        // alt
                        pair = SendFactory.getInstance(SendActivity.this).altHeterogeneous(_utxos, BigInteger.valueOf(amount), address);
                    }

                    if(pair == null)    {
                        // can't do BIP126, revert to SPEND_SIMPLE
                        SPEND_TYPE = SPEND_SIMPLE;
                    }

                }
                else    {
                    ;
                }

                // simple spend (less than balance)
                if(SPEND_TYPE == SPEND_SIMPLE)    {
                    List<UTXO> _utxos = utxos;

                    // sort in ascending order by value
                    Collections.sort(_utxos, new UTXO.UTXOComparator());
                    Collections.reverse(_utxos);

                    // get smallest 1 UTXO > than spend + fee + dust
                    for(UTXO u : _utxos)   {
                        Pair<Integer,Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(u.getOutpoints());
                        if(u.getValue() >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getRight(), 2).longValue()))    {
                            selectedUTXO.add(u);
                            totalValueSelected += u.getValue();
//                            Log.d("SendActivity", "spend type:" + SPEND_TYPE);
//                            Log.d("SendActivity", "single output");
//                            Log.d("SendActivity", "amount:" + amount);
//                            Log.d("SendActivity", "value selected:" + u.getValue());
//                            Log.d("SendActivity", "total value selected:" + totalValueSelected);
//                            Log.d("SendActivity", "nb inputs:" + u.getOutpoints().size());
                            break;
                        }
                    }

                    if(selectedUTXO.size() == 0)    {
                        // sort in descending order by value
                        Collections.sort(_utxos, new UTXO.UTXOComparator());
                        int selected = 0;
                        int p2pkh = 0;
                        int p2wpkh = 0;

                        // get largest UTXOs > than spend + fee + dust
                        for(UTXO u : _utxos)   {

                            selectedUTXO.add(u);
                            totalValueSelected += u.getValue();
                            selected += u.getOutpoints().size();

//                            Log.d("SendActivity", "value selected:" + u.getValue());
//                            Log.d("SendActivity", "total value selected/threshold:" + totalValueSelected + "/" + (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(selected, 2).longValue()));

                            Pair<Integer,Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(u.getOutpoints());
                            p2pkh += outpointTypes.getLeft();
                            p2wpkh += outpointTypes.getRight();
                            if(totalValueSelected >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2wpkh, 2).longValue()))    {
//                                Log.d("SendActivity", "spend type:" + SPEND_TYPE);
//                                Log.d("SendActivity", "multiple outputs");
//                                Log.d("SendActivity", "amount:" + amount);
//                                Log.d("SendActivity", "total value selected:" + totalValueSelected);
//                                Log.d("SendActivity", "nb inputs:" + selected);
                                break;
                            }
                        }
                    }

                }
                else if(pair != null)    {

                    selectedUTXO.clear();
                    receivers.clear();

                    long inputAmount = 0L;
                    long outputAmount = 0L;

                    for(MyTransactionOutPoint outpoint : pair.getLeft())   {
                        UTXO u = new UTXO();
                        List<MyTransactionOutPoint> outs = new ArrayList<MyTransactionOutPoint>();
                        outs.add(outpoint);
                        u.setOutpoints(outs);
                        totalValueSelected += u.getValue();
                        selectedUTXO.add(u);
                        inputAmount += u.getValue();
                    }

                    for(TransactionOutput output : pair.getRight())   {
                        try {
                            Script script = new Script(output.getScriptBytes());
                            receivers.put(script.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString(), BigInteger.valueOf(output.getValue().longValue()));
                            outputAmount += output.getValue().longValue();
                        }
                        catch(Exception e) {
                            Toast.makeText(SendActivity.this, R.string.error_bip126_output, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    change = outputAmount - amount;
                    fee = BigInteger.valueOf(inputAmount - outputAmount);

                }
                else    {
                    Toast.makeText(SendActivity.this, R.string.cannot_select_utxo, Toast.LENGTH_SHORT).show();
                    return;
                }

                // do spend here
                if(selectedUTXO.size() > 0)    {

                    // estimate fee for simple spend, already done if BIP126
                    if(SPEND_TYPE == SPEND_SIMPLE)    {
                        List<MyTransactionOutPoint> outpoints = new ArrayList<MyTransactionOutPoint>();
                        for(UTXO utxo : selectedUTXO)   {
                            outpoints.addAll(utxo.getOutpoints());
                        }
                        Pair<Integer,Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(outpoints);
                        fee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getRight(), 2);
                    }

//                    Log.d("SendActivity", "spend type:" + SPEND_TYPE);
//                    Log.d("SendActivity", "amount:" + amount);
//                    Log.d("SendActivity", "total value selected:" + totalValueSelected);
//                    Log.d("SendActivity", "fee:" + fee.longValue());
//                    Log.d("SendActivity", "nb inputs:" + selectedUTXO.size());

                    change = totalValueSelected - (amount + fee.longValue());
//                    Log.d("SendActivity", "change:" + change);

                    boolean changeIsDust = false;
                    if(change < SamouraiWallet.bDust.longValue() && SPEND_TYPE == SPEND_SIMPLE)    {

                        change = 0L;
                        fee = fee.add(BigInteger.valueOf(change));
                        amount = totalValueSelected - fee.longValue();

//                        Log.d("SendActivity", "fee:" + fee.longValue());
//                        Log.d("SendActivity", "change:" + change);
//                        Log.d("SendActivity", "amount:" + amount);
                        receivers.put(address, BigInteger.valueOf(amount));

                        changeIsDust = true;

                    }

                    final long _change = change;
                    final BigInteger _fee = fee;
                    final int _change_index = change_index;

                    String dest = null;
                    if(strPCode != null && strPCode.length() > 0)    {
                        dest = BIP47Meta.getInstance().getDisplayLabel(strPCode);
                    }
                    else    {
                        dest = address;
                    }

                    final String strPrivacyWarning;
                    if(SendAddressUtil.getInstance().get(address) == 1) {
                        strPrivacyWarning = getString(R.string.send_privacy_warning) + "\n\n";
                    }
                    else {
                        strPrivacyWarning = "";
                    }

                    String strChangeIsDust = null;
                    if(changeIsDust)    {
                        strChangeIsDust = getString(R.string.change_is_dust) + "\n\n";
                    }
                    else    {
                        strChangeIsDust = "";
                    }

                    String message = strChangeIsDust + strPrivacyWarning + "Send " + Coin.valueOf(amount).toPlainString() + " to " + dest + " (fee:" + Coin.valueOf(_fee.longValue()).toPlainString() + ")?\n";

                    final long _amount = amount;

                    AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage(message);
                    final CheckBox cbShowAgain;
                    if(strPrivacyWarning.length() > 0)    {
                        cbShowAgain = new CheckBox(SendActivity.this);
                        cbShowAgain.setText(R.string.do_not_repeat_sent_to);
                        cbShowAgain.setChecked(false);
                        builder.setView(cbShowAgain);
                    }
                    else    {
                        cbShowAgain = null;
                    }
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int whichButton) {

                            final ProgressDialog progress = new ProgressDialog(SendActivity.this);
                            progress.setCancelable(false);
                            progress.setTitle(R.string.app_name);
                            progress.setMessage(getString(R.string.please_wait_sending));
                            progress.show();

                            final List<MyTransactionOutPoint> outPoints = new ArrayList<MyTransactionOutPoint>();
                            for(UTXO u : selectedUTXO)   {
                                outPoints.addAll(u.getOutpoints());
                            }

                            // add change
                            if(_change > 0L)    {
                                if(SPEND_TYPE == SPEND_SIMPLE)    {
                                    if(isSegwitChange)    {
                                        String change_address = BIP49Util.getInstance(SendActivity.this).getAddressAt(AddressFactory.CHANGE_CHAIN, BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx()).getAddressAsString();
                                        receivers.put(change_address, BigInteger.valueOf(_change));
                                    }
                                    else    {
                                        try {
                                            String change_address = HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().getAddressAt(HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().getAddrIdx()).getAddressString();
                                            receivers.put(change_address, BigInteger.valueOf(_change));
                                        }
                                        catch(IOException ioe) {
                                            Toast.makeText(SendActivity.this, R.string.error_change_output, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        catch(MnemonicException.MnemonicLengthException mle) {
                                            Toast.makeText(SendActivity.this, R.string.error_change_output, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }

                                }
                                else if (SPEND_TYPE == SPEND_BIP126)   {
                                    // do nothing, change addresses included
                                }
                                else    {
                                    ;
                                }
                            }

                            // make tx
                            Transaction tx = SendFactory.getInstance(SendActivity.this).makeTransaction(0, outPoints, receivers);

                            final RBFSpend rbf;
                            if(PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true)    {

                                rbf = new RBFSpend();

                                for(TransactionInput input : tx.getInputs())    {

                                    boolean _isBIP49 = false;
                                    String _addr = null;
                                    Address _address = input.getConnectedOutput().getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
                                    if(_address != null)    {
                                        _addr = _address.toString();
                                    }
                                    if(_addr == null)    {
                                        _addr = input.getConnectedOutput().getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                                        _isBIP49 = true;
                                    }

                                    String path = APIFactory.getInstance(SendActivity.this).getUnspentPaths().get(_addr);
                                    if(path != null)    {
                                        if(_isBIP49)    {
                                            rbf.addKey(input.getOutpoint().toString(), path + "/49");
                                        }
                                        else    {
                                            rbf.addKey(input.getOutpoint().toString(), path);
                                        }
                                    }
                                    else    {
                                        String pcode = BIP47Meta.getInstance().getPCode4Addr(_addr);
                                        int idx = BIP47Meta.getInstance().getIdx4Addr(_addr);
                                        rbf.addKey(input.getOutpoint().toString(), pcode + "/" + idx);
                                    }

                                }

                            }
                            else    {
                                rbf = null;
                            }

                            if(tx != null)    {
                                tx = SendFactory.getInstance(SendActivity.this).signTransaction(tx);
                                final Transaction _tx = tx;
                                final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
//                                Log.d("SendActivity", hexTx);
                                final String strTxHash = tx.getHashAsString();

                                if(PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.BROADCAST_TX, true) == false)    {

                                    if(progress != null && progress.isShowing())    {
                                        progress.dismiss();
                                    }

                                    doShowTx(hexTx, strTxHash);

                                    return;

                                }

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        Looper.prepare();

                                        boolean isOK = false;
                                        String response = null;
                                        try {
                                            if(PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true)    {
                                                if(TrustedNodeUtil.getInstance().isSet())    {
                                                    response = PushTx.getInstance(SendActivity.this).trustedNode(hexTx);
                                                    JSONObject jsonObject = new org.json.JSONObject(response);
                                                    if(jsonObject.has("result"))    {
                                                        if(jsonObject.getString("result").matches("^[A-Za-z0-9]{64}$"))    {
                                                            isOK = true;
                                                        }
                                                        else    {
                                                            Toast.makeText(SendActivity.this, R.string.trusted_node_tx_error, Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                }
                                                else    {
                                                    Toast.makeText(SendActivity.this, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            else    {
                                                response = PushTx.getInstance(SendActivity.this).samourai(hexTx);

                                                if(response != null)    {
                                                    JSONObject jsonObject = new org.json.JSONObject(response);
                                                    if(jsonObject.has("status"))    {
                                                        if(jsonObject.getString("status").equals("ok"))    {
                                                            isOK = true;
                                                        }
                                                    }
                                                }
                                                else    {
                                                    Toast.makeText(SendActivity.this, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            if(isOK)    {
                                                if(PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == false)    {
                                                    Toast.makeText(SendActivity.this, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                                                }
                                                else    {
                                                    Toast.makeText(SendActivity.this, R.string.trusted_node_tx_sent, Toast.LENGTH_SHORT).show();
                                                }

                                                if(_change > 0L && SPEND_TYPE == SPEND_SIMPLE)    {

                                                    if(isSegwitChange)    {
                                                        BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().incAddrIdx();
                                                    }
                                                    else    {
                                                        try {
                                                            HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().incAddrIdx();
                                                        }
                                                        catch(IOException ioe) {
                                                            ;
                                                        }
                                                        catch(MnemonicException.MnemonicLengthException mle) {
                                                            ;
                                                        }
                                                    }
                                                }

                                                if(PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true)    {

                                                    for(TransactionOutput out : _tx.getOutputs())   {
                                                        try {
                                                            if(!isSegwitChange && !address.equals(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString()))  {
                                                                rbf.addChangeAddr(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                            }
                                                            else if(isSegwitChange && !address.equals(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString()))   {
                                                                rbf.addChangeAddr(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                                            }
                                                            else    {
                                                                ;
                                                            }
                                                        }
                                                        catch(NullPointerException npe) {
                                                            ;   // test for bech32, skip for now as it's not a change address
                                                        }
                                                    }

                                                    rbf.setHash(strTxHash);
                                                    rbf.setSerializedTx(hexTx);

                                                    RBFUtil.getInstance().add(rbf);
                                                }

                                                // increment counter if BIP47 spend
                                                if(strPCode != null && strPCode.length() > 0)    {
                                                    BIP47Meta.getInstance().getPCode4AddrLookup().put(address, strPCode);
                                                    BIP47Meta.getInstance().inc(strPCode);

                                                    SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                                                    String strTS = sd.format(currentTimeMillis());
                                                    String event = strTS + " " + SendActivity.this.getString(R.string.sent) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) _amount / 1e8) + " BTC";
                                                    BIP47Meta.getInstance().setLatestEvent(strPCode, event);

                                                    strPCode = null;
                                                }

                                                if(strPrivacyWarning.length() > 0 && cbShowAgain != null)    {
                                                    SendAddressUtil.getInstance().add(address, cbShowAgain.isChecked() ? false : true);
                                                }
                                                else if(SendAddressUtil.getInstance().get(address) == 0)    {
                                                    SendAddressUtil.getInstance().add(address, false);
                                                }
                                                else    {
                                                    SendAddressUtil.getInstance().add(address, true);
                                                }

                                                Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                                                intent.putExtra("notifTx", false);
                                                intent.putExtra("fetch", true);
                                                LocalBroadcastManager.getInstance(SendActivity.this).sendBroadcast(intent);

                                                View view = SendActivity.this.getCurrentFocus();
                                                if (view != null) {
                                                    InputMethodManager imm = (InputMethodManager)SendActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                                                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                                                }

                                                if(bViaMenu)    {
                                                    SendActivity.this.finish();
                                                }
                                                else    {
                                                    Intent _intent = new Intent(SendActivity.this, BalanceActivity.class);
                                                    _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                    startActivity(_intent);
                                                }

                                            }
                                            else    {
                                                Toast.makeText(SendActivity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                                                // reset change index upon tx fail
                                                if(isSegwitChange)    {
                                                    BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(_change_index);
                                                }
                                                else    {
                                                    HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().setAddrIdx(_change_index);
                                                }
                                            }
                                        }
                                        catch(JSONException je) {
                                            Toast.makeText(SendActivity.this, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        catch(MnemonicException.MnemonicLengthException mle) {
                                            Toast.makeText(SendActivity.this, "pushTx:" + mle.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        catch(DecoderException de) {
                                            Toast.makeText(SendActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        catch(IOException ioe) {
                                            Toast.makeText(SendActivity.this, "pushTx:" + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        finally {
                                            SendActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    btSend.setActivated(true);
                                                    btSend.setClickable(true);
                                                    progress.dismiss();
                                                    dialog.dismiss();
                                                }
                                            });
                                        }

                                        Looper.loop();

                                    }
                                }).start();

                            }
                            else    {
//                                Log.d("SendActivity", "tx error");
                                Toast.makeText(SendActivity.this, "tx error", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int whichButton) {

                            try {
                                // reset change index upon 'NO'
                                if(isSegwitChange)    {
                                    BIP49Util.getInstance(SendActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(_change_index);
                                }
                                else    {
                                    HD_WalletFactory.getInstance(SendActivity.this).get().getAccount(0).getChange().setAddrIdx(_change_index);
                                }


                            }
                            catch(Exception e) {
//                                Log.d("SendActivity", e.getMessage());
                                Toast.makeText(SendActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            finally {
                                SendActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btSend.setActivated(true);
                                        btSend.setClickable(true);
                                        dialog.dismiss();
                                    }
                                });
                            }

                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();

                }

            }

        });

        Bundle extras = getIntent().getExtras();
        if(extras != null)    {
            bViaMenu = extras.getBoolean("via_menu", false);
            String strUri = extras.getString("uri");
            strPCode = extras.getString("pcode");
            if(strUri != null && strUri.length() > 0)    {
                processScan(strUri);
            }
            if(strPCode != null && strPCode.length() > 0)    {
                processPCode(strPCode, null);
            }
        }

        validateSpend();

    }

    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(SendActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        SendActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_sweep).setVisible(false);
        menu.findItem(R.id.action_backup).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_share_receive).setVisible(false);
        menu.findItem(R.id.action_tor).setVisible(false);
        menu.findItem(R.id.action_sign).setVisible(false);
        menu.findItem(R.id.action_show_addresses).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_scan_qr) {
            doScan();
        }
        else if (id == R.id.action_ricochet) {
            Intent intent = new Intent(SendActivity.this, RicochetActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_empty_ricochet) {
            emptyRicochetQueue();
        }
        else if (id == R.id.action_utxo) {
            doUTXO();
        }
        else if (id == R.id.action_fees) {
            doFees();
        }
        else if (id == R.id.action_batch) {
            doBatchSpend();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_QR)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                processScan(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_QR)	{
            ;
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == RICOCHET)	{
            ;
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == RICOCHET)	{
            ;
        }
        else {
            ;
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {

            if(bViaMenu)    {
                SendActivity.this.finish();
            }
            else    {
                Intent _intent = new Intent(SendActivity.this, BalanceActivity.class);
                _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(_intent);
            }

            return true;
        }
        else	{
            ;
        }

        return false;
    }

    private void doScan() {
        Intent intent = new Intent(SendActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_QR);
    }

    private void processScan(String data) {

        if(data.contains("https://bitpay.com"))	{

            AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
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
            if(!isFinishing())    {
                dlg.show();
            }

            return;
        }

        if(FormatsUtil.getInstance().isValidPaymentCode(data))	{
            processPCode(data, null);
            return;
        }

        if(FormatsUtil.getInstance().isBitcoinUri(data))	{
            String address = FormatsUtil.getInstance().getBitcoinAddress(data);
            String amount = FormatsUtil.getInstance().getBitcoinAmount(data);

            edAddress.setText(address);

            if(amount != null) {
                try {
                    NumberFormat btcFormat = NumberFormat.getInstance(Locale.US);
                    btcFormat.setMaximumFractionDigits(8);
                    btcFormat.setMinimumFractionDigits(1);
                    edAmountBTC.setText(btcFormat.format(Double.parseDouble(amount) / 1e8));
                }
                catch (NumberFormatException nfe) {
                    edAmountBTC.setText("0.0");
                }
            }

            tvFiatSymbol.setText(getDisplayUnits() + "-" + strFiat);

            final String strAmount;
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            nf.setMinimumIntegerDigits(1);
            nf.setMinimumFractionDigits(1);
            nf.setMaximumFractionDigits(8);
            strAmount = nf.format(balance / 1e8);
            tvMax.setText(strAmount + " " + getDisplayUnits());

            try {
                if(amount != null && Double.parseDouble(amount) != 0.0)    {
                    edAddress.setEnabled(false);
                    edAmountBTC.setEnabled(false);
                    edAmountFiat.setEnabled(false);
//                    Toast.makeText(SendActivity.this, R.string.no_edit_BIP21_scan, Toast.LENGTH_SHORT).show();
                }
            }
            catch (NumberFormatException nfe) {
                edAmountBTC.setText("0.0");
            }

        }
        else if(FormatsUtil.getInstance().isValidBitcoinAddress(data))	{
            edAddress.setText(data);
        }
        else if(data.indexOf("?") != -1)   {

            String pcode = data.substring(0, data.indexOf("?"));
            // not valid BIP21 but seen often enough
            if(pcode.startsWith("bitcoin://"))    {
                pcode = pcode.substring(10);
            }
            if(pcode.startsWith("bitcoin:"))    {
                pcode = pcode.substring(8);
            }
            if(FormatsUtil.getInstance().isValidPaymentCode(pcode)) {
                processPCode(pcode, data.substring(data.indexOf("?")));
            }
        }
        else	{
            Toast.makeText(SendActivity.this, R.string.scan_error, Toast.LENGTH_SHORT).show();
        }

        validateSpend();
    }

    private void processPCode(String pcode, String meta) {

        if(FormatsUtil.getInstance().isValidPaymentCode(pcode))	{

            if(BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_CFM)    {
                try {
                    PaymentCode _pcode = new PaymentCode(pcode);
                    PaymentAddress paymentAddress = BIP47Util.getInstance(SendActivity.this).getSendAddress(_pcode, BIP47Meta.getInstance().getOutgoingIdx(pcode));

                    strDestinationBTCAddress = paymentAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    strPCode = _pcode.toString();
                    edAddress.setText(BIP47Meta.getInstance().getDisplayLabel(strPCode));
                    edAddress.setEnabled(false);
                }
                catch(Exception e) {
                    Toast.makeText(SendActivity.this, R.string.error_payment_code, Toast.LENGTH_SHORT).show();
                }
            }
            else    {
//                Toast.makeText(SendActivity.this, "Payment must be added and notification tx sent", Toast.LENGTH_SHORT).show();

                if(meta != null && meta.startsWith("?") && meta.length() > 1)    {
                    meta = meta.substring(1);
                }

                Intent intent = new Intent(SendActivity.this, BIP47Activity.class);
                intent.putExtra("pcode", pcode);
                if(meta != null && meta.length() > 0)    {
                    intent.putExtra("meta", meta);
                }
                startActivity(intent);
            }

        }
        else	{
            Toast.makeText(SendActivity.this, R.string.invalid_payment_code, Toast.LENGTH_SHORT).show();
        }

    }

    private void validateSpend() {

        boolean isValid = false;
        boolean insufficientFunds = false;

        double btc_amount = 0.0;

        String strBTCAddress = edAddress.getText().toString().trim();
        if(strBTCAddress.startsWith("bitcoin:"))    {
            edAddress.setText(strBTCAddress.substring(8));
        }

        try {
            btc_amount = NumberFormat.getInstance(Locale.US).parse(edAmountBTC.getText().toString().trim()).doubleValue();
//            Log.i("SendFragment", "amount entered:" + btc_amount);
        }
        catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        }
        catch (ParseException pe) {
            btc_amount = 0.0;
        }

        final double dAmount = btc_amount;

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

        return MonetaryUtil.getInstance().getBTCUnits();

    }

    private boolean hasBIP47UTXO(List<MyTransactionOutPoint> outPoints)    {

        List<String> addrs = BIP47Meta.getInstance().getUnspentAddresses(SendActivity.this, BIP47Util.getInstance(SendActivity.this).getWallet().getAccount(0).getPaymentCode());

        for(MyTransactionOutPoint o : outPoints)   {

            if(addrs.contains(o.getAddress()))    {
                return true;
            }

        }

        return false;
    }

    private String getCurrentFeeSetting()   {
        return getText(R.string.current_fee_selection) + "\n" + (FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() / 1000L) + " " + getText(R.string.slash_sat);
    }

    private void doCustomFee()   {

        long sanitySat = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;
        final long sanityValue = (long)(sanitySat * 1.5);

        final EditText etCustomFee = new EditText(SendActivity.this);
//        etCustomFee.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCustomFee.setText(Long.toString((FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() / 1000L)));

        InputFilter filter = new InputFilter() {

            String strCharset = "0123456789nollNOLL";

            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                for(int i = start; i < end; i++) {
                    if(strCharset.indexOf(source.charAt(i)) == -1) {
                        return "";
                    }
                }

                return null;
            }
        };

        etCustomFee.setFilters(new InputFilter[] { filter } );

        AlertDialog.Builder dlg = new AlertDialog.Builder(SendActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.set_sat)
                .setView(etCustomFee)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String strCustomFee = etCustomFee.getText().toString();
                        long customValue = 0L;

                        if(strCustomFee.equalsIgnoreCase("noll") && PrefsUtil.getInstance(SendActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true)    {
                            customValue = 0L;
                        }
                        else {

                            try {
                                customValue = Long.valueOf(strCustomFee);
                            } catch (Exception e) {
                                Toast.makeText(SendActivity.this, R.string.custom_fee_too_low, Toast.LENGTH_SHORT).show();
                                return;
                            }

                        }

                        if(customValue < 1 && !strCustomFee.equalsIgnoreCase("noll"))    {
                            Toast.makeText(SendActivity.this, R.string.custom_fee_too_low, Toast.LENGTH_SHORT).show();
                        }
                        else if(customValue > sanityValue)   {
                            Toast.makeText(SendActivity.this, R.string.custom_fee_too_high, Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            SuggestedFee suggestedFee = new SuggestedFee();
                            suggestedFee.setStressed(false);
                            suggestedFee.setOK(true);
                            suggestedFee.setDefaultPerKB(BigInteger.valueOf(customValue * 1000L));
                            FeeUtil.getInstance().setSuggestedFee(suggestedFee);

                            btLowFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                            btAutoFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                            btPriorityFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.darkgrey));
                            btCustomFee.setBackgroundColor(SendActivity.this.getResources().getColor(R.color.blue));

                            btCustomFee.setText((FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() / 1000L) + "\n" + getString(R.string.sat_b));
                            btCustomFee.setTypeface(null, Typeface.BOLD);
                            btLowFee.setTypeface(null, Typeface.NORMAL);
                            btAutoFee.setTypeface(null, Typeface.NORMAL);
                            btPriorityFee.setTypeface(null, Typeface.NORMAL);

                            long lowFee = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
                            long normalFee = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
                            long highFee = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

                            double pct = 0.0;
                            int nbBlocks = 6;
                            if(customValue == 0L)    {
                                customValue = 1L;
                            }
                            if(customValue <= lowFee)    {
                                pct = ((double)lowFee / (double)customValue);
                                nbBlocks = ((Double)Math.ceil(pct * 24.0)).intValue();
                            }
                            else if(customValue >= highFee)   {
                                pct = ((double)highFee / (double)customValue);
                                nbBlocks = ((Double)Math.ceil(pct * 2.0)).intValue();
                                if(nbBlocks < 1)    {
                                    nbBlocks = 1;
                                }
                            }
                            else    {
                                pct = ((double)normalFee / (double)customValue);
                                nbBlocks = ((Double)Math.ceil(pct * 6.0)).intValue();
                            }
                            tvFeePrompt.setText(getText(R.string.fee_custom_priority) + " " + nbBlocks  + " " + getText(R.string.blocks_to_cf));
                        }

                        dialog.dismiss();

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

    private void doUTXO()	{
        Intent intent = new Intent(SendActivity.this, UTXOActivity.class);
        startActivity(intent);
    }

    private void doBatchSpend()	{
        Intent intent = new Intent(SendActivity.this, BatchSendActivity.class);
        startActivity(intent);
    }

    private void doFees()	{

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
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

    private void emptyRicochetQueue()    {

        RicochetMeta.getInstance(SendActivity.this).setLastRicochet(null);
        RicochetMeta.getInstance(SendActivity.this).empty();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    PayloadUtil.getInstance(SendActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(SendActivity.this).getGUID() + AccessFactory.getInstance(SendActivity.this).getPIN()));
                }
                catch(Exception e) {
                    ;
                }

            }
        }).start();

    }

    private void doShowTx(final String hexTx, final String txHash) {

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        TextView showTx = new TextView(SendActivity.this);
        showTx.setText(hexTx);
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);

        final CheckBox cbMarkInputsUnspent = new CheckBox(SendActivity.this);
        cbMarkInputsUnspent.setText(R.string.mark_inputs_as_unspendable);
        cbMarkInputsUnspent.setChecked(false);

        LinearLayout hexLayout = new LinearLayout(SendActivity.this);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(cbMarkInputsUnspent);
        hexLayout.addView(showTx);

        new AlertDialog.Builder(SendActivity.this)
                .setTitle(txHash)
                .setView(hexLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(cbMarkInputsUnspent.isChecked())    {
                            markUTXOAsUnspendable(hexTx);
                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                            intent.putExtra("notifTx", false);
                            intent.putExtra("fetch", true);
                            LocalBroadcastManager.getInstance(SendActivity.this).sendBroadcast(intent);
                        }

                        dialog.dismiss();
                        SendActivity.this.finish();

                    }
                })
                .setNegativeButton(R.string.show_qr, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(cbMarkInputsUnspent.isChecked())    {
                            markUTXOAsUnspendable(hexTx);
                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                            intent.putExtra("notifTx", false);
                            intent.putExtra("fetch", true);
                            LocalBroadcastManager.getInstance(SendActivity.this).sendBroadcast(intent);
                        }

                        if(hexTx.length() <= QR_ALPHANUM_CHAR_LIMIT)    {

                            final ImageView ivQR = new ImageView(SendActivity.this);

                            Display display = (SendActivity.this).getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int imgWidth = Math.max(size.x - 240, 150);

                            Bitmap bitmap = null;

                            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(hexTx, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

                            try {
                                bitmap = qrCodeEncoder.encodeAsBitmap();
                            } catch (WriterException e) {
                                e.printStackTrace();
                            }

                            ivQR.setImageBitmap(bitmap);

                            LinearLayout qrLayout = new LinearLayout(SendActivity.this);
                            qrLayout.setOrientation(LinearLayout.VERTICAL);
                            qrLayout.addView(ivQR);

                            new AlertDialog.Builder(SendActivity.this)
                                    .setTitle(txHash)
                                    .setView(qrLayout)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            dialog.dismiss();
                                            SendActivity.this.finish();

                                        }
                                    })
                                    .setNegativeButton(R.string.share_qr, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String strFileName = AppUtil.getInstance(SendActivity.this).getReceiveQRFilename();
                                            File file = new File(strFileName);
                                            if(!file.exists()) {
                                                try {
                                                    file.createNewFile();
                                                }
                                                catch(Exception e) {
                                                    Toast.makeText(SendActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            file.setReadable(true, false);

                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(file);
                                            }
                                            catch(FileNotFoundException fnfe) {
                                                ;
                                            }

                                            if(file != null && fos != null) {
                                                Bitmap bitmap = ((BitmapDrawable)ivQR.getDrawable()).getBitmap();
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                                try {
                                                    fos.close();
                                                }
                                                catch(IOException ioe) {
                                                    ;
                                                }

                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.setType("image/png");
                                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                                startActivity(Intent.createChooser(intent, SendActivity.this.getText(R.string.send_tx)));
                                            }

                                        }
                                    }).show();
                        }
                        else    {

                            Toast.makeText(SendActivity.this, R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();

                        }

                    }
                }).show();

    }

    private void markUTXOAsUnspendable(String hexTx)    {

        HashMap<String, Long> utxos = new HashMap<String,Long>();

        for(UTXO utxo : APIFactory.getInstance(SendActivity.this).getUtxos(true))   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxos.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getValue().longValue());
            }
        }

        Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), Hex.decode(hexTx));
        for(TransactionInput input : tx.getInputs())   {
            BlockedUTXO.getInstance().add(input.getOutpoint().getHash().toString(), (int)input.getOutpoint().getIndex(), utxos.get(input.getOutpoint().getHash().toString() + "-" + (int)input.getOutpoint().getIndex()));
        }

    }

}
