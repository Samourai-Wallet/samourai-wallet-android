package com.samourai.wallet.whirlpool.models;

import com.samourai.wallet.send.MyTransactionOutPoint;

public class Coin {

    private MyTransactionOutPoint outpoint = null;

    private Coin()  { ; }

    public Coin(MyTransactionOutPoint outpoint)  {
        this.outpoint = outpoint;
    }

    //States for recycler view
    private Boolean isSelected = false, blocked = false;

    public String getAddress() {
        return outpoint.getAddress();
    }

    public long getValue() {
        return outpoint.getValue().longValue();
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public Coin setSelected(Boolean selected) {
        isSelected = selected;
        return this;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public String toString() {
        return this.getAddress().concat("\t");
    }

}
