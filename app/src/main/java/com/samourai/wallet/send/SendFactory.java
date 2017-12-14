package com.samourai.wallet.send;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
//import android.util.Log;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.PrivKeyReader;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.R;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;

public class SendFactory	{

    private static SendFactory instance = null;
    private static Context context = null;

    private SendFactory () { ; }

    public static SendFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null)	{
            instance = new SendFactory();
        }

        return instance;
    }

    public Transaction makeTransaction(final int accountIdx, final List<MyTransactionOutPoint> unspent, final HashMap<String, BigInteger> receivers) {

        Transaction tx = null;

        try {
            int changeIdx = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChange().getAddrIdx();
            tx = makeTransaction(accountIdx, receivers, unspent);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return tx;
    }

    public Transaction signTransaction(Transaction unsignedTx)    {

        HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();

        for (TransactionInput input : unsignedTx.getInputs()) {

            try {
                byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
                String address = new Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
//                Log.i("address from script", address);
                ECKey ecKey = null;
                ecKey = getPrivKey(address);
                if(ecKey != null) {
                    keyBag.put(input.getOutpoint().toString(), ecKey);
                }
                else {
                    throw new RuntimeException("ECKey error: cannot process private key");
//                    Log.i("ECKey error", "cannot process private key");
                }
            }
            catch(ScriptException se) {
                ;
            }
            catch(Exception e) {
                ;
            }

        }

        Transaction signedTx = signTransaction(unsignedTx, keyBag);
        if(signedTx == null)    {
            return null;
        }
        else    {
            String hexString = new String(Hex.encode(signedTx.bitcoinSerialize()));
            if(hexString.length() > (100 * 1024)) {
                Toast.makeText(context, R.string.tx_length_error, Toast.LENGTH_SHORT).show();
//              Log.i("SendFactory", "Transaction length too long");
            }

            return signedTx;
        }
    }

    public Transaction signTransactionForSweep(Transaction unsignedTx, PrivKeyReader privKeyReader)    {

        HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();

        for (TransactionInput input : unsignedTx.getInputs()) {

            try {
                byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
//                String address = new BitcoinScript(scriptBytes).getAddress().toString();
                String address = new Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
//                        Log.i("address from script", address);
                ECKey ecKey = null;
                try {
                    DumpedPrivateKey pk = new DumpedPrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), privKeyReader.getKey().getPrivateKeyAsWiF(SamouraiWallet.getInstance().getCurrentNetworkParams()));
                    ecKey = pk.getKey();
//                    Log.i("SendFactory", "ECKey address:" + ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                } catch (AddressFormatException afe) {
                    afe.printStackTrace();
                    continue;
                }

                if(ecKey != null) {
                    keyBag.put(input.getOutpoint().toString(), ecKey);
                }
                else {
                    Toast.makeText(context, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
//                    Log.i("ECKey error", "cannot process private key");
                }
            }
            catch(ScriptException se) {
                ;
            }
            catch(Exception e) {
                ;
            }

        }

        Transaction signedTx = signTransaction(unsignedTx, keyBag);
        if(signedTx == null)    {
            return null;
        }
        else    {
            String hexString = new String(Hex.encode(signedTx.bitcoinSerialize()));
            if(hexString.length() > (100 * 1024)) {
                Toast.makeText(context, R.string.tx_length_error, Toast.LENGTH_SHORT).show();
//              Log.i("SendFactory", "Transaction length too long");
            }

            return signedTx;
        }
    }

    /*
    Used by spends
     */
    private Transaction makeTransaction(int accountIdx, HashMap<String, BigInteger> receivers, List<MyTransactionOutPoint> unspent) throws Exception {

        BigInteger amount = BigInteger.ZERO;
        for(Iterator<Map.Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, BigInteger> mapEntry = iterator.next();
            amount = amount.add(mapEntry.getValue());
        }

        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams());

        for(Iterator<Map.Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, BigInteger> mapEntry = iterator.next();
            String toAddress = mapEntry.getKey();
            BigInteger value = mapEntry.getValue();
/*
            if(value.compareTo(SamouraiWallet.bDust) < 1)    {
                throw new Exception(context.getString(R.string.dust_amount));
            }
*/
            if(value == null || (value.compareTo(BigInteger.ZERO) <= 0 && !FormatsUtil.getInstance().isValidBIP47OpReturn(toAddress))) {
                throw new Exception(context.getString(R.string.invalid_amount));
            }

            TransactionOutput output = null;
            Script toOutputScript = null;
            if(!FormatsUtil.getInstance().isValidBitcoinAddress(toAddress) && FormatsUtil.getInstance().isValidBIP47OpReturn(toAddress))    {
                toOutputScript = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(Hex.decode(toAddress)).build();
                output = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(0L), toOutputScript.getProgram());
            }
            else if(toAddress.toLowerCase().startsWith("tb") || toAddress.toLowerCase().startsWith("bc"))   {

                byte[] scriptPubKey = null;

                try {
                    Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", toAddress);
                    scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
                }
                catch(Exception e) {
                    return null;
                }
                output = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(value.longValue()), scriptPubKey);
            }
            else    {
                toOutputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), toAddress));
                output = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(value.longValue()), toOutputScript.getProgram());
            }

            outputs.add(output);
        }

        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();
        for(MyTransactionOutPoint outPoint : unspent) {
            Script script = new Script(outPoint.getScriptBytes());

            if(script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], outPoint, outPoint.getTxHash().toString(), outPoint.getTxOutputN());
            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false) == true)    {
                input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_NO);
            }
            inputs.add(input);
        }

        //
        // deterministically sort inputs and outputs, see BIP69 (OBPP)
        //
        Collections.sort(inputs, new BIP69InputComparator());
        for(TransactionInput input : inputs) {
            tx.addInput(input);
        }

        Collections.sort(outputs, new BIP69OutputComparator());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        return tx;
    }

    private synchronized Transaction signTransaction(Transaction transaction, HashMap<String,ECKey> keyBag) throws ScriptException {

        List<TransactionInput> inputs = transaction.getInputs();

        TransactionInput input = null;
        TransactionOutput connectedOutput = null;
        byte[] connectedPubKeyScript = null;
        TransactionSignature sig = null;
        Script scriptPubKey = null;
        ECKey key = null;

        for (int i = 0; i < inputs.size(); i++) {

            input = inputs.get(i);

            key = keyBag.get(input.getOutpoint().toString());
            connectedPubKeyScript = input.getOutpoint().getConnectedPubKeyScript();
            connectedOutput = input.getOutpoint().getConnectedOutput();
            scriptPubKey = connectedOutput.getScriptPubKey();

            String address = new Script(connectedPubKeyScript).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
            if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {

                final SegwitAddress p2shp2wpkh = new SegwitAddress(key.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                System.out.println("pubKey:" + Hex.toHexString(key.getPubKey()));
//                final Script scriptPubKey = p2shp2wpkh.segWitOutputScript();
//                System.out.println("scriptPubKey:" + Hex.toHexString(scriptPubKey.getProgram()));
                System.out.println("to address from script:" + scriptPubKey.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                final Script redeemScript = p2shp2wpkh.segWitRedeemScript();
                System.out.println("redeem script:" + Hex.toHexString(redeemScript.getProgram()));
                final Script scriptCode = redeemScript.scriptCode();
                System.out.println("script code:" + Hex.toHexString(scriptCode.getProgram()));

                sig = transaction.calculateWitnessSignature(i, key, scriptCode, connectedOutput.getValue(), Transaction.SigHash.ALL, false);
                final TransactionWitness witness = new TransactionWitness(2);
                witness.setPush(0, sig.encodeToBitcoin());
                witness.setPush(1, key.getPubKey());
                transaction.setWitness(i, witness);

                final ScriptBuilder sigScript = new ScriptBuilder();
                sigScript.data(redeemScript.getProgram());
                transaction.getInput(i).setScriptSig(sigScript.build());

                transaction.getInput(i).getScriptSig().correctlySpends(transaction, i, scriptPubKey, connectedOutput.getValue(), Script.ALL_VERIFY_FLAGS);

            }
            else    {
                if(key != null && key.hasPrivKey() || key.isEncrypted()) {
                    sig = transaction.calculateSignature(i, key, connectedPubKeyScript, Transaction.SigHash.ALL, false);
                }
                else {
                    sig = TransactionSignature.dummy();   // watch only ?
                }

                if(scriptPubKey.isSentToAddress()) {
                    input.setScriptSig(ScriptBuilder.createInputScript(sig, key));
                }
                else if(scriptPubKey.isSentToRawPubKey()) {
                    input.setScriptSig(ScriptBuilder.createInputScript(sig));
                }
                else {
                    throw new RuntimeException("Unknown script type: " + scriptPubKey);
                }
            }

        }

        return transaction;

    }

    //
    // BIP126 hetero
    //
    public Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> heterogeneous(List<UTXO> outputs, BigInteger spendAmount, String address) {

        boolean isSegwit = FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress();

        Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> ret = Pair.of(new ArrayList<MyTransactionOutPoint>(), new ArrayList<TransactionOutput>());

        long check_total = 0L;

        long total_wallet = 0L;
        for(UTXO output : outputs)   {
            total_wallet += output.getValue();
        }
//        System.out.println("total wallet amount:" + total_wallet);
//        System.out.println("spend amount:" + spendAmount.toString());

        if(spendAmount.longValue() > (total_wallet / 2L))    {
//            System.out.println("spend amount larger than 50% of total amount available");
            return null;
        }

        List<MyTransactionOutPoint> selectedOutputs = new ArrayList<MyTransactionOutPoint>();
        BigInteger totalValue = BigInteger.ZERO;
        BigInteger totalAmount = spendAmount;
        int output_scripts = 0;

        BigInteger biFee = BigInteger.ZERO;

        for (UTXO output : outputs) {

            selectedOutputs.addAll(output.getOutpoints());
            output_scripts++;
            totalValue = totalValue.add(BigInteger.valueOf(output.getValue()));

            double pctThreshold = (100.0 / (double)outputs.size()) / 100.0;
            long _pct = (long)(totalValue.doubleValue() * pctThreshold);

            Pair<Integer,Integer> outputTypes = FeeUtil.getInstance().getOutpointCount(selectedOutputs);
            biFee = FeeUtil.getInstance().estimatedFeeSegwit(outputTypes.getLeft(), outputTypes.getRight(), output_scripts);

            if(totalValue.compareTo(totalAmount.add(biFee)) >= 0 && output_scripts >= 3)    {

                if(spendAmount.compareTo(BigInteger.valueOf(_pct)) <= 0) {
//                    System.out.println("spendAmount < _pct, breaking");
                    break;
                }
                else if(totalValue.compareTo(spendAmount.multiply(BigInteger.valueOf(2L))) > 0)   {
//                    System.out.println("totalValue > spendAmount * 2, breaking");
                    break;
                }
                else    {
                    ;
                }

            }

        }

        //
//        System.out.println("total amount (spend + fee):" + totalAmount.toString());
//        System.out.println("total value selected:" + totalValue.toString());
//        BigInteger totalChange = totalValue.subtract(totalAmount).subtract(biFee);
//        System.out.println("total change:" + totalChange.toString());
//        System.out.println("output scripts:" + output_scripts);
//        System.out.println("selected outputs:" + selectedOutputs.size());
//        System.out.println("tx size:" + txSize);
//        System.out.println("fixed fee:" + fixedFee);
//        System.out.println("fee:" + biFee.toString());
        //

        if(totalValue.compareTo(totalAmount) < 0)    {
//            System.out.println("Insufficient funds");
            return null;
        }

        if(output_scripts < 3)    {
//            System.out.println("Need at least 3 output scripts");
            return null;
        }

//        for (MyTransactionOutPoint selected : selectedOutputs) {
//            System.out.println("selected value:" + selected.getValue());
//        }

        ret.getLeft().addAll(selectedOutputs);

        check_total += biFee.longValue();

        double pctThreshold = (100.0 / (double)output_scripts) / 100.0;
        long _pct = (long)(totalValue.doubleValue() * pctThreshold);

        TransactionOutput txOut1 = null;
        TransactionOutput txOut2 = null;
        TransactionOutput txOutSpend = null;
        Script outputScript = null;
        String changeAddress = null;
        BigInteger remainder = totalValue.subtract(biFee);
        int remainingOutputs = output_scripts;
        if(spendAmount.compareTo(BigInteger.valueOf(_pct)) <= 0)    {

//            System.out.println("spend not part of pair");

            //
//            System.out.println("pair part:" + _pct);
//            System.out.println("pair part:" + _pct);
//            System.out.println("spend:" + spendAmount.toString());
            //

            try {
                // change address here
                changeAddress = getChangeAddress(isSegwit);
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), changeAddress));
                txOut1 = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(_pct), outputScript.getProgram());
                // change address here
                changeAddress = getChangeAddress(isSegwit);
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), changeAddress));
                txOut2 = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(_pct), outputScript.getProgram());
                // spend address here
                if(address.toLowerCase().startsWith("tb") || address.toLowerCase().startsWith("bc"))   {

                    byte[] scriptPubKey = null;

                    try {
                        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", address);
                        scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
                    }
                    catch(Exception e) {
                        return null;
                    }
                    txOutSpend = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), scriptPubKey);
                }
                else    {
                    outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address));
                    txOutSpend = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
                }
            }
            catch(Exception e)    {
                return null;
            }

            check_total += _pct;
            check_total += _pct;
            check_total += spendAmount.longValue();

            remainder = remainder.subtract(BigInteger.valueOf(_pct));
            remainder = remainder.subtract(BigInteger.valueOf(_pct));
            remainder = remainder.subtract(spendAmount);

            remainingOutputs -= 3;

            if(remainingOutputs < 0)    {
                System.out.println("error");
                return null;
            }
            else if(remainingOutputs == 0)    {
//                System.out.println("updating pair parts");
                long part1 = _pct + remainder.divide(BigInteger.valueOf(2L)).longValue();
                txOut1.setValue(Coin.valueOf(_pct + remainder.divide(BigInteger.valueOf(2L)).longValue()));
                remainder = remainder.subtract(remainder.divide(BigInteger.valueOf(2L)));
                long part2 = _pct + remainder.longValue();
                txOut2.setValue(Coin.valueOf(_pct + remainder.longValue()));
                remainder = remainder.subtract(remainder);
                //
//                System.out.println("part1:" + part1);
//                System.out.println("part2:" + part2);
//                System.out.println("remainder:" + remainder.toString());
                //
                check_total += part1;
                check_total += part2;
                check_total += remainder.longValue();
            }
            else    {
                ;
            }

            if(txOut1.getValue().longValue() < SamouraiWallet.bDust.longValue() || txOut2.getValue().longValue() < SamouraiWallet.bDust.longValue() || txOutSpend.getValue().longValue() < SamouraiWallet.bDust.longValue())    {
                return null;
            }

            ret.getRight().add(txOut1);
            ret.getRight().add(txOut2);
            ret.getRight().add(txOutSpend);

        }
        else    {
//            System.out.println("spend part of pair");

            try {
//                System.out.println("spend:" + spendAmount.toString());
                // spend address here
//                Log.d("SendFactory", address + ":" + org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address));
                // spend address here
                if(address.toLowerCase().startsWith("tb") || address.toLowerCase().startsWith("bc"))   {

                    byte[] scriptPubKey = null;

                    try {
                        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", address);
                        scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
                    }
                    catch(Exception e) {
                        return null;
                    }
                    txOutSpend = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), scriptPubKey);
                }
                else    {
                    outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address));
//                Log.d("SendFactory", address + ":" + outputScript.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()));
                    txOutSpend = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
                }

                //                System.out.println("pair part:" + spendAmount.toString());
                // change address here
                changeAddress = getChangeAddress(isSegwit);
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), changeAddress));
                txOut1 = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
            }
            catch(Exception e) {
                return null;
            }

            check_total += spendAmount.longValue();
            check_total += spendAmount.longValue();

            remainder = remainder.subtract(spendAmount);
            remainder = remainder.subtract(spendAmount);

            remainingOutputs -= 2;  // remainingOutputs >= 1 as we selected minimum 3 outputs above

            if(txOut1.getValue().longValue() < SamouraiWallet.bDust.longValue() || txOutSpend.getValue().longValue() < SamouraiWallet.bDust.longValue())    {
                return null;
            }

            ret.getRight().add(txOut1);
            ret.getRight().add(txOutSpend);

        }

//        System.out.println("remainder for change:" + remainder.toString());

        if(remainingOutputs > 0)    {
            BigInteger remainderPart = remainder.divide(BigInteger.valueOf(remainingOutputs));
            if(remainderPart.compareTo(SamouraiWallet.bDust) < 0)    {
//                System.out.println("dust amounts for change");
                return null;
            }

            for(int i = 0; i < remainingOutputs; i++)   {

                TransactionOutput txChange = null;
                try {
                    if(i == (remainingOutputs - 1))    {
//                    System.out.println("remainder:" + remainder.toString());
                        // change address here
                        changeAddress = getChangeAddress(isSegwit);
                        outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), changeAddress));
                        txChange = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(remainder.longValue()), outputScript.getProgram());

                        check_total += remainder.longValue();
                        remainder = remainder.subtract(remainder);
                    }
                    else    {
//                    System.out.println("remainder:" + remainderPart);
                        // change address here
                        changeAddress = getChangeAddress(isSegwit);
                        outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), changeAddress));
                        txChange = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(remainderPart.longValue()), outputScript.getProgram());

                        check_total += remainderPart.longValue();
                        remainder = remainder.subtract(remainderPart);
                    }
                }
                catch(Exception e) {
                    return null;
                }

                ret.getRight().add(txChange);

            }
        }

//        System.out.println("remainder after processing:" + remainder.toString());
//        System.out.println("output amount processed:" + check_total);

        long inValue = 0L;
        for(MyTransactionOutPoint outpoint : ret.getLeft())   {
            inValue += outpoint.getValue().longValue();
        }
        long outValue = 0L;
        for(TransactionOutput tOut : ret.getRight())   {
            outValue += tOut.getValue().longValue();
        }
        outValue += biFee.longValue();
//        System.out.println("inputs:" + inValue);
//        System.out.println("outputs:" + outValue);

        assert(inValue == outValue);

        return ret;

    }

    //
    // BIP126 alt
    //
    public Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> altHeterogeneous(List<UTXO> outputs, BigInteger spendAmount, String address) {

        boolean isSegwit = FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress();

        Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> ret = Pair.of(new ArrayList<MyTransactionOutPoint>(), new ArrayList<TransactionOutput>());

        long check_total = 0L;

        final int NB_OUTPUTS = 3;

        long total_wallet = 0L;
        for(UTXO output : outputs)   {
            total_wallet += output.getValue();
        }
//        System.out.println("total wallet amount:" + total_wallet);
//        System.out.println("spend amount:" + spendAmount.toString());

        if(spendAmount.longValue() > (total_wallet / 2L))    {
//            System.out.println("spend amount larger than 50% of total amount available");
            return null;
        }

        List<MyTransactionOutPoint> selectedOutputs = new ArrayList<MyTransactionOutPoint>();
        BigInteger totalValue = BigInteger.ZERO;
        BigInteger totalAmount = spendAmount;

        BigInteger biFee = BigInteger.ZERO;

        for (UTXO output : outputs) {

            selectedOutputs.addAll(output.getOutpoints());
            totalValue = totalValue.add(BigInteger.valueOf(output.getValue()));

            Pair<Integer,Integer> outputTypes = FeeUtil.getInstance().getOutpointCount(selectedOutputs);
            biFee = FeeUtil.getInstance().estimatedFeeSegwit(outputTypes.getLeft(), outputTypes.getRight(), NB_OUTPUTS);

            if(totalValue.compareTo(totalAmount.multiply(BigInteger.valueOf(2L)).add(biFee)) >= 0 && selectedOutputs.size() >= 4)    {
                break;
            }

        }

        //
//        System.out.println("total amount (spend + fee):" + totalAmount.add(biFee).toString());
//        System.out.println("total value selected:" + totalValue.toString());
//        BigInteger totalChange = totalValue.subtract(totalAmount).subtract(biFee);
//        System.out.println("total change:" + totalChange.toString());
//        System.out.println("output scripts:" + NB_OUTPUTS);
//        System.out.println("selected outputs:" + selectedOutputs.size());
//        System.out.println("tx size:" + txSize);
//        System.out.println("fixed fee:" + fixedFee);
//        System.out.println("fee:" + biFee.toString());
        //

        if(totalValue.compareTo(totalAmount) < 0)    {
//            System.out.println("Insufficient funds");
            return null;
        }

//        for (MyTransactionOutPoint selected : selectedOutputs) {
//            System.out.println("selected value:" + selected.getValue());
//        }

        ret.getLeft().addAll(selectedOutputs);

        check_total += biFee.longValue();

        BigInteger remainder = totalValue.subtract(biFee);

        TransactionOutput txOut1 = null;
        TransactionOutput txOutSpend = null;
        TransactionOutput txChange = null;
        Script outputScript = null;
        String changeAddress = null;

//        System.out.println("spend:" + spendAmount.toString());
        try {
            // spend address here
            if(address.toLowerCase().startsWith("tb") || address.toLowerCase().startsWith("bc"))   {

                byte[] scriptPubKey = null;

                try {
                    Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", address);
                    scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
                }
                catch(Exception e) {
                    return null;
                }
                txOutSpend = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), scriptPubKey);
            }
            else    {
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address));
                txOutSpend = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
            }

            // change address here
            changeAddress = getChangeAddress(isSegwit);
            outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), changeAddress));
            txOut1 = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
        }
        catch(Exception e) {
            return null;
        }

        check_total += spendAmount.longValue();
        check_total += spendAmount.longValue();
        remainder = remainder.subtract(spendAmount);
        remainder = remainder.subtract(spendAmount);
//        System.out.println("change:" + remainder.toString());
        try {
            // change address here
            changeAddress = getChangeAddress(isSegwit);
            outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), changeAddress));
            txChange = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(remainder.longValue()), outputScript.getProgram());
        }
        catch(Exception e) {
            return null;
        }

        check_total += remainder.longValue();
        remainder = remainder.subtract(remainder);

        if(txOut1.getValue().longValue() < SamouraiWallet.bDust.longValue() || txChange.getValue().longValue() < SamouraiWallet.bDust.longValue() || txOutSpend.getValue().longValue() < SamouraiWallet.bDust.longValue())    {
            return null;
        }

        ret.getRight().add(txOut1);
        ret.getRight().add(txOutSpend);
        ret.getRight().add(txChange);

        //
//        System.out.println("remainder after processing:" + remainder.toString());
//        System.out.println("output amount processed:" + check_total);
        //

        long inValue = 0L;
        for(MyTransactionOutPoint outpoint : ret.getLeft())   {
            inValue += outpoint.getValue().longValue();
        }
        long outValue = 0L;
        for(TransactionOutput tOut : ret.getRight())   {
            outValue += tOut.getValue().longValue();
        }
        outValue += biFee.longValue();
//        System.out.println("inputs:" + inValue);
//        System.out.println("outputs:" + outValue);

        assert(inValue == outValue);

        return ret;

    }

    private String getChangeAddress(boolean isSegwit)    {

        boolean isSegwitChange = isSegwit;

        if(isSegwit || PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false)    {
            isSegwitChange = true;
        }

        if(isSegwitChange)    {
            String change_address = BIP49Util.getInstance(context).getAddressAt(AddressFactory.CHANGE_CHAIN, BIP49Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx()).getAddressAsString();
            BIP49Util.getInstance(context).getWallet().getAccount(0).getChange().incAddrIdx();
            return change_address;
        }
        else    {
            try {
                String change_address = HD_WalletFactory.getInstance(context).get().getAccount(0).getChange().getAddressAt(HD_WalletFactory.getInstance(context).get().getAccount(0).getChange().getAddrIdx()).getAddressString();
                HD_WalletFactory.getInstance(context).get().getAccount(0).getChange().incAddrIdx();
                return change_address;
            }
            catch(IOException ioe) {
                ;
            }
            catch(MnemonicException.MnemonicLengthException mle) {
                ;
            }
        }

        return null;
    }

    public static ECKey getPrivKey(String address)    {

        ECKey ecKey = null;

        try {
            String path = APIFactory.getInstance(context).getUnspentPaths().get(address);
            Log.d("APIFactory", "address:" + path);
            if(path != null)    {
                String[] s = path.split("/");
                if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                    Log.d("APIFactory", "address type:" + "bip49");
                    HD_Address addr = BIP49Util.getInstance(context).getWallet().getAccount(0).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                    ecKey = addr.getECKey();
                }
                else    {
                    Log.d("APIFactory", "address type:" + "bip44");
                    int account_no = APIFactory.getInstance(context).getUnspentAccounts().get(address);
                    HD_Address hd_address = AddressFactory.getInstance(context).get(account_no, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                    String strPrivKey = hd_address.getPrivateKeyString();
                    DumpedPrivateKey pk = new DumpedPrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), strPrivKey);
                    ecKey = pk.getKey();
                }
            }
            else    {
                Log.d("APIFactory", "address type:" + "bip47");
                String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
                int idx = BIP47Meta.getInstance().getIdx4Addr(address);
                PaymentAddress addr = BIP47Util.getInstance(context).getReceiveAddress(new PaymentCode(pcode), idx);
                ecKey = addr.getReceiveECKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return ecKey;
    }

    public static class BIP69InputComparator implements Comparator<MyTransactionInput> {

        public int compare(MyTransactionInput i1, MyTransactionInput i2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            byte[] h1 = Hex.decode(i1.getTxHash());
            byte[] h2 = Hex.decode(i2.getTxHash());

            int pos = 0;
            while(pos < h1.length && pos < h2.length)    {

                byte b1 = h1[pos];
                byte b2 = h2[pos];

                if((b1 & 0xff) < (b2 & 0xff))    {
                    return BEFORE;
                }
                else if((b1 & 0xff) > (b2 & 0xff))    {
                    return AFTER;
                }
                else    {
                    pos++;
                }

            }

            if(i1.getTxPos() < i2.getTxPos())    {
                return BEFORE;
            }
            else if(i1.getTxPos() > i2.getTxPos())    {
                return AFTER;
            }
            else    {
                return EQUAL;
            }

        }

    }

    public static class BIP69OutputComparator implements Comparator<TransactionOutput> {

        public int compare(TransactionOutput o1, TransactionOutput o2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if(o1.getValue().compareTo(o2.getValue()) > 0) {
                return AFTER;
            }
            else if(o1.getValue().compareTo(o2.getValue()) < 0) {
                return BEFORE;
            }
            else    {

                byte[] b1 = o1.getScriptBytes();
                byte[] b2 = o2.getScriptBytes();

                int pos = 0;
                while(pos < b1.length && pos < b2.length)    {

                    if((b1[pos] & 0xff) < (b2[pos] & 0xff))    {
                        return BEFORE;
                    }
                    else if((b1[pos] & 0xff) > (b2[pos] & 0xff))    {
                        return AFTER;
                    }

                    pos++;
                }

                if(b1.length < b2.length)    {
                    return BEFORE;
                }
                else if(b1.length > b2.length)    {
                    return AFTER;
                }
                else    {
                    return EQUAL;
                }

            }

        }

    }

}
