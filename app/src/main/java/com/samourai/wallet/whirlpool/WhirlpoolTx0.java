package com.samourai.wallet.whirlpool;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.utxos.models.UTXOCoin;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.samourai.wallet.util.LogUtil.debug;
import static com.samourai.wallet.whirlpool.WhirlpoolMeta.WHIRLPOOL_FEE_RATE_POOL_DENOMINATION;

public class WhirlpoolTx0 {

    private long pool = 0L;
    private List<MyTransactionOutPoint> outpoints = null;
    private long feeSatB = 0L;
    private int premixRequested = 0;
    private Transaction tx0 = null;

    private WhirlpoolTx0()  { ; }

    public WhirlpoolTx0(long pool, List<MyTransactionOutPoint> outpoints, long feeSatB, int premixRequested)   {
        this.pool = pool;
        this.outpoints = outpoints;
        this.feeSatB = feeSatB;
        this.premixRequested = premixRequested;
    }

    public WhirlpoolTx0(long pool, long feeSatB, int premixRequested, List<UTXOCoin> coins)   {
        this.pool = pool;
        this.feeSatB = feeSatB;
        this.premixRequested = premixRequested;
        this.outpoints = new ArrayList<>();
        for(UTXOCoin coin : coins)   {
            outpoints.add(coin.getOutPoint());
        }
    }

    public long getPool() {
        return pool;
    }

    public void setPool(long pool)   {
        this.pool = pool;
    }

    public List<MyTransactionOutPoint> getOutpoints() {
        return outpoints;
    }

    public long getFeeSatB() {
        return feeSatB;
    }

    public void setFeeSatB(long feeSatB) {
        this.feeSatB = feeSatB;
    }

    public int getPremixRequested() {
        if(nbPossiblePremix() < premixRequested || premixRequested == 0)    {
            return nbPossiblePremix();
        }
        else {
            return premixRequested;
        }
    }

    public long getFeeSamourai()    {
        return (long)(getPool() * WHIRLPOOL_FEE_RATE_POOL_DENOMINATION);
    }

    public long getEstimatedBytes()    {

        int nbP2PKH = 0;
        int nbP2SH = 0;
        int nbP2WPKH = 0;

        for(MyTransactionOutPoint outPoint : outpoints)    {
            if(Bech32Util.getInstance().isP2WPKHScript(Hex.toHexString(outPoint.getScriptBytes())))    {
                nbP2WPKH++;
            }
            else    {
                String address = new Script(outPoint.getScriptBytes()).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                    nbP2SH++;
                }
                else    {
                    nbP2PKH++;
                }
            }

        }

        return FeeUtil.getInstance().estimatedSizeSegwit(nbP2PKH, nbP2SH, nbP2WPKH, 0, 1);
    }

    public long getChange() {
        return getAmountSelected() - ((getPremixRequested() * getPremixAmount()) + getFeeSamourai() + getFee());
    }

    public long getFee() {
        return getFeeSatB() * getEstimatedBytes();
    }

    public long getAmountAfterWhirlpoolFee() {
        return getAmountSelected() - getFeeSamourai();
    }

    public long getPremixAmount()   {
        return getPool() + (getFeeSatB() * 102L);
    }

    public long getAmountSelected() {

        long ret = 0L;

        for(MyTransactionOutPoint outpoint : outpoints)   {
            ret += outpoint.getValue().value;
        }

        return ret;
    }

    public int nbPossiblePremix()   {

        int ret = (int)((getAmountSelected() - (long)(getFeeSamourai() + getFee())) / getPool());

        return ret > 0 ? ret : 0;

    }

    //
    // return signed tx0 for params passed to constructor
    //
    public Transaction getTx0() {
        return tx0;
    }

    public void make()  throws Exception {

        tx0 = null;

        debug("WhirlpoolTx0", "make: ");
        //
        // calc fee here using feeSatB and utxos passed
        //
        /*
        if(getChange() < 0L)    {
            debug("WhirlpoolTx0", "Cannot make premix: negative change:" + getAmountSelected());
            throw  new Exception("Cannot make premix: negative change:"+getAmountSelected());
        }
        */
        if(nbPossiblePremix() < 1)    {
            debug("WhirlpoolTx0", "Cannot make premix: insufficient selected amount:" + getAmountSelected());
            throw  new Exception("Cannot make premix: insufficient selected amount:"+getAmountSelected());
        }

        debug("WhirlpoolTx0", "amount selected:" + getAmountSelected() / 1e8);
        debug("WhirlpoolTx0", "amount requested:" + ((getPremixRequested() * getPool())  / 1e8));
        debug("WhirlpoolTx0", "nb premix possible:" + nbPossiblePremix());
        debug("WhirlpoolTx0", "amount after Whirlpool fee:" + getAmountAfterWhirlpoolFee() / 1e8);
        debug("WhirlpoolTx0", "fee samourai:" + new DecimalFormat("0.########").format(getFeeSamourai() / 1e8));
        debug("WhirlpoolTx0", "fee miners:" + new DecimalFormat("0.########").format(getFee() / 1e8));
        debug("WhirlpoolTx0", "change amount:" + getChange() / 1e8);

        // [WIP] stub;
        tx0 = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams() instanceof TestNet3Params ? TestNet3Params.get() : MainNetParams.get());

    }

}
