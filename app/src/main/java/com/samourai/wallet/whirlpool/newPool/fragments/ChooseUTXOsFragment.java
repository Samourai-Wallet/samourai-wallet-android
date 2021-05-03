package com.samourai.wallet.whirlpool.newPool.fragments;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.samourai.wallet.utxos.PreSelectUtil;
import com.samourai.wallet.utxos.UTXOUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    private List<UTXOCoin> preselectedUTXOs = null;

    public ChooseUTXOsFragment() {
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        utxoRecyclerView = view.findViewById(R.id.whirlpool_utxo_recyclerview);
        if (getArguments() != null && getArguments().containsKey("preselected")) {
            preselectedUTXOs = PreSelectUtil.getInstance().getPreSelected(getArguments().getString("preselected"));
        }
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


        utxoCoinList.addAll(unCycled);

        /*------------------------------------------------*/
        // Change UTXO section

        ArrayList<UTXOCoin> changes = new ArrayList<>();

        // Add all change utxo's
        for (UTXOCoin model : utxoCoins) {
            if (model.account == WhirlpoolMeta.getInstance(getActivity()).getWhirlpoolPostmix()
                    && model.path.startsWith("M/1/")) {
                if (!changes.contains(model)) {
                    changes.add(model);
                }
            }
            if (model.account == 0
                    && model.path.startsWith("M/1/")) {
                if (!changes.contains(model)) {
                    changes.add(model);
                }
            }
        }
        Collections.sort(changes, (model, t1) -> Long.compare(t1.amount, model.amount));


        utxoCoinList.addAll(changes);

        utxoAdapter.updateList(utxoCoinList);

        selectUTXOs();
    }

    private void selectUTXOs() {
        if (preselectedUTXOs != null) {
            List<UTXOCoin> selectedList = new ArrayList<>();

            for (int i = 0; i < utxos.size(); i++) {

                for (int j = 0; j < preselectedUTXOs.size(); j++) {
                    //Checking current utxo lists contains preselected UTXOs
                    if (utxos.get(i).hash != null &&
                            utxos.get(i).hash.equals(preselectedUTXOs.get(j).hash)
                            && utxos.get(i).idx == (preselectedUTXOs.get(j).idx)) {
                        utxos.get(i).isSelected = true;
                        int finalI = i;
                        utxoRecyclerView.post(() -> utxoAdapter.notifyItemChanged(finalI));
                        selectedList.add(utxos.get(i));
                    }
                }
            }
            if (onUTXOSelectionListener != null)
                onUTXOSelectionListener.onSelect(selectedList);
            //Scroll to the selection position
            if (selectedList.size() != 0 && utxos.size() != 0 && utxos.indexOf(selectedList.get(selectedList.size() - 1)) != -1)
                utxoRecyclerView.smoothScrollToPosition(utxos.indexOf(selectedList.get(selectedList.size() - 1)));
        }

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



        @Override
        public WhirlpoolUTXOAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.utxo_item_layout, parent, false);
            return new WhirlpoolUTXOAdapter.ViewHolder(view);

        }

        @Override
        public long getItemId(int position) {
            return utxos.get(position).id;
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

            UTXOCoin item = utxos.get(position);
            holder.address.setText(item.address);
            holder.amount.setText(df.format(((double) (utxos.get(position).amount) / 1e8)).concat(" BTC"));
            holder.rootViewGroup.setOnClickListener(view -> {
                selectOrDeselect(position);
            });
            holder.checkBox.setVisibility(View.VISIBLE);

            holder.tagsLayout.removeAllViews();

            if (UTXOUtil.getInstance().getNote(item.hash) != null) {
                holder.notesLayout.setVisibility(View.VISIBLE);
                holder.notesLayout.removeAllViews();
                ImageView im = new ImageView(holder.rootViewGroup.getContext());
                im.setImageResource(R.drawable.ic_note_black_24dp);
                im.requestLayout();
                LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams((int) (14 * scale + 0.5f), (int) (14 * scale + 0.5f));
                im.setLayoutParams(lparams);
                im.requestLayout();
                im.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.grey_accent)));
                holder.notesLayout.addView(im);
                TextView tx = new TextView(getContext());
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
//
            String utxoIdxHash = item.hash.concat("-").concat(String.valueOf(item.idx));

            if (UTXOUtil.getInstance().get(utxoIdxHash) != null) {
                holder.tagsLayout.setVisibility(View.VISIBLE);
                holder.tagsLayout.removeAllViews();
                for (String tagString : UTXOUtil.getInstance().get(utxoIdxHash)) {
                    View tag = createTag(tagString);
                    holder.tagsLayout.addView(tag);
                }
            }

            // Whirlpool tags for PREMIX & POSTMIX
            Collection<String> whirlpoolTags = WhirlpoolUtils.getInstance().getWhirlpoolTags(item, getContext());
            if (!whirlpoolTags.isEmpty()) {
                holder.notesLayout.setVisibility(View.VISIBLE);
                holder.tagsLayout.setVisibility(View.VISIBLE);
                for (String tagString : whirlpoolTags) {
                    View tag = createTag(tagString);
                    holder.tagsLayout.addView(tag);
                }
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
            TextView address, amount;
            LinearLayout tagsLayout;
            CheckBox checkBox;
            ViewGroup rootViewGroup;
            ImageView indicator;
            LinearLayout notesLayout;

            ViewHolder(View itemView) {
                super(itemView);

                amount = itemView.findViewById(R.id.utxo_item_amount);
                address = itemView.findViewById(R.id.utxo_item_address);
                notesLayout = itemView.findViewById(R.id.utxo_item_notes_layout);
                tagsLayout = itemView.findViewById(R.id.utxo_item_tags_layout);
                checkBox = itemView.findViewById(R.id.multiselect_checkbox);
                rootViewGroup = (ViewGroup) itemView;

                checkBox.setVisibility(View.GONE);
                notesLayout.setVisibility(View.GONE);
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


    private View createTag(String tag) {
        float scale = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView tx = new TextView(getContext());
        tx.setText(tag);
        tx.setTextColor(ContextCompat.getColor(getContext(), R.color.grey_accent));
        tx.setLayoutParams(lparams);
        tx.setBackgroundResource(R.drawable.tag_round_shape);
        tx.setPadding((int) (8 * scale + 0.5f), (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));
        tx.setTypeface(Typeface.DEFAULT_BOLD);
        tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lparams.rightMargin = 4;
        return tx;
    }


    public static ChooseUTXOsFragment newInstance(String preselectId) {
        ChooseUTXOsFragment chooseUTXOsFragment = new ChooseUTXOsFragment();
        Bundle args = new Bundle();
        if (preselectId != null)
            args.putString("preselected", preselectId);
        chooseUTXOsFragment.setArguments(args);
        return chooseUTXOsFragment;
    }

}
