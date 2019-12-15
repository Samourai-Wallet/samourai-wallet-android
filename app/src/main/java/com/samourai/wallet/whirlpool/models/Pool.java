package com.samourai.wallet.whirlpool.models;

public class Pool {

    private boolean isSelected = false;
    private boolean isDisabled = false;
    private long poolAmount = 0L;
    private long poolFee = 0L;
    private long minerFee = 0L;

    public long getPoolAmount() {
        return poolAmount;
    }

    public void setPoolAmount(long poolAmount) {
        this.poolAmount = poolAmount;
    }

    public long getPoolFee() {
        return (long)(poolAmount * 0.05);
    }

    public void setPoolFee(long poolFee) {
        this.poolFee = poolFee;
    }

    public long getMinerFee() {
        return minerFee;
    }

    public void setMinerFee(long minerFee) {
        this.minerFee = minerFee;
    }

    public long getTotalFee() {
        return minerFee + poolFee;
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
