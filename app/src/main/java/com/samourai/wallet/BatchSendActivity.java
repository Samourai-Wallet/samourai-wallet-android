package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.zbar.Symbol;

import org.bitcoinj.crypto.MnemonicException;

public class BatchSendActivity extends Activity {

    private final static int SCAN_QR = 2012;

    private class DisplayData   {
        private String dest = null;
        private String addr = null;
        private long amount = 0L;
    }

    private List<DisplayData> data = null;
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
    private EditText edAmountFiat = null;
    private TextWatcher textWatcherBTC = null;
    private TextWatcher textWatcherFiat = null;

    private String defaultSeparator = null;

    private String strFiat = null;

    private double btc_fx = 286.0;
    private TextView tvFiatSymbol = null;

    private int selectedAccount = 0;

    private String strPCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batchsend);

        setTitle(R.string.options_batch);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        BatchSendActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        data = new ArrayList<DisplayData>();
        listView = (ListView)findViewById(R.id.list);
        adapter = new BatchAdapter();
        listView.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                ;
            }
        };

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
            balance = APIFactory.getInstance(BatchSendActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BatchSendActivity.this).get().getAccount(selectedAccount).xpubstr());
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

        strFiat = PrefsUtil.getInstance(BatchSendActivity.this).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        btc_fx = ExchangeRateFactory.getInstance(BatchSendActivity.this).getAvgPrice(strFiat);
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
                    Toast.makeText(BatchSendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(BatchSendActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
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

        validateSpend();

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
            _menu.findItem(R.id.action_scan_qr).setVisible(true);
            _menu.findItem(R.id.action_refresh).setVisible(true);
            _menu.findItem(R.id.action_new).setVisible(false);
            _menu.findItem(R.id.action_send).setVisible(false);

            doAddNew();
        }
        else if (id == R.id.action_refresh) {

            data.clear();

            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            nf.setMaximumFractionDigits(8);
            nf.setMinimumFractionDigits(1);
            nf.setMinimumIntegerDigits(1);
            displayBalance = balance;
            String strAmount = nf.format(displayBalance / 1e8);
            tvMax.setText(strAmount + " " + getDisplayUnits());

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
        else {
            ;
        }

    }

    private void doScan() {
        Intent intent = new Intent(BatchSendActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_QR);
    }

    private void doAddNew()  {

        DisplayData dd = new DisplayData();
        dd.addr = edAddress.getText().toString();

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

        edAmountBTC.setText("");
        edAmountFiat.setText("");
        edAddress.setText("");

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
        nf.setMinimumIntegerDigits(1);
        displayBalance -= amount;
        String strAmount = nf.format(displayBalance / 1e8);
        tvMax.setText(strAmount + " " + getDisplayUnits());

        strDestinationBTCAddress = "";

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

            tvFiatSymbol.setText(getDisplayUnits() + "-" + strFiat);

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

                    strDestinationBTCAddress = paymentAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    strPCode = _pcode.toString();
                    edAddress.setText(BIP47Meta.getInstance().getDisplayLabel(strPCode));
                    edAddress.setEnabled(false);
                }
                catch(Exception e) {
                    Toast.makeText(BatchSendActivity.this, R.string.error_payment_code, Toast.LENGTH_SHORT).show();
                }
            }
            else    {
//                Toast.makeText(SendActivity.this, "Payment must be added and notification tx sent", Toast.LENGTH_SHORT).show();

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
        if(amount > displayBalance)    {
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

        if(_menu != null)    {
            if(!isValid || insufficientFunds) {
                _menu.findItem(R.id.action_send).setVisible(false);
                _menu.findItem(R.id.action_new).setVisible(false);
            }
            else {
                _menu.findItem(R.id.action_send).setVisible(true);
                _menu.findItem(R.id.action_new).setVisible(true);
            }
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

            String addr = data.get(position).addr;
            text1.setText(addr);

            final DecimalFormat df = new DecimalFormat("#");
            df.setMinimumIntegerDigits(1);
            df.setMinimumFractionDigits(8);
            df.setMaximumFractionDigits(8);

            text2.setText(df.format(((double)(data.get(position).amount) / 1e8)) + " BTC");

            return view;
        }

    }

}
