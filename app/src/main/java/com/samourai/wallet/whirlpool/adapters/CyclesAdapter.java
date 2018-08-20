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


    public CyclesAdapter(Context context, ArrayList<Cycle> cycles) {
        mCycles = cycles;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cycle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Cycle cycle = mCycles.get(position);



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

        private TextView btcTxView, addressTxView;
        private CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);

        }
    }

}
