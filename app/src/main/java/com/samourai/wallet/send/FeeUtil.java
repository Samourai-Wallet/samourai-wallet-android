package com.samourai.wallet.send;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.core.Transaction;

public class FeeUtil  {

    private static final int ESTIMATED_INPUT_LEN = 148; // compressed key
    private static final int ESTIMATED_OUTPUT_LEN = 34;

    private static SuggestedFee suggestedFee = null;
    private static SuggestedFee highFee = null;
    private static SuggestedFee normalFee = null;
    private static SuggestedFee lowFee = null;
    private static List<SuggestedFee> estimatedFees = null;

    private static FeeUtil instance = null;

    private FeeUtil() { ; }

    public static FeeUtil getInstance() {

        if(instance == null)    {
            estimatedFees = new ArrayList<SuggestedFee>();
            highFee = new SuggestedFee();
            suggestedFee = new SuggestedFee();
            lowFee = new SuggestedFee();
            instance = new FeeUtil();
        }

        return instance;
    }

    public BigInteger estimatedFee(int inputs, int outputs)   {
        int size = estimatedSize(inputs, outputs);
        return calculateFee(size, getSuggestedFee().getDefaultPerKB());
    }

    public int estimatedSize(int inputs, int outputs)   {
        return (outputs * ESTIMATED_OUTPUT_LEN) + (inputs * ESTIMATED_INPUT_LEN) + inputs;
    }

    public synchronized SuggestedFee getSuggestedFee() {
        if(suggestedFee != null)    {
            return suggestedFee;
        }
        else    {
            SuggestedFee fee = new SuggestedFee();
            fee.setDefaultPerKB(BigInteger.valueOf(10000L));
            return fee;
        }
    }

    public synchronized void setSuggestedFee(SuggestedFee suggestedFee) {
        FeeUtil.suggestedFee = suggestedFee;
    }

    public synchronized SuggestedFee getHighFee() {
        if(highFee == null)    {
            highFee = getSuggestedFee();
        }

        return highFee;
    }

    public synchronized void setHighFee(SuggestedFee highFee) {
        FeeUtil.highFee = highFee;
    }

    public synchronized SuggestedFee getNormalFee() {
        if(normalFee == null)    {
            normalFee = getSuggestedFee();
        }

        return normalFee;
    }

    public synchronized void setNormalFee(SuggestedFee normalFee) {
        FeeUtil.normalFee = normalFee;
    }

    public synchronized SuggestedFee getLowFee() {
        if(lowFee == null)    {
            lowFee = getSuggestedFee();
        }

        return lowFee;
    }

    public synchronized void setLowFee(SuggestedFee lowFee) {
        FeeUtil.lowFee = lowFee;
    }

    public synchronized List<SuggestedFee> getEstimatedFees() {
        return estimatedFees;
    }

    public synchronized void setEstimatedFees(List<SuggestedFee> estimatedFees) {
        FeeUtil.estimatedFees = estimatedFees;

        switch(estimatedFees.size())    {
            case 1:
                suggestedFee = highFee = normalFee = lowFee = estimatedFees.get(0);
                break;
            case 2:
                suggestedFee = highFee = normalFee = estimatedFees.get(0);
                lowFee = estimatedFees.get(1);
                break;
            case 3:
                highFee = estimatedFees.get(0);
                suggestedFee = estimatedFees.get(1);
                normalFee = estimatedFees.get(1);
                lowFee = estimatedFees.get(2);
                break;
            default:
                break;
        }

    }

    public BigInteger estimatedFee(int inputs, int outputs, BigInteger feePerKb)   {
        int size = estimatedSize(inputs, outputs);
        return calculateFee(size, feePerKb);
    }

    public BigInteger calculateFee(int txSize, BigInteger feePerKb)   {
        double fee = ((double)txSize / 1000.0 ) * feePerKb.doubleValue();
        return BigInteger.valueOf((long)fee);
    }

    // use unsigned tx here
    private static long getPriority(Transaction tx, List<MyTransactionOutPoint> outputs)   {
        long priority = 0L;
        for(MyTransactionOutPoint output : outputs)   {
            priority += output.getValue().longValue() * output.getConfirmations();
        }
        //
        // calculate priority
        //
        long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());
        priority /= estimatedSize;
        return priority;
    }

}
