package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.segwit.P2SH_P2WPKH;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UTXOActivity extends Activity {

    private class DisplayData   {
        private String addr = null;
        private long amount = 0L;
        private String hash = null;
        private int idx = 0;
    }

    private List<DisplayData> data = null;
    private List<DisplayData> doNotSpend = null;
    private ListView listView = null;
    private UTXOAdapter adapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxo);
        setTitle(R.string.options_utxo);

        listView = (ListView)findViewById(R.id.list);

        data = new ArrayList<DisplayData>();
        doNotSpend = new ArrayList<DisplayData>();
        for(UTXO utxo : APIFactory.getInstance(UTXOActivity.this).getUtxos(false))   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                Pair pair = Pair.of(outpoint.getAddress(), BigInteger.valueOf(outpoint.getValue().longValue()));
                DisplayData displayData = new DisplayData();
                displayData.addr = outpoint.getAddress();
                displayData.amount = outpoint.getValue().longValue();
                displayData.hash = outpoint.getTxHash().toString();
                displayData.idx = outpoint.getTxOutputN();
                if(BlockedUTXO.getInstance().contains(outpoint.getTxHash().toString(), outpoint.getTxOutputN()))    {
                    doNotSpend.add(displayData);
                }
                else    {
                    data.add(displayData);
                }
            }
        }
        data.addAll(doNotSpend);

        adapter = new UTXOAdapter();
        listView.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                PopupMenu menu = new PopupMenu (UTXOActivity.this, view);
                menu.setOnMenuItemClickListener (new PopupMenu.OnMenuItemClickListener ()   {
                    @Override
                    public boolean onMenuItemClick (MenuItem item)  {
                        int id = item.getItemId();
                        switch (id) {
                            case R.id.item_do_not_spend:
                            {

                                if(BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx))    {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(UTXOActivity.this);
                                    builder.setTitle(R.string.dusting_tx);
                                    builder.setMessage(R.string.dusting_tx_unblock);
                                    builder.setCancelable(true);
                                    builder.setPositiveButton(R.string.dusting_unblock, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {

                                            BlockedUTXO.getInstance().remove(data.get(position).hash, data.get(position).idx);

                                        }
                                    });
                                    builder.setNegativeButton(R.string.dusting_unblock_ignore, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {

                                            BlockedUTXO.getInstance().addNotDusted(data.get(position).hash, data.get(position).idx);

                                        }
                                    });

                                    AlertDialog alert = builder.create();
                                    alert.show();

                                }

                            }

                            break;

                            case R.id.item_sign:
                            {
                                String addr = data.get(position).addr;
                                ECKey ecKey = SendFactory.getPrivKey(addr);
                                if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress())    {

                                    String msg = UTXOActivity.this.getString(R.string.utxo_sign_text3);
                                    msg += ":";
                                    msg += addr;
                                    msg += ", ";
                                    msg += "pubkey:";
                                    msg += ecKey.getPublicKeyAsHex();

                                    if(ecKey != null)    {
                                        MessageSignUtil.getInstance(UTXOActivity.this).doSign(UTXOActivity.this.getString(R.string.utxo_sign),
                                                UTXOActivity.this.getString(R.string.utxo_sign_text1),
                                                msg,
                                                ecKey);
                                    }

                                }
                                else    {

                                    if(ecKey != null)    {
                                        MessageSignUtil.getInstance(UTXOActivity.this).doSign(UTXOActivity.this.getString(R.string.utxo_sign),
                                                UTXOActivity.this.getString(R.string.utxo_sign_text1),
                                                UTXOActivity.this.getString(R.string.utxo_sign_text2),
                                                ecKey);
                                    }

                                }

                            }

                            break;

                            case R.id.item_redeem:
                            {
                                String addr = data.get(position).addr;
                                ECKey ecKey = SendFactory.getPrivKey(addr);
                                P2SH_P2WPKH p2sh_p2wpkh = new P2SH_P2WPKH(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());

                                if(ecKey != null && p2sh_p2wpkh != null) {

                                    String redeemScript = Hex.toHexString(p2sh_p2wpkh.segWitRedeemScript().getProgram());

                                    TextView showText = new TextView(UTXOActivity.this);
                                    showText.setText(redeemScript);
                                    showText.setTextIsSelectable(true);
                                    showText.setPadding(40, 10, 40, 10);
                                    showText.setTextSize(18.0f);

                                    new AlertDialog.Builder(UTXOActivity.this)
                                            .setTitle(R.string.app_name)
                                            .setView(showText)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    ;
                                                }
                                            })
                                            .show();
                                }
                            }

                            break;

                            case R.id.item_view:
                            {
                                int sel = PrefsUtil.getInstance(UTXOActivity.this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
                                if(sel >= BlockExplorerUtil.getInstance().getBlockExplorerAddressUrls().length)    {
                                    sel = 0;
                                }
                                CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerAddressUrls()[sel];

                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + data.get(position).addr));
                                startActivity(browserIntent);
                            }

                            break;

                            case R.id.item_privkey:
                            {
                                String addr = data.get(position).addr;
                                ECKey ecKey = SendFactory.getPrivKey(addr);
                                String strPrivKey = ecKey.getPrivateKeyAsWiF(SamouraiWallet.getInstance().getCurrentNetworkParams());

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

                            break;

                        }
                        return true;
                    }
                });
                menu.inflate (R.menu.utxo_menu);

                String addr = data.get(position).addr;
                if(!Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress())    {
                    menu.getMenu().findItem(R.id.item_redeem).setVisible(false);
                }

                if(!(BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx) && data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD))    {
                    menu.getMenu().findItem(R.id.item_do_not_spend).setVisible(false);
                }

                menu.show();

            }
        };

        listView.setOnItemClickListener(listener);

    }

    @Override
    public void onResume() {

        super.onResume();

        AppUtil.getInstance(UTXOActivity.this).checkTimeOut();

    }

    private class UTXOAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;

        UTXOAdapter() {
            inflater = (LayoutInflater)UTXOActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

            final DecimalFormat df = new DecimalFormat("#");
            df.setMinimumIntegerDigits(1);
            df.setMinimumFractionDigits(8);
            df.setMaximumFractionDigits(8);

            text1.setText(df.format(((double)(data.get(position).amount) / 1e8)) + " BTC");

            String addr = data.get(position).addr;
            text2.setText(addr);

            String descr = "";
            Spannable word = null;
            if(isBIP47(addr))    {
                String pcode = BIP47Meta.getInstance().getPCode4AddrLookup().get(addr);
                if(pcode != null && pcode.length() > 0)    {
                    descr = " " + BIP47Meta.getInstance().getDisplayLabel(pcode);
                }
                else    {
                    descr = " " + UTXOActivity.this.getText(R.string.paycode).toString();
                }
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFFd07de5), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if(data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx))    {
                descr = " " + UTXOActivity.this.getText(R.string.do_not_spend);
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFFe75454), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else if(data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().containsNotDusted(data.get(position).hash, data.get(position).idx))   {
                descr = " " + UTXOActivity.this.getText(R.string.dust);
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFF8c8c8c), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else    {
                ;
            }
            if(descr.length() > 0)    {
                text3.setText(word);
            }

            return view;
        }

    }

    private boolean isBIP47(String address)    {

        try {
            String path = APIFactory.getInstance(UTXOActivity.this).getUnspentPaths().get(address);
            if(path != null)    {
                return false;
            }
            else    {
                String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
                int idx = BIP47Meta.getInstance().getIdx4Addr(address);
                List<Integer> unspentIdxs = BIP47Meta.getInstance().getUnspent(pcode);

                if(unspentIdxs != null && unspentIdxs.contains(Integer.valueOf(idx)))    {
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
