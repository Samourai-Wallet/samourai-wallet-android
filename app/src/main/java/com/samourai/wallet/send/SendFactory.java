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
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.Hash;
import com.samourai.wallet.util.PrivKeyReader;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.R;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import org.bitcoinj.script.ScriptOpCodes;
import org.spongycastle.util.encoders.Hex;

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
                String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();
//                Log.i("address from script", address);
                ECKey ecKey = null;
                try {
                    String path = APIFactory.getInstance(context).getUnspentPaths().get(address);
                    if(path != null)    {
//                        Log.i("SendFactory", "unspent path:" + path);
                        String[] s = path.split("/");
                        int account_no = APIFactory.getInstance(context).getUnspentAccounts().get(address);
                        HD_Address hd_address = AddressFactory.getInstance(context).get(account_no, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
//                        Log.i("SendFactory", "unspent address:" + hd_address.getAddressString());
                        String strPrivKey = hd_address.getPrivateKeyString();
                        DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), strPrivKey);
                        ecKey = pk.getKey();
//                        Log.i("SendFactory", "ECKey address:" + ecKey.toAddress(MainNetParams.get()).toString());
                    }
                    else    {
//                        Log.i("pcode lookup size:", "" + BIP47Meta.getInstance().getPCode4AddrLookup().size());
//                        Log.i("looking up:", "" + address);
                        String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
//                        Log.i("pcode from address:", pcode);
                        int idx = BIP47Meta.getInstance().getIdx4Addr(address);
//                        Log.i("idx from address:", "" + idx);
                        PaymentAddress addr = BIP47Util.getInstance(context).getReceiveAddress(new PaymentCode(pcode), idx);
                        ecKey = addr.getReceiveECKey();
//                        Log.i("SendFactory", "ECKey address:" + ecKey.toAddress(MainNetParams.get()).toString());
                    }
                } catch (AddressFormatException afe) {
                    afe.printStackTrace();
                    continue;
                }

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
                String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();
//                        Log.i("address from script", address);
                ECKey ecKey = null;
                try {
                    DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), privKeyReader.getKey().getPrivateKeyAsWiF(MainNetParams.get()));
                    ecKey = pk.getKey();
//                    Log.i("SendFactory", "ECKey address:" + ecKey.toAddress(MainNetParams.get()).toString());
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
        Transaction tx = new Transaction(MainNetParams.get());

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
                output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(0L), toOutputScript.getProgram());
            }
            else    {
                toOutputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), toAddress));
                output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(value.longValue()), toOutputScript.getProgram());
            }

            outputs.add(output);
        }

        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();
        for(MyTransactionOutPoint outPoint : unspent) {
            Script script = new Script(outPoint.getScriptBytes());

            if(script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(MainNetParams.get(), null, new byte[0], outPoint, outPoint.getTxHash().toString(), outPoint.getTxOutputN());
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

        return transaction;

    }

    //
    // BIP126 hetero
    //
    public Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> heterogeneous(List<UTXO> outputs, BigInteger spendAmount, String address) {

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

            biFee = FeeUtil.getInstance().estimatedFee(selectedOutputs.size(), output_scripts);

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
                changeAddress = getChangeAddress();
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), changeAddress));
                txOut1 = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(_pct), outputScript.getProgram());
                // change address here
                changeAddress = getChangeAddress();
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), changeAddress));
                txOut2 = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(_pct), outputScript.getProgram());
                // spend address here
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), address));
                txOutSpend = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
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
//                Log.d("SendFactory", address + ":" + org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), address));
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), address));
//                Log.d("SendFactory", address + ":" + outputScript.getToAddress(MainNetParams.get()));
                txOutSpend = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());

//                System.out.println("pair part:" + spendAmount.toString());
                // change address here
                changeAddress = getChangeAddress();
                outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), changeAddress));
                txOut1 = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
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
                        changeAddress = getChangeAddress();
                        outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), changeAddress));
                        txChange = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(remainder.longValue()), outputScript.getProgram());

                        check_total += remainder.longValue();
                        remainder = remainder.subtract(remainder);
                    }
                    else    {
//                    System.out.println("remainder:" + remainderPart);
                        // change address here
                        changeAddress = getChangeAddress();
                        outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), changeAddress));
                        txChange = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(remainderPart.longValue()), outputScript.getProgram());

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

            biFee = FeeUtil.getInstance().estimatedFee(selectedOutputs.size(), NB_OUTPUTS);

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
            outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), address));
            txOutSpend = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
            // change address here
            changeAddress = getChangeAddress();
            outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), changeAddress));
            txOut1 = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(spendAmount.longValue()), outputScript.getProgram());
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
            changeAddress = getChangeAddress();
            outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), changeAddress));
            txChange = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(remainder.longValue()), outputScript.getProgram());
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

    private String getChangeAddress()    {
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

        return null;
    }

    public static class BIP69InputComparator implements Comparator<MyTransactionInput> {

        public int compare(MyTransactionInput i1, MyTransactionInput i2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            Hash hash1 = new Hash(Hex.decode(i1.getTxHash()));
            Hash hash2 = new Hash(Hex.decode(i2.getTxHash()));
            byte[] h1 = hash1.getBytes();
            byte[] h2 = hash2.getBytes();

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
