package com.samourai.wallet.send;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;

public class ChangeMaker {

    public final static int CHANGE_NORMAL = 0;
    public final static int CHANGE_SAMOURAI = 1;
    public final static int CHANGE_AGGRESSIVE = 2;

    public static final int DEFAULT_DECOYS = 2;

    private BigInteger spendAmount = BigInteger.ZERO;
    private BigInteger changeAmount = BigInteger.ZERO;
    private List<TransactionOutput> change_outputs = null;
    private boolean madeChange = false;
    private int changeIdx = 0;
    private int maxAddr = DEFAULT_DECOYS;
    private int accountIdx = 0;
    private boolean isAggressive = false;

    private Context context = null;

    private BigInteger bFloor = BigInteger.valueOf((long)(0.01 * 1e8));

    private ChangeMaker() { ; }

    public ChangeMaker(Context ctx, int accountIdx, BigInteger spendAmount, BigInteger changeAmount, int idx) {
        this.context = ctx;
        this.accountIdx = accountIdx;
        this.spendAmount = spendAmount;
        this.changeAmount = changeAmount;
        this.changeIdx = idx;
        this.change_outputs = new ArrayList<TransactionOutput>();
    }

    public void makeChange() {
//        Log.i("ChangeMaker", "Total change:" + changeAmount.toString());
        if(changeAmount.compareTo(BigInteger.ZERO) == 0) {
            madeChange = false;
//            Log.i("ChangeMaker", "no change");
        }
        else if(changeAmount.compareTo(SamouraiWallet.bDust) == -1) {
            madeChange = false;
//            Log.i("ChangeMaker", "no change");
        }
        else if(changeAmount.compareTo(bFloor) < 0) {
            makeOutput(0, changeAmount);
            madeChange = true;
//            Log.i("ChangeMaker", "1 output:" + amount.toString());
        }
        // simulate CoinJoin: at least 10 inputs and .1 BTC in change
        else if(isAggressive && changeAmount.compareTo(BigInteger.valueOf(10000000L)) > 0) {

            //
            // change amount must > 0.1
            // outputs will never be < 0.00666666
            //

            System.out.println("Change amount:" + changeAmount.toString());

            long baseAmount = 10000000L;

            SecureRandom random = new SecureRandom();
            int maxAddr = 10 + random.nextInt(6);
            System.out.println("Max. addresses:" + maxAddr);

            BigInteger remainder = changeAmount;
            BigInteger pocketChange = changeAmount.subtract(BigInteger.valueOf(baseAmount));

            BigInteger runningTotal = BigInteger.ZERO;

            for(int i = 0; i < maxAddr; i++) {
                if(i == (maxAddr - 1)) {
                    makeOutput(i, remainder);
                    System.out.println("Change output (remainder):" + remainder.toString());
                    runningTotal = runningTotal.add(remainder);
                }
                else    {
                    BigInteger subAmount = BigInteger.valueOf(baseAmount / maxAddr);

                    long val = nextLong(random, pocketChange.longValue());
                    BigInteger randomAmount = BigInteger.valueOf(val);

                    subAmount = subAmount.add(randomAmount);
                    pocketChange = pocketChange.subtract(randomAmount);
                    remainder = remainder.subtract(subAmount);

                    makeOutput(i, subAmount);
                    System.out.println("Change output:" + subAmount.toString());
                    runningTotal = runningTotal.add(subAmount);
                }

            }

            System.out.println("Running total:" + runningTotal.toString());

            madeChange = true;

        }
        else {

            BigInteger remainder = changeAmount;
            for(int i = 0; i < maxAddr; i++) {

                if(i == (maxAddr - 1)) {
                    makeOutput(i, remainder);
//                    Log.i("ChangeMaker", "Output " + i + ":" + remainder.toString());
                }
                else {
                    BigInteger subAmount = BigInteger.ZERO;
                    if(remainder.compareTo(spendAmount.add(bFloor)) > 0) {
                        subAmount = spendAmount;
                    }
                    else {
                        subAmount = remainder.divide(BigInteger.valueOf(2L + i));

                        if(subAmount.compareTo(BigInteger.valueOf((long)(1.5 * 1e8))) > 0) {
                            subAmount = BigInteger.valueOf((long)(1.5 * 1e8));
                        }
                        else if(subAmount.compareTo(BigInteger.valueOf((long)(1.0 * 1e8))) > 0) {
                            subAmount = BigInteger.valueOf((long)(1.0 * 1e8));
                        }
                        else if(subAmount.compareTo(BigInteger.valueOf((long)(0.5 * 1e8))) > 0) {
                            subAmount = BigInteger.valueOf((long)(0.5 * 1e8));
                        }
                        else if(subAmount.compareTo(BigInteger.valueOf((long)(0.25 * 1e8))) > 0) {
                            subAmount = BigInteger.valueOf((long)(0.25 * 1e8));
                        }
                        else {
                            ;
                        }
                    }
                    remainder = remainder.subtract(subAmount);
                    makeOutput(i, subAmount);
//                    Log.i("ChangeMaker", "Output " + i + ":" + subAmount.toString());
                }

            }

            madeChange = true;

        }
    }

    public List<TransactionOutput> getOutputs() {
        return change_outputs;
    }

    public boolean madeChange() {
        return madeChange;
    }

    public int getMaxAddr() {
        return maxAddr;
    }

    public void setMaxAddr(int maxAddr) {
        this.maxAddr = maxAddr;
    }

    public boolean isAggressive() {
        return isAggressive;
    }

    public void setAggressive(boolean isAggressive) {
        this.isAggressive = isAggressive;
    }

    public void addDecoys(int extra) {
        maxAddr += extra;
    }

    private void makeOutput(int offset, BigInteger subAmount) {

        try {
            HD_Address cAddr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChange().getAddressAt(changeIdx + offset);
            String changeAddr = cAddr.getAddressString();

            BitcoinScript change_script;
            if(changeAddr != null) {
                change_script = BitcoinScript.createSimpleOutBitcoinScript(new BitcoinAddress(changeAddr));
            }
            else {
                throw new Exception(context.getString(R.string.invalid_tx_attempt));
            }
            TransactionOutput output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(subAmount.longValue()), change_script.getProgram());
            change_outputs.add(output);
        }
        catch(Exception e) {
            ;
        }
    }

    private long nextLong(SecureRandom random, long n) {

        // error checking and 2^x checking removed for simplicity.
        long bits, val;

        do {
            bits = (random.nextLong() << 1) >>> 1;
            val = bits % n;
        } while (bits - val + (n - 1) < 0L);

        return val;
    }

}
