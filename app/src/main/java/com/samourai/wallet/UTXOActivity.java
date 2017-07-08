package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UTXOActivity extends Activity {

    private List<Pair> data = null;
    private ListView listView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxo);
        setTitle(R.string.options_utxo);

        listView = (ListView)findViewById(R.id.list);

        data = new ArrayList<Pair>();
        for(UTXO utxo : APIFactory.getInstance(UTXOActivity.this).getUtxos())   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                Pair pair = Pair.of(outpoint.getAddress(), outpoint.getValue());
                data.add(pair);
            }
        }

        final DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

        ArrayAdapter adapter = new ArrayAdapter(UTXOActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, data) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                String addr = data.get(position).getLeft().toString();
                text1.setText(addr);
                if(isBIP47(addr))    {
                    text1.setTypeface(null, Typeface.ITALIC);
                }
                text2.setText(df.format(((double)((BigInteger)data.get(position).getRight()).longValue()) / 1e8) + " BTC");

                return view;
            }
        };
        listView.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                AlertDialog.Builder builder = new AlertDialog.Builder(UTXOActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage(R.string.prompt_privkey_or_explorer);
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.options_privkey, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int whichButton) {

                        String addr = data.get(position).getLeft().toString();
                        ECKey ecKey = SendFactory.getPrivKey(addr);
                        String strPrivKey = ecKey.getPrivateKeyAsWiF(MainNetParams.get());

                        ImageView showQR = new ImageView(UTXOActivity.this);
                        Bitmap bitmap = null;
                        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strPrivKey, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
                        try {
                            bitmap = qrCodeEncoder.encodeAsBitmap();
                        }
                        catch (WriterException e) {
                            e.printStackTrace();
                        }
                        showQR.setImageBitmap(bitmap);

                        TextView showText = new TextView(UTXOActivity.this);
                        showText.setText(strPrivKey);
                        showText.setTextIsSelectable(true);
                        showText.setPadding(40, 10, 40, 10);
                        showText.setTextSize(18.0f);

                        LinearLayout privkeyLayout = new LinearLayout(UTXOActivity.this);
                        privkeyLayout.setOrientation(LinearLayout.VERTICAL);
                        privkeyLayout.addView(showQR);
                        privkeyLayout.addView(showText);

                        new AlertDialog.Builder(UTXOActivity.this)
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
                builder.setNegativeButton(R.string.options_block_explorer, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int whichButton) {

                        int sel = PrefsUtil.getInstance(UTXOActivity.this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
                        if(sel >= BlockExplorerUtil.getInstance().getBlockExplorerAddressUrls().length)    {
                            sel = 0;
                        }
                        CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerAddressUrls()[sel];

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + data.get(position).getLeft().toString()));
                        startActivity(browserIntent);

                    }
                });
                builder.setNeutralButton(R.string.utxo_sign, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int whichButton) {

                        String addr = data.get(position).getLeft().toString();
                        ECKey ecKey = SendFactory.getPrivKey(addr);

                        if(ecKey != null)    {
                            MessageSignUtil.getInstance(UTXOActivity.this).doSign(UTXOActivity.this.getString(R.string.utxo_sign),
                                    UTXOActivity.this.getString(R.string.utxo_sign_text1),
                                    UTXOActivity.this.getString(R.string.utxo_sign_text2),
                                    ecKey);
                        }

                    }
                });

                AlertDialog alert = builder.create();
                alert.show();

            }
        };
        listView.setOnItemClickListener(listener);

    }

    @Override
    public void onResume() {

        super.onResume();

        AppUtil.getInstance(UTXOActivity.this).checkTimeOut();

    }

    private boolean isBIP47(String address)    {

        try {
            String path = APIFactory.getInstance(UTXOActivity.this).getUnspentPaths().get(address);
            if(path != null)    {
                return false;
            }
            else    {
                String pcode = BIP47Meta.getInstance().getPCode4Addr(address);

                if(pcode != null)    {
                    return true;
                }
                else    {
                    return false;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
