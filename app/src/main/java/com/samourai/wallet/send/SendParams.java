package com.samourai.wallet.send;

import com.samourai.wallet.util.BatchSendUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import static com.samourai.wallet.SendActivity.SPEND_BOLTZMANN;

public class SendParams	{

    private static List<MyTransactionOutPoint> outpoints = null;
    private static HashMap<String, BigInteger> receivers = null;
    private static String strPCode = null;
    private int SPEND_TYPE =  SPEND_BOLTZMANN;
    private long changeAmount = 0L;
    private int changeType = 49;
    private String strDestAddress = null;
    private boolean hasPrivacyWarning = false;
    private boolean hasPrivacyChecked = false;
    private long spendAmount = 0L;
    private int changeIdx = 0;
    private List<BatchSendUtil.BatchSend> batchSend = null;

    private static SendParams instance = null;

    private SendParams () { ; }

    public static SendParams getInstance() {

        if(instance == null)	{
            instance = new SendParams();
        }

        return instance;
    }

    public void reset() {
        this.outpoints = null;
        this.receivers = null;
        this.strPCode = null;
        this.SPEND_TYPE = SPEND_BOLTZMANN;
        this.changeAmount = 0L;
        this.changeType = 49;
        this.strDestAddress = null;
        this.hasPrivacyWarning = false;
        this.hasPrivacyChecked = false;
        this.spendAmount = 0L;
        this.changeIdx = 0;
        this.batchSend = null;
    }

    public void setParams(List<MyTransactionOutPoint> outpoints, HashMap<String, BigInteger> receivers, String strPCode, int SPEND_TYPE, long changeAmount, int changeType, String strDestAddress, boolean hasPrivacyWarning, boolean hasPrivacyChecked, long spendAmount, int changeIdx) {
        this.outpoints = outpoints;
        this.receivers = receivers;
        this.strPCode = strPCode;
        this.SPEND_TYPE = SPEND_TYPE;
        this.changeAmount = changeAmount;
        this.changeType = changeType;
        this.strDestAddress = strDestAddress;
        this.hasPrivacyWarning = hasPrivacyWarning;
        this.hasPrivacyChecked = hasPrivacyChecked;
        this.spendAmount = spendAmount;
        this.changeIdx = changeIdx;
    }

    public void setParams(List<MyTransactionOutPoint> outpoints, HashMap<String, BigInteger> receivers, List<BatchSendUtil.BatchSend> batchSend, int SPEND_TYPE, long changeAmount, int changeType, String strDestAddress, boolean hasPrivacyWarning, boolean hasPrivacyChecked, long spendAmount, int changeIdx) {
        this.outpoints = outpoints;
        this.receivers = receivers;
        this.batchSend = batchSend;
        this.SPEND_TYPE = SPEND_TYPE;
        this.changeAmount = changeAmount;
        this.changeType = changeType;
        this.strDestAddress = strDestAddress;
        this.hasPrivacyWarning = hasPrivacyWarning;
        this.hasPrivacyChecked = hasPrivacyChecked;
        this.spendAmount = spendAmount;
        this.changeIdx = changeIdx;
    }

    public List<MyTransactionOutPoint> getOutpoints()   {
        return outpoints;
    }

    public HashMap<String, BigInteger> getReceivers() {
        return receivers;
    }

    public String getPCode() {
        return strPCode;
    }

    public List<BatchSendUtil.BatchSend> getBatchSend() {
        return batchSend;
    }

    public int getSpendType()   {
        return SPEND_TYPE;
    }

    public long getChangeAmount()   {
        return changeAmount;
    }

    public int getChangeType()  {
        return  changeType;
    }

    public String getDestAddress() {
        return strDestAddress;
    }

    public boolean hasPrivacyWarning()   {
        return hasPrivacyWarning;
    }

    public boolean hasPrivacyChecked()   {
        return hasPrivacyChecked;
    }

    public long getSpendAmount()    {
        return spendAmount;
    }

    public int getChangeIdx()   {
        return changeIdx;
    }
}
