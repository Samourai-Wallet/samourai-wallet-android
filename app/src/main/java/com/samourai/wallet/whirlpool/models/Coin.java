package com.samourai.wallet.whirlpool.models;

import com.samourai.wallet.send.MyTransactionOutPoint;

public class Coin {

    private MyTransactionOutPoint outpoint = null;

    //States for recycler view
    private Boolean isSelected = false;

    private Coin()  { ; }

    public Coin(MyTransactionOutPoint outpoint)  {
        this.outpoint = outpoint;
    }

    public MyTransactionOutPoint getOutpoint() {
        return outpoint;
    }

    public String getAddress() {
        return outpoint.getAddress();
    }

    public long getValue() {
        return outpoint.getValue().longValue();
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }

    @Override
    public String toString() {
        return "selected:" + isSelected + ", " + this.outpoint.toString();
    }

}
