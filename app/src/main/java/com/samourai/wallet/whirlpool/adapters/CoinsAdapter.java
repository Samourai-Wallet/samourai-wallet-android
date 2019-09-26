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
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.UTXOUtil;
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
        holder.btcTxView.setText(MonetaryUtil.getInstance().getBTCFormat().format(((double) coin.getValue()) / 1e8) + " BTC");

        holder.checkBox.setChecked(coin.getSelected());
        holder.checkBox.setTag(mCoins.get(position));

        String tag = UTXOUtil.getInstance().get(coin.getOutpoint().getTxHash() + "-" + coin.getOutpoint().getTxOutputN());
        if(tag != null)    {
            holder.labelGroup.setVisibility(View.VISIBLE);
            holder.labelTxView.setText(tag);
        }
        else    {
            holder.labelGroup.setVisibility(View.GONE);
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

        private TextView btcTxView, addressTxView, labelTxView;
        private CheckBox checkBox;
        private Group labelGroup;

        ViewHolder(View itemView) {
            super(itemView);
            btcTxView = itemView.findViewById(R.id.coin_item_amount);
            addressTxView = itemView.findViewById(R.id.coin_utxo_address);
            checkBox = itemView.findViewById(R.id.coin_item_checkbox);
            labelGroup = itemView.findViewById(R.id.label_group);
            labelTxView = itemView.findViewById(R.id.coin_utxo_label);
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
