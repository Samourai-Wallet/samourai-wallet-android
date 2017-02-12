package com.samourai.wallet.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.Hash;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.R;

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
import org.json.simple.JSONValue;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIFactory	{

    private static final String dataDir = "cache";
    private static final String strXPUBFilename = "xpub.json";
    private static final String strFeesFilename = "fees.json";
    private static final String strUnspentsFilename = "unspents.json";

    private static long xpub_balance = 0L;
    private static HashMap<String, Long> xpub_amounts = null;
    private static HashMap<String,List<Tx>> xpub_txs = null;
    private static HashMap<String,Integer> unspentAccounts = null;
    private static HashMap<String,String> unspentPaths = null;
    private static HashMap<String,UTXO> utxos = null;

    private static long bip47_balance = 0L;
    private static HashMap<String, Long> bip47_amounts = null;
    private static HashMap<String, String> seenBIP47Tx = null;

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
            bip47_balance = 0L;
            bip47_amounts = new HashMap<String, Long>();
            seenBIP47Tx = new HashMap<String, String>();
            unspentPaths = new HashMap<String, String>();
            unspentAccounts = new HashMap<String, Integer>();
            utxos = new HashMap<String, UTXO>();
            instance = new APIFactory();
        }

        return instance;
    }

    public synchronized void reset() {
        xpub_balance = 0L;
        bip47_balance = 0L;
        xpub_amounts.clear();
        bip47_amounts.clear();
        xpub_txs.clear();
        seenBIP47Tx.clear();
        unspentPaths = new HashMap<String, String>();
        unspentAccounts = new HashMap<String, Integer>();
        utxos = new HashMap<String, UTXO>();
    }

    private synchronized JSONObject getXPUB(String[] xpubs) {

        JSONObject jsonObject  = null;

        try {
            /*
//                StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API);
            StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
//                url.append("v1/multiaddr?active=");
            url.append("multiaddr?active=");
            url.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
//                Log.i("APIFactory", "XPUB:" + url.toString());
            String response = WebUtil.getInstance(context).getURL(url.toString());
            */

            // use POST
            StringBuilder args = new StringBuilder();
            args.append("active=");
            args.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
            String response = WebUtil.getInstance(context).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "multiaddr?", args.toString());
//                Log.i("APIFactory", "XPUB response:" + response);
            try {
                jsonObject = new JSONObject(response);
                xpub_txs.put(xpubs[0], new ArrayList<Tx>());
                if(parseXPUB(jsonObject))    {
                    serialize(strXPUBFilename, response);
                }
                long amount0 = getXpubBalance();
                xpub_amounts.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), amount0 + bip47_balance);
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

    //
    // process all XPUB txs not having any BIP47 inputs. Use 'result' as total value.
    // process all BIP47 inputs as deductions to 'result' amount
    // txs with BIP47 outputs are discarded as they are processed in parseBIP47()
    //
    private synchronized boolean parseXPUB(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {

            if(jsonObject.has("wallet"))  {
                JSONObject walletObj = (JSONObject)jsonObject.get("wallet");
                if(walletObj.has("final_balance"))  {
                    xpub_balance = walletObj.getLong("final_balance");
                }
            }

            long latest_block = 0L;

            if(jsonObject.has("info"))  {
                JSONObject infoObj = (JSONObject)jsonObject.get("info");
                if(infoObj.has("latest_block"))  {
                    JSONObject blockObj = (JSONObject)infoObj.get("latest_block");
                    if(blockObj.has("height"))  {
                        latest_block = blockObj.getLong("height");
                    }
                }
            }

            if(jsonObject.has("addresses"))  {

                JSONArray addressesArray = (JSONArray)jsonObject.get("addresses");
                JSONObject addrObj = null;
                for(int i = 0; i < addressesArray.length(); i++)  {
                    addrObj = (JSONObject)addressesArray.get(i);
                    if(addrObj.has("final_balance") && addrObj.has("address"))  {
                        xpub_amounts.put((String)addrObj.get("address"), addrObj.getLong("final_balance"));
                        AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                        AddressFactory.getInstance().setHighestTxChangeIdx(AddressFactory.getInstance().xpub2account().get((String)addrObj.get("address")), addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
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
                    String path = null;
                    String input_xpub = null;
                    String output_xpub = null;
                    long move_amount = 0L;
                    long input_amount = 0L;
                    long output_amount = 0L;

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
                                input_amount += prevOutObj.getLong("value");
                                if(prevOutObj.has("xpub"))  {
                                    JSONObject xpubObj = (JSONObject)prevOutObj.get("xpub");
                                    addr = (String)xpubObj.get("m");
                                    input_xpub = addr;
                                }
                                else if(prevOutObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr((String)prevOutObj.get("addr")) != null)  {
                                    amount -= prevOutObj.getLong("value");
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
                            output_amount += outObj.getLong("value");
                            if(outObj.has("xpub"))  {
                                JSONObject xpubObj = (JSONObject)outObj.get("xpub");
                                addr = (String)xpubObj.get("m");
                                if(input_xpub != null && !input_xpub.equals(addr))    {
                                    output_xpub = addr;
                                    move_amount = outObj.getLong("value");
                                }
                            }
                            else  {
//                                _addr = (String)outObj.get("addr");
                            }
                        }
                    }

                    if(addr != null)  {

                        //
                        // test for MOVE from Shuffling -> Samourai account
                        //
                        if(input_xpub != null && output_xpub != null && !input_xpub.equals(output_xpub))    {

                            Tx tx = new Tx(hash, output_xpub, (move_amount + Math.abs(input_amount - output_amount)) * -1.0, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);
                            if(!xpub_txs.containsKey(input_xpub))  {
                                xpub_txs.put(input_xpub, new ArrayList<Tx>());
                            }
                            xpub_txs.get(input_xpub).add(tx);

                            Tx _tx = new Tx(hash, input_xpub, move_amount, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);
                            if(!xpub_txs.containsKey(output_xpub))  {
                                xpub_txs.put(output_xpub, new ArrayList<Tx>());
                            }
                            xpub_txs.get(output_xpub).add(_tx);

                        }
                        else    {

                            if(!seenBIP47Tx.containsKey(hash))    {
                                Tx tx = new Tx(hash, _addr, amount, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);
                                if(_addr != null && BIP47Meta.getInstance().getPCode4Addr(_addr) != null)    {
                                    tx.setPaymentCode(BIP47Meta.getInstance().getPCode4Addr(_addr));
                                }
                                if(!xpub_txs.containsKey(addr))  {
                                    xpub_txs.put(addr, new ArrayList<Tx>());
                                }
                                xpub_txs.get(addr).add(tx);

                                seenBIP47Tx.put(hash, "");
                            }
                        }

                    }
                }

            }

            return true;

        }

        return false;

    }
/*
    public JSONObject getNotifAddress(String addr) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
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

        if(jsonObject != null)  {

            if(jsonObject.has("txs"))  {

                JSONArray txArray = (JSONArray)jsonObject.get("txs");
                JSONObject txObj = null;
                for(int i = 0; i < txArray.length(); i++)  {
                    txObj = (JSONObject)txArray.get(i);

                    if(!txObj.has("block_height"))    {
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

    }
*/

    public JSONObject getNotifTx(String hash, String addr) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.CHAINSO_TX_PREV_OUT_URL);
            url.append(hash);
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
            StringBuilder url = new StringBuilder(WebUtil.CHAINSO_GET_RECEIVE_TX_URL);
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

        if(jsonObject != null)  {

            if(jsonObject.has("status") && jsonObject.getString("status").equals("success") && jsonObject.has("data"))  {

                JSONObject dataObj = jsonObject.getJSONObject("data");

                if(dataObj.has("txs"))    {

                    JSONArray txArray = dataObj.getJSONArray("txs");
                    JSONObject txObj = null;
                    for(int i = 0; i < txArray.length(); i++)  {
                        txObj = (JSONObject)txArray.get(i);

                        if(!txObj.has("confirmations") || (txObj.has("confirmations") && txObj.getLong("confirmations") < 1L))    {
                            return;
                        }

                        String hash = null;

                        if(txObj.has("txid"))  {
                            hash = (String)txObj.get("txid");
                            if(BIP47Meta.getInstance().getIncomingStatus(hash) == null)    {
                                getNotifTx(hash, addr);
                            }
                        }

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

            if(jsonObject.has("data"))  {

                JSONObject data = jsonObject.getJSONObject("data");

                if(data.has("confirmations") && data.getInt("confirmations") < 1)    {
                    return;
                }

                if(data.has("inputs"))    {

                    JSONArray inArray = (JSONArray)data.get("inputs");

                    if(inArray.length() > 0 && ((JSONObject)inArray.get(0)).has("script_hex"))    {
                        String strScript = ((JSONObject)inArray.get(0)).getString("script_hex");
                        Script script = new Script(Hex.decode(strScript));
//                        Log.i("APIFactory", "pubkey from script:" + Hex.toHexString(script.getPubKey()));
                        ECKey pKey = new ECKey(null, script.getPubKey(), true);
//                        Log.i("APIFactory", "address from script:" + pKey.toAddress(MainNetParams.get()).toString());
//                        Log.i("APIFactory", "uncompressed public key from script:" + Hex.toHexString(pKey.decompress().getPubKey()));

                        if(((JSONObject)inArray.get(0)).has("received_from"))    {
                            JSONObject received_from = ((JSONObject) inArray.get(0)).getJSONObject("received_from");

                            String strHash = received_from.getString("txid");
                            int idx = received_from.getInt("output_no");

                            byte[] hashBytes = Hex.decode(strHash);
//                            Hash hash = new Hash(hashBytes);
//                            hash.reverse();
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

                if(data.has("outputs"))  {
                    JSONArray outArray = (JSONArray)data.get("outputs");
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
                        if(outObj.has("script_hex"))  {
                            script = outObj.getString("script_hex");
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
            StringBuilder url = new StringBuilder(WebUtil.CHAINSO_TX_PREV_OUT_URL);
            url.append(hash);
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

        if(jsonObject != null)  {

            if(jsonObject.has("data"))  {

                JSONObject data = jsonObject.getJSONObject("data");

                if(data.has("confirmations"))    {
//                    Log.i("APIFactory", "returning notif tx confirmations:" + data.getInt("confirmations"));
                    return data.getInt("confirmations");
                }
                else    {
//                    Log.i("APIFactory", "returning 0 notif tx confirmations");
                    return 0;
                }

            }
            else if(jsonObject.has("status") && jsonObject.getString("status").equals("fail"))   {
                return -1;
            }
            else    {
                ;
            }

        }

        return 0;
    }

    public synchronized JSONObject getUnspentOutputs(String[] xpubs) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
            url.append("unspent?active=");
            url.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
//            Log.i("APIFactory", "unspent outputs:" + url.toString());
            String response = WebUtil.getInstance(context).getURL(url.toString());
//            Log.i("APIFactory", "unspent outputs response:" + response);
            if(parseUnspentOutputs(response))    {
                serialize(strUnspentsFilename, response);
            }
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    private synchronized boolean parseUnspentOutputs(String unspents)   {

        if(unspents != null)    {

            Map<String, Object> root = (Map<String, Object>)JSONValue.parse(unspents);
            if(root == null)    {
                return false;
            }
            List<Map<String, Object>> outputsRoot = (List<Map<String, Object>>)root.get("unspent_outputs");
            if(outputsRoot == null || outputsRoot.size() == 0) {
                return false;
            }

//            Log.d("APIFactory", "unspents found:" + outputsRoot.size());

            for (Map<String, Object> outDict : outputsRoot) {

                byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));

                Hash hash = new Hash(hashBytes);
                hash.reverse();
                Sha256Hash txHash = new Sha256Hash(hash.getBytes());

                int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
//            System.out.println("output n:" + txOutputN);
                BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
//            System.out.println("value:" + value);
                String script = (String)outDict.get("script");
                byte[] scriptBytes = Hex.decode(script);
//            System.out.println("script:" + (String)outDict.get("script"));
                int confirmations = ((Number)outDict.get("confirmations")).intValue();
//            System.out.println("confirmations:" + confirmations);

                try {
//                    String address = new BitcoinScript(scriptBytes).getAddress().toString();
                    String address = new Script(scriptBytes).getToAddress(MainNetParams.get()).toString();
//                    System.out.println("address:" + address);

                    if(outDict.containsKey("xpub"))    {
                        org.json.simple.JSONObject xpubObj = (org.json.simple.JSONObject)outDict.get("xpub");
                        String path = (String)xpubObj.get("path");
                        String m = (String)xpubObj.get("m");
//                        Log.d("APIFactory", "unspent:" + address + "," + path);
//                        Log.d("APIFactory", "m:" + m);
//                        Log.d("APIFactory", "account no.:" + AddressFactory.getInstance(context).xpub2account().get(m));
                        unspentPaths.put(address, path);
                        unspentAccounts.put(address, AddressFactory.getInstance(context).xpub2account().get(m));
                    }
                    else    {
//                        Log.d("APIFactory", "no path found for:" + address);
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

        return false;

    }

    public synchronized JSONObject getAddressInfo(String addr) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
            url.append("address/");
            url.append(addr);
            url.append("?format=json");

            String response = WebUtil.getInstance(context).getURL(url.toString());
            jsonObject = new JSONObject(response);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public synchronized JSONObject getTxInfo(String hash) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
            url.append("tx/");
            url.append(hash);
            url.append("?format=json");

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
            StringBuilder url = new StringBuilder(WebUtil.DYNAMIC_FEE_URL);
//            Log.i("APIFactory", "Dynamic fees:" + url.toString());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            Log.i("APIFactory", "Dynamic fees response:" + response);
            try {
                jsonObject = new JSONObject(response);
                if(parseDynamicFees(jsonObject))    {
                    serialize(strFeesFilename, response);
                }
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

    private synchronized boolean parseDynamicFees(JSONObject jsonObject) throws JSONException  {

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

        initFromCache();

        initWalletAmounts();

    }

    private synchronized void initWalletAmounts() {

        APIFactory.getInstance(context).reset();

        //
        // bip47 balance and tx
        //
        try {
            xpub_txs.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), new ArrayList<Tx>());

            APIFactory.getInstance(context).getBIP47(BIP47Meta.getInstance().getIncomingAddresses(false), false);

            List<String> addressStrings = new ArrayList<String>();
            for(String pcode : BIP47Meta.getInstance().getUnspentProviders())   {
                addressStrings.addAll(BIP47Meta.getInstance().getUnspentAddresses(context, pcode));
            }
            if(addressStrings.size() > 0)    {
                String[] s = addressStrings.toArray(new String[0]);
//                Log.i("APIFactory", addressStrings.toString());
                getUnspentOutputs(s);
            }
            else    {
//                Log.i("APIFactory", "no BIP47 unspents found");
            }

        }
        catch (IndexOutOfBoundsException ioobe) {
            ioobe.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //
        // bip44 balance and tx
        //
        try {
//            APIFactory.getInstance(context).preloadXPUB(HD_WalletFactory.getInstance(context).get().getXPUBs());
            APIFactory.getInstance(context).getXPUB(HD_WalletFactory.getInstance(context).get().getXPUBs());
            String[] s = new String[2];
            s[0] = HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr();
            s[1] = HD_WalletFactory.getInstance(context).get().getAccount(1).xpubstr();
            getUnspentOutputs(s);
            getDynamicFees();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
        }
        finally {
            ;
        }

    }

    private synchronized void initFromCache() {

        String strJSON = null;
        try {
            strJSON = deserialize(strXPUBFilename);
            JSONObject jsonObj = new JSONObject(strJSON);
//            Log.i("APIFactory", "deserialized:" + jsonObj.toString());
            if(jsonObj != null)    {
                parseXPUB(jsonObj);
            }
        }
        catch(JSONException je) {
            ;
        }
        catch(IOException ioe) {
            ;
        }

        strJSON = null;
        try {
            strJSON = deserialize(strFeesFilename);
            JSONObject jsonObj = new JSONObject(strJSON);
            if(jsonObj != null)    {
                parseDynamicFees(jsonObj);
            }
        }
        catch(JSONException je) {
            ;
        }
        catch(IOException ioe) {
            ;
        }

        strJSON = null;
        try {
            strJSON = deserialize(strUnspentsFilename);
            if(strJSON != null)    {
                parseUnspentOutputs(strJSON);
            }
        }
        catch(JSONException je) {
            ;
        }
        catch(IOException ioe) {
            ;
        }

    }

    private synchronized JSONObject getBIP47(String[] addresses, boolean simple) {

        JSONObject jsonObject  = null;

        StringBuilder args = new StringBuilder();
        args.append("active=");
        args.append(StringUtils.join(addresses, URLEncoder.encode("|")));
        if(simple) {
            args.append("&simple=true");
        }
        else {
            args.append("&symbol_btc=" + "BTC" + "&symbol_local=" + "USD");
        }

        try {
//            Log.i("APIFactory", "BIP47 multiaddr:" + args.toString());
//            String response = WebUtil.getInstance(null).postURL(WebUtil.SAMOURAI_API + "v1/multiaddr?", args.toString());
            String response = WebUtil.getInstance(context).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "multiaddr?", args.toString());
//            Log.i("APIFactory", "BIP47 multiaddr:" + response);
            jsonObject = new JSONObject(response);
            parseBIP47(jsonObject);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public synchronized int syncBIP47Incoming(String[] addresses) {

        JSONObject jsonObject = null;
        int ret = 0;

//        StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API);
        StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
        url.append("multiaddr?active=");
        url.append(StringUtils.join(addresses, URLEncoder.encode("|")));

        try {
//            Log.i("APIFactory", "BIP47 multiaddr:" + url.toString());
            String response = WebUtil.getInstance(context).getURL(url.toString());
//            Log.i("APIFactory", "BIP47 multiaddr:" + response);
            jsonObject = new JSONObject(response);

            if(jsonObject != null)  {

                if(jsonObject.has("addresses"))  {
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

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return ret;
    }

    public synchronized int syncBIP47Outgoing(String[] addresses) {

        JSONObject jsonObject  = null;
        int ret = 0;

//        StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API);
        StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
        url.append("multiaddr?active=");
        url.append(StringUtils.join(addresses, URLEncoder.encode("|")));

        try {
//            Log.i("APIFactory", "BIP47 multiaddr:" + url.toString());
            String response = WebUtil.getInstance(context).getURL(url.toString());
//            Log.i("APIFactory", "BIP47 multiaddr:" + response);
            jsonObject = new JSONObject(response);

            if(jsonObject != null)  {

                if(jsonObject.has("addresses"))  {
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

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return ret;
    }

    //
    // only process tx having a BIP47 output for which the value is used as total tx amount
    //
    private synchronized void parseBIP47(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {

            String account0_xpub = null;
            try {
                account0_xpub = HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr();
            }
            catch(IOException ioe) {
                ;
            }
            catch(MnemonicException.MnemonicLengthException mle) {
                ;
            }

            if(jsonObject.has("wallet"))  {
                JSONObject walletObj = (JSONObject)jsonObject.get("wallet");
                if(walletObj.has("final_balance"))  {
                    bip47_balance += walletObj.getLong("final_balance");
                }

            }

            long latest_block = 0L;

            if(jsonObject.has("info"))  {
                JSONObject infoObj = (JSONObject)jsonObject.get("info");
                if(infoObj.has("latest_block"))  {
                    JSONObject blockObj = (JSONObject)infoObj.get("latest_block");
                    if(blockObj.has("height"))  {
                        latest_block = blockObj.getLong("height");
                    }
                }
            }

            if(jsonObject.has("addresses"))  {
                JSONArray addressArray = (JSONArray)jsonObject.get("addresses");
                JSONObject addrObj = null;
                for(int i = 0; i < addressArray.length(); i++)  {
                    addrObj = (JSONObject)addressArray.get(i);
                    long amount = 0L;
                    String addr = null;
                    if(addrObj.has("address"))  {
                        addr = (String)addrObj.get("address");
                    }
                    if(addrObj.has("final_balance"))  {
                        amount = addrObj.getLong("final_balance");

                        String pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                        int idx = BIP47Meta.getInstance().getIdx4Addr(addr);
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
                    boolean hasBIP47Input = false;
                    boolean hasBIP47Output = false;

                    if(txObj.has("block_height"))  {
                        height = txObj.getLong("block_height");
                    }
                    else  {
                        height = -1L;  // 0 confirmations
                    }
                    if(txObj.has("hash"))  {
                        hash = (String)txObj.get("hash");
                    }
                    if(txObj.has("time"))  {
                        ts = txObj.getLong("time");
                    }

                    if(txObj.has("out"))  {
                        JSONArray outArray = (JSONArray)txObj.get("out");
                        JSONObject outObj = null;
                        for(int j = 0; j < outArray.length(); j++)  {
                            outObj = (JSONObject)outArray.get(j);

                            if(outObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr(outObj.getString("addr")) != null)   {
//                                Log.i("APIFactory", "found output:" + outObj.getString("addr"));
                                addr = outObj.getString("addr");
                                amount = outObj.getLong("value");
                                hasBIP47Output = true;
                            }
                            else    {
                                ;
                            }
                        }
                    }

                    if(addr != null)  {

//                        Log.i("APIFactory", "found BIP47 tx, value:" + amount + "," + addr);

                        if(hasBIP47Output && !seenBIP47Tx.containsKey(hash))    {
                            Tx tx = new Tx(hash, addr, amount, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);
                            if(hasBIP47Output && (BIP47Meta.getInstance().getPCode4Addr(addr) != null))    {
                                tx.setPaymentCode(BIP47Meta.getInstance().getPCode4Addr(addr));
                            }
                            if(!xpub_txs.containsKey(account0_xpub))  {
                                xpub_txs.put(account0_xpub, new ArrayList<Tx>());
                            }
                            xpub_txs.get(account0_xpub).add(tx);
                            seenBIP47Tx.put(hash, "");
                        }
                        else    {
                            ;
                        }

                    }

                }

            }

        }

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
            ret.addAll(txs);
        }

        Collections.sort(ret, new TxMostRecentDateComparator());

        return ret;
    }

    public synchronized UTXO getUnspentOutputsForSweep(String address) {

        String response = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
            url.append("unspent?active=" + address);
//            Log.i("APIFactory", "unspent outputs:" + url.toString());
            response = WebUtil.getInstance(context).getURL(url.toString());
//            Log.i("APIFactory", "unspent outputs response:" + response);
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

            Map<String, Object> root = (Map<String, Object>)JSONValue.parse(unspents);
            if(root == null)    {
                return null;
            }
            List<Map<String, Object>> outputsRoot = (List<Map<String, Object>>)root.get("unspent_outputs");
            if(outputsRoot == null || outputsRoot.size() == 0) {
                return null;
            }

//            Log.d("APIFactory", "unspents found:" + outputsRoot.size());

            for (Map<String, Object> outDict : outputsRoot) {

                byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));

                Hash hash = new Hash(hashBytes);
                hash.reverse();
                Sha256Hash txHash = new Sha256Hash(hash.getBytes());

                int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
//            System.out.println("output n:" + txOutputN);
                BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
//            System.out.println("value:" + value);
                String script = (String)outDict.get("script");
                byte[] scriptBytes = Hex.decode(script);
//            System.out.println("script:" + (String)outDict.get("script"));
                int confirmations = ((Number)outDict.get("confirmations")).intValue();
//            System.out.println("confirmations:" + confirmations);

                try {
//                    String address = new BitcoinScript(scriptBytes).getAddress().toString();
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

        return utxo;

    }

    private synchronized void serialize(String filename, String data) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, filename);

        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            out.write(data);
//            Log.i("APIFactory", "serialize:" + data);
        }
        finally {
            out.close();
        }

    }

    private synchronized String deserialize(String filename) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, filename);
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        String str = null;

        while((str = in.readLine()) != null) {
            sb.append(str);
        }

        in.close();

//        Log.i("APIFactory", "deserialize:" + sb.toString());

        return sb.toString();
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
