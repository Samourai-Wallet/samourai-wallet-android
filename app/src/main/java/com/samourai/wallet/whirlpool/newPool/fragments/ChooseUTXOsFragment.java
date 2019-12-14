package com.samourai.wallet.whirlpool.newPool.fragments;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.utxos.models.UTXOCoinSegment;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.widgets.ItemDividerDecorator;

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


public class ChooseUTXOsFragment extends Fragment {

    private static final String TAG = "ChooseUTXOsFragment";

    private OnUTXOSelectionListener onUTXOSelectionListener;
    private ArrayList<UTXOCoin> utxos = new ArrayList<UTXOCoin>();
    private WhirlpoolUTXOAdapter utxoAdapter;
    final DecimalFormat df = new DecimalFormat("#");
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private RecyclerView utxoRecyclerView;

    public ChooseUTXOsFragment() {
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        utxoRecyclerView = view.findViewById(R.id.whirlpool_utxo_recyclerview);
        utxoRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        utxoAdapter = new WhirlpoolUTXOAdapter();
        loadCoins();
        utxoRecyclerView.setAdapter(utxoAdapter);
        utxoRecyclerView.addItemDecoration(new ItemDividerDecorator(getActivity().getDrawable(R.color.disabled_white)));
        utxoRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void loadCoins() {
        Disposable disposable = getUTXOs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::applyFilters, err -> {
                    Toast.makeText(getContext(), "Error Loading UTXO: ".concat(err.getMessage()), Toast.LENGTH_SHORT).show();
                    err.printStackTrace();
                });
        compositeDisposable.add(disposable);

    }

    private void applyFilters(List<UTXOCoin> utxoCoins) {
        ArrayList<UTXOCoin> utxoCoinList = new ArrayList<>();


        /*------------------------------------------------*/
        ArrayList<UTXOCoin> unCycled = new ArrayList<>();

        // Add all received utxo's
        for (UTXOCoin model : utxoCoins) {
            if (model.account == 0 && model.path.startsWith("M/0/")) {
                if (!unCycled.contains(model)) {
                    unCycled.add(model);
                }
            } else if (model.account == 0 && model.path.equals("")) {
                if (!unCycled.contains(model)) {
                    unCycled.add(model);
                }
            }
        }
        Collections.sort(unCycled, (model, t1) -> Long.compare(t1.amount, model.amount));

        // UTXO section
        UTXOCoinSegment utxoSegment = new UTXOCoinSegment(null, null);
        utxoSegment.unCycled = true;

        //add utxo segment at the top of the list
        unCycled.add(0, utxoSegment);
        /*------------------------------------------------*/


        utxoCoinList.addAll(unCycled);

        /*------------------------------------------------*/
        // Change UTXO section
        UTXOCoinSegment changeSegment = new UTXOCoinSegment(null, null);
        changeSegment.unCycled = false;
        ArrayList<UTXOCoin> changes = new ArrayList<>();

        // Add all change utxo's
        for (UTXOCoin model : utxoCoins) {
            if (model.account == WhirlpoolMeta.getInstance(getActivity()).getWhirlpoolPostmix()
                    && model.path.startsWith("M/1/")) {
                if (!changes.contains(model)) {
                    changes.add(model);
                }
            }
        }
        Collections.sort(changes, (model, t1) -> Long.compare(t1.amount, model.amount));

        LogUtil.info(TAG, "applyFilters: ".concat(String.valueOf(changes.size())));
        //add change utxo segment at the top of the list
        changes.add(0, changeSegment);
        /*------------------------------------------------*/


        utxoCoinList.addAll(changes);

        utxoAdapter.updateList(utxoCoinList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choose_utxos, container, false);
    }


    public void setOnUTXOSelectionListener(OnUTXOSelectionListener onUTXOSelectionListener) {
        this.onUTXOSelectionListener = onUTXOSelectionListener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.onUTXOSelectionListener = null;
        compositeDisposable.dispose();
    }

    public interface OnUTXOSelectionListener {
        void onSelect(List<UTXOCoin> coins);
    }


    private void selectOrDeselect(int position) {
        utxos.get(position).isSelected = !utxos.get(position).isSelected;
        utxoRecyclerView.post(() -> utxoAdapter.notifyItemChanged(position));

        List<UTXOCoin> selectedList = new ArrayList<>();

        for (UTXOCoin model : utxos) {
            if (model.isSelected) {
                selectedList.add(model);
            }
        }

        if (onUTXOSelectionListener != null)
            onUTXOSelectionListener.onSelect(selectedList);
    }

    private Observable<List<UTXOCoin>> getUTXOs() {
        return Observable.fromCallable(() -> {
            Map<String, Object> dataSet = new HashMap<>();
            List<UTXO> utxos = new ArrayList<>();
            List<UTXO> account0 = APIFactory.getInstance(getActivity()).getUtxos(true);
//            List<UTXO> accountWhirlpool = APIFactory.getInstance(getActivity()).getUtxosPostMix(false);

            utxos.addAll(account0);
//            utxos.addAll(accountWhirlpool);


            long amount = 0L;
            for (UTXO utxo : utxos) {
                for (MyTransactionOutPoint out : utxo.getOutpoints()) {
                    debug(TAG, "utxo:" + out.getAddress() + "," + out.getValue());
                    debug(TAG, "utxo:" + utxo.getPath());
                    amount += out.getValue().longValue();
                }
            }

            ArrayList<UTXOCoin> items = new ArrayList<>();
            for (UTXO utxo : utxos) {
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    UTXOCoin displayData = new UTXOCoin(outpoint, utxo);
                    if (account0.contains(utxo)) {
                        displayData.account = 0;
                    } else {
                        displayData.account = WhirlpoolMeta.getInstance(getActivity()).getWhirlpoolPostmix();
                    }
                    items.add(displayData);
                }

            }
            return items;
        });
    }


    class WhirlpoolUTXOAdapter extends RecyclerView.Adapter<WhirlpoolUTXOAdapter.ViewHolder> {


        int SECTION = 0, UTXO = 1;


        @Override
        public WhirlpoolUTXOAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == SECTION) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.utxo_section_layout, parent, false);
                return new WhirlpoolUTXOAdapter.ViewHolder(view, SECTION);
            } else {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.utxo_item_layout, parent, false);
                return new WhirlpoolUTXOAdapter.ViewHolder(view, UTXO);

            }

        }

        @Override
        public long getItemId(int position) {
            return utxos.get(position).id;
        }

        @Override
        public int getItemViewType(int position) {
            if (utxos.get(position) instanceof UTXOCoinSegment) {
                return SECTION;
            } else {
                return UTXO;
            }
        }

        void updateList(List<UTXOCoin> newList) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UTXODiffCallback(utxos, newList));
            utxos = new ArrayList<>();
            utxos.addAll(newList);
            diffResult.dispatchUpdatesTo(this);
        }


        @Override
        public void onBindViewHolder(WhirlpoolUTXOAdapter.ViewHolder holder, int position) {
            float scale = getResources().getDisplayMetrics().density;

            if (utxos.get(position) instanceof UTXOCoinSegment) {
                UTXOCoinSegment utxoCoinSegment = (UTXOCoinSegment) utxos.get(position);
                holder.section.setText(utxoCoinSegment.unCycled ? "Uncycled UTXO's" : "Change UTXO's");
                holder.indicator.setVisibility(View.VISIBLE);
                if (utxoCoinSegment.unCycled) {
                    holder.indicator.setColorFilter(ContextCompat.getColor(getContext(), R.color.disabledRed), PorterDuff.Mode.SRC_IN);

                } else {
                    holder.indicator.setColorFilter(ContextCompat.getColor(getContext(), R.color.warning_yellow), PorterDuff.Mode.SRC_IN);
                }
                return;
            }
            UTXOCoin item = utxos.get(position);
            holder.address.setText(item.address);
            holder.amount.setText(df.format(((double) (utxos.get(position).amount) / 1e8)).concat(" BTC"));
            holder.rootViewGroup.setOnClickListener(view -> {
                selectOrDeselect(position);
            });
            holder.checkBox.setVisibility(View.VISIBLE);

            holder.tagsLayout.removeAllViews();

            if (item.path.contains("M/1/") && item.account == WhirlpoolMeta.getInstance(getContext()).getWhirlpoolPostmix()) {
                ImageView im = new ImageView(holder.rootViewGroup.getContext());
                im.setImageResource(R.drawable.ic_note_black_24dp);
                im.setPadding((int) (8 * scale + 0.5f), 0, (int) (8 * scale + 0.5f), 0);
                im.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.grey_accent)));
                holder.tagsLayout.addView(im);
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                TextView tx = new TextView(holder.rootViewGroup.getContext());
                tx.setText(getText(R.string.change_from_postmix));
                tx.setTextColor(ContextCompat.getColor(getContext(), R.color.grey_accent));
                tx.setLayoutParams(lparams);
                tx.setBackgroundResource(R.drawable.tag_round_shape);
                tx.setPadding((int) (8 * scale + 0.5f), (int) (4 * scale + 0.5f), (int) (8 * scale + 0.5f), (int) (4 * scale + 0.5f));
                lparams.leftMargin = 8;
                tx.setTypeface(Typeface.DEFAULT_BOLD);
                tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                holder.tagsLayout.addView(tx);
            }
            holder.checkBox.setChecked(item.isSelected);

            if (item.isSelected) {
                holder.rootViewGroup.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.select_overlay));
            } else {
                holder.rootViewGroup.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.window));
            }

            holder.checkBox.setOnClickListener((v) -> selectOrDeselect(position));


        }


        @Override
        public int getItemCount() {
            return utxos.size();
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView address, amount, label, section;
            LinearLayout tagsLayout;
            CheckBox checkBox;
            ViewGroup rootViewGroup;
            ImageView indicator;

            ViewHolder(View itemView, int viewType) {
                super(itemView);
                if (viewType == SECTION) {
                    section = itemView.findViewById(R.id.section_title);
                    indicator = itemView.findViewById(R.id.utxo_section_indicator);
                    return;
                }
                amount = itemView.findViewById(R.id.utxo_item_amount);
                label = itemView.findViewById(R.id.label);
                address = itemView.findViewById(R.id.utxo_item_address);
                tagsLayout = itemView.findViewById(R.id.utxo_item_tags_layout);
                checkBox = itemView.findViewById(R.id.multiselect_checkbox);
                checkBox.setVisibility(View.GONE);
                rootViewGroup = (ViewGroup) itemView;
                itemView.findViewById(R.id.utxo_more_details_icon).setVisibility(View.GONE);
            }
        }

        public class UTXODiffCallback extends DiffUtil.Callback {

            List<UTXOCoin> oldList;
            List<UTXOCoin> newList;

            UTXODiffCallback(List<UTXOCoin> newPersons, List<UTXOCoin> oldPersons) {
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


}
