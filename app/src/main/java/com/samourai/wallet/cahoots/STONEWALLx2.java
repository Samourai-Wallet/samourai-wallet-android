package com.samourai.wallet.cahoots;

import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.cahoots.psbt.PSBTEntry;
import com.samourai.wallet.segwit.SegwitAddress;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class STONEWALLx2 extends Cahoots {

    private STONEWALLx2()    { ; }

    public STONEWALLx2(STONEWALLx2 stonewallx2)    {
        super(stonewallx2);
    }

    public STONEWALLx2(long spendAmount, NetworkParameters params)    {
        this.ts = System.currentTimeMillis() / 1000L;
        this.strID = Hex.toHexString(Sha256Hash.hash(BigInteger.valueOf(new SecureRandom().nextLong()).toByteArray()));
        this.type = Cahoots.CAHOOTS_STONEWALLx2;
        this.step = 0;
        this.spendAmount = spendAmount;
        this.params = params;
    }

    public boolean inc(HashMap<_TransactionOutPoint, Triple<byte[],byte[],String>> inputs, HashMap<_TransactionOutput,Triple<byte[],byte[],String>> outputs, HashMap<String,ECKey> keyBag) throws Exception    {

        switch(this.getStep())    {
            case 0:
                return doStep1(inputs, outputs);
            case 1:
                return doStep2(inputs, outputs);
            case 2:
                return doStep3(keyBag);
            case 3:
                return doStep4(keyBag);
            default:
                // error
                return false;
        }
    }

    public boolean doStep1(HashMap<_TransactionOutPoint,Triple<byte[],byte[],String>> inputs, HashMap<_TransactionOutput,Triple<byte[],byte[],String>> outputs) throws Exception    {

//        this.transaction = transaction;
        this.psbt = psbt;

        this.setStep(1);

        return true;
    }

    public boolean doStep2(HashMap<_TransactionOutPoint,Triple<byte[],byte[],String>> inputs, HashMap<_TransactionOutput,Triple<byte[],byte[],String>> outputs) throws Exception    {

        this.setStep(2);

        return true;
    }

    public boolean doStep3(HashMap<String,ECKey> keyBag)    {

        //        signTx(keyBag);

        this.setStep(3);

        return true;
    }

    public boolean doStep4(HashMap<String,ECKey> keyBag)    {

        //        signTx(keyBag);

        this.setStep(4);

        return true;
    }

}
