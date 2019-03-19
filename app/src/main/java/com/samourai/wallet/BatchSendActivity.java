package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.Vector;

import com.yanzhenjie.zbar.Symbol;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.util.encoders.Hex;

public class BatchSendActivity extends Activity {

    private final static int SCAN_QR = 2012;
    private final static int FEE_SELECT = 2013;

    private List<BatchSendUtil.BatchSend> data = null;
    private ListView listView = null;
    private BatchAdapter adapter = null;

    private Menu _menu = null;

    private TextView tvMaxPrompt = null;
    private TextView tvMax = null;
    private TextView tvCurrentFeePrompt = null;
    private long balance = 0L;
    private long displayBalance = 0L;

    private EditText edAddress = null;
    private String strDestinationBTCAddress = null;
    private TextWatcher textWatcherAddress = null;

    private EditText edAmountBTC = null;
    private TextWatcher textWatcherBTC = null;

    private String defaultSeparator = null;

    private String strPCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batchsend);

        setTitle(R.string.options_batch);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        BatchSendActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        data = new ArrayList<BatchSendUtil.BatchSend>();
        listView = (ListView)findViewById(R.id.list);
        adapter = new BatchAdapter();
        listView.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                ;
            }
        };

        tvMaxPrompt = (TextView)findViewById(R.id.max_prompt);
        tvMax = (TextView)findViewById(R.id.max);
        try    {
            balance = APIFactory.getInstance(BatchSendActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BatchSendActivity.this).get().getAccount(0).xpubstr());
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
        displayBalance = balance;

        final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
        nf.setMinimumIntegerDigits(1);

        tvMax.setText(nf.format(displayBalance / 1e8) + " " + getDisplayUnits());
        tvMaxPrompt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                edAmountBTC.setText(nf.format(displayBalance / 1e8));
                return false;
            }
        });
        tvMax.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                edAmountBTC.setText(nf.format(displayBalance / 1e8));
                return false;
            }
        });

        DecimalFormat format = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        defaultSeparator = Character.toString(symbols.getDecimalSeparator());

        edAddress = (EditText)findViewById(R.id.destination);

        textWatcherAddress = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                if(_menu != null)    {
                    if(edAddress.getText().toString().length() > 0)    {
                        _menu.findItem(R.id.action_scan_qr).setVisible(false);
                    }
                    else    {
                        _menu.findItem(R.id.action_scan_qr).setVisible(true);
                    }
                }

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

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(BatchSendActivity.this, android.R.layout.select_dialog_singlechoice);
                    for(int i = 0; i < entries.size(); i++)   {
                        arrayAdapter.add(BIP47Meta.getInstance().getDisplayLabel(entries.get(i)));
                    }

                    AlertDialog.Builder dlg = new AlertDialog.Builder(BatchSendActivity.this);
                    dlg.setIcon(R.drawable.ic_launcher);
                    dlg.setTitle(R.string.app_name);

                    dlg.setAdapter(arrayAdapter,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

//                                    Toast.makeText(BatchSendActivity.this, BIP47Meta.getInstance().getDisplayLabel(entries.get(which)), Toast.LENGTH_SHORT).show();
//                                    Toast.makeText(BatchSendActivity.this, entries.get(which), Toast.LENGTH_SHORT).show();

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

        textWatcherBTC = new TextWatcher() {

            public void afterTextChanged(Editable s) {

                edAmountBTC.removeTextChangedListener(this);

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
                    edAmountBTC.setText("0");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    Toast.makeText(BatchSendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }

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

        validateSpend();

        if(BatchSendUtil.getInstance().getSends().size() > 0)    {
            List<BatchSendUtil.BatchSend> sends = BatchSendUtil.getInstance().getSends();
            for(BatchSendUtil.BatchSend send : sends)   {
                data.add(send);
                displayBalance -= send.amount;
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    String strAmount = nf.format(displayBalance / 1e8);
                    tvMax.setText(strAmount + " " + getDisplayUnits());

                    if(_menu != null)    {
                        _menu.findItem(R.id.action_scan_qr).setVisible(true);
                        _menu.findItem(R.id.action_refresh).setVisible(true);
                        _menu.findItem(R.id.action_new).setVisible(false);
                        _menu.findItem(R.id.action_send).setVisible(true);
                    }
                }
            }, 1000L);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(BatchSendActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        BatchSendActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.batch_menu, menu);

        _menu = menu;

        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_new).setVisible(false);
        menu.findItem(R.id.action_send).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);

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
        else if (id == R.id.action_new) {
            doAddNew();
        }
        else if (id == R.id.action_support) {
            doSupport();
        }
        else if (id == R.id.action_refresh) {

            data.clear();
            edAddress.setText("");
            edAmountBTC.setText("");
            edAddress.setEnabled(true);

            strPCode = "";
            strDestinationBTCAddress = "";

            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            nf.setMaximumFractionDigits(8);
            nf.setMinimumFractionDigits(1);
            nf.setMinimumIntegerDigits(1);
            displayBalance = balance;
            String strAmount = nf.format(displayBalance / 1e8);
            tvMax.setText(strAmount + " " + getDisplayUnits());

            BatchSendUtil.getInstance().clear();

            refreshDisplay();

            _menu.findItem(R.id.action_scan_qr).setVisible(true);
            _menu.findItem(R.id.action_refresh).setVisible(false);
            _menu.findItem(R.id.action_new).setVisible(false);
            _menu.findItem(R.id.action_send).setVisible(false);
        }
        else if (id == R.id.action_send) {
            _menu.findItem(R.id.action_scan_qr).setVisible(false);
            _menu.findItem(R.id.action_refresh).setVisible(false);
            _menu.findItem(R.id.action_new).setVisible(false);
            _menu.findItem(R.id.action_send).setVisible(false);

            doFee();
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

                strPCode = null;
                strDestinationBTCAddress = null;

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                processScan(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_QR)	{
            ;
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == FEE_SELECT)	{

            Log.d("BatchSpendActivity", "selected fee:" + FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() / 1000L);

            doSpend();

        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == FEE_SELECT)	{
            ;
        }
        else {
            ;
        }

    }

    private void doScan() {
        Intent intent = new Intent(BatchSendActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_QR);
    }

    private void doSupport()	{
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/section/8-sending-bitcoin"));
        startActivity(intent);
    }

    private void doAddNew()  {

        BatchSendUtil.BatchSend dd = BatchSendUtil.getInstance().getBatchSend();
        if(FormatsUtil.getInstance().isValidBitcoinAddress(edAddress.getText().toString().trim()))    {
            dd.addr = edAddress.getText().toString().trim();
            dd.pcode = null;
        }
        else if(FormatsUtil.getInstance().isValidPaymentCode(strPCode))   {
            dd.addr = strDestinationBTCAddress;
            dd.pcode = strPCode;
            Toast.makeText(BatchSendActivity.this, dd.addr, Toast.LENGTH_SHORT).show();
        }
        else    {
            ;
        }

        double btc_amount = 0.0;

        try {
            btc_amount = NumberFormat.getInstance(Locale.US).parse(edAmountBTC.getText().toString().trim()).doubleValue();
        } catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        } catch (ParseException pe) {
            btc_amount = 0.0;
        }

        double dAmount = btc_amount;

        long amount = (long)(Math.round(dAmount * 1e8));;

        dd.amount = amount;

        data.add(dd);
        BatchSendUtil.getInstance().add(dd);

        edAmountBTC.setText("");
        edAddress.setText("");
        edAddress.setEnabled(true);

        strPCode = "";
        strDestinationBTCAddress = "";

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
        nf.setMinimumIntegerDigits(1);
        displayBalance -= amount;
        String strAmount = nf.format(displayBalance / 1e8);
        tvMax.setText(strAmount + " " + getDisplayUnits());

        if(_menu != null)    {
            _menu.findItem(R.id.action_scan_qr).setVisible(true);
            _menu.findItem(R.id.action_refresh).setVisible(true);
            _menu.findItem(R.id.action_new).setVisible(false);
            _menu.findItem(R.id.action_send).setVisible(true);
        }

        refreshDisplay();
    }

    private void refreshDisplay()   {
        adapter.notifyDataSetInvalidated();
    }

    private void processScan(String data) {

        if(data.contains("https://bitpay.com"))	{

            AlertDialog.Builder dlg = new AlertDialog.Builder(BatchSendActivity.this)
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

            final String strAmount;
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            nf.setMinimumIntegerDigits(1);
            nf.setMinimumFractionDigits(1);
            nf.setMaximumFractionDigits(8);
            strAmount = nf.format(displayBalance / 1e8);
            tvMax.setText(strAmount + " " + getDisplayUnits());

            try {
                if(amount != null && Double.parseDouble(amount) != 0.0)    {
                    edAddress.setEnabled(false);
                    edAmountBTC.setEnabled(false);
//                    Toast.makeText(BatchSendActivity.this, R.string.no_edit_BIP21_scan, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BatchSendActivity.this, R.string.scan_error, Toast.LENGTH_SHORT).show();
        }

        validateSpend();
    }

    private void processPCode(String pcode, String meta) {

        if(FormatsUtil.getInstance().isValidPaymentCode(pcode))	{

            if(BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_CFM)    {
                try {
                    PaymentCode _pcode = new PaymentCode(pcode);
                    PaymentAddress paymentAddress = BIP47Util.getInstance(BatchSendActivity.this).getSendAddress(_pcode, BIP47Meta.getInstance().getOutgoingIdx(pcode));

                    if(BIP47Meta.getInstance().getSegwit(pcode))    {
                        SegwitAddress segwitAddress = new SegwitAddress(paymentAddress.getSendECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                        strDestinationBTCAddress = segwitAddress.getBech32AsString();
                    }
                    else    {
                        strDestinationBTCAddress = paymentAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    }

                    strPCode = _pcode.toString();
                    edAddress.setText(BIP47Meta.getInstance().getDisplayLabel(strPCode));
                    edAddress.setEnabled(false);
                }
                catch(Exception e) {
                    Toast.makeText(BatchSendActivity.this, R.string.error_payment_code, Toast.LENGTH_SHORT).show();
                }
            }
            else    {
//                Toast.makeText(BatchSendActivity.this, "Payment must be added and notification tx sent", Toast.LENGTH_SHORT).show();

                if(meta != null && meta.startsWith("?") && meta.length() > 1)    {
                    meta = meta.substring(1);
                }

                Intent intent = new Intent(BatchSendActivity.this, BIP47Activity.class);
                intent.putExtra("pcode", pcode);
                if(meta != null && meta.length() > 0)    {
                    intent.putExtra("meta", meta);
                }
                startActivity(intent);
            }

        }
        else	{
            Toast.makeText(BatchSendActivity.this, R.string.invalid_payment_code, Toast.LENGTH_SHORT).show();
        }

    }

    private boolean validateSpend() {

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
        if(amount > displayBalance)    {
            insufficientFunds = true;
        }

//        Log.i("SendFragment", "insufficient funds:" + insufficientFunds);

        if(amount >= SamouraiWallet.bDust.longValue() && FormatsUtil.getInstance().isValidBitcoinAddress(edAddress.getText().toString().trim())) {
            isValid = true;
        }
        else if(amount >= SamouraiWallet.bDust.longValue() && strDestinationBTCAddress != null && FormatsUtil.getInstance().isValidBitcoinAddress(strDestinationBTCAddress)) {
            isValid = true;
        }
        else    {
            isValid = false;
        }

        if(_menu != null)    {
            if(!isValid || insufficientFunds) {
                _menu.findItem(R.id.action_send).setVisible(false);
                _menu.findItem(R.id.action_new).setVisible(false);
                return false;
            }
            else {
                _menu.findItem(R.id.action_send).setVisible(false);
                _menu.findItem(R.id.action_new).setVisible(true);
                return true;
            }
        }

        return false;

    }

    private void doFee() {
        Intent intent = new Intent(BatchSendActivity.this, FeeActivity.class);
        startActivityForResult(intent, FEE_SELECT);
    }

    private void doSpend()  {

        final HashMap<String,BigInteger> receivers = new HashMap<String,BigInteger>();
        long amount = 0L;
        for(BatchSendUtil.BatchSend _data : data)   {
            Log.d("BatchSendActivity", "output:" + _data.amount);
            Log.d("BatchSendActivity", "output:" + _data.addr);
            Log.d("BatchSendActivity", "output:" + _data.pcode);
            amount += _data.amount;
            if(receivers.containsKey(_data.addr))    {
                BigInteger _amount = receivers.get(_data.addr);
                receivers.put(_data.addr, _amount.add(BigInteger.valueOf(_data.amount)));
            }
            else    {
                receivers.put(_data.addr, BigInteger.valueOf(_data.amount));
            }
        }
        Log.d("BatchSendActivity", "amount:" + amount);

        List<UTXO> utxos = APIFactory.getInstance(BatchSendActivity.this).getUtxos(true);
        Collections.sort(utxos, new UTXO.UTXOComparator());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        int p2pkh = 0;
        int p2sh_p2wpkh = 0;
        int p2wpkh = 0;
        long totalValueSelected = 0L;
        int totalSelected = 0;

        for(UTXO utxo : utxos)   {

            Log.d("BatchSendActivity", "utxo value:" + utxo.getValue());
            selectedUTXO.add(utxo);
            totalValueSelected += utxo.getValue();
            totalSelected += utxo.getOutpoints().size();

            Triple<Integer,Integer,Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
            p2pkh += outpointTypes.getLeft();
            p2sh_p2wpkh += outpointTypes.getMiddle();
            p2wpkh += outpointTypes.getRight();
            if(totalValueSelected >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, receivers.size() + 1).longValue()))    {
                break;
            }
        }

        Log.d("BatchSendActivity", "totalSelected:" + totalSelected);
        Log.d("BatchSendActivity", "totalValueSelected:" + totalValueSelected);

        final List<MyTransactionOutPoint> outpoints = new ArrayList<MyTransactionOutPoint>();
        for(UTXO utxo : selectedUTXO)   {
            outpoints.addAll(utxo.getOutpoints());

            for(MyTransactionOutPoint out : utxo.getOutpoints())   {
                Log.d("BatchSendActivity", "outpoint hash:" + out.getTxHash().toString());
                Log.d("BatchSendActivity", "outpoint idx:" + out.getTxOutputN());
                Log.d("BatchSendActivity", "outpoint address:" + out.getAddress());
            }

        }
        Triple<Integer,Integer,Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(outpoints));
        BigInteger fee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), receivers.size() + 1);
        Log.d("BatchSendActivity", "fee:" + fee.longValue());

        if(amount + fee.longValue() > balance)    {
            Toast.makeText(BatchSendActivity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
            return;
        }

        long changeAmount = totalValueSelected - (amount + fee.longValue());
        String change_address = null;
        int change_idx = 0;
        if(changeAmount > 0L)    {
            change_idx = BIP49Util.getInstance(BatchSendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx();
            change_address = BIP49Util.getInstance(BatchSendActivity.this).getAddressAt(AddressFactory.CHANGE_CHAIN, change_idx).getAddressAsString();
            receivers.put(change_address, BigInteger.valueOf(changeAmount));
            Log.d("BatchSendActivity", "change output:" + changeAmount);
            Log.d("BatchSendActivity", "change output:" + change_address);
        }

        Transaction tx = SendFactory.getInstance(BatchSendActivity.this).makeTransaction(0, outpoints, receivers);

        if(tx != null)    {

            final RBFSpend rbf;
            if(PrefsUtil.getInstance(BatchSendActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true)    {

                rbf = new RBFSpend();

                for(TransactionInput input : tx.getInputs())    {

                    boolean _isBIP49 = false;
                    boolean _isBIP84 = false;
                    String _addr = null;
                    String script = Hex.toHexString(input.getConnectedOutput().getScriptBytes());
                    if(Bech32Util.getInstance().isBech32Script(script))    {
                        try {
                            _addr = Bech32Util.getInstance().getAddressFromScript(script);
                            _isBIP84 = true;
                        }
                        catch(Exception e) {
                            ;
                        }
                    }
                    else    {
                        Address _address = input.getConnectedOutput().getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                        if(_address != null)    {
                            _addr = _address.toString();
                            _isBIP49 = true;
                        }
                    }
                    if(_addr == null)    {
                        _addr = input.getConnectedOutput().getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    }

                    String path = APIFactory.getInstance(BatchSendActivity.this).getUnspentPaths().get(_addr);
                    if(path != null)    {
                        if(_isBIP84)    {
                            rbf.addKey(input.getOutpoint().toString(), path + "/84");
                        }
                        else if(_isBIP49)    {
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

            String strChangeIsDust = "";
            String strPrivacyWarning = "";
            String strMessage = strChangeIsDust + strPrivacyWarning + "Send " + Coin.valueOf(amount).toPlainString() + " BTC. (fee:" + Coin.valueOf(fee.longValue()).toPlainString() + ")?\n";

            final long _change = changeAmount;
            final String _change_address = change_address;
            final int _change_idx = change_idx;

            final long _amount = amount;

            tx = SendFactory.getInstance(BatchSendActivity.this).signTransaction(tx);
            final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
            final String strTxHash = tx.getHashAsString();

            //        Log.d("BatchSendActivity", "tx hash:" + tx.getHashAsString());
            //        Log.d("BatchSendActivity", "hex signed tx:" + hexTx);

            AlertDialog.Builder dlg = new AlertDialog.Builder(BatchSendActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(strMessage)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int whichButton) {

                            dialog.dismiss();

                            if(PrefsUtil.getInstance(BatchSendActivity.this).getValue(PrefsUtil.BROADCAST_TX, true) == false)    {

                                doShowTx(hexTx, strTxHash);

                                return;

                            }

                            SendParams.getInstance().setParams(outpoints,
                                    receivers,
                                    data,
                                    SendActivity.SPEND_SIMPLE,
                                    _change,
                                    49,
                                    "",
                                    false,
                                    false,
                                    _amount,
                                    _change_idx
                            );
                            Intent _intent = new Intent(BatchSendActivity.this, TxAnimUIActivity.class);
                            startActivity(_intent);

                        }
                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            dialog.dismiss();

                        }
                    });
            if(!isFinishing())    {
                dlg.show();
            }

        }
        else    {
//                                Log.d("SendActivity", "tx error");
            Toast.makeText(BatchSendActivity.this, "tx error", Toast.LENGTH_SHORT).show();
        }

    }

    private String getDisplayUnits() {

        return MonetaryUtil.getInstance().getBTCUnits();

    }

    private class BatchAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;

        BatchAdapter() {
            inflater = (LayoutInflater)BatchSendActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return (String)data.get(position).addr;
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            view = inflater.inflate(R.layout.simple_list_item3, parent, false);

            TextView text1 = (TextView) view.findViewById(R.id.text1);
            TextView text2 = (TextView) view.findViewById(R.id.text2);
            TextView text3 = (TextView) view.findViewById(R.id.text3);
            text3.setVisibility(View.GONE);

            if(data.get(position).pcode != null)    {
                String label = BIP47Meta.getInstance().getDisplayLabel(data.get(position).pcode);
                text1.setText(label);
            }
            else    {
                String addr = data.get(position).addr;
                text1.setText(addr);
            }

            final DecimalFormat df = new DecimalFormat("#");
            df.setMinimumIntegerDigits(1);
            df.setMinimumFractionDigits(8);
            df.setMaximumFractionDigits(8);

            text2.setText(df.format(((double)(data.get(position).amount) / 1e8)) + " BTC");

            return view;
        }

    }

    private void doShowTx(final String hexTx, final String txHash) {

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        TextView showTx = new TextView(BatchSendActivity.this);
        showTx.setText(hexTx);
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);

        final CheckBox cbMarkInputsUnspent = new CheckBox(BatchSendActivity.this);
        cbMarkInputsUnspent.setText(R.string.mark_inputs_as_unspendable);
        cbMarkInputsUnspent.setChecked(false);

        LinearLayout hexLayout = new LinearLayout(BatchSendActivity.this);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(cbMarkInputsUnspent);
        hexLayout.addView(showTx);

        new AlertDialog.Builder(BatchSendActivity.this)
                .setTitle(txHash)
                .setView(hexLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(cbMarkInputsUnspent.isChecked())    {
                            UTXOFactory.getInstance(BatchSendActivity.this).markUTXOAsUnspendable(hexTx);
                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                            intent.putExtra("notifTx", false);
                            intent.putExtra("fetch", true);
                            LocalBroadcastManager.getInstance(BatchSendActivity.this).sendBroadcast(intent);
                        }

                        dialog.dismiss();
                        BatchSendActivity.this.finish();

                    }
                })
                .setNegativeButton(R.string.show_qr, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(cbMarkInputsUnspent.isChecked())    {
                            UTXOFactory.getInstance(BatchSendActivity.this).markUTXOAsUnspendable(hexTx);
                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                            intent.putExtra("notifTx", false);
                            intent.putExtra("fetch", true);
                            LocalBroadcastManager.getInstance(BatchSendActivity.this).sendBroadcast(intent);
                        }

                        if(hexTx.length() <= QR_ALPHANUM_CHAR_LIMIT)    {

                            final ImageView ivQR = new ImageView(BatchSendActivity.this);

                            Display display = (BatchSendActivity.this).getWindowManager().getDefaultDisplay();
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

                            LinearLayout qrLayout = new LinearLayout(BatchSendActivity.this);
                            qrLayout.setOrientation(LinearLayout.VERTICAL);
                            qrLayout.addView(ivQR);

                            new AlertDialog.Builder(BatchSendActivity.this)
                                    .setTitle(txHash)
                                    .setView(qrLayout)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            dialog.dismiss();
                                            BatchSendActivity.this.finish();

                                        }
                                    })
                                    .setNegativeButton(R.string.share_qr, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String strFileName = AppUtil.getInstance(BatchSendActivity.this).getReceiveQRFilename();
                                            File file = new File(strFileName);
                                            if(!file.exists()) {
                                                try {
                                                    file.createNewFile();
                                                }
                                                catch(Exception e) {
                                                    Toast.makeText(BatchSendActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                                    //From API 24 sending FIle on intent ,require custom file provider
                                                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                            BatchSendActivity.this,
                                                            getApplicationContext()
                                                                    .getPackageName() + ".provider", file));
                                                } else {
                                                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                                }                                                startActivity(Intent.createChooser(intent, BatchSendActivity.this.getText(R.string.send_tx)));
                                            }

                                        }
                                    }).show();
                        }
                        else    {

                            Toast.makeText(BatchSendActivity.this, R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();

                        }

                    }
                }).show();

    }

}
