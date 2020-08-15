package com.samourai.wallet.send;

import android.util.Log;

import com.samourai.wallet.SamouraiWallet;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.NetworkParameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FeeUtil extends com.samourai.wallet.util.FeeUtil {

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

    public BigInteger estimatedFee(int inputs, int outputs)   {
        int size = estimatedSize(inputs, outputs);
        return calculateFee(size, getSuggestedFee().getDefaultPerKB());
    }

    public BigInteger estimatedFeeSegwit(int inputsP2PKH, int inputsP2SHP2WPKH, int inputsP2WPKH, int outputs)   {
        int size = estimatedSizeSegwit(inputsP2PKH, inputsP2SHP2WPKH, inputsP2WPKH, outputs, 0);
        return calculateFee(size, getSuggestedFee().getDefaultPerKB());
    }

    public int estimatedSize(int inputs, int outputs)   {
        return estimatedSizeSegwit(inputs, 0, 0, outputs, 0);
    }

    public BigInteger estimatedFee(int inputs, int outputs, BigInteger feePerKb)   {
        int size = estimatedSize(inputs, outputs);
        return calculateFee(size, feePerKb);
    }

    public BigInteger calculateFee(int txSize, BigInteger feePerKb)   {
        long feePerB = toFeePerB(feePerKb);
        long fee = calculateFee(txSize, feePerB);
        return BigInteger.valueOf(fee);
    }

    public void sanitizeFee()  {
        if(FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() < 1000L)    {
            SuggestedFee suggestedFee = new SuggestedFee();
            suggestedFee.setDefaultPerKB(BigInteger.valueOf(1200L));
            Log.d("FeeUtil", "adjusted fee:" + suggestedFee.getDefaultPerKB().longValue());
            FeeUtil.getInstance().setSuggestedFee(suggestedFee);
        }
    }

    private long toFeePerB(BigInteger feePerKb) {
        long feePerB = Math.round(feePerKb.doubleValue() / 1000.0);
        return feePerB;
    }

    public long getSuggestedFeeDefaultPerB() {
        return toFeePerB(getSuggestedFee().getDefaultPerKB());
    }

    public Triple<Integer, Integer, Integer> getOutpointCount(Vector<MyTransactionOutPoint> outpoints) {
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
        return super.getOutpointCount(outpoints, params);
    }
}
