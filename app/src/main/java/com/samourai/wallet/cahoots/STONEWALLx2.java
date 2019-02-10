package com.samourai.wallet.cahoots;

import com.samourai.wallet.bip69.BIP69InputComparator;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

import android.util.Log;

public class STONEWALLx2 extends Cahoots {

    private STONEWALLx2()    { ; }

    public STONEWALLx2(STONEWALLx2 stonewall)    {
        super(stonewall);
    }

    public STONEWALLx2(JSONObject obj)    {
        this.fromJSON(obj);
    }

    public STONEWALLx2(long spendAmount, String address, NetworkParameters params)    {
        this.ts = System.currentTimeMillis() / 1000L;
        this.strID = Hex.toHexString(Sha256Hash.hash(BigInteger.valueOf(new SecureRandom().nextLong()).toByteArray()));
        this.type = Cahoots.CAHOOTS_STONEWALLx2;
        this.step = 0;
        this.spendAmount = spendAmount;
        this.outpoints = new HashMap<String, Long>();
        this.strDestination = address;
        this.params = params;
    }

    public STONEWALLx2(long spendAmount, String address, NetworkParameters params, String strPayNymInit, String strPayNymCollab)    {
        this.ts = System.currentTimeMillis() / 1000L;
        this.strID = Hex.toHexString(Sha256Hash.hash(BigInteger.valueOf(new SecureRandom().nextLong()).toByteArray()));
        this.type = Cahoots.CAHOOTS_STONEWALLx2;
        this.step = 0;
        this.spendAmount = spendAmount;
        this.outpoints = new HashMap<String, Long>();
        this.strDestination = address;
        this.strPayNymInit = strPayNymInit;
        this.strPayNymCollab = strPayNymCollab;
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

    protected boolean doStep1(HashMap<_TransactionOutPoint,Triple<byte[],byte[],String>> inputs, HashMap<_TransactionOutput,Triple<byte[],byte[],String>> outputs) throws Exception    {

        if(this.getStep() != 0 || this.getSpendAmount() == 0L)   {
            return false;
        }
        if(this.getType() == Cahoots.CAHOOTS_STONEWALLx2 && outputs == null)    {
            return false;
        }

        Transaction transaction = new Transaction(params);
        for(_TransactionOutPoint outpoint : inputs.keySet())   {
            TransactionInput input = new TransactionInput(params, null, new byte[0], outpoint, outpoint.getValue());
            transaction.addInput(input);
            outpoints.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getValue().longValue());
        }
        for(_TransactionOutput output : outputs.keySet())   {
            transaction.addOutput(output);
        }

        PSBT psbt = new PSBT(transaction);
        for(_TransactionOutPoint outpoint : inputs.keySet())   {
            Triple triple = inputs.get(outpoint);
            // input type 1
            SegwitAddress segwitAddress = new SegwitAddress((byte[])triple.getLeft(), params);
            psbt.addInput(PSBT.PSBT_IN_WITNESS_UTXO, null, PSBT.writeSegwitInputUTXO(outpoint.getValue().longValue(), segwitAddress.segWitRedeemScript().getProgram()));
            // input type 6
            String[] s = ((String)triple.getRight()).split("/");
            psbt.addInput(PSBT.PSBT_IN_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, 0, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }
        for(_TransactionOutput output : outputs.keySet())   {
            Triple triple = outputs.get(output);
            // output type 2
            String[] s = ((String)triple.getRight()).split("/");
            psbt.addOutput(PSBT.PSBT_OUT_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, 0, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }

        this.psbt = psbt;

//        Log.d("STONEWALLx2", "input value:" + psbt.getTransaction().getInputs().get(0).getValue().longValue());

        this.setStep(1);

        return true;
    }

    protected boolean doStep2(HashMap<_TransactionOutPoint,Triple<byte[],byte[],String>> inputs, HashMap<_TransactionOutput,Triple<byte[],byte[],String>> outputs) throws Exception    {

        Transaction transaction = psbt.getTransaction();
        Log.d("STONEWALLx2", "step2 tx:" + transaction.toString());
        Log.d("STONEWALLx2", "step2 tx:" + Hex.toHexString(transaction.bitcoinSerialize()));

        for(_TransactionOutPoint outpoint : inputs.keySet())   {
            Log.d("STONEWALLx2", "outpoint value:" + outpoint.getValue().longValue());
            TransactionInput input = new TransactionInput(params, null, new byte[0], outpoint, outpoint.getValue());
            transaction.addInput(input);
            outpoints.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getValue().longValue());
        }
        for(_TransactionOutput output : outputs.keySet())   {
            transaction.addOutput(output);
        }

        Pair<Byte, byte[]> pair = Bech32Segwit.decode(params instanceof TestNet3Params ? "tb" : "bc", strDestination);
        byte[] scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        TransactionOutput _output = new TransactionOutput(params, null, Coin.valueOf(spendAmount), scriptPubKey);
        transaction.addOutput(_output);

        for(_TransactionOutPoint outpoint : inputs.keySet())   {
            Triple triple = inputs.get(outpoint);
            // input type 1
            SegwitAddress segwitAddress = new SegwitAddress((byte[])triple.getLeft(), params);
            psbt.addInput(PSBT.PSBT_IN_WITNESS_UTXO, null, PSBT.writeSegwitInputUTXO(outpoint.getValue().longValue(), segwitAddress.segWitRedeemScript().getProgram()));
            // input type 6
            String[] s = ((String)triple.getRight()).split("/");
            psbt.addInput(PSBT.PSBT_IN_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, 0, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }
        for(_TransactionOutput output : outputs.keySet())   {
            Triple triple = outputs.get(output);
            // output type 2
            String[] s = ((String)triple.getRight()).split("/");
            psbt.addOutput(PSBT.PSBT_OUT_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, 0, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }

        psbt.setTransaction(transaction);

        this.setStep(2);

        return true;
    }

    protected boolean doStep3(HashMap<String,ECKey> keyBag)    {

        Transaction transaction = this.getTransaction();
        List<TransactionInput> inputs = new ArrayList<TransactionInput>();
        inputs.addAll(transaction.getInputs());
        Collections.sort(inputs, new BIP69InputComparator());
        transaction.clearInputs();
        for(TransactionInput input : inputs)    {
            transaction.addInput(input);
        }
        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        outputs.addAll(transaction.getOutputs());
        Collections.sort(outputs, new BIP69OutputComparator());
        transaction.clearOutputs();
        for(TransactionOutput output : outputs)    {
            transaction.addOutput(output);
        }

        psbt.setTransaction(transaction);

        signTx(keyBag);

        this.setStep(3);

        return true;
    }

    protected boolean doStep4(HashMap<String,ECKey> keyBag)    {

        signTx(keyBag);

        this.setStep(4);

        return true;
    }

}