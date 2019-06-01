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
import com.samourai.wallet.whirlpool.models.Coin;

import java.util.ArrayList;

public class CoinsAdapter extends RecyclerView.Adapter<CoinsAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Coin> mCoins;
    private OnItemsSelected onItemsSelected;
    private static final String TAG = "CoinsAdapter";

    public CoinsAdapter(Context context, ArrayList<Coin> coins) {
        mCoins = coins;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Coin coin = mCoins.get(position);

        holder.addressTxView.setText(coin.getAddress());
        holder.btcTxView.setText(String.valueOf(coin.getValue()).concat(" BTC"));
        holder.checkBox.setChecked(coin.getSelected());
        holder.checkBox.setTag(mCoins.get(position));

        // Placeholder for showing tx with coinbase tag
        if(position==2){
            holder.coinbaseGroup.setVisibility(View.VISIBLE);
        }else {
            holder.coinbaseGroup.setVisibility(View.GONE);

        }

        if (coin.getBlocked()) {
            holder.btcTxView.setAlpha(.6f);
            holder.addressTxView.setAlpha(.5f);
            holder.checkBox.setEnabled(false);
        }
        holder.itemView.setOnClickListener(view -> selectItem(holder, position));
        holder.checkBox.setOnClickListener(view -> selectItem(holder, position));

    }

    private void selectItem(ViewHolder holder, int position) {
        Coin mCoin = mCoins.get(position);
        mCoins.get(position).setSelected(!mCoin.getSelected());
        holder.checkBox.setChecked(mCoin.getSelected());
        if (onItemsSelected != null) {
            onItemsSelected.onItemsSelected(getSelectedCoins());
        }
    }


    @Override
    public int getItemCount() {
        if (mCoins.isEmpty()) {
            return 0;
        }
        return mCoins.size();
    }

    public void setOnItemsSelectListener(OnItemsSelected onItemsSelected) {
        this.onItemsSelected = onItemsSelected;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView btcTxView, addressTxView;
        private CheckBox checkBox;
        private Group coinbaseGroup;

        ViewHolder(View itemView) {
            super(itemView);
            btcTxView = itemView.findViewById(R.id.coin_item_amount);
            addressTxView = itemView.findViewById(R.id.coin_utxo_address);
            checkBox = itemView.findViewById(R.id.coin_item_checkbox);
            coinbaseGroup = itemView.findViewById(R.id.coinbase_tx_group);
        }
    }


    public interface OnItemsSelected {
        void onItemsSelected(ArrayList<Coin> coins);
    }

    public ArrayList<Coin> getSelectedCoins() {
        ArrayList<Coin> coins = new ArrayList<>();
        for (Coin coin : this.mCoins) {
            if (coin.getSelected()) {
                coins.add(coin);
            }

        }
        return coins;
    }
}
