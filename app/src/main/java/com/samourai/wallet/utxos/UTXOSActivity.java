package com.samourai.wallet.utxos;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.UTXOUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.widgets.ItemDividerDecorator;

import org.bitcoinj.core.Address;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.util.LogUtil.debug;

public class UTXOSActivity extends AppCompatActivity implements ActionMode.Callback {


    private boolean shownWalletLoadingMessage = false;

    //flag to check UTXO sets have been flagged or not
    private boolean utxoChange = false;
    private boolean multiSelect = false;

    private ActionMode toolbarActionMode;


    public class UTXOModel {
        private String addr = null;
        int id;
        long amount = 0L;
        String hash = null;
        private int idx = 0;
        private boolean doNotSpend = false;
        private boolean isSelected = false;
    }

    private class UTXOModelSection extends UTXOModel {
        boolean isActive = false;
    }

    private List<UTXOModel> filteredUTXOs = new ArrayList<>();
    private List<UTXOModel> unFilteredUTXOS = new ArrayList<>();
    private List<String> selectedHashes = new ArrayList<>();
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
    private Toolbar toolbar;

    //Filter states
    private boolean addressFilterLegacy = true, addressFilterSegwitCompat = true, addressFilterSegwitNat = true, statusSpendable = true, statusUnSpendable = true;
    private boolean utxoSortOrder = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxos);
        toolbar = findViewById(R.id.toolbar_utxos);
        setSupportActionBar(toolbar);
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
        utxoList.setItemViewCacheSize(24);
        utxoProgressBar = findViewById(R.id.utxo_progressBar);
        utxoSwipeRefresh = findViewById(R.id.utxo_swipe_container);
        adapter = new UTXOListAdapter();
        adapter.setHasStableIds(true);
        utxoList.setLayoutManager(new LinearLayoutManager(this));
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
        }
        if (item.getItemId() == R.id.action_utxo_amounts) {
            doDisplayAmounts();
        }
        if (item.getItemId() == R.id.action_refresh) {
            loadUTXOs(false);
        }
        if (item.getItemId() == R.id.action_refresh) {
            loadUTXOs(false);
        }
        if (item.getItemId() == R.id.action_utxo_filter) {
            showFilterOptions();
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
        ArrayList<UTXOModel> filteredStatus = new ArrayList<>();

        for (UTXOModel model : unFilteredUTXOS) {
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
        List<UTXOModel> filteredAddress = new ArrayList<>(filteredStatus);


        for (UTXOModel model : filteredStatus) {
            UTXOUtil.AddressTypes type = UTXOUtil.getAddressType(model.addr);
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
        List<UTXOModel> sectioned = new ArrayList<>();

        UTXOModelSection active = new UTXOModelSection();
        active.id = 0;
        active.isActive = true;
        sectioned.add(active);

        for (UTXOModel models : filteredAddress) {
            if (!models.doNotSpend) {
                models.id = filteredAddress.indexOf(models) + 1;
                sectioned.add(models);
            }
        }

        UTXOModelSection doNotSpend = new UTXOModelSection();
        doNotSpend.id = filteredAddress.size() + 1;
        doNotSpend.isActive = false;
        doNotSpend.hash = "not_active";
        sectioned.add(doNotSpend);

        for (UTXOModel models : filteredAddress) {
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
        Disposable disposable = getDataUTXOData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stringObjectMap -> {
                    List<UTXOModel> items = (List<UTXOModel>) stringObjectMap.get("utxos");

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
            for (UTXO utxo : utxos) {
                for (MyTransactionOutPoint out : utxo.getOutpoints()) {
                    debug("UTXOSActivity", "utxo:" + out.getAddress() + "," + out.getValue());
                    debug("UTXOSActivity", "utxo:" + utxo.getPath());
                    amount += out.getValue().longValue();
                }
            }

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

                    //TODO: REMOVE THIS
                    boolean duplicatesexist = false;
                    for (UTXOModel u : items) {
                        if (u.hash.equals(displayData.hash)) {
                            duplicatesexist = true;
                        }
                    }
                    if (!duplicatesexist)
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

    /**
     * this start will start {@link UTXODetailsActivity} activity
     **/
    private void onItemClick(int position, View view) {
        Intent intent = new Intent(this, UTXODetailsActivity.class);
        intent.putExtra("hash", filteredUTXOs.get(position).hash);
        intent.putExtra("account", account);
        startActivity(intent);
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
        message += "\n";

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
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
        for (UTXOModel model : this.filteredUTXOs) {
            if (model.isSelected) {
                amount = amount + model.amount;
            }
        }
        toolbarActionMode.setTitle(df.format(((double) (amount) / 1e8)) + " BTC");
    }

    private void markAsSpendable() {
        for (UTXOModel model : filteredUTXOs) {
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


            }
        }
    }

    private void markAsUnSpendable() {
        for (UTXOModel model : filteredUTXOs) {
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

                }

                utxoList.post(() -> loadUTXOs(true));

            }
        }
    }

    //Clears current Toolbar action mode
    void clearSelection() {

        ArrayList<UTXOModel> models = new ArrayList<>();
        for (UTXOModel model : this.filteredUTXOs) {
            model.isSelected = false;
            models.add(model);
        }
        this.filteredUTXOs = new ArrayList<>();
        this.filteredUTXOs.addAll(models);

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
                Toast.makeText(this, "Sent to Whirlpool", Toast.LENGTH_SHORT).show();
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

    class UTXOListAdapter extends RecyclerView.Adapter<UTXOListAdapter.ViewHolder> {


        int SECTION = 0, UTXO = 1;


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
            return filteredUTXOs.get(position).id;
        }

        @Override
        public int getItemViewType(int position) {
            if (filteredUTXOs.get(position) instanceof UTXOSActivity.UTXOModelSection) {
                return SECTION;
            } else {
                return UTXO;
            }
        }

        public void updateList(List<UTXOModel> newList) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UTXODiffCallback(filteredUTXOs, newList));
            filteredUTXOs = new ArrayList<>();
            filteredUTXOs.addAll(newList);
            diffResult.dispatchUpdatesTo(this);
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            if (filteredUTXOs.get(position) instanceof UTXOSActivity.UTXOModelSection) {
                UTXOSActivity.UTXOModelSection utxoModelSection = (UTXOSActivity.UTXOModelSection) filteredUTXOs.get(position);
                holder.section.setText(utxoModelSection.isActive ? "Active" : "Do not spend");
                if (!utxoModelSection.isActive) {
                    holder.section.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                    holder.section.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    holder.section.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }
                return;
            }
            UTXOSActivity.UTXOModel item = filteredUTXOs.get(position);
            holder.address.setText(item.addr);
            holder.amount.setText(df.format(((double) (filteredUTXOs.get(position).amount) / 1e8)) + " BTC");
            holder.rootViewGroup.setOnClickListener(view -> {
                if (!multiSelect)
                    onItemClick(position, view);
                else
                    selectOrDeselect(position);
            });
//            holder.tagsLayout.setVisibility(View.GONE);
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
            holder.tagsLayout.removeAllViews();

            if (item.amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD) {
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                TextView tx = new TextView(holder.rootViewGroup.getContext());
                tx.setText("DUST");
                tx.setLayoutParams(lparams);
                tx.setBackgroundResource(R.drawable.tag_round_shape);
                float scale = getResources().getDisplayMetrics().density;
                int dpAsPixels = (int) (4 * scale + 0.5f);
                tx.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
                lparams.leftMargin = 8;
                tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                holder.tagsLayout.addView(tx);
            }
            holder.checkBox.setChecked(item.isSelected);

            if (item.isSelected) {
                holder.rootViewGroup.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.select_overlay));
            } else {
                holder.rootViewGroup.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.window));
            }

            holder.checkBox.setOnClickListener((v) -> {
                selectOrDeselect(position);
            });

//            if (UTXOUtil.getInstance().get(filteredUTXOs.get(position).hash + "-" + filteredUTXOs.get(position).idx) != null) {
//                holder.label.setVisibility(View.VISIBLE);
//                holder.label.setText(UTXOUtil.getInstance().get(filteredUTXOs.get(position).hash + "-" + filteredUTXOs.get(position).idx));
//            } else {
//                holder.label.setVisibility(View.GONE);
//            }
//            if (isBIP47(filteredUTXOs.get(position).addr)) {
//                holder.paynym.setVisibility(View.VISIBLE);
//                String pcode = BIP47Meta.getInstance().getPCode4AddrLookup().get(filteredUTXOs.get(position).addr);
//                if (pcode != null && pcode.length() > 0) {
//
//                    holder.paynym.setText(BIP47Meta.getInstance().getDisplayLabel(pcode));
//
//                } else {
//                    holder.paynym.setText(getText(R.string.paycode).toString());
//                }
//
//            } else {
//                holder.paynym.setVisibility(View.GONE);
//            }
//
//            if (filteredUTXOs.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(filteredUTXOs.get(position).hash, filteredUTXOs.get(position).idx)) {
//                holder.doNotSpend.setVisibility(View.VISIBLE);
//                holder.doNotSpend.setText(getText(R.string.dust).toString().concat(" ").concat(getText(R.string.do_not_spend).toString()));
//            } else if (filteredUTXOs.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().containsNotDusted(filteredUTXOs.get(position).hash, filteredUTXOs.get(position).idx)) {
//                holder.doNotSpend.setVisibility(View.VISIBLE);
//                holder.doNotSpend.setText(getText(R.string.dust).toString());
//            } else if (BlockedUTXO.getInstance().contains(filteredUTXOs.get(position).hash, filteredUTXOs.get(position).idx) ||
//                    BlockedUTXO.getInstance().containsPostMix(filteredUTXOs.get(position).hash, filteredUTXOs.get(position).idx)) {
//                holder.doNotSpend.setVisibility(View.VISIBLE);
//                holder.doNotSpend.setText(getText(R.string.do_not_spend));
//            } else {
//                holder.doNotSpend.setVisibility(View.GONE);
//
//            }


        }


        @Override
        public int getItemCount() {
            return filteredUTXOs.size();
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
            TextView address, amount, doNotSpend, label, paynym, section;
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
                label = itemView.findViewById(R.id.label);
                address = itemView.findViewById(R.id.utxo_item_address);
                tagsLayout = itemView.findViewById(R.id.utxo_item_tags_layout);
                checkBox = itemView.findViewById(R.id.multiselect_checkbox);
                checkBox.setVisibility(View.GONE);
//                paynym = itemView.findViewById(R.id.paynym_txt);
                rootViewGroup = (ViewGroup) itemView;
            }
        }
    }

    public class UTXODiffCallback extends DiffUtil.Callback {

        List<UTXOModel> oldList;
        List<UTXOModel> newList;

        public UTXODiffCallback(List<UTXOModel> newPersons, List<UTXOModel> oldPersons) {
            this.newList = newPersons;
            this.oldList = oldPersons;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).id == newList.get(newItemPosition).id;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }

}
