package com.samourai.wallet.utxos.models;

import androidx.annotation.Nullable;

import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;

/**
 * UTXO model for UI
 * since there is already a UTXO class exist, {@link UTXOCoin} class is mainly used for RecyclerView
 * UTXOCoin support extra UI related states like isSelected doNotSpend etc..
 */
public class UTXOCoin {
    public String address = "";
    public int id;
    public int account = 0;
    public long amount = 0L;
    public String hash = "";
    public String path = "";
    public int idx = 0;
    public boolean doNotSpend = false;
    public boolean isSelected = false;
    private MyTransactionOutPoint outPoint;

    public MyTransactionOutPoint getOutPoint() {
        return outPoint;
    }


    public UTXOCoin(MyTransactionOutPoint outPoint, UTXO utxo) {
        if (outPoint == null || utxo == null) {
            return;
        }
        this.outPoint = outPoint;
        this.address = outPoint.getAddress();
        this.path = utxo.getPath() == null ? "" : utxo.getPath();
        this.amount = outPoint.getValue().longValue();
        this.hash = outPoint.getTxHash().toString();
        this.idx = outPoint.getTxOutputN();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof UTXOCoin) {
            UTXOCoin coin = ((UTXOCoin) obj);
            return (this.id == coin.id &&
                    this.idx == coin.idx &&
                    this.amount == coin.amount &&
                    this.address.equals(coin.address) &&
                    this.account == coin.account);
        }
        return super.equals(obj);
    }
}


