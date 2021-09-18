package com.samourai.wallet.utxos;

import static com.samourai.wallet.util.LogUtil.debug;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.LinearLayoutManagerWrapper;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.utxos.models.UTXOCoinSegment;
import com.samourai.wallet.whirlpool.WhirlpoolHome;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UTXOSActivity extends SamouraiActivity implements ActionMode.Callback {


    private boolean shownWalletLoadingMessage = false;

    //flag to check UTXO sets have been flagged or not
    private boolean utxoChange = false;
    private boolean multiSelect = false;

    private ActionMode toolbarActionMode;


    private List<UTXOCoin> filteredUTXOs = new ArrayList<>();
    private List<UTXOCoin> unFilteredUTXOS = new ArrayList<>();
    private static final String TAG = "UTXOSActivity";
    private long totalP2PKH = 0L;
    private long totalP2SH_P2WPKH = 0L;
    private long totalP2WPKH = 0L;
    private long totalBlocked = 0L;
    private TreeMap<String, Long> noteAmounts = null;
    private TreeMap<String, Long> tagAmounts = null;
    final DecimalFormat df = new DecimalFormat("#");
    private RecyclerView utxoList;
    private SwipeRefreshLayout utxoSwipeRefresh;
    private UTXOListAdapter adapter;
    private ProgressBar utxoProgressBar;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Toolbar toolbar;

    //Filter states
    private boolean
            addressFilterLegacy = true,
            addressFilterSegwitCompat = true,
            addressFilterSegwitNat = true,
            statusSpendable = true,
            statusUnSpendable = true,
            utxoSortOrder = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Switch themes based on accounts (blue theme for whirlpool account)
        setSwitchThemes(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxos);
        toolbar = findViewById(R.id.toolbar_utxos);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            getSupportActionBar().setTitle(getText(R.string.unspent_outputs_post_mix));
        }

        noteAmounts = new TreeMap<String, Long>();
        tagAmounts = new TreeMap<String, Long>();

        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);
        utxoList = findViewById(R.id.utxo_rv_list);
        utxoList.setItemViewCacheSize(24);
        utxoProgressBar = findViewById(R.id.utxo_progressBar);
        utxoSwipeRefresh = findViewById(R.id.utxo_swipe_container);
        adapter = new UTXOListAdapter();
        adapter.setHasStableIds(true);
        utxoList.setLayoutManager(new LinearLayoutManagerWrapper(this));
        utxoList.addItemDecoration(new ItemDividerDecorator(getDrawable(R.color.disabled_white)));
        utxoList.setAdapter(adapter);
        loadUTXOs(false);

        utxoSwipeRefresh.setOnRefreshListener(() -> {
            loadUTXOs(false);
        });

        if (!APIFactory.getInstance(getApplicationContext())
                .walletInit) {
            if (!shownWalletLoadingMessage) {
                Snackbar.make(utxoList.getRootView(), "Please wait... your wallet is still loading ", Snackbar.LENGTH_LONG).show();
                shownWalletLoadingMessage = true;
            }

        }

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
        } else if (item.getItemId() == R.id.action_utxo_amounts) {
            doDisplayAmounts();
        } else if (item.getItemId() == R.id.action_refresh) {
            loadUTXOs(false);
        } else if (item.getItemId() == R.id.action_utxo_filter) {
            showFilterOptions();
        } else {
            ;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.bottomsheet_utxo_filter, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.show();

        CheckBox segNativeCheck = dialog.findViewById(R.id.segwit_native_checkBox_btn);
        segNativeCheck.setChecked(addressFilterSegwitNat);
        segNativeCheck.setOnCheckedChangeListener(this::onCheckChanges);

        CheckBox segCompatCheck = dialog.findViewById(R.id.segwit_compat_checkbox_btn);
        segCompatCheck.setChecked(addressFilterSegwitCompat);
        segCompatCheck.setOnCheckedChangeListener(this::onCheckChanges);

        CheckBox legacyCheck = dialog.findViewById(R.id.legacy_checkbox_btn);
        legacyCheck.setChecked(addressFilterLegacy);
        legacyCheck.setOnCheckedChangeListener(this::onCheckChanges);

        CheckBox unSpendableCheck = dialog.findViewById(R.id.upspendable_checkBox_btn);
        unSpendableCheck.setChecked(statusUnSpendable);
        unSpendableCheck.setOnCheckedChangeListener(this::onCheckChanges);

        CheckBox spendableCheckBox = dialog.findViewById(R.id.spendable_checkBox_btn);
        spendableCheckBox.setChecked(statusSpendable);
        spendableCheckBox.setOnCheckedChangeListener(this::onCheckChanges);


        RadioGroup group = dialog.findViewById(R.id.filter_amount_radio_group);
        if (utxoSortOrder) {
            group.check(R.id.asc_amount_radio_btn);

        } else {
            group.check(R.id.dsc_amount_radio_btn);

        }
        group.setOnCheckedChangeListener((radioGroup, i) -> utxoSortOrder = i == R.id.asc_amount_radio_btn);

        dialog.findViewById(R.id.utxo_filter_apply_btn).setOnClickListener(view -> {
            dialog.dismiss();
            applyFilters();
            adapter.notifyDataSetChanged();
        });

    }

    private void applyFilters() {

        //ArrayList will store UTXOs that or filtered based on spending status
        ArrayList<UTXOCoin> filteredStatus = new ArrayList<>();

        for (UTXOCoin model : unFilteredUTXOS) {
            if (statusUnSpendable) {
                if (model.doNotSpend) {
                    if (!filteredStatus.contains(model)) {
                        filteredStatus.add(model);
                    }
                }
            }
            if (statusSpendable) {
                if (!model.doNotSpend) {
                    if (!filteredStatus.contains(model)) {
                        filteredStatus.add(model);
                    }
                }
            }
        }

        //ArrayList will store UTXOs that or filtered based on Address types
        //types includes SEGWIT_NATIVE,SEGWIT_COMPAT,LEGACY
        List<UTXOCoin> filteredAddress = new ArrayList<>(filteredStatus);

        //counters to check spendables and unspendables
        //sections will be added based on this counters
        int unspendables = 0;
        int spendables = 0;

        for (UTXOCoin model : filteredStatus) {
            if (model.doNotSpend) {
                unspendables = unspendables + 1;
            } else {
                spendables = spendables + 1;

            }
            UTXOUtil.AddressTypes type = UTXOUtil.getAddressType(model.address);
            switch (type) {
                case LEGACY:
                    if (!addressFilterLegacy) {
                        filteredAddress.remove(model);
                    }
                    break;
                case SEGWIT_COMPAT: {
                    if (!addressFilterSegwitCompat) {
                        filteredAddress.remove(model);
                    }
                    break;
                }
                case SEGWIT_NATIVE: {
                    if (!addressFilterSegwitNat) {
                        filteredAddress.remove(model);
                    }
                    break;
                }
            }
        }

        if (utxoSortOrder) {
            // Ascending order sorting based on the UTXO amount
            Collections.sort(filteredAddress, (model, t1) -> Long.compare(model.amount, t1.amount));
        } else {
            // Descending  order sorting based on the UTXO amount
            Collections.sort(filteredAddress, (model, t1) -> Long.compare(t1.amount, model.amount));
        }

        //Sectioned dataset for RecyclerView adapter
        //here array will split based on spending status
        List<UTXOCoin> sectioned = new ArrayList<>();


        if (spendables > 0) {

            UTXOCoinSegment active = new UTXOCoinSegment(null, null);
            active.id = 0;
            active.isActive = true;
            sectioned.add(active);

        }
        for (UTXOCoin models : filteredAddress) {
            if (!models.doNotSpend) {
                models.id = filteredAddress.indexOf(models) + 1;
                sectioned.add(models);
            }
        }
        if (unspendables > 0) {

            UTXOCoinSegment doNotSpend = new UTXOCoinSegment(null, null);
            doNotSpend.id = filteredAddress.size() + 1;
            doNotSpend.isActive = false;
            doNotSpend.hash = "not_active";
            sectioned.add(doNotSpend);
        }
        for (UTXOCoin models : filteredAddress) {
            if (models.doNotSpend) {
                models.id = filteredAddress.indexOf(models) + 1;
                sectioned.add(models);
            }
        }

        this.adapter.updateList(sectioned);
    }

    //Filter checkbox change callback
    void onCheckChanges(CompoundButton compoundButton, boolean check) {
        switch (compoundButton.getId()) {
            case R.id.segwit_native_checkBox_btn: {
                addressFilterSegwitNat = check;
                break;
            }
            case R.id.segwit_compat_checkbox_btn: {
                addressFilterSegwitCompat = check;

                break;
            }
            case R.id.legacy_checkbox_btn: {
                addressFilterLegacy = check;
                break;
            }
            case R.id.upspendable_checkBox_btn: {
                statusUnSpendable = check;
                break;
            }
            case R.id.spendable_checkBox_btn: {
                statusSpendable = check;
                break;
            }
        }
    }

    private void loadUTXOs(boolean loadSilently) {
        //loading data during selection could cause selection state loss
        //so using this check we will prevent unwanted loading
        if (multiSelect) {
            utxoSwipeRefresh.setRefreshing(false);
            return;
        }
        if (!loadSilently) {
            utxoSwipeRefresh.setRefreshing(false);
            utxoProgressBar.setVisibility(View.VISIBLE);
        }
        Disposable disposable = getUTXOs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stringObjectMap -> {
                    List<UTXOCoin> items = (List<UTXOCoin>) stringObjectMap.get("utxos");
                    LogUtil.info(TAG, "loadUTXOs: ".concat(String.valueOf(items.size())));
                    try {
                        unFilteredUTXOS = new ArrayList<>();
                        unFilteredUTXOS.addAll(items);
                        applyFilters();
                    } catch (Exception ex) {
                        ex.printStackTrace();
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

    private Observable<Map<String, Object>> getUTXOs() {
        return Observable.fromCallable(() -> {
            long totalP2WPKH = 0L;
            long totalBlocked = 0L;
            long totalP2PKH = 0L;
            long totalP2SH_P2WPKH = 0L;

            noteAmounts.clear();
            tagAmounts.clear();

            Map<String, Object> dataSet = new HashMap<>();
            List<UTXO> utxos = null;
            if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                utxos = APIFactory.getInstance(getApplicationContext()).getUtxosPostMix(false);
            } else {
//                utxos = APIFactory.getInstance(getApplicationContext()).getUtxosWithLocalCache(false, true);
                utxos = APIFactory.getInstance(getApplicationContext()).getUtxos(false);
            }

            long amount = 0L;
            for (UTXO utxo : utxos) {
                for (MyTransactionOutPoint out : utxo.getOutpoints()) {
                    debug("UTXOSActivity", "utxo:" + out.getAddress() + "," + out.getValue());
                    debug("UTXOSActivity", "utxo:" + utxo.getPath());
                    amount += out.getValue().longValue();
                }
            }

            ArrayList<UTXOCoin> items = new ArrayList<>();
            for (UTXO utxo : utxos) {
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    UTXOCoin displayData = new UTXOCoin(outpoint, utxo);
                    if (BlockedUTXO.getInstance().contains(outpoint.getTxHash().toString(), outpoint.getTxOutputN())) {
                        displayData.doNotSpend = true;
                        totalBlocked += displayData.amount;

                    } else if (BlockedUTXO.getInstance().containsPostMix(outpoint.getTxHash().toString(), outpoint.getTxOutputN())) {
                        displayData.doNotSpend = true;
//                    Log.d("UTXOActivity", "marked as do not spend");
                        totalBlocked += displayData.amount;
                    } else {
//                    Log.d("UTXOActivity", "unmarked");
                        if (FormatsUtil.getInstance().isValidBech32(displayData.address)) {
                            totalP2WPKH += displayData.amount;
                        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), displayData.address).isP2SHAddress()) {
                            totalP2SH_P2WPKH += displayData.amount;
                        } else {
                            totalP2PKH += displayData.amount;
                        }
                    }

                    if (UTXOUtil.getInstance().get(outpoint.getTxHash().toString(), outpoint.getTxOutputN()) != null) {
                        List<String> tags = UTXOUtil.getInstance().get(outpoint.getTxHash().toString(), outpoint.getTxOutputN());

                        for (String tag : tags) {
                            if (tagAmounts.containsKey(tag.toLowerCase())) {
                                long val = tagAmounts.get(tag.toLowerCase());
                                val += displayData.amount;
                                tagAmounts.put(tag.toLowerCase(), val);
                            } else {
                                tagAmounts.put(tag.toLowerCase(), displayData.amount);
                            }
                        }

                    }
                    if (UTXOUtil.getInstance().getNote(outpoint.getTxHash().toString()) != null) {
                        String note = UTXOUtil.getInstance().getNote(outpoint.getTxHash().toString());

                        if (noteAmounts.containsKey(note.toLowerCase())) {
                            long val = noteAmounts.get(note.toLowerCase());
                            val += displayData.amount;
                            noteAmounts.put(note.toLowerCase(), val);
                        } else {
                            noteAmounts.put(note.toLowerCase(), displayData.amount);
                        }

                    }

                    boolean exist = false;
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).hash.equals(displayData.hash) && items.get(i).idx == displayData.idx && items.get(i).path.equals(displayData.path)) {
                            exist = true;
                        }
                    }
                    if (!exist) {
                        items.add(displayData);
                    }

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

    /**
     * this start will start {@link UTXODetailsActivity} activity
     **/
    private void onItemClick(UTXOCoin utxoCoin, View view) {
        Intent intent = new Intent(this, UTXODetailsActivity.class);
        intent.putExtra("hashIdx", utxoCoin.hash.concat("-").concat(String.valueOf(utxoCoin.idx)));
        intent.putExtra("_account", account);
        startActivityForResult(intent, 0);
    }

    private boolean onListLongPress(int postion) {
        multiSelect = !multiSelect;
        adapter.notifyDataSetChanged();
        toolbarActionMode = startSupportActionMode(this);
        selectOrDeselect(postion);
        return true;
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
        message += "\n\n";

        for (String key : noteAmounts.keySet()) {
            message += key + ": " + df.format(((double) (noteAmounts.get(key)) / 1e8)) + " BTC";
            message += "\n";
        }

        for (String key : tagAmounts.keySet()) {
            message += key + ": " + df.format(((double) (tagAmounts.get(key)) / 1e8)) + " BTC";
            message += "\n";
        }

        MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> dialog.dismiss());
        if (!isFinishing()) {
            dlg.show();
        }

    }

    private void selectOrDeselect(int position) {
        filteredUTXOs.get(position).isSelected = !filteredUTXOs.get(position).isSelected;
        utxoList.post(() -> adapter.notifyItemChanged(position));
        calculateSelectedAmount();
    }

    private void calculateSelectedAmount() {
        long amount = 0L;
        for (UTXOCoin model : this.filteredUTXOs) {
            if (model.isSelected) {
                amount = amount + model.amount;
            }
        }
        toolbarActionMode.setTitle(df.format(((double) (amount) / 1e8)) + " BTC");
    }

    private void markAsSpendable() {
        for (UTXOCoin model : filteredUTXOs) {
            if (model.isSelected) {
                if (model.amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(model.hash, model.idx)) {
                    BlockedUTXO.getInstance().remove(model.hash, model.idx);
                    BlockedUTXO.getInstance().addNotDusted(model.hash, model.idx);

                } else if (BlockedUTXO.getInstance().contains(model.hash, model.idx)) {

                    BlockedUTXO.getInstance().remove(model.hash, model.idx);


                } else if (BlockedUTXO.getInstance().containsPostMix(model.hash, model.idx)) {

                    BlockedUTXO.getInstance().removePostMix(model.hash, model.idx);

                }
                utxoList.post(() -> loadUTXOs(true));
                setResult(RESULT_OK);

            }
        }
        saveWalletState();
    }

    private void markAsUnSpendable() {
        for (UTXOCoin model : filteredUTXOs) {
            if (model.isSelected) {
                if (model.amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(model.hash, model.idx)) {

                    //No-op


                } else if (BlockedUTXO.getInstance().contains(model.hash, model.idx)) {
//No-op

                } else if (BlockedUTXO.getInstance().containsPostMix(model.hash, model.idx)) {


                } else {

                    if (account == 0) {
                        BlockedUTXO.getInstance().add(model.hash, model.idx, model.amount);
                    } else {
                        BlockedUTXO.getInstance().addPostMix(model.hash, model.idx, model.amount);
                    }
                    LogUtil.debug("UTXOActivity", "added:" + model.hash + "-" + model.idx);
                    setResult(RESULT_OK);

                }

                utxoList.post(() -> loadUTXOs(true));

            }
        }
        saveWalletState();
    }

    //Clears current Toolbar action mode
    void clearSelection() {

        ArrayList<UTXOCoin> models = new ArrayList<>();
        for (UTXOCoin model : this.filteredUTXOs) {
            model.isSelected = false;
            models.add(model);
        }
        this.filteredUTXOs = new ArrayList<>();
        this.filteredUTXOs.addAll(models);

    }

    public String getPreSelected() {

        ArrayList<UTXOCoin> utxos = new ArrayList<>();

        for (UTXOCoin utxo : this.filteredUTXOs) {
            if (utxo.isSelected) {
                utxos.add(utxo);
            }
        }

        String id = null;
        if (utxos.size() > 0) {
            id = UUID.randomUUID().toString();
            PreSelectUtil.getInstance().add(id, utxos);
        }

        return id;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.utxo_details_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        switch (item.getItemId()) {
            case R.id.utxo_details_action_whirlpool: {
                String id = getPreSelected();
                if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                    if (PreSelectUtil.getInstance().getPreSelected(id).size() != 1) {
                        Snackbar.make(utxoList, R.string.only_a_single_change_utxo_may, Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                    boolean blockedExist = false, receiveExist = false;
                    for (UTXOCoin coin : PreSelectUtil.getInstance().getPreSelected(id)) {
                        if (coin.doNotSpend) {
                            blockedExist = true;
                        }
                        if (!coin.path.startsWith("M/1/")) {
                            receiveExist = true;
                        }
                    }
                    if (receiveExist) {
                        Snackbar.make(utxoList, R.string.only_a_single_change_utxo_may, Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                    if (blockedExist) {
                        Snackbar.make(utxoList, R.string.selection_contains_blocked_utxo, Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                }

                new MaterialAlertDialogBuilder(this)
                        .setMessage("Send selected utxo's to whirlpool")
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                            if (id != null) {
                                Intent intent = new Intent(UTXOSActivity.this, WhirlpoolHome.class);
                                intent.putExtra("preselected", id);
                                intent.putExtra("_account", account);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {

                        })
                        .show();

                break;
            }
            case R.id.utxo_details_action_send: {
                String id = getPreSelected();
                if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                    if (PreSelectUtil.getInstance().getPreSelected(id).size() != 1) {
                        Snackbar.make(utxoList, R.string.only_single_utxo_may_selected, Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                }
                boolean blockedExist = false;
                for (UTXOCoin coin : PreSelectUtil.getInstance().getPreSelected(id)) {
                    if (coin.doNotSpend) {
                        blockedExist = true;
                    }
                }
                if (blockedExist) {
                    Snackbar.make(utxoList, R.string.selection_contains_blocked_utxo, Snackbar.LENGTH_LONG).show();
                    return false;
                }
                if (id != null) {
                    Intent intent = new Intent(UTXOSActivity.this, SendActivity.class);
                    intent.putExtra("preselected", id);
                    intent.putExtra("_account", account);
                    startActivity(intent);
                }
                break;
            }
            case R.id.utxo_details_action_spendable: {
                markAsSpendable();
                break;
            }
            case R.id.utxo_details_action_do_not_spend: {
                markAsUnSpendable();
                break;
            }
        }
        if (multiSelect) {
            multiSelect = false;
            if (toolbarActionMode != null) {
                toolbarActionMode.finish();
            }
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        toolbarActionMode = null;
        multiSelect = false;
        clearSelection();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }


    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(UTXOSActivity.this).checkTimeOut();

    }

    @Override
    public void onBackPressed() {
        if (multiSelect) {
            clearSelection();
            multiSelect = false;
            adapter.notifyDataSetChanged();
            if (toolbarActionMode != null) {
                toolbarActionMode.finish();
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (utxoChange) {
            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
            intent.putExtra("notifTx", false);
            intent.putExtra("fetch", true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void saveWalletState() {
        Disposable disposable = Completable.fromCallable(() -> {
            try {
                PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance().getPIN()));
            } catch (MnemonicException.MnemonicLengthException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (DecryptionException e) {
                e.printStackTrace();
            }
            return true;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        compositeDisposable.add(disposable);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.loadUTXOs(true);
        super.onActivityResult(requestCode, resultCode, data);
    }

    class UTXOListAdapter extends RecyclerView.Adapter<UTXOListAdapter.ViewHolder> {


        int SECTION = 0, UTXO = 1;
        AsyncListDiffer<UTXOCoin> mAsyncListDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == SECTION) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.utxo_section_layout, parent, false);
                return new ViewHolder(view, SECTION);
            } else {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.utxo_item_layout, parent, false);
                return new ViewHolder(view, UTXO);

            }

        }

        @Override
        public long getItemId(int position) {
            return mAsyncListDiffer.getCurrentList().get(position).id;
        }

        @Override
        public int getItemViewType(int position) {
            if (filteredUTXOs.get(position) instanceof UTXOCoinSegment) {
                return SECTION;
            } else {
                return UTXO;
            }
        }

        public void updateList(List<UTXOCoin> newList) {
            mAsyncListDiffer.submitList(newList);
            filteredUTXOs = new ArrayList<>();
            filteredUTXOs.addAll(newList);
            mAsyncListDiffer.submitList(filteredUTXOs);
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            if (mAsyncListDiffer.getCurrentList().get(position) instanceof UTXOCoinSegment) {
                UTXOCoinSegment utxoCoinSegment = (UTXOCoinSegment) filteredUTXOs.get(position);
                holder.section.setText(utxoCoinSegment.isActive ? getString(R.string.active) : getString(R.string.do_not_spend));
                if (!utxoCoinSegment.isActive) {
                    holder.section.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                    holder.section.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    holder.section.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }
                return;
            }
            UTXOCoin item = mAsyncListDiffer.getCurrentList().get(position);
            holder.address.setText(item.address);
            holder.amount.setText(df.format(((double) (mAsyncListDiffer.getCurrentList().get(position).amount) / 1e8)).concat(" BTC"));
            holder.rootViewGroup.setOnClickListener(view -> {
                if (!multiSelect)
                    onItemClick(item, view);
                else
                    selectOrDeselect(position);
            });
            if (multiSelect) {
                if (holder.checkBox.getVisibility() != View.VISIBLE) {
                    holder.checkBox.setVisibility(View.VISIBLE);

                }
            } else {
                if (holder.checkBox.getVisibility() == View.VISIBLE) {
                    holder.checkBox.setVisibility(View.GONE);
                }

            }
            holder.rootViewGroup.setOnLongClickListener(view -> onListLongPress(position));
            holder.notesLayout.removeAllViews();
            holder.tagsLayout.removeAllViews();
            if (item.amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD) {
                holder.tagsLayout.setVisibility(View.VISIBLE);
                View dustTag = createTag(getBaseContext(), getString(R.string.dust));
                holder.tagsLayout.addView(dustTag);
            } else {
                holder.tagsLayout.setVisibility(View.GONE);
            }

            if (UTXOUtil.getInstance().getNote(item.hash) != null && UTXOUtil.getInstance().getNote(item.hash).length() > 0) {
                float scale = getResources().getDisplayMetrics().density;
                holder.notesLayout.setVisibility(View.VISIBLE);
                ImageView im = new ImageView(holder.rootViewGroup.getContext());
                im.setImageResource(R.drawable.ic_note_black_24dp);
                im.requestLayout();
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams((int) (18 * scale + 0.5f), (int) (18 * scale + 0.5f));
                im.setLayoutParams(lparams);
                im.requestLayout();
                im.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.grey_accent)));
                holder.notesLayout.addView(im);
                TextView tx = new TextView(getBaseContext());
                tx.setText(UTXOUtil.getInstance().getNote(item.hash));
                tx.setMaxLines(1);
                tx.setTextSize(11);
                tx.setTextColor(getResources().getColor(R.color.white));
                tx.setEllipsize(TextUtils.TruncateAt.END);
                tx.setPadding((int) (8 * scale + 0.5f), 0, (int) (8 * scale + 0.5f), 0);
                holder.notesLayout.addView(tx);
            } else {
                holder.notesLayout.setVisibility(View.GONE);
            }

            String utxoIdxHash = item.hash.concat("-").concat(String.valueOf(item.idx));

            if (UTXOUtil.getInstance().get(utxoIdxHash) != null) {
                holder.tagsLayout.setVisibility(View.VISIBLE);
                for (String tagString : UTXOUtil.getInstance().get(utxoIdxHash)) {
                    View tag = createTag(getBaseContext(), tagString);
                    holder.tagsLayout.addView(tag);
                }
            }

            // Whirlpool tags for PREMIX & POSTMIX
            Collection<String> whirlpoolTags = WhirlpoolUtils.getInstance().getWhirlpoolTags(item, getApplicationContext());
            if (!whirlpoolTags.isEmpty()) {
                holder.notesLayout.setVisibility(View.VISIBLE);
                holder.tagsLayout.setVisibility(View.VISIBLE);
                for (String tagString : whirlpoolTags) {
                    View tag = createTag(getBaseContext(), tagString);
                    holder.tagsLayout.addView(tag);
                }
            }

            holder.checkBox.setChecked(item.isSelected);

            if (item.isSelected) {
                holder.rootViewGroup.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.select_overlay));
            } else {
                holder.rootViewGroup.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.windowDark));
            }

            holder.checkBox.setOnClickListener((v) -> {
                selectOrDeselect(position);
            });

        }

        private View createTag(Context context, String tag) {
            float scale = getResources().getDisplayMetrics().density;
            LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView tx = new TextView(context);
            tx.setText(tag);
            tx.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.txt_grey));
            tx.setLayoutParams(lparams);
            tx.setBackgroundResource(R.drawable.tag_round_shape);
            tx.setPadding((int) (8 * scale + 0.5f), (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));
            tx.setTypeface(Typeface.DEFAULT_BOLD);
            tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            lparams.rightMargin = 4;
            return tx;
        }

        @Override
        public int getItemCount() {
            return mAsyncListDiffer.getCurrentList().size();
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

                    return unspentIdxs != null && unspentIdxs.contains(Integer.valueOf(idx));

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView address, amount, section;
            LinearLayout notesLayout;
            LinearLayout tagsLayout;
            CheckBox checkBox;
            ViewGroup rootViewGroup;

            ViewHolder(View itemView, int viewType) {
                super(itemView);
                if (viewType == SECTION) {
                    section = itemView.findViewById(R.id.section_title);
                    return;
                }
                amount = itemView.findViewById(R.id.utxo_item_amount);
//                doNotSpend = itemView.findViewById(R.id.do_not_spend_text);
                address = itemView.findViewById(R.id.utxo_item_address);
                notesLayout = itemView.findViewById(R.id.utxo_item_notes_layout);
                tagsLayout = itemView.findViewById(R.id.utxo_item_tags_layout);
                notesLayout.setVisibility(View.GONE);
                checkBox = itemView.findViewById(R.id.multiselect_checkbox);
                checkBox.setVisibility(View.GONE);
//                paynym = itemView.findViewById(R.id.paynym_txt);
                rootViewGroup = (ViewGroup) itemView;
            }
        }



    }

    static final DiffUtil.ItemCallback<UTXOCoin> DIFF_CALLBACK
            = new DiffUtil.ItemCallback<UTXOCoin>() {

        @Override
        public boolean areItemsTheSame(@NonNull UTXOCoin oldItem, @NonNull UTXOCoin newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull UTXOCoin oldItem, @NonNull UTXOCoin newItem) {
            return oldItem.equals(newItem);
        }
    };

}
