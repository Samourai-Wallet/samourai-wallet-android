package com.samourai.wallet.ricochet;

import android.content.Context;
//import android.util.Log;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.SendNotifTxFactory;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

public class RicochetMeta {

    private final static int RICOCHET_ACCOUNT = Integer.MAX_VALUE;

    public final static BigInteger samouraiFeeAmount = BigInteger.valueOf(100000L);

    public static final int STATUS_NOT_SENT = -1;
    public static final int STATUS_SENT_NO_CFM = 0;
    public static final int STATUS_SENT_CFM = 1;

    private static RicochetMeta instance = null;

    private static int index = 0;
    private static LinkedList<JSONObject> fifo = null;
    private static JSONObject lastRicochet = null;

    private static Context context = null;

    private RicochetMeta() { ; }

    public static RicochetMeta getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            fifo = new LinkedList<JSONObject>();

            instance = new RicochetMeta();
        }

        return instance;
    }

    public Iterator<JSONObject> getIterator() {
        return fifo.iterator();
    }

    public LinkedList<JSONObject> getQueue() {
        return fifo;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        RicochetMeta.index = index;
    }

    public void add(JSONObject jObj)   {
        fifo.add(jObj);
    }

    public JSONObject peek()   {
        if(!fifo.isEmpty())    {
            return fifo.peek();
        }
        else    {
            return null;
        }
    }

    public JSONObject get(int pos)   {
        if(!fifo.isEmpty() && pos < fifo.size())    {
            return fifo.get(pos);
        }
        else    {
            return null;
        }
    }

    public JSONObject remove()   {
        if(!fifo.isEmpty())    {
            return fifo.remove();
        }
        else    {
            return null;
        }
    }

    public void empty()   {
        fifo.clear();
    }

    public int size()   {
        return fifo.size();
    }

    public JSONObject getLastRicochet() {
        return lastRicochet;
    }

    public void setLastRicochet(JSONObject lastRicochet) {
        RicochetMeta.lastRicochet = lastRicochet;
    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();
        try {

            jsonPayload.put("index", index);

            if(lastRicochet != null)    {
                jsonPayload.put("last_ricochet", lastRicochet);
            }

            JSONArray array = new JSONArray();
            Iterator<JSONObject> itr = getIterator();
            while(itr.hasNext()){
                JSONObject obj = itr.next();
                array.put(obj);
            }
            jsonPayload.put("queue", array);

        }
        catch(JSONException je) {
            ;
        }

//        Log.i("RicochetMeta", jsonPayload.toString());

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

//        Log.i("RicochetMeta", jsonPayload.toString());

        try {

            if(jsonPayload.has("index"))    {
                index = jsonPayload.getInt("index");
            }
            if(jsonPayload.has("last_ricochet"))    {
                lastRicochet = jsonPayload.getJSONObject("last_ricochet");
            }
            if(jsonPayload.has("queue"))    {

                fifo.clear();

                JSONArray array = jsonPayload.getJSONArray("queue");
                for(int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    fifo.add(obj);
                }

            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

    public JSONObject script(long spendAmount, long feePerKBAmount, String strDestination, int nbHops, String strPCode, boolean samouraiFeeViaBIP47) {

        JSONObject jObj = new JSONObject();

        try {

            BigInteger biSpend = BigInteger.valueOf(spendAmount);
            BigInteger biSamouraiFee = BigInteger.valueOf(samouraiFeeAmount.longValue() * ((nbHops - 4) + 1));    // 4 hops min. for 0.001, each additional hop 0.001
            BigInteger biFeePerKB = BigInteger.valueOf(feePerKBAmount);

            jObj.put("ts", System.currentTimeMillis() / 1000L);
            jObj.put("hops", nbHops);
            jObj.put("spend_amount", biSpend.longValue());
            jObj.put("samourai_fee", biSamouraiFee.longValue());
            jObj.put("samourai_fee_via_bip47", samouraiFeeViaBIP47);
            jObj.put("feeKB", biFeePerKB.longValue());
            jObj.put("destination", strDestination);
            if(strPCode != null)    {
                jObj.put("pcode", strPCode);
            }

            JSONObject jHop = new JSONObject();
            JSONArray jHops = new JSONArray();

            int hopSz = FeeUtil.getInstance().estimatedSize(1, 1);
            BigInteger biFeePerHop = FeeUtil.getInstance().calculateFee(hopSz, biFeePerKB);

            Pair<List<UTXO>, BigInteger> pair = getHop0UTXO(spendAmount, nbHops, biFeePerHop.longValue());
            List<UTXO> utxos = pair.getLeft();
            long totalValueSelected = 0L;
            for(UTXO u : utxos)   {
                totalValueSelected += u.getValue();
            }
//            Log.d("RicochetMeta", "totalValueSelected (return):" + totalValueSelected);

            // hop0 'leaves' wallet, change returned to wallet
            BigInteger hop0 = biSpend.add(biSamouraiFee).add(biFeePerHop.multiply(BigInteger.valueOf((long) nbHops)));
//            BigInteger hop0Fee = FeeUtil.getInstance().calculateFee(hop0sz, biFeePerKB);
            BigInteger hop0Fee = pair.getRight();
//            Log.d("RicochetMeta", "hop0Fee (return):" + hop0Fee.longValue());

            Transaction txHop0 = getHop0Tx(utxos, hop0.longValue(), getDestinationAddress(index), hop0Fee.longValue(), samouraiFeeViaBIP47);
            if(txHop0 == null)    {
                return null;
            }

            int prevTxN = 0;
            for(int i = 0; i < txHop0.getOutputs().size(); i++)   {
                Script script = txHop0.getOutputs().get(i).getScriptPubKey();
                String address = new Script(script.getProgram()).getToAddress(MainNetParams.get()).toString();
//                Log.d("RicochetMeta", "address from script:" + address);
                if(address.equals(getDestinationAddress(index)))    {
                    prevTxN = i;
//                    Log.d("RicochetMeta", "tx output n:" + prevTxN);
                }
            }

            jHop.put("seq", 0);
            jHop.put("spend_amount", hop0.longValue());
            jHop.put("fee", hop0Fee.longValue());
            jHop.put("fee_per_hop", biFeePerHop.longValue());
            jHop.put("index", index);
            jHop.put("destination", getDestinationAddress(index));
            int prevIndex = index;
            index++;
            jHop.put("tx", new String(Hex.encode(txHop0.bitcoinSerialize())));
            jHop.put("hash", txHop0.getHash().toString());

            jHops.put(jHop);

            Transaction txHop = null;
            String prevTxHash = txHop0.getHash().toString();
            String scriptPubKey = Hex.toHexString(txHop0.getOutput(prevTxN).getScriptPubKey().getProgram());
            int _hop = 0;
            for (int i = (nbHops - 1); i >= 0; i--) {
                _hop++;
                BigInteger hopx = biSpend.add(biFeePerHop.multiply(BigInteger.valueOf((long) i)));

//                Log.d("RicochetMeta", "doing hop:" + _hop);
                txHop = getHopTx(prevTxHash, prevTxN, scriptPubKey, prevIndex, hopx.longValue(), _hop < nbHops ? getDestinationAddress(index) : strDestination);
                if(txHop == null)    {
                    return null;
                }

                jHop = new JSONObject();
                jHop.put("seq", (nbHops - i));
                jHop.put("spend_amount", hopx.longValue());
                jHop.put("fee", biFeePerHop.longValue());
                jHop.put("prev_tx_hash", prevTxHash);
                jHop.put("prev_tx_n", prevTxN);
                jHop.put("script", scriptPubKey);
                jHop.put("tx", new String(Hex.encode(txHop.bitcoinSerialize())));
                jHop.put("hash", txHop.getHash().toString());
                if(_hop < nbHops)    {
                    jHop.put("index", index);
                    jHop.put("destination", getDestinationAddress(index));
                    prevIndex = index;
                    index++;
                }
                else    {
                    jHop.put("destination", strDestination);
                }

                jHops.put(jHop);
                System.out.println(jHop.toString());

                prevTxHash = txHop.getHash().toString();
                prevTxN = 0;
                scriptPubKey = Hex.toHexString(txHop.getOutput(prevTxN).getScriptPubKey().getProgram());
            }

            jObj.put("hops", jHops);

            BigInteger totalAmount = hop0.add(hop0Fee);

            jObj.put("total_spend", totalAmount.longValue());

        }
        catch(JSONException je) {
            return null;
        }

        return jObj;

    }

    private String getDestinationAddress(int idx)    {

        String address = null;

        try {
            address = HD_WalletFactory.getInstance(context).get().getAccountAt(RICOCHET_ACCOUNT).getChain(0).getAddressAt(idx).getAddressString();

            String privkey = HD_WalletFactory.getInstance(context).get().getAccountAt(RICOCHET_ACCOUNT).getChain(0).getAddressAt(idx).getPrivateKeyString();
//            Log.d("RicochetMeta", "getDestinationAddress address:" + address);

        }
        catch(IOException ioe) {
            ;
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            ;
        }

        return address;
    }

    private Pair<List<UTXO>, BigInteger> getHop0UTXO(long spendAmount, int nbHops, long feePerHop) {

        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos();

        final List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalValueSelected = 0L;
        long totalSpendAmount = 0L;
        int selected = 0;

        // sort in ascending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        Collections.reverse(utxos);

        for(UTXO u : utxos)   {
            selectedUTXO.add(u);
            totalValueSelected += u.getValue();
            selected += u.getOutpoints().size();
//            Log.d("RicochetMeta", "selected:" + u.getValue());

            totalSpendAmount = spendAmount + samouraiFeeAmount.longValue() + (feePerHop * nbHops) + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(selected, 3).longValue();
//            Log.d("RicochetMeta", "totalSpendAmount:" + totalSpendAmount);
//            Log.d("RicochetMeta", "totalValueSelected:" + totalValueSelected);
            if(totalValueSelected >= totalSpendAmount)    {
//                Log.d("RicochetMeta", "breaking");
                break;
            }
        }

        if(selectedUTXO.size() < 1)    {
            return Pair.of(null, null);
        }
        else    {
            return Pair.of(selectedUTXO, FeeUtil.getInstance().estimatedFee(selected, 3));
        }
    }

    private Transaction getHop0Tx(List<UTXO> utxos, long spendAmount, String destination, long fee, boolean samouraiFeeViaBIP47) {

        List<MyTransactionOutPoint> unspent = new ArrayList<MyTransactionOutPoint>();
        long totalValueSelected = 0L;
        for(UTXO u : utxos)   {
            totalValueSelected += u.getValue();
            unspent.addAll(u.getOutpoints());
        }

//        Log.d("RicochetMeta", "spendAmount:" + spendAmount);
//        Log.d("RicochetMeta", "fee:" + fee);
//        Log.d("RicochetMeta", "totalValueSelected:" + totalValueSelected);

        long changeAmount = totalValueSelected - (spendAmount + fee);
//        Log.d("RicochetMeta", "changeAmount:" + changeAmount);
        HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();

        if(changeAmount > 0L)    {
            String change_address = null;
            try {
                change_address = HD_WalletFactory.getInstance(context).get().getAccount(0).getChange().getAddressAt(HD_WalletFactory.getInstance(context).get().getAccount(0).getChange().getAddrIdx()).getAddressString();
                receivers.put(change_address, BigInteger.valueOf(changeAmount));
            }
            catch(IOException ioe) {
                ;
            }
            catch(MnemonicException.MnemonicLengthException mle) {
                ;
            }
        }
        receivers.put(destination, BigInteger.valueOf(spendAmount - samouraiFeeAmount.longValue()));
        if(samouraiFeeViaBIP47)    {

            try {
                PaymentCode pcode = new PaymentCode(BIP47Meta.strSamouraiDonationPCode);
                PaymentAddress paymentAddress = BIP47Util.getInstance(context).getSendAddress(pcode, BIP47Meta.getInstance().getOutgoingIdx(BIP47Meta.strSamouraiDonationPCode));
                String strAddress = paymentAddress.getSendECKey().toAddress(MainNetParams.get()).toString();

                receivers.put(strAddress, samouraiFeeAmount);
            }
            catch(Exception e) {
                receivers.put(SendNotifTxFactory.SAMOURAI_NOTIF_TX_FEE_ADDRESS, samouraiFeeAmount);
            }

        }
        else    {
            receivers.put(SendNotifTxFactory.SAMOURAI_NOTIF_TX_FEE_ADDRESS, samouraiFeeAmount);
        }

        Transaction tx = SendFactory.getInstance(context).makeTransaction(0, unspent, receivers);
        tx = SendFactory.getInstance(context).signTransaction(tx);

        return tx;
    }

    private Transaction getHopTx(String prevTxHash, int prevTxN, String scriptPubKey, int prevIndex, long spendAmount, String destination) {

        Transaction tx = null;

        HD_Address address = null;
        try {
            address = HD_WalletFactory.getInstance(context).get().getAccountAt(RICOCHET_ACCOUNT).getChain(0).getAddressAt(prevIndex);
            ECKey ecKey = address.getECKey();
//            Log.d("RicochetMeta", "getHopTx address:" + ecKey.toAddress(MainNetParams.get()).toString());

            byte[] hashBytes = Hex.decode(prevTxHash);
            Sha256Hash txHash = new Sha256Hash(hashBytes);
            TransactionOutPoint outpoint = new TransactionOutPoint(MainNetParams.get(), prevTxN, txHash);
            TransactionInput input = new TransactionInput(MainNetParams.get(), null, Hex.decode(scriptPubKey), outpoint);

            Script outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(MainNetParams.get(), destination));
            TransactionOutput output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(spendAmount), outputScript.getProgram());

            tx = new Transaction(MainNetParams.get());
            tx.addInput(input);
            tx.addOutput(output);

            TransactionSignature sig = tx.calculateSignature(0, ecKey, Hex.decode(scriptPubKey), Transaction.SigHash.ALL, false);
            tx.getInput(0).setScriptSig(ScriptBuilder.createInputScript(sig, ecKey));

        }
        catch(IOException ioe) {
            ;
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            ;
        }

        return tx;
    }

}
