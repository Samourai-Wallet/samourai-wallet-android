package com.samourai.wallet.send;

import java.math.BigInteger;
import java.util.List;

public class UnspentOutputsBundle	{

    private List<MyTransactionOutPoint> outputs = null;
    private int nbAddress = 0;
    private boolean isChangeSafe = false;
    private BigInteger totalAmount = BigInteger.ZERO;
    private int type = -1;

    public static final int SINGLE_OUTPUT = 1;
    public static final int SINGLE_ADDRESS = 2;
    public static final int ONLY_RECEIVE = 3;
    public static final int ONLY_CHANGE_DEDUP_TX = 4;
    public static final int MIXED_DEDUP_TX = 5;
    public static final int MIXED = 6;
    public static final int REFRESHED = 7;

    public UnspentOutputsBundle() {
        outputs = null;
        nbAddress = 0;
        isChangeSafe = false;
        totalAmount = BigInteger.ZERO;
        type = -1;
    }

    public List<MyTransactionOutPoint> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<MyTransactionOutPoint> outputs) {
        this.outputs = outputs;
    }

    public int getNbAddress() {
        return nbAddress;
    }

    public void setNbAddress(int nbAddress) {
        this.nbAddress = nbAddress;
    }

    public boolean isChangeSafe() {
        return isChangeSafe;
    }

    public void setChangeSafe(boolean isChangeSafe) {
        this.isChangeSafe = isChangeSafe;
    }

    public BigInteger getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigInteger totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
