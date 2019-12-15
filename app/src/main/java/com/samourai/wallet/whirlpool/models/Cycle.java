package com.samourai.wallet.whirlpool.models;

import com.samourai.whirlpool.client.mix.listener.MixStep;

import java.util.Date;

public class Cycle {

    public enum CycleStatus {PENDING, SUCCESS, FAILED}

    private long amount, totalFees;
    private CycleStatus status;
    private Date startTime;
    private String txHash;
    private int progress = 0;
    private String poolId = "";
    private MixStep mixStep;



    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(long totalFees) {
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

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public MixStep getMixStep() {
        return mixStep;
    }

    public void setMixStep(MixStep mixStep) {
        this.mixStep = mixStep;
    }
}
