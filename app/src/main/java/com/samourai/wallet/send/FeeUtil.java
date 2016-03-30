package com.samourai.wallet.send;

import android.content.Context;
//import android.util.Log;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bitcoinj.core.Transaction;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

public class FeeUtil {

    private static final int ESTIMATED_INPUT_LEN = 148; // compressed key
    private static final int ESTIMATED_OUTPUT_LEN = 34;

    private static BigInteger bAvgFee = null;
    private static BigInteger bPriorityFee = null;  // recommended priority fee
    private static double dPriorityMultiplier = 1.5;
    private static int totalBytes = -1;

    private static Context context = null;

    private static FeeUtil instance = null;

    private FeeUtil()    { ; }

    public static FeeUtil getInstance()  {

        bAvgFee = BigInteger.valueOf(10000L);
        bPriorityFee = calcPriority();

        if(instance == null)    {
            instance = new FeeUtil();
        }

        return instance;
    }

    public static FeeUtil getInstance(Context ctx)  {

        context = ctx;

        bAvgFee = BigInteger.valueOf(10000L);
        bPriorityFee = calcPriority();

        if(instance == null)    {
            instance = new FeeUtil();
        }

        return instance;
    }

    public BigInteger calculatedFee(Transaction tx)   {

        String hexString = new String(Hex.encode(tx.bitcoinSerialize()));
        int size = hexString.length();

        return feeCalculation(size);
    }

    public BigInteger estimatedFee(int inputs, int outputs)   {

        int size = estimatedSize(inputs, outputs);

        return feeCalculation(size);
    }

    public BigInteger getPriorityFee() {
        return bPriorityFee;
    }

    public void update()    {

        try {
            String response = WebUtil.getInstance(context).getURL(WebUtil.BTCX_FEE_URL);
            averageFee(response);
            totalBytes(response);
        }
        catch(Exception e) {
            ;
        }

    }

    public boolean isStressed()   {
        return (totalBytes > 15000000 && bAvgFee.compareTo(BigInteger.valueOf(30000L)) >= 0);
    }

    public BigInteger getStressFee()   {
        return bAvgFee;
    }

    private void averageFee(String s) {

        double avg_fee = -1.0;

        try {
            Pattern pattern = Pattern.compile("Average fee for payers: ([.\\d]+)");
            Matcher matcher = pattern.matcher(s);
            if(matcher.find())  {
                String value = matcher.group(1);

                NumberFormat nf = NumberFormat.getInstance(Locale.US);
                nf.setMaximumFractionDigits(8);

                avg_fee = nf.parse(value.trim()).doubleValue() / 1000;
                bAvgFee = BigInteger.valueOf((long)(Math.round(avg_fee * 1e8)));
                bPriorityFee = calcPriority();
            }

        }
        catch(Exception e) {
            ;
        }

    }

    private void totalBytes(String s) {

        try {
            Pattern pattern = Pattern.compile("Total bytes: ([\\d]+)");
            Matcher matcher = pattern.matcher(s);
            if(matcher.find())  {
                String value = matcher.group(1);
                totalBytes = Integer.parseInt(value);
            }

        }
        catch(Exception e) {
            ;
        }

    }

    private static BigInteger calcPriority()   {
        return BigInteger.valueOf((long)Math.round(bAvgFee.doubleValue() * dPriorityMultiplier));
    }

    private BigInteger feeCalculation(int size)   {

        int thousands = size / 1000;
        int remainder = size % 1000;

        long fee = SamouraiWallet.bFee.longValue() * thousands;
        if(remainder > 0L)   {
            fee += SamouraiWallet.bFee.longValue();
        }

        return BigInteger.valueOf(fee);
    }

    private int estimatedSize(int inputs, int outputs)   {
        return (outputs * ESTIMATED_OUTPUT_LEN) + (inputs * ESTIMATED_INPUT_LEN) + inputs;
    }

}
