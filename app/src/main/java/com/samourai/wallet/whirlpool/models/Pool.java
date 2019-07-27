package com.samourai.wallet.whirlpool.models;


public class Pool {

    boolean isSelected = false;
    long poolAmount = 0L, poolFee = 0L, minerFee = 0L, totalFee = 0L;

    public long getPoolAmount() {
        return poolAmount;
    }

    public void setPoolAmount(long poolAmount) {
        this.poolAmount = poolAmount;
    }

    public long getPoolFee() {
        return poolFee;
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
        return totalFee;
    }

    public void setTotalFee(long totalFee) {
        this.totalFee = totalFee;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
