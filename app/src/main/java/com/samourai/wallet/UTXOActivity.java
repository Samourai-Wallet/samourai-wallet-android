package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.UTXOUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.samourai.wallet.util.LogUtil.debug;

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

    private long totalP2PKH = 0L;
    private long totalP2SH_P2WPKH = 0L;
    private long totalP2WPKH = 0L;
    private long totalBlocked = 0L;

    private int account = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxo);
        setTitle(R.string.options_utxo);

        data = new ArrayList<DisplayData>();
        doNotSpend = new ArrayList<DisplayData>();

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("_account")) {
            if (getIntent().getExtras().getInt("_account") == WhirlpoolMeta.getInstance(UTXOActivity.this).getWhirlpoolPostmix()) {
                account = WhirlpoolMeta.getInstance(UTXOActivity.this).getWhirlpoolPostmix();
            }
        }

        listView = (ListView)findViewById(R.id.list);

        update(false);
        adapter = new UTXOAdapter();
        listView.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                debug("UTXOActivity", "menu:" + data.get(position).addr);
                debug("UTXOActivity", "menu:" + data.get(position).hash + "-" + data.get(position).idx);

                PopupMenu menu = new PopupMenu (UTXOActivity.this, view, Gravity.RIGHT);
                menu.setOnMenuItemClickListener (new PopupMenu.OnMenuItemClickListener ()   {
                    @Override
                    public boolean onMenuItemClick (MenuItem item)  {
                        int id = item.getItemId();
                        switch (id) {
                            case R.id.item_tag:
                            {

                                final EditText edTag = new EditText(UTXOActivity.this);
                                edTag.setSingleLine(true);
                                if(UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx) != null)    {
                                    edTag.setText(UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx));
                                }

                                AlertDialog.Builder dlg = new AlertDialog.Builder(UTXOActivity.this)
                                        .setTitle(R.string.app_name)
                                        .setView(edTag)
                                        .setMessage(R.string.label)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                final String strTag = edTag.getText().toString().trim();

                                                if(strTag != null && strTag.length() > 0)    {
                                                    UTXOUtil.getInstance().add(data.get(position).hash + "-" + data.get(position).idx, strTag);
                                                }
                                                else    {
                                                    UTXOUtil.getInstance().remove(data.get(position).hash + "-" + data.get(position).idx);
                                                }

                                                update(false);

                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                ;
                                            }
                                        });
                                if(!isFinishing())    {
                                    dlg.show();
                                }

                            }

                            break;

                            case R.id.item_do_not_spend:
                            {

                                if(data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx))    {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(UTXOActivity.this);
                                    builder.setTitle(R.string.dusting_tx);
                                    builder.setMessage(R.string.dusting_tx_unblock);
                                    builder.setCancelable(true);
                                    builder.setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {
                                            ;
                                        }
                                    });
                                    builder.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {

                                            BlockedUTXO.getInstance().remove(data.get(position).hash, data.get(position).idx);
                                            BlockedUTXO.getInstance().addNotDusted(data.get(position).hash, data.get(position).idx);

                                            update(true);

                                        }
                                    });

                                    AlertDialog alert = builder.create();
                                    alert.show();

                                }
                                else if(BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx))    {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(UTXOActivity.this);
                                    builder.setTitle(R.string.mark_spend);
                                    builder.setMessage(R.string.mark_utxo_spend);
                                    builder.setCancelable(true);
                                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {

                                            BlockedUTXO.getInstance().remove(data.get(position).hash, data.get(position).idx);

                                            debug("UTXOActivity", "removed:" + data.get(position).hash + "-" + data.get(position).idx);

                                            update(true);

                                        }
                                    });
                                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {
                                            ;
                                        }
                                    });

                                    AlertDialog alert = builder.create();
                                    alert.show();

                                }
                                else if(BlockedUTXO.getInstance().containsPostMix(data.get(position).hash, data.get(position).idx))    {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(UTXOActivity.this);
                                    builder.setTitle(R.string.mark_spend);
                                    builder.setMessage(R.string.mark_utxo_spend);
                                    builder.setCancelable(true);
                                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {

                                            BlockedUTXO.getInstance().removePostMix(data.get(position).hash, data.get(position).idx);

                                            debug("UTXOActivity", "removed:" + data.get(position).hash + "-" + data.get(position).idx);

                                            update(true);

                                        }
                                    });
                                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {
                                            ;
                                        }
                                    });

                                    AlertDialog alert = builder.create();
                                    alert.show();

                                }
                                else    {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(UTXOActivity.this);
                                    builder.setTitle(R.string.mark_do_not_spend);
                                    builder.setMessage(R.string.mark_utxo_do_not_spend);
                                    builder.setCancelable(true);
                                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {

                                            if(account == 0)    {
                                                BlockedUTXO.getInstance().add(data.get(position).hash, data.get(position).idx, data.get(position).amount);
                                            }
                                            else    {
                                                BlockedUTXO.getInstance().addPostMix(data.get(position).hash, data.get(position).idx, data.get(position).amount);
                                            }

                                            debug("UTXOActivity", "added:" + data.get(position).hash + "-" + data.get(position).idx);

                                            update(true);

                                        }
                                    });
                                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, int whichButton) {
                                            ;
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
                                ECKey ecKey = SendFactory.getPrivKey(addr, account);
                                String msg = null;

                                if(FormatsUtil.getInstance().isValidBech32(addr) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress())    {

                                    msg = UTXOActivity.this.getString(R.string.utxo_sign_text3);

                                    try {
                                        JSONObject obj = new JSONObject();
                                        obj.put("pubkey", ecKey.getPublicKeyAsHex());
                                        obj.put("address", addr);
                                        msg +=  " " + obj.toString();
                                    }
                                    catch(JSONException je) {
                                        msg += ":";
                                        msg += addr;
                                        msg += ", ";
                                        msg += "pubkey:";
                                        msg += ecKey.getPublicKeyAsHex();
                                    }

                                }
                                else    {

                                    msg = UTXOActivity.this.getString(R.string.utxo_sign_text2);

                                }

                                if(ecKey != null)    {
                                    MessageSignUtil.getInstance(UTXOActivity.this).doSign(UTXOActivity.this.getString(R.string.utxo_sign),
                                            UTXOActivity.this.getString(R.string.utxo_sign_text1),
                                            msg,
                                            ecKey);
                                }

                            }

                            break;

                            case R.id.item_redeem:
                            {
                                String addr = data.get(position).addr;
                                ECKey ecKey = SendFactory.getPrivKey(addr, account);
                                SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());

                                if(ecKey != null && segwitAddress != null) {

                                    String redeemScript = Hex.toHexString(segwitAddress.segWitRedeemScript().getProgram());

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
                                String blockExplorer = "https://m.oxt.me/transaction/";
                                if (SamouraiWallet.getInstance().isTestNet()) {
                                    blockExplorer = "https://blockstream.info/testnet/";
                                }
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + data.get(position).hash));
                                startActivity(browserIntent);
                            }

                            break;

                            case R.id.item_privkey:
                            {
                                String addr = data.get(position).addr;
                                ECKey ecKey = SendFactory.getPrivKey(addr, account);
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
                menu.inflate (R.menu.utxo_popup_menu);

                if(BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx) ||
                        BlockedUTXO.getInstance().containsPostMix(data.get(position).hash, data.get(position).idx))    {
                    menu.getMenu().findItem(R.id.item_do_not_spend).setTitle(R.string.mark_spend);
                }
                else    {
                    menu.getMenu().findItem(R.id.item_do_not_spend).setTitle(R.string.mark_do_not_spend);
                }

                String addr = data.get(position).addr;
                if(!FormatsUtil.getInstance().isValidBech32(addr) && !Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress())    {
                    menu.getMenu().findItem(R.id.item_redeem).setVisible(false);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.utxo_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_utxo_amounts) {
            doDisplayAmounts();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void update(boolean broadcast)   {

        totalP2SH_P2WPKH = totalP2PKH = totalP2WPKH = totalBlocked = 0L;
        data.clear();
        doNotSpend.clear();

        List<UTXO> utxos = null;
        if(account == WhirlpoolMeta.getInstance(UTXOActivity.this).getWhirlpoolPostmix())    {
            utxos = APIFactory.getInstance(UTXOActivity.this).getUtxosPostMix(false);
        }
        else    {
            utxos = APIFactory.getInstance(UTXOActivity.this).getUtxos(false);
        }

        for(UTXO utxo : utxos)   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                DisplayData displayData = new DisplayData();
                displayData.addr = outpoint.getAddress();
                displayData.amount = outpoint.getValue().longValue();
                displayData.hash = outpoint.getTxHash().toString();
                displayData.idx = outpoint.getTxOutputN();
                if(BlockedUTXO.getInstance().contains(outpoint.getTxHash().toString(), outpoint.getTxOutputN()))    {
                    doNotSpend.add(displayData);
//                    debug("UTXOActivity", "marked as do not spend");
                    totalBlocked += displayData.amount;
                }
                else if(BlockedUTXO.getInstance().containsPostMix(outpoint.getTxHash().toString(), outpoint.getTxOutputN()))    {
                    doNotSpend.add(displayData);
//                    debug("UTXOActivity", "marked as do not spend");
                    totalBlocked += displayData.amount;
                }
                else    {
                    data.add(displayData);
//                    debug("UTXOActivity", "unmarked");
                    if(FormatsUtil.getInstance().isValidBech32(displayData.addr))    {
                        totalP2WPKH += displayData.amount;
                    }
                    else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), displayData.addr).isP2SHAddress())  {
                        totalP2SH_P2WPKH += displayData.amount;
                    }
                    else    {
                        totalP2PKH += displayData.amount;
                    }
                }
            }
        }

        data.addAll(doNotSpend);

        if(adapter != null)    {
            adapter.notifyDataSetInvalidated();
//            debug("UTXOActivity", "nb:" + data.size());
        }

        if(broadcast)    {
            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
            intent.putExtra("notifTx", false);
            intent.putExtra("fetch", true);
            LocalBroadcastManager.getInstance(UTXOActivity.this).sendBroadcast(intent);
        }

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

//            debug("UTXOActivity", "list:" + data.get(position).addr);

            String descr = "";
            Spannable word = null;
            if(UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx) != null)    {
                descr += " " + UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx);
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFF33ff00), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                debug("UTXOActivity", "list: tag");
            }
            if(isBIP47(addr))    {
                String pcode = BIP47Meta.getInstance().getPCode4AddrLookup().get(addr);
                if(pcode != null && pcode.length() > 0)    {
                    descr += " " + BIP47Meta.getInstance().getDisplayLabel(pcode);
                }
                else    {
                    descr += " " + UTXOActivity.this.getText(R.string.paycode).toString();
                }
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFFd07de5), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                debug("UTXOActivity", "list: bip47");
            }
            if(data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx))    {
                descr += " " + UTXOActivity.this.getText(R.string.dust) + " " + UTXOActivity.this.getText(R.string.do_not_spend);
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFFe75454), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                debug("UTXOActivity", "list: dust/do not spend");
            }
            else if(data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().containsNotDusted(data.get(position).hash, data.get(position).idx))   {
                descr += " " + UTXOActivity.this.getText(R.string.dust);
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFF8c8c8c), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                debug("UTXOActivity", "list: dust");
            }
            else if(BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx) ||
                    BlockedUTXO.getInstance().containsPostMix(data.get(position).hash, data.get(position).idx))    {
                descr += " " + UTXOActivity.this.getText(R.string.do_not_spend);
                word = new SpannableString(descr);
                word.setSpan(new ForegroundColorSpan(0xFFe75454), 1, descr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                debug("UTXOActivity", "list: do not spend");
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

    private void doDisplayAmounts() {

        final DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

        String message = getText(R.string.total_p2pkh) + " " + df.format(((double)(totalP2PKH) / 1e8)) + " BTC";
        message += "\n";
        message += getText(R.string.total_p2sh_p2wpkh) + " " + df.format(((double)(totalP2SH_P2WPKH) / 1e8)) + " BTC";
        message += "\n";
        message += getText(R.string.total_p2wpkh) + " " + df.format(((double)(totalP2WPKH) / 1e8)) + " BTC";
        message += "\n";
        message += getText(R.string.total_blocked) + " " + df.format(((double)(totalBlocked) / 1e8)) + " BTC";
        message += "\n";

        AlertDialog.Builder dlg = new AlertDialog.Builder(UTXOActivity.this)
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

}
