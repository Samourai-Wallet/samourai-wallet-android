package com.samourai.wallet.utxos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.UTXOUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.widgets.ItemDividerDecorator;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.util.LogUtil.debug;

public class UTXOSActivity extends AppCompatActivity {


    private boolean shownWalletLoadingMessage = false;

    //flag to check UTXO sets have been flagged or not
    private boolean utxoChange = false;

    private class UTXOModel {
        private String addr = null;
        private long amount = 0L;
        private String hash = null;
        private int idx = 0;
        private boolean doNotSpend = false;
    }

    private List<UTXOModel> data = new ArrayList<>();
    private static final String TAG = "UTXOSActivity";
    private long totalP2PKH = 0L;
    private long totalP2SH_P2WPKH = 0L;
    private long totalP2WPKH = 0L;
    private long totalBlocked = 0L;
    final DecimalFormat df = new DecimalFormat("#");
    private RecyclerView utxoList;
    private SwipeRefreshLayout utxoSwipeRefresh;
    private UTXOListAdapter adapter;
    private ProgressBar utxoProgressBar;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private int account = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxos);

        setSupportActionBar(findViewById(R.id.toolbar_utxos));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("_account")) {
            if (getIntent().getExtras().getInt("_account") == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                account = WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix();
                getSupportActionBar().setTitle(getText(R.string.unspent_outputs_post_mix));
            }
        }


        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);
        utxoList = findViewById(R.id.utxo_rv_list);
        utxoProgressBar = findViewById(R.id.utxo_progressBar);
        utxoSwipeRefresh = findViewById(R.id.utxo_swipe_container);
        adapter = new UTXOListAdapter();
        utxoList.setLayoutManager(new LinearLayoutManager(this));
        utxoList.addItemDecoration(new ItemDividerDecorator(getDrawable(R.color.disabled_white)));
        utxoList.setAdapter(adapter);
        loadUTXOs(false);

        utxoSwipeRefresh.setOnRefreshListener(() -> {
            loadUTXOs(false);
        });

        Disposable disposable = APIFactory.getInstance(getApplicationContext())
                .walletBalanceObserver
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    loadUTXOs(true);
                }, Throwable::printStackTrace);
        compositeDisposable.add(disposable);

        if (!APIFactory.getInstance(getApplicationContext())
                .walletInit) {
            if (!shownWalletLoadingMessage) {
                Snackbar.make(utxoList.getRootView(), "Please wait... your wallet is still loading ", Snackbar.LENGTH_LONG).show();
                shownWalletLoadingMessage = true;
            }

        }

        recheckChanges();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.utxo_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        if (item.getItemId() == R.id.action_utxo_amounts) {
            doDisplayAmounts();
        }
        if (item.getItemId() == R.id.action_refresh) {
           loadUTXOs(false);
        }
        return super.onOptionsItemSelected(item);
    }

    public void recheckChanges() {
        Disposable subscribe = Observable.interval(2000, 4000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    loadUTXOs(true);
                });
        compositeDisposable.add(subscribe);
    }

    @Override
    protected void onDestroy() {
        if(utxoChange){
            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
            intent.putExtra("notifTx", false);
            intent.putExtra("fetch", true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void loadUTXOs(boolean loadSilently) {
        if (!loadSilently) {
            utxoSwipeRefresh.setRefreshing(false);
            utxoProgressBar.setVisibility(View.VISIBLE);
        }
        Disposable disposable = getDataUTXOData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stringObjectMap -> {
                    List<UTXOModel> items = (List<UTXOModel>) stringObjectMap.get("utxos");

                    if (items.size() != data.size()) {

                        try {
                            data.clear();
                            for (UTXOModel models : items) {
                                if (!models.doNotSpend) {
                                    data.add(models);
                                }
                            }

                            for (UTXOModel models : items) {
                                if (models.doNotSpend) {
                                    data.add(models);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }


                        adapter.notifyDataSetChanged();
                    }

                    totalP2WPKH = (long) stringObjectMap.get("totalP2WPKH");
                    totalBlocked = (long) stringObjectMap.get("totalBlocked");
                    totalP2SH_P2WPKH = (long) stringObjectMap.get("totalP2SH_P2WPKH");
                    totalP2PKH = (long) stringObjectMap.get("totalP2PKH");
                    if (!loadSilently) {
                        utxoProgressBar.setVisibility(View.GONE);
                    }


                }, err -> {
                    if (!loadSilently) {
                        utxoSwipeRefresh.setRefreshing(false);
                        utxoProgressBar.setVisibility(View.GONE);
                    }
                });
        compositeDisposable.add(disposable);
    }

    private Observable<Map<String, Object>> getDataUTXOData() {
        return Observable.fromCallable(() -> {
            long totalP2WPKH = 0L;
            long totalBlocked = 0L;
            long totalP2PKH = 0L;
            long totalP2SH_P2WPKH = 0L;

            Map<String, Object> dataSet = new HashMap<>();
            List<UTXO> utxos = null;
            if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                utxos = APIFactory.getInstance(getApplicationContext()).getUtxosPostMix(false);
            } else {
//                utxos = APIFactory.getInstance(getApplicationContext()).getUtxosWithLocalCache(false, true);
                utxos = APIFactory.getInstance(getApplicationContext()).getUtxos(false);
            }

            long amount = 0L;
            for(UTXO utxo : utxos)   {
                for(MyTransactionOutPoint out : utxo.getOutpoints())    {
                    debug("UTXOSActivity", "utxo:" + out.getAddress() + "," + out.getValue());
                    debug("UTXOSActivity", "utxo:" + utxo.getPath());
                    amount += out.getValue().longValue();
                }
            }
            debug("UTXOSActivity", "utxos by value:" + amount);

            ArrayList<UTXOModel> items = new ArrayList<>();
            for (UTXO utxo : utxos) {
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    UTXOModel displayData = new UTXOModel();
                    displayData.addr = outpoint.getAddress();
                    displayData.amount = outpoint.getValue().longValue();
                    displayData.hash = outpoint.getTxHash().toString();
                    displayData.idx = outpoint.getTxOutputN();
                    if (BlockedUTXO.getInstance().contains(outpoint.getTxHash().toString(), outpoint.getTxOutputN())) {
                        displayData.doNotSpend = true;
                        totalBlocked += displayData.amount;

                    } else if (BlockedUTXO.getInstance().containsPostMix(outpoint.getTxHash().toString(), outpoint.getTxOutputN())) {
                        displayData.doNotSpend = true;
//                    Log.d("UTXOActivity", "marked as do not spend");
                        totalBlocked += displayData.amount;
                    } else {
//                    Log.d("UTXOActivity", "unmarked");
                        if (FormatsUtil.getInstance().isValidBech32(displayData.addr)) {
                            totalP2WPKH += displayData.amount;
                        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), displayData.addr).isP2SHAddress()) {
                            totalP2SH_P2WPKH += displayData.amount;
                        } else {
                            totalP2PKH += displayData.amount;
                        }
                    }

                    items.add(displayData);

                }

            }
            dataSet.put("totalP2WPKH", totalP2WPKH);
            dataSet.put("totalBlocked", totalBlocked);
            dataSet.put("totalP2SH_P2WPKH", totalP2SH_P2WPKH);
            dataSet.put("totalP2PKH", totalP2PKH);
            dataSet.put("utxos", items);

            return dataSet;
        });
    }

    private void onItemClick(int position, View view) {
        PopupMenu menu = new PopupMenu(getApplicationContext(), view, Gravity.RIGHT);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            switch (id) {
                case R.id.item_tag: {
                    tagItem(position);
                }

                break;
//
                case R.id.item_do_not_spend: {
                    doNotSpend(position);

                }

                break;
//
                case R.id.item_sign: {
                    sign(position);
                }

                break;

                case R.id.item_redeem: {
                    redeem(position);
                }

                break;

                case R.id.item_view: {
                    viewInExplorer(position);
                }

                break;
//
                case R.id.item_privkey: {
                    viewPrivateKey(position);
                }

                break;

            }
            adapter.notifyItemChanged(position);

            return true;
        });
        menu.inflate(R.menu.utxo_popup_menu);

        if (BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx) ||
                BlockedUTXO.getInstance().containsPostMix(data.get(position).hash, data.get(position).idx)) {
            menu.getMenu().findItem(R.id.item_do_not_spend).setTitle(R.string.mark_spend);
        } else {
            menu.getMenu().findItem(R.id.item_do_not_spend).setTitle(R.string.mark_do_not_spend);
        }

        String addr = data.get(position).addr;
        if (!FormatsUtil.getInstance().isValidBech32(addr) && !Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {
            menu.getMenu().findItem(R.id.item_redeem).setVisible(false);
        }

        menu.show();
    }

    private void viewPrivateKey(int position) {
        String addr = data.get(position).addr;
        ECKey ecKey = SendFactory.getPrivKey(addr, account);
        String strPrivKey = ecKey.getPrivateKeyAsWiF(SamouraiWallet.getInstance().getCurrentNetworkParams());

        ImageView showQR = new ImageView(this);
        Bitmap bitmap = null;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strPrivKey, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        showQR.setImageBitmap(bitmap);

        TextView showText = new TextView(this);
        showText.setText(strPrivKey);
        showText.setTextIsSelectable(true);
        showText.setPadding(40, 10, 40, 10);
        showText.setTextSize(18.0f);

        LinearLayout privkeyLayout = new LinearLayout(this);
        privkeyLayout.setOrientation(LinearLayout.VERTICAL);
        privkeyLayout.addView(showQR);
        privkeyLayout.addView(showText);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setView(privkeyLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                }).show();
    }

    private void viewInExplorer(int position) {
        String blockExplorer = "https://m.oxt.me/transaction/";
        if (SamouraiWallet.getInstance().isTestNet()) {
            blockExplorer = "https://blockstream.info/testnet/";
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + data.get(position).hash));
        startActivity(browserIntent);
    }

    private void redeem(int position) {
        String addr = data.get(position).addr;
        ECKey ecKey = SendFactory.getPrivKey(addr, account);
        SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());

        if (ecKey != null && segwitAddress != null) {

            String redeemScript = Hex.toHexString(segwitAddress.segWitRedeemScript().getProgram());

            TextView showText = new TextView(this);
            showText.setText(redeemScript);
            showText.setTextIsSelectable(true);
            showText.setPadding(40, 10, 40, 10);
            showText.setTextSize(18.0f);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setView(showText)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    })
                    .show();
        }
    }

    private void sign(int position) {
        String addr = data.get(position).addr;
        ECKey ecKey = SendFactory.getPrivKey(addr, account);
        String msg = null;

        if (FormatsUtil.getInstance().isValidBech32(addr) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {

            msg = getString(R.string.utxo_sign_text3);

            try {
                JSONObject obj = new JSONObject();
                obj.put("pubkey", ecKey.getPublicKeyAsHex());
                obj.put("address", addr);
                msg += " " + obj.toString();
            } catch (JSONException je) {
                msg += ":";
                msg += addr;
                msg += ", ";
                msg += "pubkey:";
                msg += ecKey.getPublicKeyAsHex();
            }

        } else {

            msg = getString(R.string.utxo_sign_text2);

        }

        if (ecKey != null) {
            MessageSignUtil.getInstance(this).doSign(this.getString(R.string.utxo_sign),
                    this.getString(R.string.utxo_sign_text1),
                    msg,
                    ecKey);
        }

    }

    private void doNotSpend(int position) {

        if (data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dusting_tx);
            builder.setMessage(R.string.dusting_tx_unblock);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.no, (dialog, whichButton) -> {
                ;
            });
            builder.setNegativeButton(R.string.yes, (dialog, whichButton) -> {

                BlockedUTXO.getInstance().remove(data.get(position).hash, data.get(position).idx);
                BlockedUTXO.getInstance().addNotDusted(data.get(position).hash, data.get(position).idx);
                adapter.notifyItemChanged(position);

                //to recalculate amounts
                loadUTXOs(true);

            });

            AlertDialog alert = builder.create();
            alert.show();

        } else if (BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.mark_spend);
            builder.setMessage(R.string.mark_utxo_spend);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                BlockedUTXO.getInstance().remove(data.get(position).hash, data.get(position).idx);

                adapter.notifyItemChanged(position);

                //to recalculate amounts
                loadUTXOs(true);


            });
            builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
                ;
            });

            AlertDialog alert = builder.create();
            alert.show();

        } else if (BlockedUTXO.getInstance().containsPostMix(data.get(position).hash, data.get(position).idx)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.mark_spend);
            builder.setMessage(R.string.mark_utxo_spend);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                BlockedUTXO.getInstance().removePostMix(data.get(position).hash, data.get(position).idx);

                Log.d("UTXOActivity", "removed:" + data.get(position).hash + "-" + data.get(position).idx);

                adapter.notifyItemChanged(position);

                //to recalculate amounts
                loadUTXOs(true);


            });
            builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
                ;
            });

            AlertDialog alert = builder.create();
            alert.show();

        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.mark_do_not_spend);
            builder.setMessage(R.string.mark_utxo_do_not_spend);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                if (account == 0) {
                    BlockedUTXO.getInstance().add(data.get(position).hash, data.get(position).idx, data.get(position).amount);
                } else {
                    BlockedUTXO.getInstance().addPostMix(data.get(position).hash, data.get(position).idx, data.get(position).amount);
                }

                Log.d("UTXOActivity", "added:" + data.get(position).hash + "-" + data.get(position).idx);

                adapter.notifyItemChanged(position);

                //to recalculate amounts
                loadUTXOs(true);


            });
            builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
                ;
            });

            AlertDialog alert = builder.create();
            alert.show();

        }
        utxoChange = true;

    }

    private void tagItem(int position) {

        final EditText edTag = new EditText(this);
        edTag.setSingleLine(true);
        if (UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx) != null) {
            edTag.setText(UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx));
        }

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setView(edTag)
                .setMessage(R.string.label)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {

                    final String strTag = edTag.getText().toString().trim();

                    if (strTag != null && strTag.length() > 0) {
                        UTXOUtil.getInstance().add(data.get(position).hash + "-" + data.get(position).idx, strTag);
                    } else {
                        UTXOUtil.getInstance().remove(data.get(position).hash + "-" + data.get(position).idx);
                    }

//
                    adapter.notifyItemChanged(position);

                }).setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
                    ;
                });
        if (!isFinishing()) {
            dlg.show();
        }

    }

    private void doDisplayAmounts() {

        final DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

        String message = getText(R.string.total_p2pkh) + " " + df.format(((double) (totalP2PKH) / 1e8)) + " BTC";
        message += "\n";
        message += getText(R.string.total_p2sh_p2wpkh) + " " + df.format(((double) (totalP2SH_P2WPKH) / 1e8)) + " BTC";
        message += "\n";
        message += getText(R.string.total_p2wpkh) + " " + df.format(((double) (totalP2WPKH) / 1e8)) + " BTC";
        message += "\n";
        message += getText(R.string.total_blocked) + " " + df.format(((double) (totalBlocked) / 1e8)) + " BTC";
        message += "\n";

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }
                });
        if (!isFinishing()) {
            dlg.show();
        }

    }

    class UTXOListAdapter extends RecyclerView.Adapter<UTXOListAdapter.ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.utxo_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UTXOModel item = data.get(position);
            holder.address.setText(item.addr);
            holder.amount.setText(df.format(((double) (data.get(position).amount) / 1e8)) + " BTC");
            holder.rootViewGroup.setOnClickListener(view -> onItemClick(position, view));

            if (UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx) != null) {
                holder.label.setVisibility(View.VISIBLE);
                holder.label.setText(UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx));
            } else {
                holder.label.setVisibility(View.GONE);
            }
            if (isBIP47(data.get(position).addr)) {
                holder.paynym.setVisibility(View.VISIBLE);
                String pcode = BIP47Meta.getInstance().getPCode4AddrLookup().get(data.get(position).addr);
                if (pcode != null && pcode.length() > 0) {

                    holder.paynym.setText(BIP47Meta.getInstance().getDisplayLabel(pcode));

                } else {
                    holder.paynym.setText(getText(R.string.paycode).toString());
                }

            } else {
                holder.paynym.setVisibility(View.GONE);
            }

            if (data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx)) {
                holder.doNotSpend.setVisibility(View.VISIBLE);
                holder.doNotSpend.setText(getText(R.string.dust).toString().concat(" ").concat(getText(R.string.do_not_spend).toString()));
            } else if (data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().containsNotDusted(data.get(position).hash, data.get(position).idx)) {
                holder.doNotSpend.setVisibility(View.VISIBLE);
                holder.doNotSpend.setText(getText(R.string.dust).toString());
            } else if (BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx) ||
                    BlockedUTXO.getInstance().containsPostMix(data.get(position).hash, data.get(position).idx)) {
                holder.doNotSpend.setVisibility(View.VISIBLE);
                holder.doNotSpend.setText(getText(R.string.do_not_spend));
            } else {
                holder.doNotSpend.setVisibility(View.GONE);

            }


        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private boolean isBIP47(String address) {

            try {
                String path = APIFactory.getInstance(getApplicationContext()).getUnspentPaths().get(address);
                if (path != null) {
                    return false;
                } else {
                    String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
                    int idx = BIP47Meta.getInstance().getIdx4Addr(address);
                    List<Integer> unspentIdxs = BIP47Meta.getInstance().getUnspent(pcode);

                    if (unspentIdxs != null && unspentIdxs.contains(Integer.valueOf(idx))) {
                        return true;
                    } else {
                        return false;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView address, amount, doNotSpend, label, paynym;
            View rootViewGroup;

            ViewHolder(View itemView) {
                super(itemView);
                amount = itemView.findViewById(R.id.utxo_item_amount);
                doNotSpend = itemView.findViewById(R.id.do_not_spend_text);
                label = itemView.findViewById(R.id.label);
                address = itemView.findViewById(R.id.utxo_item_address);
                paynym = itemView.findViewById(R.id.paynym_txt);
                rootViewGroup = itemView;
            }
        }
    }
}
