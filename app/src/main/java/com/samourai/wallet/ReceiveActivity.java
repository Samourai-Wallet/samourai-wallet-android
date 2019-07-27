package com.samourai.wallet;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.DecimalDigitsInputFilter;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.uri.BitcoinURI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

public class ReceiveActivity extends AppCompatActivity {

    private static int imgWidth = 0;

    private ImageView ivQR = null;
    private TextView tvAddress = null;
    private TextView tvPath = null;

    private EditText edAmountBTC, edAmountSAT = null;
    private TextWatcher textWatcherBTC = null;
    private LinearLayout advancedButton = null;
    private ConstraintLayout advanceOptionsContainer = null;
    private Spinner addressTypesSpinner = null;

    private boolean useSegwit = true;

    private String defaultSeparator = null;

    private String addr = null;
    private String addr44 = null;
    private String addr49 = null;
    private String addr84 = null;
    private int idx44 = 0;
    private int idx49 = 0;
    private int idx84 = 0;

    private boolean canRefresh44 = false;
    private boolean canRefresh49 = false;
    private boolean canRefresh84 = false;
    private Menu _menu = null;

    public static final String ACTION_INTENT = "com.samourai.wallet.ReceiveFragment.REFRESH";

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {

                Bundle extras = intent.getExtras();
                if (extras != null && extras.containsKey("received_on")) {
                    String in_addr = extras.getString("received_on");

                    if (in_addr.equals(addr) || in_addr.equals(addr44) || in_addr.equals(addr49)) {
                        ReceiveActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                ReceiveActivity.this.finish();

                            }
                        });

                    }

                }

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle("");

        advanceOptionsContainer = findViewById(R.id.container_advance_options);
        tvAddress = findViewById(R.id.receive_address);
        tvPath = findViewById(R.id.path);
        addressTypesSpinner = findViewById(R.id.address_type_spinner);
        ivQR = findViewById(R.id.qr);
        advancedButton = findViewById(R.id.advance_button);
        edAmountBTC = findViewById(R.id.amountBTC);
        edAmountSAT = findViewById(R.id.amountSAT);
        populateSpinner();
        edAmountBTC.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(8,8)});

        edAmountBTC.addTextChangedListener(BTCWatcher);
        edAmountSAT.addTextChangedListener(satWatcher);

        Display display = (ReceiveActivity.this).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        imgWidth = Math.max(size.x - 460, 150);
        ivQR.setMaxWidth(imgWidth);

        useSegwit = PrefsUtil.getInstance(ReceiveActivity.this).getValue(PrefsUtil.USE_SEGWIT, true);

        advancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(advanceOptionsContainer, new AutoTransition());
                int visibility = advanceOptionsContainer.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
                advanceOptionsContainer.setVisibility(visibility);
            }
        });
        idx84 = BIP84Util.getInstance(ReceiveActivity.this).getWallet().getAccount(0).getChain(0).getAddrIdx();
        idx49 = BIP49Util.getInstance(ReceiveActivity.this).getWallet().getAccount(0).getChain(0).getAddrIdx();
        try {
            idx44 = HD_WalletFactory.getInstance(ReceiveActivity.this).get().getAccount(0).getChain(0).getAddrIdx();
        } catch (IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }
        addr84 = AddressFactory.getInstance(ReceiveActivity.this).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
        addr49 = AddressFactory.getInstance(ReceiveActivity.this).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
        addr44 = AddressFactory.getInstance(ReceiveActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();

        if (useSegwit && isBIP84Selected()) {
            addr = addr84;
        } else if (useSegwit && !isBIP84Selected()) {
            addr = addr49;
        } else {
            addr = addr44;
        }
        addressTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: {
                        addr = addr49;
                        break;
                    }
                    case 1: {
                        addr = addr84;
                        break;
                    }
                    case 2: {
                        addr = addr44;
                        break;
                    }
                    default: {
                        addr = addr49;
                    }
                }
                displayQRCode();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                ;
            }
        });

        if (useSegwit) {
            addressTypesSpinner.setSelection(1);
        } else {
            addressTypesSpinner.setSelection(2);
        }

        tvAddress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                new AlertDialog.Builder(ReceiveActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addr);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(ReceiveActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();

                return false;
            }
        });

        ivQR.setOnTouchListener(new OnSwipeTouchListener(ReceiveActivity.this) {
            @Override
            public void onSwipeLeft() {
                if (useSegwit && isBIP84Selected() && canRefresh84) {
                    addr84 = AddressFactory.getInstance(ReceiveActivity.this).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
                    addr = addr84;
                    canRefresh84 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                } else if (useSegwit && !isBIP84Selected() && canRefresh49) {
                    addr49 = AddressFactory.getInstance(ReceiveActivity.this).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
                    addr = addr49;
                    canRefresh49 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                } else if (!useSegwit && canRefresh44) {
                    addr44 = AddressFactory.getInstance(ReceiveActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                    addr = addr44;
                    canRefresh44 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                } else {
                    ;
                }

            }
        });

        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        defaultSeparator = Character.toString(symbols.getDecimalSeparator());

        displayQRCode();
    }


    private Double getSatValue(Double btc) {
        if (btc == 0) {
            return (double) 0;
        }
        return btc * 100000000;
    }


    private String formattedSatValue(Object number) {
        NumberFormat nformat = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat decimalFormat = (DecimalFormat) nformat;
        decimalFormat.applyPattern("#,###");
        return decimalFormat.format(number).replace(",", " ");
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
            edAmountSAT.removeTextChangedListener(satWatcher);
            edAmountBTC.removeTextChangedListener(this);

            try {
                if (editable.toString().length() == 0) {
                    edAmountSAT.setText("0");
                    edAmountBTC.setText("");
                    edAmountSAT.setSelection(edAmountSAT.getText().length());
                    edAmountSAT.addTextChangedListener(satWatcher);
                    edAmountBTC.addTextChangedListener(this);
                    return;
                }

                Double btc = Double.parseDouble(String.valueOf(editable));

                if (btc > 21000000.0) {
                    edAmountBTC.setText("0.00");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    edAmountSAT.setText("0");
                    edAmountSAT.setSelection(edAmountSAT.getText().length());
                    Toast.makeText(ReceiveActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
                else    {
                    DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
                    DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
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
                                    edAmountBTC.setText(s1.substring(0, s1.length() - 1));
                                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                                    editable = edAmountBTC.getEditableText();
                                    btc = Double.parseDouble(edAmountBTC.getText().toString());
                                }
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        ;
                    }
                    catch(ParseException pe) {
                        ;
                    }

                    Double sats = getSatValue(Double.valueOf(btc));
                    edAmountSAT.setText(formattedSatValue(sats));
                }

                //
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            edAmountSAT.addTextChangedListener(satWatcher);
            edAmountBTC.addTextChangedListener(this);

            displayQRCode();
        }
    };


    private Double getBtcValue(Double sats) {
        return (double) (sats / 1e8);
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
            edAmountSAT.removeTextChangedListener(this);
            edAmountBTC.removeTextChangedListener(BTCWatcher);

            try {
                if (editable.toString().length() == 0) {
                    edAmountBTC.setText("0.00");
                    edAmountSAT.setText("");
                    edAmountSAT.addTextChangedListener(this);
                    edAmountBTC.addTextChangedListener(BTCWatcher);
                    return;
                }
                String cleared_space = editable.toString().replace(" ", "");

                Double sats = Double.parseDouble(cleared_space);
                Double btc = getBtcValue(sats);
                String formatted = formattedSatValue(sats);


                edAmountSAT.setText(formatted);
                edAmountSAT.setSelection(formatted.length());
                edAmountBTC.setText(String.format(Locale.ENGLISH, "%.8f", btc));
                if (btc > 21000000.0) {
                    edAmountBTC.setText("0.00");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    edAmountSAT.setText("0");
                    edAmountSAT.setSelection(edAmountSAT.getText().length());
                    Toast.makeText(ReceiveActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();

            }
            edAmountSAT.addTextChangedListener(this);
            edAmountBTC.addTextChangedListener(BTCWatcher);
            displayQRCode();
        }
    };


    private void populateSpinner() {

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.address_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addressTypesSpinner.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(ReceiveActivity.this).registerReceiver(receiver, filter);

        AppUtil.getInstance(ReceiveActivity.this).checkTimeOut();

    }

    private boolean isBIP84Selected() {
        return addressTypesSpinner.getSelectedItemPosition() == 1;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(ReceiveActivity.this).unregisterReceiver(receiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.receive_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_share_receive: {
                new AlertDialog.Builder(ReceiveActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_share)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                String strFileName = AppUtil.getInstance(ReceiveActivity.this).getReceiveQRFilename();
                                File file = new File(strFileName);
                                if (!file.exists()) {
                                    try {
                                        file.createNewFile();
                                    } catch (Exception e) {
                                        Toast.makeText(ReceiveActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                file.setReadable(true, false);

                                FileOutputStream fos = null;
                                try {
                                    fos = new FileOutputStream(file);
                                } catch (FileNotFoundException fnfe) {
                                    ;
                                }

                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addr);
                                clipboard.setPrimaryClip(clip);

                                if (file != null && fos != null) {
                                    Bitmap bitmap = ((BitmapDrawable) ivQR.getDrawable()).getBitmap();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                    try {
                                        fos.close();
                                    } catch (IOException ioe) {
                                        ;
                                    }

                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_SEND);
                                    intent.setType("image/png");
                                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                                        //From API 24 sending FIle on intent ,require custom file provider
                                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                ReceiveActivity.this,
                                                getApplicationContext()
                                                        .getPackageName() + ".provider", file));
                                    } else {
                                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                    }
                                    startActivity(Intent.createChooser(intent, ReceiveActivity.this.getText(R.string.send_payment_code)));
                                }

                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();
                break;
            }
            case R.id.action_refresh: {
                if (useSegwit && isBIP84Selected() && canRefresh84) {
                    addr84 = AddressFactory.getInstance(ReceiveActivity.this).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
                    addr = addr84;
                    canRefresh84 = false;
                    item.setVisible(false);
                    displayQRCode();
                } else if (useSegwit && !isBIP84Selected() && canRefresh49) {
                    addr49 = AddressFactory.getInstance(ReceiveActivity.this).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
                    addr = addr49;
                    canRefresh49 = false;
                    item.setVisible(false);
                    displayQRCode();
                } else if (!useSegwit && canRefresh44) {
                    addr44 = AddressFactory.getInstance(ReceiveActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                    addr = addr44;
                    canRefresh44 = false;
                    item.setVisible(false);
                    displayQRCode();
                } else {
                    ;
                }
                break;
            }
            case R.id.action_support: {
                doSupport();
                break;
            }

//           Handle Toolbar back button press
            case android.R.id.home: {
                finish();
                break;
            }
        }


        return super.onOptionsItemSelected(item);
    }

    private void doSupport() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/section/7-receiving-bitcoin"));
        startActivity(intent);
    }

    private void displayQRCode() {

        String _addr = null;
        if (useSegwit && isBIP84Selected()) {
            _addr = addr.toUpperCase();
        } else {
            _addr = addr;
        }

        try {
            Number amount = NumberFormat.getInstance(Locale.US).parse(edAmountBTC.getText().toString().trim());

            long lamount = (long) (amount.doubleValue() * 1e8);
            if (lamount != 0L) {
                if (!FormatsUtil.getInstance().isValidBech32(_addr)) {
                    ivQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), _addr), Coin.valueOf(lamount), null, null)));
                } else {
                    String strURI = "bitcoin:" + _addr;
                    DecimalFormat df = new DecimalFormat("#");
                    df.setMinimumIntegerDigits(1);
                    df.setMaximumFractionDigits(8);
                    strURI += "?amount=" + df.format(amount);
                    ivQR.setImageBitmap(generateQRCode(strURI));
                }
            } else {
                ivQR.setImageBitmap(generateQRCode(_addr));
            }
        } catch (NumberFormatException nfe) {
            ivQR.setImageBitmap(generateQRCode(_addr));
        } catch (ParseException pe) {
            ivQR.setImageBitmap(generateQRCode(_addr));
        }

        tvAddress.setText(addr);
        displayPath();
        if(!AppUtil.getInstance(ReceiveActivity.this).isOfflineMode())    {
            checkPrevUse();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent _intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
                LocalBroadcastManager.getInstance(ReceiveActivity.this).sendBroadcast(_intent);
            }
        }).start();

    }

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private void checkPrevUse() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject jsonObject = APIFactory.getInstance(ReceiveActivity.this).getAddressInfo(addr);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (jsonObject != null && jsonObject.has("addresses") && jsonObject.getJSONArray("addresses").length() > 0) {
                                    JSONArray addrs = jsonObject.getJSONArray("addresses");
                                    JSONObject _addr = addrs.getJSONObject(0);
                                    if (_addr.has("n_tx") && _addr.getLong("n_tx") > 0L) {
                                        Toast.makeText(ReceiveActivity.this, R.string.address_used_previously, Toast.LENGTH_SHORT).show();
                                        if (useSegwit && isBIP84Selected()) {
                                            canRefresh84 = true;
                                        } else if (useSegwit && !isBIP84Selected()) {
                                            canRefresh49 = true;
                                        } else {
                                            canRefresh44 = true;
                                        }
                                        if (_menu != null) {
                                            _menu.findItem(R.id.action_refresh).setVisible(true);
                                        }
                                    } else {
                                        if (useSegwit && isBIP84Selected()) {
                                            canRefresh84 = false;
                                        } else if (useSegwit && !isBIP84Selected()) {
                                            canRefresh49 = false;
                                        } else {
                                            canRefresh44 = false;
                                        }
                                        if (_menu != null) {
                                            _menu.findItem(R.id.action_refresh).setVisible(false);
                                        }
                                    }
                                }

                            } catch (Exception e) {
                                if (useSegwit && isBIP84Selected()) {
                                    canRefresh84 = false;
                                } else if (useSegwit && !isBIP84Selected()) {
                                    canRefresh49 = false;
                                } else {
                                    canRefresh44 = false;
                                }
                                if (_menu != null) {
                                    _menu.findItem(R.id.action_refresh).setVisible(false);
                                }
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    if (useSegwit && isBIP84Selected()) {
                        canRefresh84 = false;
                    } else if (useSegwit && !isBIP84Selected()) {
                        canRefresh49 = false;
                    } else {
                        canRefresh44 = false;
                    }
                    if (_menu != null) {
                        _menu.findItem(R.id.action_refresh).setVisible(false);
                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void displayPath()  {

        int sel = addressTypesSpinner.getSelectedItemPosition();
        String path = "m/";
        int idx = 0 ;

        switch(sel)    {
            case 0:
                path += "49'";
                idx = BIP49Util.getInstance(ReceiveActivity.this).getWallet().getAccount(0).getChain(0).getAddrIdx() - 1;
                break;
            case 1:
                path += "84'";
                idx = BIP84Util.getInstance(ReceiveActivity.this).getWallet().getAccount(0).getChain(0).getAddrIdx() - 1;
                break;
            default:
                path += "44'";
                try {
                    idx = HD_WalletFactory.getInstance(ReceiveActivity.this).get().getAccount(0).getChain(0).getAddrIdx() -1;
                }
                catch(IOException | MnemonicException.MnemonicLengthException e) {
                    ;
                }
        }

        path += "/0'/0'/0/";
        path += idx;

        tvPath.setText(path);

    }

}