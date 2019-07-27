package com.samourai.wallet.cahoots;

import android.util.Log;

import com.samourai.wallet.bip69.BIP69InputComparator;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.cahoots.psbt.PSBTEntry;
import com.samourai.wallet.segwit.SegwitAddress;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public class Stowaway extends Cahoots {

    private Stowaway()    { ; }

    public Stowaway(Stowaway stowaway)    {
        super(stowaway);
    }

    public Stowaway(JSONObject obj)    {
        this.fromJSON(obj);
    }

    public Stowaway(long spendAmount, NetworkParameters params, int account)    {
        this.ts = System.currentTimeMillis() / 1000L;
        this.strID = Hex.toHexString(Sha256Hash.hash(BigInteger.valueOf(new SecureRandom().nextLong()).toByteArray()));
        this.type = Cahoots.CAHOOTS_STOWAWAY;
        this.step = 0;
        this.spendAmount = spendAmount;
        this.outpoints = new HashMap<String, Long>();
        this.params = params;
        this.account = account;
    }

    public Stowaway(long spendAmount, NetworkParameters params, String strPayNymInit, String strPayNymCollab, int account)    {
        this.ts = System.currentTimeMillis() / 1000L;
        this.strID = Hex.toHexString(Sha256Hash.hash(BigInteger.valueOf(new SecureRandom().nextLong()).toByteArray()));
        this.type = Cahoots.CAHOOTS_STOWAWAY;
        this.step = 0;
        this.spendAmount = spendAmount;
        this.outpoints = new HashMap<String, Long>();
        this.strPayNymInit = strPayNymInit;
        this.strPayNymCollab = strPayNymCollab;
        this.params = params;
        this.account = account;
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

    //
    // receiver
    //
    public boolean doStep1(HashMap<_TransactionOutPoint,Triple<byte[],byte[],String>> inputs, HashMap<_TransactionOutput,Triple<byte[],byte[],String>> outputs) throws Exception    {

        if(this.getStep() != 0 || this.getSpendAmount() == 0L)   {
            return false;
        }
        if(this.getType() == Cahoots.CAHOOTS_STONEWALLx2 && outputs == null)    {
            return false;
        }

        Transaction transaction = new Transaction(params);
        for(_TransactionOutPoint outpoint : inputs.keySet())   {
            TransactionInput input = new TransactionInput(params, null, new byte[0], outpoint, outpoint.getValue());
            Log.d("Stowaway", "input value:" + input.getValue().longValue());
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
            psbt.addInput(PSBT.PSBT_IN_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, account, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }
        for(_TransactionOutput output : outputs.keySet())   {
            Triple triple = outputs.get(output);
            // output type 2
            String[] s = ((String)triple.getRight()).split("/");
            psbt.addOutput(PSBT.PSBT_OUT_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, account, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }

        this.psbt = psbt;

        Log.d("Stowaway", "input value:" + psbt.getTransaction().getInputs().get(0).getValue().longValue());

        this.setStep(1);

        return true;
    }

    //
    // sender
    //
    public boolean doStep2(HashMap<_TransactionOutPoint,Triple<byte[],byte[],String>> inputs, HashMap<_TransactionOutput,Triple<byte[],byte[],String>> outputs) throws Exception    {

        Transaction transaction = psbt.getTransaction();
        Log.d("Stowaway", "step2 tx:" + transaction.toString());
        Log.d("Stowaway", "step2 tx:" + Hex.toHexString(transaction.bitcoinSerialize()));

        // tx: modify spend output
        long contributedAmount = 0L;
        /*
        for(TransactionInput input : transaction.getInputs())   {
//            Log.d("Stowaway", input.getOutpoint().toString());
            contributedAmount += input.getOutpoint().getValue().longValue();
        }
        */
        for(String outpoint : outpoints.keySet())   {
            contributedAmount += outpoints.get(outpoint);
        }
        long outputAmount = transaction.getOutput(0).getValue().longValue();
        TransactionOutput spendOutput = transaction.getOutput(0);
        spendOutput.setValue(Coin.valueOf(outputAmount + contributedAmount));
        transaction.clearOutputs();
        transaction.addOutput(spendOutput);

        for(_TransactionOutPoint outpoint : inputs.keySet())   {
            Log.d("Stowaway", "outpoint value:" + outpoint.getValue().longValue());
            TransactionInput input = new TransactionInput(params, null, new byte[0], outpoint, outpoint.getValue());
            transaction.addInput(input);
            outpoints.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getValue().longValue());
        }
        for(_TransactionOutput output : outputs.keySet())   {
            transaction.addOutput(output);
        }

        // psbt: modify spend output
        List<PSBTEntry> updatedEntries = new ArrayList<PSBTEntry>();
        for(PSBTEntry entry : psbt.getPsbtInputs())   {
            if(entry.getKeyType()[0] == PSBT.PSBT_IN_WITNESS_UTXO)    {
                byte[] data = entry.getData();
                byte[] scriptPubKey = new byte[data.length - Long.BYTES];
                System.arraycopy(data, Long.BYTES, scriptPubKey, 0, scriptPubKey.length);
                entry.setData(PSBT.writeSegwitInputUTXO(outputAmount + contributedAmount, scriptPubKey));
            }
            updatedEntries.add(entry);
        }
        psbt.setPsbtInputs(updatedEntries);

        for(_TransactionOutPoint outpoint : inputs.keySet())   {
            Triple triple = inputs.get(outpoint);
            // input type 1
            SegwitAddress segwitAddress = new SegwitAddress((byte[])triple.getLeft(), params);
            psbt.addInput(PSBT.PSBT_IN_WITNESS_UTXO, null, PSBT.writeSegwitInputUTXO(outpoint.getValue().longValue(), segwitAddress.segWitRedeemScript().getProgram()));
            // input type 6
            String[] s = ((String)triple.getRight()).split("/");
            psbt.addInput(PSBT.PSBT_IN_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, account, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }
        for(_TransactionOutput output : outputs.keySet())   {
            Triple triple = outputs.get(output);
            // output type 2
            String[] s = ((String)triple.getRight()).split("/");
            psbt.addOutput(PSBT.PSBT_OUT_BIP32_DERIVATION, (byte[])triple.getLeft(), PSBT.writeBIP32Derivation((byte[])triple.getMiddle(), 84, params instanceof TestNet3Params ? 1 : 0, account, Integer.valueOf(s[1]), Integer.valueOf(s[2])));
        }

        psbt.setTransaction(transaction);

        this.setStep(2);

        return true;
    }

    //
    // receiver
    //
    public boolean doStep3(HashMap<String,ECKey> keyBag)    {

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

    //
    // sender
    //
    public boolean doStep4(HashMap<String,ECKey> keyBag)    {

        signTx(keyBag);

        this.setStep(4);

        return true;
    }

}
