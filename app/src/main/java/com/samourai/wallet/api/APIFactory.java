package com.samourai.wallet.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.samourai.wallet.JSONRPC.JSONRPC;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TorUtil;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.R;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.spongycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class APIFactory	{

    private static long xpub_balance = 0L;
    private static HashMap<String, Long> xpub_amounts = null;
    private static HashMap<String,List<Tx>> xpub_txs = null;
    private static HashMap<String,Integer> unspentAccounts = null;
    private static HashMap<String,String> unspentPaths = null;
    private static HashMap<String,UTXO> utxos = null;

    private static HashMap<String, Long> bip47_amounts = null;

    private static long latest_block_height = -1L;
    private static String latest_block_hash = null;

    private static APIFactory instance = null;

    private static Context context = null;

    private static AlertDialog alertDialog = null;

    private APIFactory()	{ ; }

    public static APIFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            xpub_amounts = new HashMap<String, Long>();
            xpub_txs = new HashMap<String,List<Tx>>();
            xpub_balance = 0L;
            bip47_amounts = new HashMap<String, Long>();
            unspentPaths = new HashMap<String, String>();
            unspentAccounts = new HashMap<String, Integer>();
            utxos = new HashMap<String, UTXO>();
            instance = new APIFactory();
        }

        return instance;
    }

    public synchronized void reset() {
        xpub_balance = 0L;
        xpub_amounts.clear();
        bip47_amounts.clear();
        xpub_txs.clear();
        unspentPaths = new HashMap<String, String>();
        unspentAccounts = new HashMap<String, Integer>();
        utxos = new HashMap<String, UTXO>();
    }

    private synchronized JSONObject getXPUB(String[] xpubs) {

        JSONObject jsonObject  = null;

        try {

            String response = null;

            if(!TorUtil.getInstance(context).statusFromBroadcast())    {
                // use POST
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
                Log.i("APIFactory", "XPUB:" + args.toString());
                response = WebUtil.getInstance(context).postURL(WebUtil.SAMOURAI_API2 + "multiaddr?", args.toString());
                Log.i("APIFactory", "XPUB response:" + response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", StringUtils.join(xpubs, "|"));
                Log.i("APIFactory", "XPUB:" + args.toString());
                response = WebUtil.getInstance(context).tor_postURL(WebUtil.SAMOURAI_API2 + "multiaddr", args);
                Log.i("APIFactory", "XPUB response:" + response);
            }

            try {
                jsonObject = new JSONObject(response);
                xpub_txs.put(xpubs[0], new ArrayList<Tx>());
                parseXPUB(jsonObject);
                xpub_amounts.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), xpub_balance);
            }
            catch(JSONException je) {
                je.printStackTrace();
                jsonObject = null;
            }
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    private synchronized boolean parseXPUB(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {

            if(jsonObject.has("wallet"))  {
                JSONObject walletObj = (JSONObject)jsonObject.get("wallet");
                if(walletObj.has("final_balance"))  {
                    xpub_balance = walletObj.getLong("final_balance");
                    Log.d("APIFactory", "xpub_balance:" + xpub_balance);
                }
            }

            if(jsonObject.has("info"))  {
                JSONObject infoObj = (JSONObject)jsonObject.get("info");
                if(infoObj.has("latest_block"))  {
                    JSONObject blockObj = (JSONObject)infoObj.get("latest_block");
                    if(blockObj.has("height"))  {
                        latest_block_height = blockObj.getLong("height");
                    }
                    if(blockObj.has("hash"))  {
                        latest_block_hash = blockObj.getString("hash");
                    }
                }
            }

            if(jsonObject.has("addresses"))  {

                JSONArray addressesArray = (JSONArray)jsonObject.get("addresses");
                JSONObject addrObj = null;
                for(int i = 0; i < addressesArray.length(); i++)  {
                    addrObj = (JSONObject)addressesArray.get(i);
                    if(addrObj != null && addrObj.has("final_balance") && addrObj.has("address"))  {
                        if(FormatsUtil.getInstance().isValidXpub((String)addrObj.get("address")))    {
                            xpub_amounts.put((String)addrObj.get("address"), addrObj.getLong("final_balance"));
                            AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                            AddressFactory.getInstance().setHighestTxChangeIdx(AddressFactory.getInstance().xpub2account().get((String)addrObj.get("address")), addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                        }
                        else    {
                            long amount = 0L;
                            String addr = null;
                            addr = (String)addrObj.get("address");
                            amount = addrObj.getLong("final_balance");
                            String pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                            int idx = BIP47Meta.getInstance().getIdx4Addr(addr);
                            if(pcode != null && pcode.length() > 0)    {
                                if(amount > 0L)    {
                                    BIP47Meta.getInstance().addUnspent(pcode, idx);
                                }
                                else    {
                                    BIP47Meta.getInstance().removeUnspent(pcode, Integer.valueOf(idx));
                                }
                            }
                            if(addr != null)  {
                                bip47_amounts.put(addr, amount);
                            }

                        }
                    }
                }
            }

            if(jsonObject.has("txs"))  {

                JSONArray txArray = (JSONArray)jsonObject.get("txs");
                JSONObject txObj = null;
                for(int i = 0; i < txArray.length(); i++)  {

                    txObj = (JSONObject)txArray.get(i);
                    long height = 0L;
                    long amount = 0L;
                    long ts = 0L;
                    String hash = null;
                    String addr = null;
                    String _addr = null;

                    if(txObj.has("block_height"))  {
                        height = txObj.getLong("block_height");
                    }
                    else  {
                        height = -1L;  // 0 confirmations
                    }
                    if(txObj.has("hash"))  {
                        hash = (String)txObj.get("hash");
                    }
                    if(txObj.has("result"))  {
                        amount = txObj.getLong("result");
                    }
                    if(txObj.has("time"))  {
                        ts = txObj.getLong("time");
                    }

                    if(txObj.has("inputs"))  {
                        JSONArray inputArray = (JSONArray)txObj.get("inputs");
                        JSONObject inputObj = null;
                        for(int j = 0; j < inputArray.length(); j++)  {
                            inputObj = (JSONObject)inputArray.get(j);
                            if(inputObj.has("prev_out"))  {
                                JSONObject prevOutObj = (JSONObject)inputObj.get("prev_out");
                                if(prevOutObj.has("xpub"))  {
                                    JSONObject xpubObj = (JSONObject)prevOutObj.get("xpub");
                                    addr = (String)xpubObj.get("m");
                                }
                                else if(prevOutObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr((String)prevOutObj.get("addr")) != null)  {
                                    _addr = (String)prevOutObj.get("addr");
                                }
                                else  {
                                    _addr = (String)prevOutObj.get("addr");
                                }
                            }
                        }
                    }

                    if(txObj.has("out"))  {
                        JSONArray outArray = (JSONArray)txObj.get("out");
                        JSONObject outObj = null;
                        for(int j = 0; j < outArray.length(); j++)  {
                            outObj = (JSONObject)outArray.get(j);
                            if(outObj.has("xpub"))  {
                                JSONObject xpubObj = (JSONObject)outObj.get("xpub");
                                addr = (String)xpubObj.get("m");
                            }
                            else  {
                                _addr = (String)outObj.get("addr");
                            }
                        }
                    }

                    if(addr != null || _addr != null)  {

                        if(addr == null)    {
                            addr = _addr;
                        }

                        Tx tx = new Tx(hash, addr, amount, ts, (latest_block_height > 0L && height > 0L) ? (latest_block_height - height) + 1 : 0);
                        if(BIP47Meta.getInstance().getPCode4Addr(addr) != null)    {
                            tx.setPaymentCode(BIP47Meta.getInstance().getPCode4Addr(addr));
                        }
                        if(!xpub_txs.containsKey(addr))  {
                            xpub_txs.put(addr, new ArrayList<Tx>());
                        }
                        if(FormatsUtil.getInstance().isValidXpub(addr))    {
                            xpub_txs.get(addr).add(tx);
                        }
                        else    {
                            xpub_txs.get(AddressFactory.getInstance().account2xpub().get(0)).add(tx);
                        }

                        if(height > 0L)    {
                            RBFUtil.getInstance().remove(hash);
                        }

                    }
                }

            }

            return true;

        }

        return false;

    }

    public long getLatestBlockHeight()  {
        return latest_block_height;
    }

    public String getLatestBlockHash()  {
        return latest_block_hash;
    }

    public JSONObject getNotifTx(String hash, String addr) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API2);
            url.append("tx/");
            url.append(hash);
            url.append("?fees=1");
//            Log.i("APIFactory", "Notif tx:" + url.toString());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            Log.i("APIFactory", "Notif tx:" + response);
            try {
                jsonObject = new JSONObject(response);
                parseNotifTx(jsonObject, addr, hash);
            }
            catch(JSONException je) {
                je.printStackTrace();
                jsonObject = null;
            }
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public JSONObject getNotifAddress(String addr) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API2);
            url.append("multiaddr?active=");
            url.append(addr);
//            Log.i("APIFactory", "Notif address:" + url.toString());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            Log.i("APIFactory", "Notif address:" + response);
            try {
                jsonObject = new JSONObject(response);
                parseNotifAddress(jsonObject, addr);
            }
            catch(JSONException je) {
                je.printStackTrace();
                jsonObject = null;
            }
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public void parseNotifAddress(JSONObject jsonObject, String addr) throws JSONException  {

        if(jsonObject != null && jsonObject.has("txs"))  {

            JSONArray txArray = jsonObject.getJSONArray("txs");
            JSONObject txObj = null;
            for(int i = 0; i < txArray.length(); i++)  {
                txObj = (JSONObject)txArray.get(i);

                if(!txObj.has("block_height") || (txObj.has("block_height") && txObj.getLong("block_height") < 1L))    {
                    return;
                }

                String hash = null;

                if(txObj.has("hash"))  {
                    hash = (String)txObj.get("hash");
                    if(BIP47Meta.getInstance().getIncomingStatus(hash) == null)    {
                        getNotifTx(hash, addr);
                    }
                }

            }

        }

    }

    public void parseNotifTx(JSONObject jsonObject, String addr, String hash) throws JSONException  {

        if(jsonObject != null)  {

            byte[] mask = null;
            byte[] payload = null;
            PaymentCode pcode = null;

            if(jsonObject.has("inputs"))    {

                JSONArray inArray = (JSONArray)jsonObject.get("inputs");

                if(inArray.length() > 0 && ((JSONObject)inArray.get(0)).has("sig"))    {
                    String strScript = ((JSONObject)inArray.get(0)).getString("sig");
                    Script script = new Script(Hex.decode(strScript));
//                        Log.i("APIFactory", "pubkey from script:" + Hex.toHexString(script.getPubKey()));
                    ECKey pKey = new ECKey(null, script.getPubKey(), true);
//                        Log.i("APIFactory", "address from script:" + pKey.toAddress(MainNetParams.get()).toString());
//                        Log.i("APIFactory", "uncompressed public key from script:" + Hex.toHexString(pKey.decompress().getPubKey()));

                    if(((JSONObject)inArray.get(0)).has("outpoint"))    {
                        JSONObject received_from = ((JSONObject) inArray.get(0)).getJSONObject("outpoint");

                        String strHash = received_from.getString("txid");
                        int idx = received_from.getInt("vout");

                        byte[] hashBytes = Hex.decode(strHash);
                        Sha256Hash txHash = new Sha256Hash(hashBytes);
                        TransactionOutPoint outPoint = new TransactionOutPoint(MainNetParams.get(), idx, txHash);
                        byte[] outpoint = outPoint.bitcoinSerialize();
//                            Log.i("APIFactory", "outpoint:" + Hex.toHexString(outpoint));

                        try {
                            mask = BIP47Util.getInstance(context).getIncomingMask(script.getPubKey(), outpoint);
//                                Log.i("APIFactory", "mask:" + Hex.toHexString(mask));
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }

                    }

                }

            }

            if(jsonObject.has("outputs"))  {
                JSONArray outArray = (JSONArray)jsonObject.get("outputs");
                JSONObject outObj = null;
                boolean isIncoming = false;
                String _addr = null;
                String script = null;
                String op_return = null;
                for(int j = 0; j < outArray.length(); j++)  {
                    outObj = (JSONObject)outArray.get(j);
                    if(outObj.has("address"))  {
                        _addr = outObj.getString("address");
                        if(addr.equals(_addr))    {
                            isIncoming = true;
                        }
                    }
                    if(outObj.has("scriptpubkey"))  {
                        script = outObj.getString("scriptpubkey");
                        if(script.startsWith("6a4c50"))    {
                            op_return = script;
                        }
                    }
                }
                if(isIncoming && op_return != null && op_return.startsWith("6a4c50"))    {
                    payload = Hex.decode(op_return.substring(6));
                }

            }

            if(mask != null && payload != null)    {
                try {
                    byte[] xlat_payload = PaymentCode.blind(payload, mask);
//                        Log.i("APIFactory", "xlat_payload:" + Hex.toHexString(xlat_payload));

                    pcode = new PaymentCode(xlat_payload);
//                        Log.i("APIFactory", "incoming payment code:" + pcode.toString());

                    if(!pcode.toString().equals(BIP47Util.getInstance(context).getPaymentCode().toString()) && pcode.isValid() && !BIP47Meta.getInstance().incomingExists(pcode.toString()))    {
                        BIP47Meta.getInstance().setLabel(pcode.toString(), "");
                        BIP47Meta.getInstance().setIncomingStatus(hash);
                    }

                }
                catch(AddressFormatException afe) {
                    afe.printStackTrace();
                }

            }

            //
            // get receiving addresses for spends from decoded payment code
            //
            if(pcode != null)    {
                try {

                    //
                    // initial lookup
                    //
                    for(int i = 0; i < 3; i++)   {
                        PaymentAddress receiveAddress = BIP47Util.getInstance(context).getReceiveAddress(pcode, i);
//                        Log.i("APIFactory", "receive from " + i + ":" + receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                        BIP47Meta.getInstance().setIncomingIdx(pcode.toString(), i, receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                        BIP47Meta.getInstance().getIdx4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString(), i);
                        BIP47Meta.getInstance().getPCode4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString(), pcode.toString());
//                        PaymentAddress sendAddress = BIP47Util.getInstance(context).getSendAddress(pcode, i);
//                        Log.i("APIFactory", "send to " + i + ":" + sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString());
                    }

                }
                catch(Exception e) {
                    ;
                }
            }

        }

    }

    public synchronized int getNotifTxConfirmations(String hash) {

//        Log.i("APIFactory", "Notif tx:" + hash);

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API2);
            url.append("tx/");
            url.append(hash);
            url.append("?fees=1");
//            Log.i("APIFactory", "Notif tx:" + url.toString());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            Log.i("APIFactory", "Notif tx:" + response);
            jsonObject = new JSONObject(response);
//            Log.i("APIFactory", "Notif tx json:" + jsonObject.toString());

            return parseNotifTx(jsonObject);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return 0;
    }

    public synchronized int parseNotifTx(JSONObject jsonObject) throws JSONException  {

        int cf = 0;

        if(jsonObject != null && jsonObject.has("block") && jsonObject.getJSONObject("block").has("height"))  {

            long latestBlockHeght = getLatestBlockHeight();
            long height = jsonObject.getJSONObject("block").getLong("height");

            cf = (int)((latestBlockHeght - height) + 1);

            if(cf < 0)    {
                cf = 0;
            }

        }

        return cf;
    }

    public synchronized JSONObject getUnspentOutputs(String[] xpubs) {

        JSONObject jsonObject  = null;

        try {

            String response = null;

            if(!TorUtil.getInstance(context).statusFromBroadcast())    {
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
                response = WebUtil.getInstance(context).postURL(WebUtil.SAMOURAI_API2 + "unspent?", args.toString());
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", StringUtils.join(xpubs, "|"));
                response = WebUtil.getInstance(context).tor_postURL(WebUtil.SAMOURAI_API2 + "unspent", args);
            }

            parseUnspentOutputs(response);

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    private synchronized boolean parseUnspentOutputs(String unspents)   {

        if(unspents != null)    {

            try {
                JSONObject jsonObj = new JSONObject(unspents);

                if(jsonObj == null || !jsonObj.has("unspent_outputs"))    {
                    return false;
                }
                JSONArray utxoArray = jsonObj.getJSONArray("unspent_outputs");
                if(utxoArray == null || utxoArray.length() == 0) {
                    return false;
                }

                for (int i = 0; i < utxoArray.length(); i++) {

                    JSONObject outDict = utxoArray.getJSONObject(i);

                    byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));
                    Sha256Hash txHash = Sha256Hash.wrap(hashBytes);
                    int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
                    BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
                    String script = (String)outDict.get("script");
                    byte[] scriptBytes = Hex.decode(script);
                    int confirmations = ((Number)outDict.get("confirmations")).intValue();

                    try {
                        String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();

                        if(outDict.has("xpub"))    {
                            JSONObject xpubObj = (JSONObject)outDict.get("xpub");
                            String path = (String)xpubObj.get("path");
                            String m = (String)xpubObj.get("m");
                            unspentPaths.put(address, path);
                            unspentAccounts.put(address, AddressFactory.getInstance(context).xpub2account().get(m));
                        }

                        // Construct the output
                        MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, address);
                        outPoint.setConfirmations(confirmations);

                        if(utxos.containsKey(script))    {
                            utxos.get(script).getOutpoints().add(outPoint);
                        }
                        else    {
                            UTXO utxo = new UTXO();
                            utxo.getOutpoints().add(outPoint);
                            utxos.put(script, utxo);
                        }

                    }
                    catch(Exception e) {
                        ;
                    }

                }

                return true;

            }
            catch(JSONException je) {
                ;
            }

        }

        return false;

    }

    public synchronized JSONObject getAddressInfo(String addr) {

        return getXPUB(new String[] { addr });

    }

    public synchronized JSONObject getTxInfo(String hash) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API2);
            url.append("tx/");
            url.append(hash);
            url.append("?fees=true");

            String response = WebUtil.getInstance(context).getURL(url.toString());
            jsonObject = new JSONObject(response);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public synchronized JSONObject getBlockHeader(String hash) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API2);
            url.append("header/");
            url.append(hash);

            String response = WebUtil.getInstance(context).getURL(url.toString());
            jsonObject = new JSONObject(response);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public synchronized JSONObject getDynamicFees() {

        JSONObject jsonObject  = null;

        try {
            int sel = PrefsUtil.getInstance(context).getValue(PrefsUtil.FEE_PROVIDER_SEL, 0);
            if(sel == 2)    {

                int[] blocks = new int[] { 2, 6, 24 };

                List<SuggestedFee> suggestedFees = new ArrayList<SuggestedFee>();

                JSONRPC jsonrpc = new JSONRPC(TrustedNodeUtil.getInstance().getUser(), TrustedNodeUtil.getInstance().getPassword(), TrustedNodeUtil.getInstance().getNode(), TrustedNodeUtil.getInstance().getPort());

                for(int i = 0; i < blocks.length; i++)   {
                    JSONObject feeObj = jsonrpc.getFeeEstimate(blocks[i]);
                    if(feeObj != null && feeObj.has("result"))    {
                        double fee = feeObj.getDouble("result");

                        SuggestedFee suggestedFee = new SuggestedFee();
                        suggestedFee.setDefaultPerKB(BigInteger.valueOf((long)(fee * 1e8)));
                        suggestedFee.setStressed(false);
                        suggestedFee.setOK(true);
                        suggestedFees.add(suggestedFee);
                    }
                }

                if(suggestedFees.size() > 0)    {
                    FeeUtil.getInstance().setEstimatedFees(suggestedFees);
                }

            }
            else    {
                StringBuilder url = new StringBuilder(sel == 0 ? WebUtil._21CO_FEE_URL : WebUtil.BITCOIND_FEE_URL);
//            Log.i("APIFactory", "Dynamic fees:" + url.toString());
                String response = WebUtil.getInstance(null).getURL(url.toString());
//            Log.i("APIFactory", "Dynamic fees response:" + response);
                try {
                    jsonObject = new JSONObject(response);
                    if(sel == 0)    {
                        parseDynamicFees_21(jsonObject);
                    }
                    else    {
                        parseDynamicFees_bitcoind(jsonObject);
                    }
                }
                catch(JSONException je) {
                    je.printStackTrace();
                    jsonObject = null;
                }
            }
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    private synchronized boolean parseDynamicFees_21(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {

            //
            // 21.co API
            //
            List<SuggestedFee> suggestedFees = new ArrayList<SuggestedFee>();

            if(jsonObject.has("fastestFee"))    {
                long fee = jsonObject.getInt("fastestFee");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if(jsonObject.has("halfHourFee"))    {
                long fee = jsonObject.getInt("halfHourFee");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if(jsonObject.has("hourFee"))    {
                long fee = jsonObject.getInt("hourFee");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if(suggestedFees.size() > 0)    {
                FeeUtil.getInstance().setEstimatedFees(suggestedFees);

//                Log.d("APIFactory", "high fee:" + FeeUtil.getInstance().getHighFee().getDefaultPerKB().toString());
//                Log.d("APIFactory", "suggested fee:" + FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().toString());
//                Log.d("APIFactory", "low fee:" + FeeUtil.getInstance().getLowFee().getDefaultPerKB().toString());
            }

            return true;

        }

        return false;

    }

    private synchronized boolean parseDynamicFees_bitcoind(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {

            //
            // bitcoind
            //
            List<SuggestedFee> suggestedFees = new ArrayList<SuggestedFee>();

            if(jsonObject.has("2"))    {
                long fee = jsonObject.getInt("2");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if(jsonObject.has("6"))    {
                long fee = jsonObject.getInt("6");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if(jsonObject.has("24"))    {
                long fee = jsonObject.getInt("24");
                SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(fee * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if(suggestedFees.size() > 0)    {
                FeeUtil.getInstance().setEstimatedFees(suggestedFees);

//                Log.d("APIFactory", "high fee:" + FeeUtil.getInstance().getHighFee().getDefaultPerKB().toString());
//                Log.d("APIFactory", "suggested fee:" + FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().toString());
//                Log.d("APIFactory", "low fee:" + FeeUtil.getInstance().getLowFee().getDefaultPerKB().toString());
            }

            return true;

        }

        return false;

    }

    public synchronized void validateAPIThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if(ConnectivityStatus.hasConnectivity(context)) {

                    try {
                        String response = WebUtil.getInstance(context).getURL(WebUtil.SAMOURAI_API_CHECK);

                        JSONObject jsonObject = new JSONObject(response);
                        if(!jsonObject.has("process"))    {
                            showAlertDialog(context.getString(R.string.api_error), false);
                        }

                    }
                    catch(Exception e) {
                        showAlertDialog(context.getString(R.string.cannot_reach_api), false);
                    }

                } else {
                    showAlertDialog(context.getString(R.string.no_internet), false);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();

            }
        }).start();
    }

    private void showAlertDialog(final String message, final boolean forceExit){

        if (!((Activity) context).isFinishing()) {

            if(alertDialog != null)alertDialog.dismiss();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message);
            builder.setCancelable(false);

            if(!forceExit) {
                builder.setPositiveButton(R.string.retry,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.dismiss();
                                //Retry
                                validateAPIThread();
                            }
                        });
            }

            builder.setNegativeButton(R.string.exit,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int id) {
                            d.dismiss();
                            ((Activity) context).finish();
                        }
                    });

            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public synchronized void initWallet()    {

        Log.i("APIFactory", "initWallet()");

        initWalletAmounts();

    }

    private synchronized void initWalletAmounts() {

        APIFactory.getInstance(context).reset();

        List<String> addressStrings = new ArrayList<String>();
        String[] s = null;

        try {
            xpub_txs.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), new ArrayList<Tx>());

            addressStrings.addAll(Arrays.asList(BIP47Meta.getInstance().getIncomingAddresses(false)));
            for(String pcode : BIP47Meta.getInstance().getUnspentProviders())   {
                for(String addr : BIP47Meta.getInstance().getUnspentAddresses(context, pcode))   {
                    if(!addressStrings.contains(addr))    {
                        addressStrings.add(addr);
                    }
                }
            }
            if(addressStrings.size() > 0)    {
                s = addressStrings.toArray(new String[0]);
//                Log.i("APIFactory", addressStrings.toString());
                getUnspentOutputs(s);
            }

            HD_Wallet hdw = HD_WalletFactory.getInstance(context).get();
            if(hdw != null && hdw.getXPUBs() != null)    {
                String[] all = null;
                if(s != null && s.length > 0)    {
                    all = new String[hdw.getXPUBs().length + s.length];
                    System.arraycopy(hdw.getXPUBs(), 0, all, 0, hdw.getXPUBs().length);
                    System.arraycopy(s, 0, all, hdw.getXPUBs().length, s.length);
                }
                else    {
                    all = hdw.getXPUBs();
                }
                APIFactory.getInstance(context).getXPUB(all);
                String[] xs = new String[2];
                xs[0] = HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr();
                xs[1] = HD_WalletFactory.getInstance(context).get().getAccount(1).xpubstr();
                getUnspentOutputs(xs);
                getDynamicFees();
            }

        }
        catch (IndexOutOfBoundsException ioobe) {
            ioobe.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized int syncBIP47Incoming(String[] addresses) {

        JSONObject jsonObject = getXPUB(addresses);
        int ret = 0;

        try {

            if(jsonObject != null && jsonObject.has("addresses"))  {

                JSONArray addressArray = (JSONArray)jsonObject.get("addresses");
                JSONObject addrObj = null;
                for(int i = 0; i < addressArray.length(); i++)  {
                    addrObj = (JSONObject)addressArray.get(i);
                    long amount = 0L;
                    int nbTx = 0;
                    String addr = null;
                    String pcode = null;
                    int idx = -1;
                    if(addrObj.has("address"))  {
                        addr = (String)addrObj.get("address");
                        pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                        idx = BIP47Meta.getInstance().getIdx4Addr(addr);

                        if(addrObj.has("final_balance"))  {
                            amount = addrObj.getLong("final_balance");
                            if(amount > 0L)    {
                                BIP47Meta.getInstance().addUnspent(pcode, idx);
                            }
                            else    {
                                BIP47Meta.getInstance().removeUnspent(pcode, Integer.valueOf(idx));
                            }
                        }
                        if(addrObj.has("n_tx"))  {
                            nbTx = addrObj.getInt("n_tx");
                            if(nbTx > 0)    {
//                                    Log.i("APIFactory", "sync receive idx:" + idx + ", " + addr);
                                ret++;
                            }
                        }

                    }
                }

            }

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return ret;
    }

    public synchronized int syncBIP47Outgoing(String[] addresses) {

        JSONObject jsonObject = getXPUB(addresses);
        int ret = 0;

        try {

            if(jsonObject != null && jsonObject.has("addresses"))  {

                JSONArray addressArray = (JSONArray)jsonObject.get("addresses");
                JSONObject addrObj = null;
                for(int i = 0; i < addressArray.length(); i++)  {
                    addrObj = (JSONObject)addressArray.get(i);
                    int nbTx = 0;
                    String addr = null;
                    String pcode = null;
                    int idx = -1;
                    if(addrObj.has("address"))  {
                        addr = (String)addrObj.get("address");
                        pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                        idx = BIP47Meta.getInstance().getIdx4Addr(addr);

                        if(addrObj.has("n_tx"))  {
                            nbTx = addrObj.getInt("n_tx");
                            if(nbTx > 0)    {
                                int stored = BIP47Meta.getInstance().getOutgoingIdx(pcode);
                                if(idx >= stored)    {
//                                        Log.i("APIFactory", "sync send idx:" + idx + ", " + addr);
                                    BIP47Meta.getInstance().setOutgoingIdx(pcode, idx + 1);
                                }
                                ret++;
                            }

                        }

                    }
                }

            }

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return ret;
    }

    public long getXpubBalance()  {
        return xpub_balance;
    }

    public void setXpubBalance(long value)  {
        xpub_balance = value;
    }

    public HashMap<String,Long> getXpubAmounts()  {
        return xpub_amounts;
    }

    public HashMap<String,List<Tx>> getXpubTxs()  {
        return xpub_txs;
    }

    public HashMap<String, String> getUnspentPaths() {
        return unspentPaths;
    }

    public HashMap<String, Integer> getUnspentAccounts() {
        return unspentAccounts;
    }

    public List<UTXO> getUtxos() {

        List<UTXO> unspents = new ArrayList<UTXO>();
        unspents.addAll(utxos.values());
        return unspents;

    }

    public void setUtxos(HashMap<String, UTXO> utxos) {
        APIFactory.utxos = utxos;
    }

    public synchronized List<Tx> getAllXpubTxs()  {

        List<Tx> ret = new ArrayList<Tx>();
        for(String key : xpub_txs.keySet())  {
            List<Tx> txs = xpub_txs.get(key);
            for(Tx tx : txs)   {
                ret.add(tx);
            }
        }

        Collections.sort(ret, new TxMostRecentDateComparator());

        return ret;
    }

    public synchronized UTXO getUnspentOutputsForSweep(String address) {

        try {

            String response = null;

            if(!TorUtil.getInstance(context).statusFromBroadcast())    {
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(address);
                response = WebUtil.getInstance(context).postURL(WebUtil.SAMOURAI_API2 + "unspent?", args.toString());
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", address);
                response = WebUtil.getInstance(context).tor_postURL(WebUtil.SAMOURAI_API2 + "unspent", args);
            }

            return parseUnspentOutputsForSweep(response);

        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private synchronized UTXO parseUnspentOutputsForSweep(String unspents)   {

        UTXO utxo = null;

        if(unspents != null)    {

            try {
                JSONObject jsonObj = new JSONObject(unspents);

                if(jsonObj == null || !jsonObj.has("unspent_outputs"))    {
                    return null;
                }
                JSONArray utxoArray = jsonObj.getJSONArray("unspent_outputs");
                if(utxoArray == null || utxoArray.length() == 0) {
                    return null;
                }

//            Log.d("APIFactory", "unspents found:" + outputsRoot.size());

                for (int i = 0; i < utxoArray.length(); i++) {

                    JSONObject outDict = utxoArray.getJSONObject(i);

                    byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));
                    Sha256Hash txHash = Sha256Hash.wrap(hashBytes);
                    int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
                    BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
                    String script = (String)outDict.get("script");
                    byte[] scriptBytes = Hex.decode(script);
                    int confirmations = ((Number)outDict.get("confirmations")).intValue();

                    try {
                        String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();

                        // Construct the output
                        MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, address);
                        outPoint.setConfirmations(confirmations);
                        if(utxo == null)    {
                            utxo = new UTXO();
                        }
                        utxo.getOutpoints().add(outPoint);

                    }
                    catch(Exception e) {
                        ;
                    }

                }

            }
            catch(JSONException je) {
                ;
            }

        }

        return utxo;

    }

    public static class TxMostRecentDateComparator implements Comparator<Tx> {

        public int compare(Tx t1, Tx t2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            int ret = 0;

            if(t1.getTS() > t2.getTS()) {
                ret = BEFORE;
            }
            else if(t1.getTS() < t2.getTS()) {
                ret = AFTER;
            }
            else    {
                ret = EQUAL;
            }

            return ret;
        }

    }

}
