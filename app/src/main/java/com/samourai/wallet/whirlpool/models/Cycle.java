package com.samourai.wallet.whirlpool.models;

import android.content.Intent;

import java.util.Date;

public class Cycle {

    public enum CycleStatus {PENDING, SUCCESS, FAILED}

    private Float amount, totalFees;
    private CycleStatus status;
    private Date startTime;
    private String txID;
    private int progress = 0;

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public Float getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(Float totalFees) {
        this.totalFees = totalFees;
    }

    public CycleStatus getStatus() {
        return status;
    }

    public void setStatus(CycleStatus status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getTxID() {
        return txID;
    }

    public void setTxID(String txID) {
        this.txID = txID;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
