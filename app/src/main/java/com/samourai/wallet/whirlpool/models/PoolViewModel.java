package com.samourai.wallet.whirlpool.models;

import com.samourai.whirlpool.client.whirlpool.beans.Pool;

public class PoolViewModel extends com.samourai.whirlpool.client.whirlpool.beans.Pool {

    private boolean isSelected = false;
    private boolean isDisabled = false;
    private long minerFee = 0L;

    public PoolViewModel(Pool pool) {
        this.setDenomination(pool.getDenomination());
        this.setFeeValue(pool.getFeeValue());
        this.setPoolId(pool.getPoolId());
    }

    public long getMinerFee() {
        return minerFee;
    }

    public void setMinerFee(long minerFee) {
        this.minerFee = minerFee;
    }

    public long getTotalFee() {
        return minerFee + this.getFeeValue();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

}
