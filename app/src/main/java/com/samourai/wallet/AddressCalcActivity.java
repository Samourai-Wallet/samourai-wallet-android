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
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.ExchangeRateFactory;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import static java.lang.System.currentTimeMillis;

import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.yanzhenjie.zbar.Symbol;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

public class AddressCalcActivity extends Activity {

    private EditText edIndex = null;
    private Spinner spType = null;
    private RadioGroup rChain = null;
    private TextView tvChain = null;

    private Button btOK = null;
    private Button btCancel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_calc);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        edIndex = (EditText)findViewById(R.id.index);

        spType = (Spinner)findViewById(R.id.address_type_spinner);
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                if(position == 3 || position == 4)    {
                    tvChain.setVisibility(View.INVISIBLE);
                    rChain.setVisibility(View.INVISIBLE);
                }
                else    {
                    tvChain.setVisibility(View.VISIBLE);
                    rChain.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                ;
            }

        });
        populateSpinner();

        tvChain = (TextView)findViewById(R.id.chain_label);
        rChain = (RadioGroup)findViewById(R.id.chain);

        btOK = (Button)findViewById(R.id.ok);
        btOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    HD_Address hd_addr = null;
                    final SegwitAddress segwitAddress;
                    String strIndex = edIndex.getText().toString();

                    if(strIndex == null || strIndex.length() < 1)   {
                        Toast.makeText(AddressCalcActivity.this, R.string.invalid_index, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int index = Integer.parseInt(strIndex);

                    int chain = 0;
                    int selectedId = rChain.getCheckedRadioButtonId();
                    if(spType.getSelectedItemPosition() == 3 || spType.getSelectedItemPosition() == 4)    {
                        chain = 0;
                    }
                    else if(selectedId == R.id.change)    {
                        chain = 1;
                    }
                    else    {
                        chain = 0;
                    }

                    if(spType.getSelectedItemPosition() == 1)    {
                        hd_addr = BIP84Util.getInstance(AddressCalcActivity.this).getWallet().getAccountAt(0).getChain(chain).getAddressAt(index);
                        segwitAddress = new SegwitAddress(hd_addr.getECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    }
                    else if(spType.getSelectedItemPosition() == 2)    {
                        segwitAddress = null;
                        hd_addr = HD_WalletFactory.getInstance(AddressCalcActivity.this).get().getAccount(0).getChain(chain).getAddressAt(index);
                    }
                    else if(spType.getSelectedItemPosition() == 3)    {
                        hd_addr = BIP84Util.getInstance(AddressCalcActivity.this).getWallet().getAccountAt(RicochetMeta.getInstance(AddressCalcActivity.this).getRicochetAccount()).getChain(chain).getAddressAt(index);
                        segwitAddress = new SegwitAddress(hd_addr.getECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    }
                    else if(spType.getSelectedItemPosition() == 4)    {
                        hd_addr = BIP84Util.getInstance(AddressCalcActivity.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(AddressCalcActivity.this).getWhirlpoolPremixAccount()).getChain(chain).getAddressAt(index);
                        segwitAddress = new SegwitAddress(hd_addr.getECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    }
                    else if(spType.getSelectedItemPosition() == 5)    {
                        PaymentCode paymentCodeCP = BIP47Util.getInstance(AddressCalcActivity.this).getPaymentCode(WhirlpoolMeta.getInstance(AddressCalcActivity.this).getWhirlpoolPostmixCP());
                        PaymentAddress receiveAddress = BIP47Util.getInstance(AddressCalcActivity.this).getReceiveAddress(paymentCodeCP, WhirlpoolMeta.getInstance(AddressCalcActivity.this).getWhirlpoolPostmix(), index);
                        segwitAddress = new SegwitAddress(receiveAddress.getReceiveECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    }
                    else    {
                        hd_addr = BIP49Util.getInstance(AddressCalcActivity.this).getWallet().getAccountAt(0).getChain(chain).getAddressAt(index);
                        segwitAddress = new SegwitAddress(hd_addr.getECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    }

                    final ECKey ecKey;
                    final String strAddress;
                    if(spType.getSelectedItemPosition() == 2)    {
                        ecKey = hd_addr.getECKey();
                        strAddress = hd_addr.getAddressString();
                    }
                    else    {
                        ecKey = segwitAddress.getECKey();
                        if(spType.getSelectedItemPosition() == 0)    {
                            strAddress = segwitAddress.getAddressAsString();
                        }
                        else    {
                            strAddress = segwitAddress.getBech32AsString();
                        }
                    }

                    String message = spType.getSelectedItem().toString();
                    message += "\n";
                    message += (chain == 1) ? AddressCalcActivity.this.getText(R.string.change_chain) : AddressCalcActivity.this.getText(R.string.receive_chain);
                    message += "\n";
                    message += index + ":";
                    message += "\n";
                    message += strAddress;

                    final TextView tvText = new TextView(getApplicationContext());
//                    tvText.setTextSize(12);
                    tvText.setText(message);
                    tvText.setTextIsSelectable(true);
                    tvText.setPadding(40, 10, 40, 10);
                    tvText.setTextColor(0xffffffff);
                    AlertDialog.Builder dlg = new AlertDialog.Builder(AddressCalcActivity.this)
                            .setTitle(R.string.app_name)
                            .setView(tvText)
                            .setCancelable(true)
                            .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(R.string.options_display_privkey, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String strPrivKey = ecKey.getPrivateKeyAsWiF(SamouraiWallet.getInstance().getCurrentNetworkParams());

                                    ImageView showQR = new ImageView(AddressCalcActivity.this);
                                    Bitmap bitmap = null;
                                    QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strPrivKey, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
                                    try {
                                        bitmap = qrCodeEncoder.encodeAsBitmap();
                                    }
                                    catch (WriterException e) {
                                        e.printStackTrace();
                                    }
                                    showQR.setImageBitmap(bitmap);

                                    TextView showText = new TextView(AddressCalcActivity.this);
                                    showText.setText(strPrivKey);
                                    showText.setTextIsSelectable(true);
                                    showText.setPadding(40, 10, 40, 10);
                                    showText.setTextSize(18.0f);

                                    LinearLayout privkeyLayout = new LinearLayout(AddressCalcActivity.this);
                                    privkeyLayout.setOrientation(LinearLayout.VERTICAL);
                                    privkeyLayout.addView(showQR);
                                    privkeyLayout.addView(showText);

                                    new AlertDialog.Builder(AddressCalcActivity.this)
                                            .setTitle(R.string.app_name)
                                            .setView(privkeyLayout)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    ;
                                                }
                                            }).show();
                                }
                            });

                    if(spType.getSelectedItemPosition() != 2)    {
                            dlg.setPositiveButton(R.string.options_display_redeem_script, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        String redeemScript = org.spongycastle.util.encoders.Hex.toHexString(segwitAddress.segWitRedeemScript().getProgram());

                                        TextView showText = new TextView(AddressCalcActivity.this);
                                        showText.setText(redeemScript);
                                        showText.setTextIsSelectable(true);
                                        showText.setPadding(40, 10, 40, 10);
                                        showText.setTextSize(18.0f);

                                        new AlertDialog.Builder(AddressCalcActivity.this)
                                                .setTitle(R.string.app_name)
                                                .setView(showText)
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .show();

                                    }

                                });

                            }

                    if(!isFinishing())    {
                        dlg.show();
                    }

                }
                catch(Exception e) {
                    Toast.makeText(AddressCalcActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });

        btCancel = (Button)findViewById(R.id.cancel);
        btCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(AddressCalcActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void populateSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.account_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(adapter);
    }

}
