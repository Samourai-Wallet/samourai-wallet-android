package com.samourai.wallet.home.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.samourai.wallet.R;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.utxos.UTXOUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TxAdapter extends RecyclerView.Adapter<TxAdapter.TxViewHolder> {

    private final int VIEW_ITEM = 1;
    private final int VIEW_SECTION = 0;
    private final Context mContext;
    private final int account;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private OnClickListener listener;
    private static final int MAX_CONFIRM_COUNT = 3;
    private final AsyncListDiffer<Tx> mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnClickListener {
        void onClick(int position, Tx tx);
    }

    public TxAdapter(Context mContext, List<Tx> txes, int account) {
        this.mContext = mContext;
        this.account = account;
        fmt.setTimeZone(TimeZone.getDefault());
        this.updateList(txes);
    }

    public void setClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        disposables.dispose();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public TxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        if (viewType == VIEW_ITEM) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tx_item_layout_, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tx_item_section_layout, parent, false);
        }

        return new TxViewHolder(view, viewType);

    }

    @Override
    public void onBindViewHolder(@NonNull TxViewHolder holder, int position) {
        boolean is_sat_prefs = PrefsUtil.getInstance(this.mContext).getValue(PrefsUtil.IS_SAT, false);

        Tx tx = mDiffer.getCurrentList().get(position);
        if (tx.section == null) {
            long _amount = 0L;
            if (tx.getAmount() < 0.0) {
                _amount = Math.abs((long) tx.getAmount());

            } else {
                _amount = (long) tx.getAmount();

            }
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm", Locale.US);
            sdf.setTimeZone(TimeZone.getDefault());

            holder.tvDateView.setText(sdf.format(tx.getTS() * 1000L));
            if (tx.getPaymentCode() != null) {
                holder.txSubText.setVisibility(View.VISIBLE);
                holder.txSubText.setText(BIP47Meta.getInstance().getDisplayLabel(tx.getPaymentCode()));
            } else {
                holder.txSubText.setVisibility(View.GONE);
            }
            if (this.listener != null)
                holder.itemView.setOnClickListener(view -> {
                    listener.onClick(position, tx);
                });
            if (tx.getAmount() < 0.0) {

                holder.tvDirection.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.out_going_tx_whtie_arrow));

                holder.tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.white));
                holder.tvAmount.setText("-".concat(is_sat_prefs ? FormatsUtil.formatSats(_amount) : FormatsUtil.formatBTC(_amount)));
                if(account==WhirlpoolAccount.POSTMIX.getAccountIndex()){
                    holder.txSubText.setVisibility(View.VISIBLE);
                    holder.txSubText.setText("Postmix spend");
                }else{
                    holder.txSubText.setVisibility(View.GONE);
                }
            } else {
                TransitionManager.beginDelayedTransition((ViewGroup) holder.tvAmount.getRootView(), new ChangeBounds());

                holder.tvDirection.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.incoming_tx_green));
                String amount = is_sat_prefs ? FormatsUtil.formatSats(_amount) : FormatsUtil.formatBTC(_amount);
                if (this.account == WhirlpoolMeta.getInstance(mContext).getWhirlpoolPostmix() && _amount == 0) {
                    amount = amount.concat(mContext.getString(R.string.remix_note_tag));
                }
                holder.tvAmount.setText(amount);
                holder.tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.green_ui_2));
                if(account==WhirlpoolAccount.POSTMIX.getAccountIndex() && _amount!=0){
                    holder.txSubText.setVisibility(View.VISIBLE);
                    holder.txSubText.setText("Mixed");
                    holder.tvDirection.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_whirlpool));
                }else{
                    holder.txSubText.setVisibility(View.GONE);
                }
            }

            if (this.account == WhirlpoolAccount.POSTMIX.getAccountIndex() && _amount == 0) {
                holder.tvDirection.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_repeat_24dp));
            }

            if (UTXOUtil.getInstance().getNote(tx.getHash()) != null) {
                holder.txNoteGroup.setVisibility(View.VISIBLE);
                holder.tvNoteView.setText(UTXOUtil.getInstance().getNote(tx.getHash()));
            } else {
                holder.txNoteGroup.setVisibility(View.GONE);
            }

        } else {

            Date date = new Date(tx.getTS());
            if (tx.getTS() == -1L) {
                holder.tvSection.setText(holder.itemView.getContext().getString(R.string.pending));
                holder.tvSection.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning_yellow));
            } else {
                holder.tvSection.setTextColor(ContextCompat.getColor(holder.tvSection.getContext(), R.color.text_primary));
                if (DateUtils.isToday(tx.getTS())) {
                    holder.tvSection.setText(holder.itemView.getContext().getString(R.string.timeline_today));
                } else {
                    holder.tvSection.setText(fmt.format(date));
                }
            }

        }

    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDiffer.getCurrentList().get(position).section != null ? VIEW_SECTION : VIEW_ITEM;
    }

    public void setTxes(List<Tx> txs) {
        if (txs == null) {
            return;
        }
        this.updateList(txs);
    }

    private void updateList(List<Tx> txes) {
        Disposable disposable = makeSectionedDataSet(txes)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this.mDiffer::submitList);
        disposables.add(disposable);
    }

    public static final DiffUtil.ItemCallback<Tx> DIFF_CALLBACK
            = new DiffUtil.ItemCallback<Tx>() {
        @Override
        public boolean areItemsTheSame(@NonNull Tx oldItem, @NonNull Tx newItem) {
            return oldItem.getTS() == newItem.getTS();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Tx oldItem, @NonNull Tx newItem) {
            if (oldItem.section != null || newItem.section != null) {
                return true;
            }
            boolean reRender = false;
            if (oldItem.getConfirmations() != newItem.getConfirmations()) {
                reRender = true;
            }
            if (!oldItem.getHash().equals(newItem.getHash())) {
                reRender = true;
            }
            return reRender;
        }

    };


    class TxViewHolder extends RecyclerView.ViewHolder {

        private TextView tvSection, tvDateView, tvAmount, txSubText, tvNoteView;
        private ImageView tvDirection;
        private LinearLayout txNoteGroup;


        TxViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType == VIEW_SECTION) {
                tvSection = itemView.findViewById(R.id.section_title);

            } else {
                tvDateView = itemView.findViewById(R.id.tx_time);
                tvDirection = itemView.findViewById(R.id.TransactionDirection);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                txSubText = itemView.findViewById(R.id.txSubText);
                txNoteGroup = itemView.findViewById(R.id.tx_note_group);
                tvNoteView = itemView.findViewById(R.id.tx_note_view);
            }

        }
    }


    private synchronized Observable<List<Tx>> makeSectionedDataSet(List<Tx> txes) {
        return Observable.fromCallable(() -> {
            Collections.sort(txes, (tx, t1) -> Long.compare(tx.getTS(), t1.getTS()));
            ArrayList<Long> sectionDates = new ArrayList<>();
            List<Tx> sectioned = new ArrayList<>();
            // for pending state
            WhirlpoolWallet wallet =   AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
            boolean contains_pending = false;
            boolean containsNonPendingTxForTodaySection = false;
            for (int i = 0; i < txes.size(); i++) {
                Tx tx = txes.get(i);
                if (tx.getConfirmations() < MAX_CONFIRM_COUNT) {
                    contains_pending = true;
                }
                if(tx.getConfirmations() >= MAX_CONFIRM_COUNT && DateUtils.isToday(tx.getTS() * 1000)){
                    containsNonPendingTxForTodaySection = true;
                }
                if(wallet !=null ){
                    for (WhirlpoolUtxo whirlpoolUtxo: wallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.POSTMIX)) {
                        if(whirlpoolUtxo.getUtxo().tx_hash.equals(tx.getHash())){

                        }
                    }
                }
            }
            for (Tx tx : txes) {
                Date date = new Date();
                date.setTime(tx.getTS() * 1000);
                Calendar calendarDM = Calendar.getInstance();
                calendarDM.setTimeZone(TimeZone.getDefault());
                calendarDM.setTime(date);
                calendarDM.set(Calendar.HOUR_OF_DAY, 0);
                calendarDM.set(Calendar.MINUTE, 0);
                calendarDM.set(Calendar.SECOND, 0);
                calendarDM.set(Calendar.MILLISECOND, 0);

                if (!sectionDates.contains(calendarDM.getTime().getTime())) {
                    if(DateUtils.isToday(calendarDM.getTime().getTime())){
                        if(containsNonPendingTxForTodaySection){
                            sectionDates.add(calendarDM.getTime().getTime());
                        }
                    }else{
                        sectionDates.add(calendarDM.getTime().getTime());
                    }
                }

            }

            Collections.sort(sectionDates, Long::compare);


            if (contains_pending)
                sectionDates.add(-1L);

            for (Long key : sectionDates) {

                Tx section = new Tx(new JSONObject());
                if (key != -1) {
                    section.section = new Date(key).toString();
                } else {
                    section.section = mContext.getString(R.string.pending);
                }

                section.setTS(key);
                for (Tx tx : txes) {
                    Date date = new Date();
                    date.setTime(tx.getTS() * 1000);
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
                    fmt.setTimeZone(TimeZone.getDefault());
                    if (key == -1) {
                        if (tx.getConfirmations() < MAX_CONFIRM_COUNT) {
                            sectioned.add(tx);
                        }
                    } else if (fmt.format(key).equals(fmt.format(date))) {

                        if (tx.getConfirmations() >= MAX_CONFIRM_COUNT) {
                            sectioned.add(tx);
                        }

                    }

                }
                sectioned.add(section);
            }

            Collections.reverse(sectioned);
            return sectioned;
        });

    }


}
