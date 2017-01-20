package com.samourai.wallet.bip47;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Address;
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
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import com.samourai.wallet.OpCallback;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UnspentOutputsBundle;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.Hash;
import com.samourai.wallet.util.PushTx;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.SecretPoint;
import com.samourai.wallet.R;

import org.bitcoinj.script.ScriptOpCodes;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendNotifTxFactory	{

    public static final BigInteger _bNotifTxValue = SamouraiWallet.bDust;
    public static final BigInteger _bFee = BigInteger.valueOf(Coin.parseCoin("0.00015").longValue());
    public static final BigInteger _bSWFee = SamouraiWallet.bFee;

    public static final BigInteger _bNotifTxTotalAmount = _bFee.add(_bSWFee).add(_bNotifTxValue);

    public static final String SAMOURAI_NOTIF_TX_FEE_ADDRESS = "3Pof32GmAoSpUfzPiCWTu3y7Ni9qxM7Hvc";

    private static SendNotifTxFactory instance = null;
    private static Context context = null;

    private SendNotifTxFactory () { ; }

    private String[] from = null;
    private HashMap<String,String> froms = null;

    private boolean sentChange = false;
    private int changeAddressesUsed = 0;

    List<MyTransactionOutPoint> allOutputs = null;

    public static SendNotifTxFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null)	{
            instance = new SendNotifTxFactory();
        }

        return instance;
    }

    public UnspentOutputsBundle phase1(final int accountIdx) {

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

        /*
        *
        *
        *
        *
        *
        *
        HashMap<String,List<String>> unspentOutputs = APIFactory.getInstance(context).getUnspentOuts();
        List<String> data = unspentOutputs.get(xpub);
        froms = new HashMap<String,String>();
        if(data != null)    {
            for(String f : data) {
                if(f != null) {
                    String[] s = f.split(",");
//                Log.i("address path", s[1] + " " + s[0]);
                    froms.put(s[1], s[0]);
                }
            }
        }
        *
        *
        *
        *
        *
        */

        UnspentOutputsBundle unspentCoinsBundle = null;
        try {
//            unspentCoinsBundle = getRandomizedUnspentOutputPoints(new String[]{xpub});

            ArrayList<String> addressStrings = new ArrayList<String>();
            addressStrings.add(xpub);
            for(String pcode : BIP47Meta.getInstance().getUnspentProviders())   {
                addressStrings.addAll(BIP47Meta.getInstance().getUnspentAddresses(context, pcode));
            }
            unspentCoinsBundle = getRandomizedUnspentOutputPoints(addressStrings.toArray(new String[addressStrings.size()]));

        }
        catch(Exception e) {
            return null;
        }
        if(unspentCoinsBundle == null || unspentCoinsBundle.getOutputs() == null) {
//                        Log.i("SpendThread", "allUnspent == null");
            return null;
        }

        return unspentCoinsBundle;
    }

    public Pair<Transaction, Long> phase2(final int accountIdx, final List<MyTransactionOutPoint> unspent, PaymentCode notifPcode) {

        sentChange = false;

        Pair<Transaction, Long> pair = null;

        try {
            int changeIdx = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChange().getAddrIdx();
            pair = makeTx(accountIdx, unspent, notifPcode, changeIdx);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return pair;
    }

    public void phase3(final Pair<Transaction, Long> pair, final int accountIdx, final PaymentCode notifPcode, final OpCallback opc) {

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
                        String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();
//                        Log.i("address from script", address);
                        ECKey ecKey = null;
                        try {
                            String path = APIFactory.getInstance(context).getUnspentPaths().get(address);
                            if(path != null)    {
//                                Log.i("SendNotifTxFactory", "unspent path:" + path);
                                String[] s = path.split("/");
                                HD_Address hd_address = AddressFactory.getInstance(context).get(0, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
//                                Log.i("SendNotifTxFactory", "unspent address:" + hd_address.getAddressString());
                                String strPrivKey = hd_address.getPrivateKeyString();
                                DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), strPrivKey);
                                ecKey = pk.getKey();
//                                Log.i("SendNotifTxFactory", "ECKey address:" + ecKey.toAddress(MainNetParams.get()).toString());
                            }
                            else    {
//                        Log.i("pcode lookup size:", "" + BIP47Meta.getInstance().getPCode4AddrLookup().size());
//                        Log.i("looking up:", "" + address);
                                String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
//                                Log.i("pcode from address:", pcode);
                                int idx = BIP47Meta.getInstance().getIdx4Addr(address);
//                                Log.i("idx from address:", "" + idx);
                                PaymentAddress addr = BIP47Util.getInstance(context).getReceiveAddress(new PaymentCode(pcode), idx);
                                ecKey = addr.getReceiveECKey();
//                                Log.i("SendNotifTxFactory", "ECKey address:" + ecKey.toAddress(MainNetParams.get()).toString());
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

                    String response = PushTx.getInstance(context).samourai(hexString);

                    try {
                        org.json.JSONObject jsonObject = new org.json.JSONObject(response);
                        if(jsonObject.has("status"))    {
                            if(jsonObject.getString("status").equals("ok"))    {
                                opc.onSuccess();
                                if(sentChange) {
                                    HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(AddressFactory.CHANGE_CHAIN).incAddrIdx();
                                }

                                BIP47Meta.getInstance().setOutgoingIdx(notifPcode.toString(), 0);
//                        Log.i("SendNotifTxFactory", "tx hash:" + tx.getHashAsString());
                                BIP47Meta.getInstance().setOutgoingStatus(notifPcode.toString(), tx.getHashAsString(), BIP47Meta.STATUS_SENT_NO_CFM);

//                            SendAddressUtil.getInstance().add(notifPcode.notificationAddress().toString(), true);

                                PayloadUtil.getInstance(context).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance(context).getPIN()));
                            }
                            else {
                                Toast.makeText(context, jsonObject.getString("status"), Toast.LENGTH_SHORT).show();
                                opc.onFail();
                            }
                        }
                        else    {
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                            opc.onFail();
                        }
                    }
                    catch(JSONException je) {
                        Toast.makeText(context, je.getMessage(), Toast.LENGTH_SHORT).show();
                        opc.onFail();
                    }
                    catch(DecryptionException de) {
                        Toast.makeText(context, de.getMessage(), Toast.LENGTH_SHORT).show();
                        opc.onFail();
                    }
                    catch(UnsupportedEncodingException uee) {
                        Toast.makeText(context, uee.getMessage(), Toast.LENGTH_SHORT).show();
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

    private UnspentOutputsBundle getRandomizedUnspentOutputPoints(String[] from) throws Exception {

        BigInteger totalAmountPlusDust = _bNotifTxTotalAmount.add(SamouraiWallet.bDust);

        UnspentOutputsBundle ret = new UnspentOutputsBundle();

        HashMap<String,List<MyTransactionOutPoint>> outputsByAddress = new HashMap<String,List<MyTransactionOutPoint>>();

//        Log.i("Unspent outputs url", WebUtil.BLOCKCHAIN_DOMAIN + "unspent?active=" + StringUtils.join(from, "|"));
        String response = WebUtil.getInstance(null).getURL(WebUtil.BLOCKCHAIN_DOMAIN + "unspent?active=" + StringUtils.join(from, "|"));
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
//            Log.i("Unspent output",  "n:" + txOutputN);
            BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
//            Log.i("Unspent output",  "value:" + value.toString());
            byte[] scriptBytes = Hex.decode((String)outDict.get("script"));
            int confirmations = ((Number)outDict.get("confirmations")).intValue();
//            Log.i("Unspent output",  "confirmations:" + confirmations);

            String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();
            String path = null;
            if(outDict.containsKey("xpub")) {
                JSONObject obj = (JSONObject)outDict.get("xpub");
                if(obj.containsKey("path")) {
                    path = (String)obj.get("path");
                    APIFactory.getInstance(context).getUnspentPaths().put(address, path);
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

        return null;
    }

    private Pair<Transaction,Long> makeTx(int accountIdx, List<MyTransactionOutPoint> unspent, PaymentCode notifPcode, int changeIdx) throws Exception {

        BigInteger amount = _bNotifTxValue.add(_bSWFee);

        long priority = 0;

        if(unspent == null || unspent.size() == 0) {
//			throw new InsufficientFundsException("No free outputs to spend.");
//            Log.i("SendFactory", "no unspents");
            return null;
        }

        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

        //Construct a new transaction
        Transaction tx = new Transaction(MainNetParams.get());

        BigInteger outputValueSum = BigInteger.ZERO;

        outputValueSum = outputValueSum.add(_bNotifTxValue);
        Script toOutputValueScript = ScriptBuilder.createOutputScript(Address.fromBase58(MainNetParams.get(), notifPcode.notificationAddress().getAddressString()));
        TransactionOutput outputValue = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(_bNotifTxValue.longValue()), toOutputValueScript.getProgram());
        outputs.add(outputValue);

        outputValueSum = outputValueSum.add(_bSWFee);
        Script toOutputSWFeeScript = ScriptBuilder.createOutputScript(Address.fromBase58(MainNetParams.get(), SAMOURAI_NOTIF_TX_FEE_ADDRESS));
        TransactionOutput outputSWFee = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(_bSWFee.longValue()), toOutputSWFeeScript.getProgram());
        outputs.add(outputSWFee);

        BigInteger valueSelected = BigInteger.ZERO;
        BigInteger valueNeeded =  outputValueSum.add(_bFee);
        BigInteger minFreeOutputSize = BigInteger.valueOf(1000000);

        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();
        byte[] op_return = null;

        for(int i = 0; i < unspent.size(); i++) {

            MyTransactionOutPoint outPoint = unspent.get(i);

            Script script = new Script(outPoint.getScriptBytes());

            if(script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            Script inputScript = new Script(outPoint.getConnectedPubKeyScript());
            String address = inputScript.getToAddress(MainNetParams.get()).toString();
            MyTransactionInput input = new MyTransactionInput(MainNetParams.get(), null, new byte[0], outPoint, outPoint.getTxHash().toString(), outPoint.getTxOutputN());
            inputs.add(input);
            valueSelected = valueSelected.add(outPoint.getValue());
            priority += outPoint.getValue().longValue() * outPoint.getConfirmations();

            if(i == 0)    {
                ECKey ecKey = null;
                String privStr = null;
                String path = APIFactory.getInstance(context).getUnspentPaths().get(address);
                if(path == null)    {
                    String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
                    int idx = BIP47Meta.getInstance().getIdx4Addr(address);
                    PaymentAddress addr = BIP47Util.getInstance(context).getReceiveAddress(new PaymentCode(pcode), idx);
                    ecKey = addr.getReceiveECKey();
                }
                else    {
                    String[] s = path.split("/");
                    HD_Address hd_address = AddressFactory.getInstance(context).get(accountIdx, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                    privStr = hd_address.getPrivateKeyString();
                    DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), privStr);
                    ecKey = pk.getKey();
                }

                byte[] privkey = ecKey.getPrivKeyBytes();
                byte[] pubkey = notifPcode.notificationAddress().getPubKey();
                byte[] outpoint = outPoint.bitcoinSerialize();
//                Log.i("SendFactory", "outpoint:" + Hex.toHexString(outpoint));
//                Log.i("SendFactory", "payer shared secret:" + Hex.toHexString(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes()));
                byte[] mask = notifPcode.getMask(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes(), outpoint);
//                Log.i("SendFactory", "mask:" + Hex.toHexString(mask));
//                Log.i("SendFactory", "mask length:" + mask.length);
//                Log.i("SendFactory", "payload0:" + Hex.toHexString(BIP47Util.getInstance(context).getPaymentCode().getPayload()));
                op_return = PaymentCode.blind(BIP47Util.getInstance(context).getPaymentCode().getPayload(), mask);
//                Log.i("SendFactory", "payload1:" + Hex.toHexString(op_return));
            }

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

        BigInteger change = valueSelected.subtract(outputValueSum).subtract(_bFee);
        if(change.compareTo(BigInteger.ZERO) == 1 && change.compareTo(SamouraiWallet.bDust) == -1)    {
            Toast.makeText(context, R.string.dust_change, Toast.LENGTH_SHORT).show();
            return null;
        }

        if(change.compareTo(BigInteger.ZERO) > 0) {

            try {
                HD_Address cAddr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChange().getAddressAt(changeIdx);
                String changeAddr = cAddr.getAddressString();

                Script change_script = null;
                if(changeAddr != null) {
                    change_script = ScriptBuilder.createOutputScript(Address.fromBase58(MainNetParams.get(), changeAddr));
                    TransactionOutput change_output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(change.longValue()), change_script.getProgram());
                    outputs.add(change_output);
                }
                else {
                    throw new Exception(context.getString(R.string.invalid_tx_attempt));
                }
            }
            catch(Exception e) {
                ;
            }
        }
        else {
            sentChange = false;
        }

        //
        // deterministically sort inputs and outputs, see OBPP BIP proposal
        //
//        Collections.sort(inputs, new InputComparator());
        for(TransactionInput input : inputs) {
            tx.addInput(input);
        }

        tx.addOutput(Coin.valueOf(0L), new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(op_return).build());
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

    //    private synchronized void signTx(Transaction transaction, Wallet wallet) throws ScriptException {
    private synchronized void signTx(Transaction transaction, HashMap<String,ECKey> keyBag) throws ScriptException {

        List<TransactionInput> inputs = transaction.getInputs();

        TransactionSignature[] sigs = new TransactionSignature[inputs.size()];
        ECKey[] keys = new ECKey[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {

            TransactionInput input = inputs.get(i);

            // Find the signing key
//            ECKey key = input.getOutpoint().getConnectedKey(wallet);
            ECKey key = keyBag.get(input.getOutpoint().toString());
            // Keep key for script creation step below
            keys[i] = key;
            byte[] connectedPubKeyScript = input.getOutpoint().getConnectedPubKeyScript();
            if(key.hasPrivKey() || key.isEncrypted()) {
                sigs[i] = transaction.calculateSignature(i, key, connectedPubKeyScript, SigHash.ALL, false);
            }
            else {
                sigs[i] = TransactionSignature.dummy();   // watch only ?
            }
        }

        for(int i = 0; i < inputs.size(); i++) {

            if(sigs[i] == null)   {
                continue;
            }

            TransactionInput input = inputs.get(i);
            final TransactionOutput connectedOutput = input.getOutpoint().getConnectedOutput();

            Script scriptPubKey = connectedOutput.getScriptPubKey();
            if(scriptPubKey.isSentToAddress()) {
                input.setScriptSig(ScriptBuilder.createInputScript(sigs[i], keys[i]));
            }
            else if(scriptPubKey.isSentToRawPubKey()) {
                input.setScriptSig(ScriptBuilder.createInputScript(sigs[i]));
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

}
