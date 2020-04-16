package com.samourai.wallet.home.adapters;

import androidx.recyclerview.widget.DiffUtil;

import com.samourai.wallet.api.Tx;

import java.util.List;

public class TxDiffUtil extends DiffUtil.Callback {

    private List<Tx> oldTxes;
    private List<Tx> newTxes;


    TxDiffUtil(List<Tx> txes, List<Tx> txs) {
        this.newTxes = txes;
        this.oldTxes = txs;
    }

    @Override
    public int getOldListSize() {
        return oldTxes.size();
    }

    @Override
    public int getNewListSize() {
        return newTxes.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldTxes.get(oldItemPosition).getTS() == newTxes.get(newItemPosition).getTS();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Tx oldItem;
        Tx newItem;
        try {
            oldItem = oldTxes.get(oldItemPosition);
            newItem = oldTxes.get(newItemPosition);
        } catch(Exception e) {
            // IndexOutOfBoundsException
            return false;
        }
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
}
