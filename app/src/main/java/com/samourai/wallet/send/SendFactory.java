package com.samourai.wallet.send;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang.StringUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import com.samourai.wallet.OpCallback;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.Hash;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SendFactory	{

    private static SendFactory instance = null;
    private static Context context = null;

    private SendFactory () { ; }

    private String[] from = null;
    private HashMap<String,String> froms = null;

    private boolean sentChange = false;
    private int changeAddressesUsed = 0;

    List<MyTransactionOutPoint> allOutputs = null;

    public static SendFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null)	{
            instance = new SendFactory();
        }

        return instance;
    }

    public UnspentOutputsBundle phase1(final int accountIdx, final BigInteger amount, final BigInteger fee) {

//        Log.i("accountIdx", "" + accountIdx);

        final String xpub;
        try {
            xpub = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).xpubstr();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            return null;
        }
//        Log.i("xpub", xpub);

        HashMap<String,List<String>> unspentOutputs = APIFactory.getInstance(context).getUnspentOuts();
        List<String> data = unspentOutputs.get(xpub);
        froms = new HashMap<String,String>();
        if(data != null && data.size() > 0)    {
            for(String f : data) {
                if(f != null) {
                    String[] s = f.split(",");
//                Log.i("address path", s[1] + " " + s[0]);
                    froms.put(s[1], s[0]);
                }
            }
        }

        UnspentOutputsBundle unspentCoinsBundle = null;
        try {
//            unspentCoinsBundle = getRandomizedUnspentOutputPoints(new String[]{ xpub }, amount.add(fee));

            ArrayList<String> addressStrings = new ArrayList<String>();
            addressStrings.add(xpub);
            for(String pcode : BIP47Meta.getInstance().getUnspentProviders())   {
                addressStrings.addAll(BIP47Meta.getInstance().getUnspentAddresses(context, pcode));
            }
            unspentCoinsBundle = getRandomizedUnspentOutputPoints(addressStrings.toArray(new String[addressStrings.size()]), amount.add(fee));
        }
        catch(Exception e) {
            return null;
        }

        if(unspentCoinsBundle.getOutputs() == null) {
//                        Log.i("SpendThread", "allUnspent == null");
            return null;
        }

        return unspentCoinsBundle;
    }

    public Pair<Transaction, Long> phase2(final int accountIdx, final List<MyTransactionOutPoint> unspent, final HashMap<String, BigInteger> receivers, final BigInteger fee, final int spendType) {

        sentChange = false;

        Pair<Transaction, Long> pair = null;

        try {
            int changeIdx = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChange().getAddrIdx();
            pair = _makeTx(accountIdx, spendType, unspent, receivers, fee, changeIdx);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return pair;
    }

    public void phase3(final Pair<Transaction, Long> pair, final int accountIdx, final List<MyTransactionOutPoint> unspent, final HashMap<String, BigInteger> receivers, final BigInteger fee, final OpCallback opc) {

        final Handler handler = new Handler();
        final HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();

                    Transaction tx = pair.first;
                    Long priority = pair.second;

                    for (TransactionInput input : tx.getInputs()) {
                        byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
                        String address = new BitcoinScript(scriptBytes).getAddress().toString();
//                        Log.i("address from script", address);
                        ECKey ecKey = null;
                        try {
                            String path = froms.get(address);
                            if(path == null)    {
//                                Log.i("pcode lookup size:", "" + BIP47Meta.getInstance().getPCode4AddrLookup().size());
//                                Log.i("looking up:", "" + address);
                                String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
//                                Log.i("pcode from address:", pcode);
                                int idx = BIP47Meta.getInstance().getIdx4Addr(address);
//                                Log.i("idx from address:", "" + idx);
                                PaymentAddress addr = BIP47Util.getInstance(context).getReceiveAddress(new PaymentCode(pcode), idx);
                                ecKey = addr.getReceiveECKey();
//                                Log.i("ECKey address:", ecKey.toAddress(MainNetParams.get()).toString());
                            }
                            else    {
                                String[] s = path.split("/");
                                HD_Address hd_address = AddressFactory.getInstance(context).get(accountIdx, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
//                            Log.i("HD address", hd_address.getAddressString());
                                String strPrivKey = hd_address.getPrivateKeyString();
                                DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), strPrivKey);
                                ecKey = pk.getKey();
//                            Log.i("ECKey address", ecKey.toAddress(MainNetParams.get()).toString());
                            }
                        } catch (AddressFormatException afe) {
                            afe.printStackTrace();
                            continue;
                        }

                        if(ecKey != null) {
                            keyBag.put(input.getOutpoint().toString(), ecKey);
                        }
                        else {
                            opc.onFail();
//                            Log.i("ECKey error", "cannot process private key");
                        }

                    }

                    signTx(tx, keyBag);
                    String hexString = new String(Hex.encode(tx.bitcoinSerialize()));
                    if(hexString.length() > (100 * 1024)) {
                        Toast.makeText(context, R.string.tx_length_error, Toast.LENGTH_SHORT).show();
//                        Log.i("SendFactory", "Transaction length too long");
                        opc.onFail();
                        throw new Exception(context.getString(R.string.tx_length_error));
                    }

//                    Log.i("SendFactory tx hash", tx.getHashAsString());
//                    Log.i("SendFactory tx string", hexString);
                    String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "pushtx", "tx=" + hexString);
//                    Log.i("Send response", response);
                    if(response.contains("Transaction Submitted")) {
                        opc.onSuccess();
                        if(sentChange) {
                            for(int i = 0; i < changeAddressesUsed; i++) {
                                HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(AddressFactory.CHANGE_CHAIN).incAddrIdx();
                            }
                        }

                        for(Iterator<Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
                            Entry<String, BigInteger> mapEntry = iterator.next();
                            String toAddress = mapEntry.getKey();
                            SendAddressUtil.getInstance().add(toAddress, true);
                        }

                        HD_WalletFactory.getInstance(context).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance(context).getPIN()));
                    }
                    else {
                        Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        opc.onFail();
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ;
                        }
                    });

                    Looper.loop();

                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public BigInteger sweep(final ECKey ecKey) {

        BigInteger ret = BigInteger.ZERO;

        if(ecKey != null) {
            try {
                String response = WebUtil.getInstance(null).getURL(WebUtil.BLOCKCHAIN_DOMAIN + "multiaddr?active=" + ecKey.toAddress(MainNetParams.get()) + "&simple=true");
//                Log.i("sweep", response);
                org.json.JSONObject jsonObject = new org.json.JSONObject(response);
                if(jsonObject != null && jsonObject.has(ecKey.toAddress(MainNetParams.get()).toString())) {
                    org.json.JSONObject addrObj = (org.json.JSONObject)jsonObject.get(ecKey.toAddress(MainNetParams.get()).toString());
                    if(addrObj.has("final_balance")) {
                        long value = addrObj.getLong("final_balance");
//                        Log.i("sweep", "sweep value:" + value);
                        ret = BigInteger.valueOf(value);
                    }
                }
            }
            catch(JSONException je) {
                je.getMessage();
                je.printStackTrace();
            }
            catch(Exception e) {
                e.getMessage();
                e.printStackTrace();
            }
        }

        return ret;
    }

    public void sweep(final ECKey ecKey, final BigInteger amount, final BigInteger fee, final OpCallback opc) {

        if(ecKey == null) {
            return;
        }

        final HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();
        final HD_Address addr = AddressFactory.getInstance(context).get(AddressFactory.RECEIVE_CHAIN);
        final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put(addr.getAddressString(), amount);

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();

                    List<MyTransactionOutPoint> allUnspent = null;
                    allUnspent = getUnspentOutputPoints(false, new String[]{ ecKey.toAddress(MainNetParams.get()).toString() }, amount.add(fee));
                    if(allUnspent == null) {
//                        Log.i("SpendThread", "allUnspent == null");
                    }
//                    Log.i("allUnspent list size", "" + allUnspent.size());
                    BigInteger bTotalUnspent = BigInteger.ZERO;
                    for(MyTransactionOutPoint outp : allUnspent) {
//                        Log.i("allUnspent add value", "" + outp.getValue().toString());
                        bTotalUnspent = bTotalUnspent.add(outp.getValue());
                    }
//                    Log.i("bTotalUnspent", "" + bTotalUnspent.toString());
                    if(amount.compareTo(bTotalUnspent) == 1) {
//                        Log.i("Funds error", "amount exceeds unspent outputs:" + amount.toString() + " > " + bTotalUnspent.toString());
                        return;
                    }
                    Pair<Transaction, Long> pair = null;
                    int changeIdx = HD_WalletFactory.getInstance(context).get().getAccount(SamouraiWallet.SAMOURAI_ACCOUNT).getChange().getAddrIdx();
//                    String changeAddr = AddressFactory.getInstance(context).get(AddressFactory.CHANGE_CHAIN).getAddressString();
                    HD_Address cAddr = HD_WalletFactory.getInstance(context).get().getAccount(SamouraiWallet.SAMOURAI_ACCOUNT).getChange().getAddressAt(changeIdx);
                    String changeAddr = cAddr.getAddressString();
//                    Log.i("change address", changeAddr + "," + cAddr.getPath());
                    pair = makeTx(allUnspent, receivers, fee, changeAddr);
//                    Log.i("pair is", pair == null ? "null" : "not null");
                    // Transaction cancelled
                    if(pair == null) {
                        opc.onFail();
                        return;
                    }
                    Transaction tx = pair.first;
                    Long priority = pair.second;

                    Wallet wallet = new Wallet(MainNetParams.get());
                    for (TransactionInput input : tx.getInputs()) {
                        byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
                        String address = new BitcoinScript(scriptBytes).getAddress().toString();
//                        Log.i("address from script", address);
                        if(ecKey != null) {
//                            wallet.addKey(ecKey);
                            keyBag.put(input.getOutpoint().toString(), ecKey);
                        }
                        else {
                            opc.onFail();
//                            Log.i("ECKey error", "cannot process private key");
                        }

                    }

                    // Now sign the inputs
//                    tx.signInputs(SigHash.ALL, wallet);
                    signTx(tx, keyBag);
                    String hexString = new String(Hex.encode(tx.bitcoinSerialize()));
                    if(hexString.length() > (100 * 1024)) {
                        opc.onFail();
                        throw new Exception(context.getString(R.string.tx_length_error));
                    }

//                    Log.i("SendFactory tx hash", tx.getHashAsString());
//                    Log.i("SendFactory tx string", hexString);
                    String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "pushtx", "tx=" + hexString);
//                    Log.i("Send response", response);
                    if(response.contains("Transaction Submitted")) {
                        opc.onSuccess();
                        HD_WalletFactory.getInstance(context).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance(context).getPIN()));
                    }
                    else {
                        Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        opc.onFail();
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ;
                        }
                    });

                    Looper.loop();

                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /*
    Used by sweeps
     */
    private List<MyTransactionOutPoint> getUnspentOutputPoints(boolean isHD, String[] from, BigInteger totalAmount) throws Exception {

        BigInteger totalValue = BigInteger.ZERO;

        String args = null;
        if(isHD) {
            args = from[0];
        }
        else {
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i < from.length; i++) {
                buffer.append(from[i]);
                if(i != (from.length - 1)) {
                    buffer.append("|");
                }
            }

            args = buffer.toString();
        }

//        Log.i("Unspent outputs url", WebUtil.BLOCKCHAIN_DOMAIN + "unspent?active=" + args);
        String response = WebUtil.getInstance(null).getURL(WebUtil.BLOCKCHAIN_DOMAIN + "unspent?active=" + args);
//        Log.i("Unspent outputs", response);

        List<MyTransactionOutPoint> outputs = new ArrayList<MyTransactionOutPoint>();

        Map<String, Object> root = (Map<String, Object>)JSONValue.parse(response);
        List<Map<String, Object>> outputsRoot = (List<Map<String, Object>>)root.get("unspent_outputs");
        if(outputsRoot == null) {
            return null;
        }
        for (Map<String, Object> outDict : outputsRoot) {

            byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));

            Hash hash = new Hash(hashBytes);
            hash.reverse();
            Sha256Hash txHash = new Sha256Hash(hash.getBytes());

            int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
//            Log.i("Unspent output",  "n:" + txOutputN);
            BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
//            Log.i("Unspent output",  "value:" + value.toString());
            totalValue = totalValue.add(value);
//            Log.i("Unspent output",  "totalValue:" + totalValue.toString());
            byte[] scriptBytes = Hex.decode((String)outDict.get("script"));
            int confirmations = ((Number)outDict.get("confirmations")).intValue();
//            Log.i("Unspent output",  "confirmations:" + confirmations);

            if(isHD) {
                String address = new BitcoinScript(scriptBytes).getAddress().toString();
                String path = null;
                if(outDict.containsKey("xpub")) {
                    JSONObject obj = (JSONObject)outDict.get("xpub");
                    if(obj.containsKey("path")) {
                        path = (String)obj.get("path");
                        froms.put(address, path);
                    }
                }
            }

            // Construct the output
            MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, new BitcoinScript(scriptBytes).getAddress().toString());
            outPoint.setConfirmations(confirmations);
            outputs.add(outPoint);

            if(totalValue.compareTo(totalAmount) >= 0) {
                break;
            }

        }

        return outputs;
    }

    /*
    Was used by spends which now use phase1() and phase2()
     */
    private UnspentOutputsBundle getRandomizedUnspentOutputPoints(String[] from, BigInteger totalAmount) throws Exception {

        BigInteger totalAmountPlusDust = totalAmount.add(SamouraiWallet.bDust);

        UnspentOutputsBundle ret = new UnspentOutputsBundle();
/*
        String args = StringUtils.join(from, "|");

        HashMap<String,List<MyTransactionOutPoint>> outputsByAddress = new HashMap<String,List<MyTransactionOutPoint>>();

//        Log.i("Unspent outputs url", WebUtil.BLOCKCHAIN_DOMAIN + "unspent?active=" + args);
        String response = WebUtil.getInstance(null).getURL(WebUtil.BLOCKCHAIN_DOMAIN + "unspent?active=" + args);
//        Log.i("Unspent outputs", response);
*/

        HashMap<String,List<MyTransactionOutPoint>> outputsByAddress = new HashMap<String,List<MyTransactionOutPoint>>();
        String args = "active=" + StringUtils.join(from, "|");
//        Log.i("Unspent outputs url", WebUtil.BLOCKCHAIN_DOMAIN + "unspent?active=" + args);
        String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "unspent?", args);
//        Log.i("Unspent outputs", response);

        List<MyTransactionOutPoint> outputs = new ArrayList<MyTransactionOutPoint>();

        Map<String, Object> root = (Map<String, Object>)JSONValue.parse(response);
        List<Map<String, Object>> outputsRoot = (List<Map<String, Object>>)root.get("unspent_outputs");
        if(outputsRoot == null) {
//            Log.i("SendFactory", "JSON parse failed");
            return null;
        }
        boolean isChange = false;
        for (Map<String, Object> outDict : outputsRoot) {

            isChange = false;

            byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));

            Hash hash = new Hash(hashBytes);
            hash.reverse();
            Sha256Hash txHash = new Sha256Hash(hash.getBytes());

            int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
//            Log.i("Unspent output", "n:" + txOutputN);
            BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
//            Log.i("Unspent output", "value:" + value.toString());
            byte[] scriptBytes = Hex.decode((String)outDict.get("script"));
            int confirmations = ((Number)outDict.get("confirmations")).intValue();
//            Log.i("Unspent output", "confirmations:" + confirmations);

            String address = new BitcoinScript(scriptBytes).getAddress().toString();
//            Log.i("Unspent output", "address from script:" + address);
            String path = null;
            if(outDict.containsKey("xpub")) {
                JSONObject obj = (JSONObject)outDict.get("xpub");
                if(obj.containsKey("path")) {
                    path = (String)obj.get("path");
                    froms.put(address, path);
                    String[] s = path.split("/");
                    if(s[1].equals("1")) {
                        isChange = true;
                    }
                }
            }

            // Construct the output
            MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, address);
            outPoint.setConfirmations(confirmations);
            outPoint.setIsChange(isChange);
            outputs.add(outPoint);

            //
            // get all outputs from same public address
            //
            if(!outputsByAddress.containsKey(outPoint.getAddress())) {
                outputsByAddress.put(outPoint.getAddress(), new ArrayList<MyTransactionOutPoint>());
            }
            outputsByAddress.get(outPoint.getAddress()).add(outPoint);
            //
            //
            //

        }

        allOutputs = outputs;

        //
        // look for smallest UTXO that is >= totalAmount and return it
        //
        Collections.sort(outputs, new UnspentOutputAmountComparator());
        Collections.reverse(outputs);
        for (MyTransactionOutPoint output : outputs) {

//            Log.i("SendFactory", output.getValue().toString());

            if(output.getValue().compareTo(totalAmountPlusDust) >= 0) {
//                Log.i("SendFactory", "Single output:" + output.getAddress() + "," + output.getValue().toString());
                List<MyTransactionOutPoint> single_output = new ArrayList<MyTransactionOutPoint>();
                single_output.add(output);
                ret.setOutputs(single_output);
                ret.setChangeSafe(true);
                ret.setNbAddress(1);
                ret.setTotalAmount(output.getValue());
                ret.setType(UnspentOutputsBundle.SINGLE_OUTPUT);

                //
                // get all outputs from same public address
                //
                if(outputsByAddress.get(output.getAddress()).size() > 1) {
//                    Log.i("SendFactory", "Single address:" + output.getAddress() + "," + output.getValue().toString());
                    ret = new UnspentOutputsBundle();
                    List<MyTransactionOutPoint> same_address_outputs = new ArrayList<MyTransactionOutPoint>();
                    same_address_outputs.addAll(outputsByAddress.get(output.getAddress()));
                    ret.setOutputs(same_address_outputs);
                    ret.setChangeSafe(true);
                    ret.setNbAddress(same_address_outputs.size());
                    BigInteger total = BigInteger.ZERO;
                    for(MyTransactionOutPoint out : same_address_outputs) {
                        total = total.add(out.getValue());
                    }
                    ret.setTotalAmount(total);
                    ret.setType(UnspentOutputsBundle.SINGLE_ADDRESS);
                }
                //
                //
                //

                return ret;
            }

        }

        Collections.shuffle(outputs, new SecureRandom());
        List<MyTransactionOutPoint> _outputs = new ArrayList<MyTransactionOutPoint>();
        BigInteger totalValue = BigInteger.ZERO;
        List<String> seen_txs = new ArrayList<String>();
        List<String> seen_addresses = new ArrayList<String>();
        //
        // try only change addresses (smallest UTXO >= totalAmount, but avoid same tx)
        //
        for (MyTransactionOutPoint output : outputs) {

            if(!output.isChange()) {
                continue;
            }

            if(seen_txs.contains(output.getTxHash().toString())) {
//                    Log.i("SendFactory", "change output already seen:" + output.getTxHash().toString());
                continue;
            }
            else {
                seen_txs.add(output.getTxHash().toString());
            }

            if(seen_addresses.contains(output.getAddress())) {
                ;
            }
            else {
                seen_addresses.add(output.getAddress());
            }

//            Log.i("SendFactory", "output:" + output.getValue().toString());
//            Log.i("SendFactory", "output:" + output.getTxHash().toString());
            totalValue = totalValue.add(output.getValue());
            _outputs.add(output);
            if(totalValue.compareTo(totalAmountPlusDust) >= 0) {
//                Log.i("SendFactory", "min. number of outputs spend");
                break;
            }
        }

        if(totalValue.compareTo(totalAmountPlusDust) >= 0) {
//            Log.i("SendFactory", "Change address(es):" + totalValue.toString());
            ret = new UnspentOutputsBundle();
            ret.setChangeSafe(true);
            ret.setTotalAmount(totalValue);
            ret.setOutputs(_outputs);
            ret.setNbAddress(seen_addresses.size());
            ret.setType(UnspentOutputsBundle.ONLY_CHANGE_DEDUP_TX);
            return ret;
        }

        //
        // try only receive addresses (smallest UTXO >= totalAmount)
        //
        _outputs.clear();
        totalValue = BigInteger.ZERO;
        seen_addresses.clear();
        for (MyTransactionOutPoint output : outputs) {

            if(output.isChange()) {
                continue;
            }

            if(seen_addresses.contains(output.getAddress())) {
                ;
            }
            else {
                seen_addresses.add(output.getAddress());
            }

//            Log.i("SendFactory", "output:" + output.getValue().toString());
//            Log.i("SendFactory", "output:" + output.getTxHash().toString());
            totalValue = totalValue.add(output.getValue());
            _outputs.add(output);
            if(totalValue.compareTo(totalAmountPlusDust) >= 0) {
//                Log.i("SendFactory", "min. number of outputs spend");
                break;
            }
        }

        if(totalValue.compareTo(totalAmountPlusDust) >= 0) {
//            Log.i("SendFactory", "Receive address(es):" + totalValue.toString());
            ret = new UnspentOutputsBundle();
            ret.setChangeSafe(true);
            ret.setTotalAmount(totalValue);
            ret.setOutputs(_outputs);
            ret.setNbAddress(seen_addresses.size());
            ret.setType(UnspentOutputsBundle.ONLY_RECEIVE);
            return ret;
        }

        ret = new UnspentOutputsBundle();

        //
        // choose min. without using more than one unspent change output from a same tx
        // should minimize generation of very small outputs
        //
        _outputs.clear();
        totalValue = BigInteger.ZERO;
        seen_txs.clear();
        seen_addresses.clear();
        for (MyTransactionOutPoint output : outputs) {

            if(output.isChange()) {
                if(seen_txs.contains(output.getTxHash().toString())) {
//                    Log.i("SendFactory", "change output already seen:" + output.getTxHash().toString());
                    continue;
                }
                else {
                    seen_txs.add(output.getTxHash().toString());
                }
            }

            if(seen_addresses.contains(output.getAddress())) {
                ;
            }
            else {
                seen_addresses.add(output.getAddress());
            }

//            Log.i("SendFactory", "output:" + output.getValue().toString());
//            Log.i("SendFactory", "output:" + output.getTxHash().toString());
            totalValue = totalValue.add(output.getValue());
            _outputs.add(output);
            if(totalValue.compareTo(totalAmountPlusDust) >= 0) {
//                Log.i("SendFactory", "min. number of outputs spend");
                break;
            }
        }

        //
        // if not possible via previous method, randomize and accept outputs until >= totalAmount
        //
        if(totalValue.compareTo(totalAmountPlusDust) < 0) {
//            Log.i("SendFactory", "Randomized w/tx:" + totalValue.toString());
            ret.setChangeSafe(false);
            ret.setType(UnspentOutputsBundle.MIXED);
            seen_addresses.clear();
            _outputs.clear();
            totalValue = BigInteger.ZERO;
            Collections.shuffle(outputs, new SecureRandom());

            for (MyTransactionOutPoint output : outputs) {

                if(seen_addresses.contains(output.getAddress())) {
                    ;
                }
                else {
                    seen_addresses.add(output.getAddress());
                }

//                Log.i("SendFactory", "output:" + output.getValue().toString());
                totalValue = totalValue.add(output.getValue());
                _outputs.add(output);
                ret.setTotalAmount(totalValue);
                if(totalValue.compareTo(totalAmountPlusDust) >= 0) {
//                    Log.i("SendFactory", "randomized outputs spend");
                    break;
                }
            }
        }
        else {
//            Log.i("SendFactory", "Randomized wo/tx:" + totalValue.toString());
            ret.setChangeSafe(true);
            ret.setType(UnspentOutputsBundle.MIXED_DEDUP_TX);
        }

        ret.setTotalAmount(totalValue);
        ret.setOutputs(_outputs);
        ret.setNbAddress(seen_addresses.size());
        return ret;
    }

    /*
    Used by sweeps
     */
    private Pair<Transaction,Long> makeTx(List<MyTransactionOutPoint> unspent, HashMap<String, BigInteger> receivingAddresses, BigInteger fee, final String changeAddress) throws Exception {

        long priority = 0;

        if(unspent == null || unspent.size() == 0) {
//			throw new InsufficientFundsException("No free outputs to spend.");
            return null;
        }

        if(fee == null) {
            fee = BigInteger.ZERO;
        }

        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

        //Construct a new transaction
        Transaction tx = new Transaction(MainNetParams.get());

        BigInteger outputValueSum = BigInteger.ZERO;

        for(Iterator<Entry<String, BigInteger>> iterator = receivingAddresses.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, BigInteger> mapEntry = iterator.next();
            String toAddress = mapEntry.getKey();
            BigInteger amount = mapEntry.getValue();

            if(amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
                throw new Exception(context.getString(R.string.invalid_amount));
            }

            outputValueSum = outputValueSum.add(amount);
            //Add the output
            BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitcoinScript(new BitcoinAddress(toAddress));
            TransactionOutput output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(amount.longValue()), toOutputScript.getProgram());
            outputs.add(output);
        }

        //Now select the appropriate inputs
        BigInteger valueSelected = BigInteger.ZERO;
        BigInteger valueNeeded =  outputValueSum.add(fee);
        BigInteger minFreeOutputSize = BigInteger.valueOf(1000000);

        // changeAddress should never be null
//        MyTransactionOutPoint changeOutPoint = null;

        for(MyTransactionOutPoint outPoint : unspent) {

            BitcoinScript script = new BitcoinScript(outPoint.getScriptBytes());

            if(script.getOutType() == BitcoinScript.ScriptOutTypeStrange) {
                continue;
            }

            BitcoinScript inputScript = new BitcoinScript(outPoint.getConnectedPubKeyScript());
            String address = inputScript.getAddress().toString();

            //if isSimpleSend don't use address as input if is output
            if(receivingAddresses.get(address) != null) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(MainNetParams.get(), null, new byte[0], outPoint);
            tx.addInput(input);
            valueSelected = valueSelected.add(outPoint.getValue());
            priority += outPoint.getValue().longValue() * outPoint.getConfirmations();

//            // changeAddress should never be null
//            if(changeAddress == null) {
//                changeOutPoint = outPoint;
//            }
//            //

            if(valueSelected.compareTo(valueNeeded) == 0 || valueSelected.compareTo(valueNeeded.add(minFreeOutputSize)) >= 0) {
                break;
            }
        }

        //Check the amount we have selected is greater than the amount we need
        if(valueSelected.compareTo(valueNeeded) < 0) {
//			throw new InsufficientFundsException("Insufficient Funds");
            return null;
        }

        BigInteger change = valueSelected.subtract(outputValueSum).subtract(fee);

        //Now add the change if there is any
        if (change.compareTo(BigInteger.ZERO) > 0) {
            BitcoinScript change_script;
            if(changeAddress != null) {
                change_script = BitcoinScript.createSimpleOutBitcoinScript(new BitcoinAddress(changeAddress));
//				Log.d("MyRemoteWallet", "MyRemoteWallet makeTransaction changeAddress != null: " + changeAddress + "change: " + change);
                sentChange = true;
            }
            else {
                throw new Exception(context.getString(R.string.invalid_tx_attempt));
            }
            TransactionOutput change_output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(change.longValue()), change_script.getProgram());
            outputs.add(change_output);
        }
        else {
            sentChange = false;
        }

        Collections.shuffle(outputs, new SecureRandom());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());
        priority /= estimatedSize;

        return new Pair<Transaction, Long>(tx, priority);
    }

    /*
    Used by spends
     */
    private Pair<Transaction,Long> _makeTx(int accountIdx, int change_type, List<MyTransactionOutPoint> unspent, HashMap<String, BigInteger> receivers, BigInteger fee, int changeIdx) throws Exception {

        BigInteger amount = BigInteger.ZERO;
        for(Iterator<Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, BigInteger> mapEntry = iterator.next();
            amount = amount.add(mapEntry.getValue());
        }

        long priority = 0;

        if(unspent == null || unspent.size() == 0) {
//			throw new InsufficientFundsException("No free outputs to spend.");
//            Log.i("SendFactory", "no unspents");
            return null;
        }

        if(fee == null) {
            fee = BigInteger.ZERO;
        }

        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

        //Construct a new transaction
        Transaction tx = new Transaction(MainNetParams.get());

        BigInteger outputValueSum = BigInteger.ZERO;

        for(Iterator<Entry<String, BigInteger>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, BigInteger> mapEntry = iterator.next();
            String toAddress = mapEntry.getKey();
            BigInteger value = mapEntry.getValue();

            if(value == null || value.compareTo(BigInteger.ZERO) <= 0) {
                throw new Exception(context.getString(R.string.invalid_amount));
            }

            if(value.compareTo(SamouraiWallet.bDust) < 1)    {
                throw new Exception(context.getString(R.string.dust_amount));
            }

            outputValueSum = outputValueSum.add(value);
            //Add the output
            BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitcoinScript(new BitcoinAddress(toAddress));
            TransactionOutput output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(value.longValue()), toOutputScript.getProgram());
            outputs.add(output);
        }

        //Now select the appropriate inputs
        BigInteger valueSelected = BigInteger.ZERO;
        BigInteger valueNeeded =  outputValueSum.add(fee);
        BigInteger minFreeOutputSize = BigInteger.valueOf(1000000);

        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();

        for(MyTransactionOutPoint outPoint : unspent) {

            BitcoinScript script = new BitcoinScript(outPoint.getScriptBytes());

            if(script.getOutType() == BitcoinScript.ScriptOutTypeStrange) {
                continue;
            }

            BitcoinScript inputScript = new BitcoinScript(outPoint.getConnectedPubKeyScript());
            String address = inputScript.getAddress().toString();

            //if isSimpleSend don't use address as input if is output
            if(receivers.get(address) != null) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(MainNetParams.get(), null, new byte[0], outPoint, outPoint.getTxHash().toString(), outPoint.getTxOutputN());
            inputs.add(input);
            valueSelected = valueSelected.add(outPoint.getValue());
            priority += outPoint.getValue().longValue() * outPoint.getConfirmations();

            if(valueSelected.compareTo(valueNeeded) == 0 || valueSelected.compareTo(valueNeeded.add(minFreeOutputSize)) >= 0) {
                break;
            }
        }

        //Check the amount we have selected is greater than the amount we need
        if(valueSelected.compareTo(valueNeeded) < 0) {
//			throw new InsufficientFundsException("Insufficient Funds");
//            Log.i("SendFactory", "valueSelected:" + valueSelected.toString());
//            Log.i("SendFactory", "valueNeeded:" + valueNeeded.toString());
            return null;
        }

        BigInteger change = valueSelected.subtract(outputValueSum).subtract(fee);
        if(change.compareTo(BigInteger.ZERO) == 1 && change.compareTo(SamouraiWallet.bDust) == -1)    {
            Toast.makeText(context, R.string.dust_change, Toast.LENGTH_SHORT).show();
            return null;
        }
        ChangeMaker cm = new ChangeMaker(context, accountIdx, amount, change, changeIdx);
        if(change_type == ChangeMaker.CHANGE_AGGRESSIVE && inputs.size() >= 10)    {
            cm.setAggressive(true);
        }
        else    {
            cm.setAggressive(false);
            if(change.compareTo(BigInteger.valueOf(10000000L)) > 0)    {
                cm.addDecoys(1);
            }
        }
        cm.makeChange();
        sentChange = cm.madeChange();
        if(sentChange) {
//            Log.i("SendFactory", "Change outputs:" + cm.getOutputs().size());
            outputs.addAll(cm.getOutputs());
            changeAddressesUsed = cm.getOutputs().size();
        }
        else {
            changeAddressesUsed = 0;
        }

        //
        // deterministically sort inputs and outputs, see OBPP BIP proposal
        //
        Collections.sort(inputs, new InputComparator());
        for(TransactionInput input : inputs) {
            tx.addInput(input);
        }

        Collections.sort(outputs, new OutputComparator());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        //
        // calculate priority
        //
        long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());
        priority /= estimatedSize;

        return new Pair<Transaction, Long>(tx, priority);
    }

    private List<MyTransactionOutPoint> getAllOutputsSorted() {
        List<MyTransactionOutPoint> ret = allOutputs;
        Collections.sort(ret, new UnspentOutputAmountComparator());
        Collections.reverse(ret);
        return ret;
    }

    private boolean isSelected(UnspentOutputsBundle unspentCoinsBundle, MyTransactionOutPoint output) {

        for(MyTransactionOutPoint _output : unspentCoinsBundle.getOutputs())   {
            if(_output.getTxHash().toString().equals(output.getTxHash().toString()) && _output.getTxOutputN() == output.getTxOutputN())    {
                return true;
            }
        }

        return false;
    }

    public UnspentOutputsBundle supplementRandomizedUnspentOutputPoints(UnspentOutputsBundle unspentCoinsBundle, BigInteger minAmount)   {

        List<MyTransactionOutPoint> allOutputsSorted = getAllOutputsSorted();
        boolean foundSingle = false;
        boolean foundOthers = false;
        List<MyTransactionOutPoint> supplementedOutputs = new ArrayList<MyTransactionOutPoint>();
        BigInteger supplementValue = BigInteger.ZERO;

        for(MyTransactionOutPoint output : allOutputsSorted)   {
            if(output.getValue().compareTo(minAmount) >= 0 && !isSelected(unspentCoinsBundle, output))   {
                supplementedOutputs.add(output);
                supplementValue = output.getValue();
                foundSingle = true;
                break;
            }
        }

        if(!foundSingle)    {
            for(MyTransactionOutPoint output : allOutputsSorted)   {
                if(!isSelected(unspentCoinsBundle, output))   {
                    supplementedOutputs.add(output);
                    supplementValue = supplementValue.add(output.getValue());
                    foundOthers = true;
                }
            }
        }

        if(foundSingle || foundOthers)    {
            unspentCoinsBundle.setTotalAmount(unspentCoinsBundle.getTotalAmount().add(supplementValue));
            List<MyTransactionOutPoint> refreshedOutputs = unspentCoinsBundle.getOutputs();
            refreshedOutputs.addAll(supplementedOutputs);
            unspentCoinsBundle.setOutputs(refreshedOutputs);
            unspentCoinsBundle.setType(UnspentOutputsBundle.REFRESHED);

            List<String> seen_txs = new ArrayList<String>();
            List<String> seen_addresses = new ArrayList<String>();

            boolean isChangeSafe = true;
            for(MyTransactionOutPoint output : unspentCoinsBundle.getOutputs())   {
                if(!seen_addresses.contains(output.getAddress()))    {
                    seen_addresses.add(output.getAddress());
                }
                if(output.isChange())    {
                    if(seen_txs.contains(output.getTxHash().toString()))    {
                        isChangeSafe = false;
                    }
                    else    {
                        seen_txs.add(output.getHash().toString());
                    }
                }
            }
            unspentCoinsBundle.setChangeSafe(isChangeSafe);
            unspentCoinsBundle.setNbAddress(seen_addresses.size());

            return unspentCoinsBundle;
        }

        return null;
    }

    public synchronized void signTx(Transaction transaction, HashMap<String,ECKey> keyBag) throws ScriptException {

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
                sig = transaction.calculateSignature(i, key, connectedPubKeyScript, SigHash.ALL, false);
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

    private class UnspentOutputAmountComparator implements Comparator<MyTransactionOutPoint> {

        public int compare(MyTransactionOutPoint o1, MyTransactionOutPoint o2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if(o1.getValue().compareTo(o2.getValue()) > 0) {
                return BEFORE;
            }
            else if(o1.getValue().compareTo(o2.getValue()) < 0) {
                return AFTER;
            }
            else    {
                return EQUAL;
            }

        }

    }

    private class InputComparator implements Comparator<MyTransactionInput> {

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

    private class OutputComparator implements Comparator<TransactionOutput> {

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

    private interface SendProgress {

        public void onStart();

        // Return false to cancel
//		public boolean onReady(Transaction tx, BigInteger fee, FeePolicy feePolicy, long priority);
        public boolean onReady(Transaction tx, BigInteger fee, long priority);
        public void onSend(Transaction tx, String message);

        // Return true to cancel the transaction or false to continue without it
        public ECKey onPrivateKeyMissing(String address);

        public void onError(String message);
        public void onProgress(String message);
    }

}
