package com.samourai.wallet.whirlpool.adapters;

import android.content.Context;
import android.support.constraint.Group;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.models.Pool;

import java.util.ArrayList;

public class PoolsAdapter extends RecyclerView.Adapter<PoolsAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Pool> mPools;
    private OnItemsSelected onItemsSelected;
    private static final String TAG = "CoinsAdapter";

    public PoolsAdapter(Context context, ArrayList<Pool> coins) {
        mPools = coins;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pool, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Pool pool = mPools.get(position);
        holder.poolAmount.setText(getBTCDisplayAmount(pool.getPoolAmount()).concat(" BTC Pool"));
        holder.poolFees.setText(mContext.getString(R.string.pool_fee).concat("    ").concat(getBTCDisplayAmount(pool.getPoolFee())).concat(" BTC"));
        holder.totalFees.setText(mContext.getString(R.string.total_fees).concat("    ").concat(getBTCDisplayAmount(pool.getTotalFee())).concat(" BTC"));
        holder.minorFees.setText(mContext.getString(R.string.miner_fee).concat("    ").concat(getBTCDisplayAmount(pool.getMinerFee())).concat(" BTC"));
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(pool.isSelected());
        if (pool.isSelected()) {
            holder.feesGroup.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(view -> {
            holder.feesGroup.setVisibility(holder.feesGroup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        holder.checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            onItemsSelected.onItemsSelected(position);
        });
    }

    private void selectItem(ViewHolder holder, int position) {
        Pool pool = mPools.get(position);
        mPools.get(position).setSelected(!pool.isSelected());
        holder.checkBox.setChecked(pool.isSelected());

    }


    @Override
    public int getItemCount() {
        if (mPools.isEmpty()) {
            return 0;
        }
        return mPools.size();
    }

    public void setOnItemsSelectListener(OnItemsSelected onItemsSelected) {
        this.onItemsSelected = onItemsSelected;
    }

    public void update(ArrayList<Pool> pools) {
        this.mPools = pools;
        this.notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView poolAmount, poolFees, minorFees, totalFees;
        private CheckBox checkBox;
        private Group feesGroup;

        ViewHolder(View itemView) {
            super(itemView);
            poolAmount = itemView.findViewById(R.id.pool_item_amount);
            poolFees = itemView.findViewById(R.id.pool_item_fee);
            minorFees = itemView.findViewById(R.id.pool_item_miner_fee);
            totalFees = itemView.findViewById(R.id.pool_item_total_fee);
            checkBox = itemView.findViewById(R.id.pool_item_checkbox);
            feesGroup = itemView.findViewById(R.id.item_pool_fees_group);
        }
    }


    public interface OnItemsSelected {
        void onItemsSelected(int position);
    }


    private String getBTCDisplayAmount(long value) {
        return org.bitcoinj.core.Coin.valueOf(value).toPlainString();
    }


}
