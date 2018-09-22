package com.samourai.wallet.whirlpool.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.models.Coin;
import com.samourai.wallet.whirlpool.models.Cycle;

import java.util.ArrayList;

public class CyclesAdapter extends RecyclerView.Adapter<CyclesAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Cycle> mCycles;
    private View.OnClickListener listener;

    public CyclesAdapter(Context context, ArrayList<Cycle> cycles) {
        mCycles = cycles;
        mContext = context;
    }

    public void setItemClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cycle, parent, false);
        if (listener != null)
            view.setOnClickListener(listener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Cycle cycle = mCycles.get(position);
        if (cycle.getStatus() == Cycle.CycleStatus.PENDING) {
            holder.status.setText(mContext.getResources().getText(R.string.pending));
        }
        if (cycle.getStatus() == Cycle.CycleStatus.SUCCESS) {
            holder.status.setText(R.string.completed);
        }
        holder.btcAmount.setText(String.valueOf(cycle.getAmount()).concat(" BTC"));

    }

    public ArrayList<Cycle> getCoins() {
        return mCycles;
    }

    @Override
    public int getItemCount() {
        if (mCycles.isEmpty()) {
            return 0;
        }
        return mCycles.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView btcAmount, status, date;

        ViewHolder(View itemView) {
            super(itemView);
            btcAmount = itemView.findViewById(R.id.cycle_amount);
            status = itemView.findViewById(R.id.cycle_status);
            date = itemView.findViewById(R.id.cycle_date);
        }
    }

}
