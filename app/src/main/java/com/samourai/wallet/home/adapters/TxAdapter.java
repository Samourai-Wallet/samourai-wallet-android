package com.samourai.wallet.home.adapters;

import android.content.Context;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.text.format.DateUtils;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.utxos.UTXOUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.BtcFormat;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
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
    private static final String TAG = "TxAdapter";
    private Context mContext;
    private List<Tx> txes;
    private int account = 0;
    private CompositeDisposable disposables = new CompositeDisposable();
    private OnClickListener listener;
    private static final int MAX_CONFIRM_COUNT = 3;

    public interface OnClickListener {
        void onClick(int position, Tx tx);
    }

    public TxAdapter(Context mContext, List<Tx> txes, int account) {
        this.mContext = mContext;
        this.account = account;
        this.txes = new ArrayList<>();
        Disposable disposable = makeSectionedDataSet(txes)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((List<Tx> list) -> {
                    this.txes = list;
                });
        disposables.add(disposable);

    }

    public void setClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        disposables.dispose();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public long getItemId(int position) {
        return txes.get(position).getTS();
    }

    @Override
    public TxViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(TxViewHolder holder, int position) {
        boolean is_sat_prefs = PrefsUtil.getInstance(this.mContext).getValue(PrefsUtil.IS_SAT, false);

        Tx tx = txes.get(position);
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
                holder.tvPaynymId.setVisibility(View.VISIBLE);
                holder.tvPaynymId.setText(BIP47Meta.getInstance().getDisplayLabel(tx.getPaymentCode()));
            } else {
                holder.tvPaynymId.setVisibility(View.INVISIBLE);
            }
            if (this.listener != null)
                holder.itemView.setOnClickListener(view -> {
                    listener.onClick(position, tx);
                });
            if (tx.getAmount() < 0.0) {

                holder.tvDirection.setImageDrawable(mContext.getDrawable(R.drawable.out_going_tx_whtie_arrow));

                holder.tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.white));
                holder.tvAmount.setText("-".concat(is_sat_prefs ? FormatsUtil.formatSats(_amount) :FormatsUtil.formatBTC(_amount)));

            } else {
                TransitionManager.beginDelayedTransition((ViewGroup) holder.tvAmount.getRootView(), new ChangeBounds());

                holder.tvDirection.setImageDrawable(mContext.getDrawable(R.drawable.incoming_tx_green));
                String amount = is_sat_prefs ?  FormatsUtil.formatSats(_amount): FormatsUtil.formatBTC(_amount);
                if (this.account == WhirlpoolMeta.getInstance(mContext).getWhirlpoolPostmix() && _amount == 0) {
                    amount = amount.concat(" (Remix)");
                }
                holder.tvAmount.setText(amount);
                holder.tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.green_ui_2));
            }

            if (this.account == WhirlpoolMeta.getInstance(mContext).getWhirlpoolPostmix() && _amount == 0) {
                holder.tvDirection.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_repeat_24dp));
            }

            if (UTXOUtil.getInstance().getNote(tx.getHash()) != null) {
                holder.txNoteGroup.setVisibility(View.VISIBLE);
                holder.tvNoteView.setText(UTXOUtil.getInstance().getNote(tx.getHash()));
            } else {
                holder.txNoteGroup.setVisibility(View.INVISIBLE);
            }

        } else {
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            fmt.setTimeZone(TimeZone.getDefault());
            Date date = new Date(tx.getTS());
            if (tx.getTS() == -1L) {
                holder.tvSection.setText("  ".concat(holder.itemView.getContext().getString(R.string.pending)));
            } else {
                holder.tvSection.setText(fmt.format(date));
            }

        }

    }

    @Override
    public int getItemCount() {
        return txes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return txes.get(position).section != null ? VIEW_SECTION : VIEW_ITEM;
    }

    public void setTxes(List<Tx> txs) {
        if (txs == null) {
            return;
        }
        Disposable disposable = makeSectionedDataSet(txs)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((List<Tx> list) -> {
                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TxDiffUtil(this.txes, list));
                    this.txes = list;
                    diffResult.dispatchUpdatesTo(this);
                    this.notifyDataSetChanged();
                });
        disposables.add(disposable);

    }

    class TxViewHolder extends RecyclerView.ViewHolder {

        private TextView tvSection, tvDateView, tvAmount, tvPaynymId, tvNoteView;
        private ImageView tvDirection;
        private Group txNoteGroup;


        TxViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType == VIEW_SECTION) {
                tvSection = itemView.findViewById(R.id.section_title);

            } else {

                tvDateView = itemView.findViewById(R.id.tx_time);

            }

            tvDirection = itemView.findViewById(R.id.TransactionDirection);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPaynymId = itemView.findViewById(R.id.paynymId);
            txNoteGroup = itemView.findViewById(R.id.tx_note_group);
            tvNoteView = itemView.findViewById(R.id.tx_note_view);

        }
    }


    private synchronized Observable<List<Tx>> makeSectionedDataSet(List<Tx> txes) {
        return Observable.fromCallable(() -> {
            Collections.sort(txes, (tx, t1) -> Long.compare(tx.getTS(), t1.getTS()));
            ArrayList<Long> sectionDates = new ArrayList<>();
            List<Tx> sectioned = new ArrayList<>();
            // for pending state
            boolean contains_pending = false;
            //if there is only pending tx today we don't want to add today's section
            boolean show_todays_tx = false;
            for (int i = 0; i < txes.size(); i++) {
                Tx tx = txes.get(i);
                if (tx.getConfirmations() < MAX_CONFIRM_COUNT) {
                    contains_pending = true;
                }
                if (tx.getConfirmations() >= MAX_CONFIRM_COUNT && DateUtils.isToday(tx.getTS() * 1000)) {
                    show_todays_tx = true;
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
                    if (DateUtils.isToday(calendarDM.getTime().getTime())) {
                        if (show_todays_tx)
                            sectionDates.add(calendarDM.getTime().getTime());
                    } else {
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
                    section.section = "pending";
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
