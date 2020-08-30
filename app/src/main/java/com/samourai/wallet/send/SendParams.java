package com.samourai.wallet.send;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.util.BatchSendUtil;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.samourai.wallet.send.SendActivity.SPEND_BOLTZMANN;
import static com.samourai.wallet.util.LogUtil.debug;

public class SendParams	{

    private static List<MyTransactionOutPoint> outpoints = null;
    private static HashMap<String, BigInteger> receivers = null;
    private static String strPCode = null;
    private int SPEND_TYPE =  SPEND_BOLTZMANN;
    private long changeAmount = 0L;
    private int changeType = 49;
    private int account = 0;
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
        this.account = 0;
        this.strDestAddress = null;
        this.hasPrivacyWarning = false;
        this.hasPrivacyChecked = false;
        this.spendAmount = 0L;
        this.changeIdx = 0;
        this.batchSend = null;
    }

    public void setParams(List<MyTransactionOutPoint> outpoints, HashMap<String, BigInteger> receivers, String strPCode, int SPEND_TYPE, long changeAmount, int changeType, int account, String strDestAddress, boolean hasPrivacyWarning, boolean hasPrivacyChecked, long spendAmount, int changeIdx) {
        this.outpoints = outpoints;
        this.receivers = receivers;
        this.strPCode = strPCode;
        this.SPEND_TYPE = SPEND_TYPE;
        this.changeAmount = changeAmount;
        this.changeType = changeType;
        this.account = account;
        this.strDestAddress = strDestAddress;
        this.hasPrivacyWarning = hasPrivacyWarning;
        this.hasPrivacyChecked = hasPrivacyChecked;
        this.spendAmount = spendAmount;
        this.changeIdx = changeIdx;
    }

    public void setParams(List<MyTransactionOutPoint> outpoints, HashMap<String, BigInteger> receivers, List<BatchSendUtil.BatchSend> batchSend, int SPEND_TYPE, long changeAmount, int changeType, int account, String strDestAddress, boolean hasPrivacyWarning, boolean hasPrivacyChecked, long spendAmount, int changeIdx) {
        this.outpoints = outpoints;
        this.receivers = receivers;
        this.batchSend = batchSend;
        this.SPEND_TYPE = SPEND_TYPE;
        this.changeAmount = changeAmount;
        this.changeType = changeType;
        this.account = account;
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
        return changeType;
    }

    public int getAccount()  {
        return account;
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

    public List<Integer> getSpendOutputIndex(Transaction tx)   {

        List<Integer> ret = new ArrayList<Integer>();

        for (int i = 0; i < tx.getOutputs().size(); i++) {
            TransactionOutput output = tx.getOutput(i);
            Script script = output.getScriptPubKey();
            String scriptPubKey = Hex.toHexString(script.getProgram());
            Address _p2sh = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
            Address _p2pkh = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
            try {
                if(Bech32Util.getInstance().isBech32Script(scriptPubKey)) {
                    if(Bech32Util.getInstance().getAddressFromScript(scriptPubKey).compareToIgnoreCase(getDestAddress()) == 0) {
                        debug("SendParams", "send address identified:" + Bech32Util.getInstance().getAddressFromScript(scriptPubKey));
                        debug("SendParams", "send address output index:" + i);
                        ret.add(i);
                    }
                }
                else if(_p2sh != null && _p2pkh == null && _p2sh.toString().compareTo(getDestAddress()) == 0) {
                    debug("SendParams", "send address identified:" + _p2sh.toString());
                    debug("SendParams", "send address output index:" + i);
                    ret.add(i);
                }
                else if(_p2sh == null && _p2pkh != null && _p2pkh.toString().compareTo(getDestAddress()) == 0) {
                    debug("SendParams", "send address identified:" + _p2pkh.toString());
                    debug("SendParams", "send address output index:" + i);
                    ret.add(i);
                }
                else  {
                    ;
                }
            } catch (Exception e) {
                ;
            }

        }

        return ret;
    }

}
