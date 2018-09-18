package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.AppUtil;

import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.core.ECKey;

public class PayNymCalcActivity extends Activity {

    private EditText edPayNym = null;
    private EditText edIndex = null;

    private Button btOK = null;
    private Button btCancel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paynym_calc);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        edPayNym = (EditText)findViewById(R.id.paynym);
        edIndex = (EditText)findViewById(R.id.index);

        edPayNym.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //final int DRAWABLE_LEFT = 0;
                //final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                //final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP && event.getRawX() >= (edPayNym.getRight() - edPayNym.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    final List<String> entries = new ArrayList<String>();
                    entries.addAll(BIP47Meta.getInstance().getSortedByLabels(true));

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(PayNymCalcActivity.this, android.R.layout.select_dialog_singlechoice);
                    for(int i = 0; i < entries.size(); i++)   {
                        arrayAdapter.add(BIP47Meta.getInstance().getDisplayLabel(entries.get(i)));
                    }

                    AlertDialog.Builder dlg = new AlertDialog.Builder(PayNymCalcActivity.this);
                    dlg.setIcon(R.drawable.ic_launcher);
                    dlg.setTitle(R.string.app_name);

                    dlg.setAdapter(arrayAdapter,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    edPayNym.setText(entries.get(which));
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

        btOK = (Button)findViewById(R.id.ok);
        btOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String strPayNym = edPayNym.getText().toString();
                String strIndex = edIndex.getText().toString();

                PaymentCode pcode = new PaymentCode(strPayNym);
                if(pcode == null)    {
                    Toast.makeText(PayNymCalcActivity.this, R.string.invalid_payment_code, Toast.LENGTH_SHORT).show();
                    return;
                }

                if(strIndex == null || strIndex.length() < 1)   {
                    Toast.makeText(PayNymCalcActivity.this, R.string.invalid_index, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int index = Integer.parseInt(strIndex);

                    String message = strPayNym;
                    final ECKey receiveECKey;
                    final SegwitAddress receiveSegwit;

                    PaymentAddress receiveAddress = BIP47Util.getInstance(PayNymCalcActivity.this).getReceiveAddress(new PaymentCode(strPayNym), index);
                    PaymentAddress sendAddress = BIP47Util.getInstance(PayNymCalcActivity.this).getSendAddress(new PaymentCode(strPayNym), index);
                    receiveECKey = receiveAddress.getReceiveECKey();
                    ECKey sendECKey = sendAddress.getSendECKey();

                    receiveSegwit = new SegwitAddress(receiveECKey, SamouraiWallet.getInstance().getCurrentNetworkParams());
                    SegwitAddress sendSegwit = new SegwitAddress(sendECKey, SamouraiWallet.getInstance().getCurrentNetworkParams());

                    message += "\n";
                    message += index + ":";
                    message += "\n";
                    message += "\n";
                    message += PayNymCalcActivity.this.getText(R.string.receive_addresses).toString() + ":";
                    message += "\n";
                    message += receiveECKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    message += "\n";
                    message += receiveSegwit.getAddressAsString();
                    message += "\n";
                    message += receiveSegwit.getBech32AsString();
                    message += "\n";
                    message += PayNymCalcActivity.this.getText(R.string.send_addresses).toString() + ":";
                    message += "\n";
                    message += sendECKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    message += "\n";
                    message += sendSegwit.getAddressAsString();
                    message += "\n";
                    message += sendSegwit.getBech32AsString();
                    message += "\n";

                    final TextView tvText = new TextView(getApplicationContext());
                    tvText.setTextSize(12);
                    tvText.setText(message);
                    tvText.setTextIsSelectable(true);
                    tvText.setPadding(40, 10, 40, 10);
                    tvText.setTextColor(0xffffffff);
                    AlertDialog.Builder dlg = new AlertDialog.Builder(PayNymCalcActivity.this)
                            .setTitle(R.string.app_name)
                            .setView(tvText)
                            .setCancelable(true)
                            .setPositiveButton(R.string.display_receive_redeem, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    String redeemScript = org.spongycastle.util.encoders.Hex.toHexString(receiveSegwit.segWitRedeemScript().getProgram());

                                    TextView showText = new TextView(PayNymCalcActivity.this);
                                    showText.setText(redeemScript);
                                    showText.setTextIsSelectable(true);
                                    showText.setPadding(40, 10, 40, 10);
                                    showText.setTextSize(18.0f);

                                    new AlertDialog.Builder(PayNymCalcActivity.this)
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

                            })
                            .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(R.string.display_receive_privkey, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String strPrivKey = receiveECKey.getPrivateKeyAsWiF(SamouraiWallet.getInstance().getCurrentNetworkParams());

                                    ImageView showQR = new ImageView(PayNymCalcActivity.this);
                                    Bitmap bitmap = null;
                                    QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strPrivKey, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
                                    try {
                                        bitmap = qrCodeEncoder.encodeAsBitmap();
                                    }
                                    catch (WriterException e) {
                                        e.printStackTrace();
                                    }
                                    showQR.setImageBitmap(bitmap);

                                    TextView showText = new TextView(PayNymCalcActivity.this);
                                    showText.setText(strPrivKey);
                                    showText.setTextIsSelectable(true);
                                    showText.setPadding(40, 10, 40, 10);
                                    showText.setTextSize(18.0f);

                                    LinearLayout privkeyLayout = new LinearLayout(PayNymCalcActivity.this);
                                    privkeyLayout.setOrientation(LinearLayout.VERTICAL);
                                    privkeyLayout.addView(showQR);
                                    privkeyLayout.addView(showText);

                                    new AlertDialog.Builder(PayNymCalcActivity.this)
                                            .setTitle(R.string.app_name)
                                            .setView(privkeyLayout)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    ;
                                                }
                                            }).show();
                                }
                            })
                            ;
                    if(!isFinishing())    {
                        dlg.show();
                    }

                }
                catch(Exception e) {
                    Toast.makeText(PayNymCalcActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
        AppUtil.getInstance(PayNymCalcActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
