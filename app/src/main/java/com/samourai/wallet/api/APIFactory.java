package com.samourai.wallet.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
//import android.util.Log;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.PaymentAddress;

import org.apache.commons.lang.StringUtils;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class APIFactory	{

    private static long xpub_balance = 0L;
    private static HashMap<String, Long> xpub_amounts = null;
    private static HashMap<String,List<Tx>> xpub_txs = null;
    private static HashMap<String,List<String>> haveUnspentOuts = null;

    private static long bip47_balance = 0L;
    private static HashMap<String, Long> bip47_amounts = null;
    private static HashMap<String, String> seenBIP47Tx = null;

    private static boolean hasShuffled = false;

    private static APIFactory instance = null;

    private static Context context = null;

    private static AlertDialog alertDialog = null;

    private APIFactory()	{ ; }

    public static APIFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            xpub_amounts = new HashMap<String, Long>();
            xpub_txs = new HashMap<String,List<Tx>>();
            haveUnspentOuts = new HashMap<String,List<String>>();
            xpub_balance = 0L;
            hasShuffled = false;
            bip47_balance = 0L;
            bip47_amounts = new HashMap<String, Long>();
            seenBIP47Tx = new HashMap<String, String>();
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
        haveUnspentOuts.clear();
        seenBIP47Tx.clear();
    }

    private synchronized void preloadXPUB(String[] xpubs) {

        JSONObject jsonObject = null;

        for(int i = 0; i < xpubs.length; i++)   {
            if(!PrefsUtil.getInstance(context).getValue(xpubs[i], false))    {
                try {
                    StringBuilder url = new StringBuilder(WebUtil.SAMOURAI_API);
                    url.append("xpub?xpub=");
                    url.append(xpubs[i]);
                    String response = WebUtil.getInstance(null).getURL(url.toString());
//                Log.i("APIFactory", "XPUB response:" + response);
                    try {
                        jsonObject = new JSONObject(response);
                        if(jsonObject.has("status") && jsonObject.getString("status").equals("ok") &&
                                jsonObject.has("comment") && (jsonObject.getString("comment").equals("added") || jsonObject.getString("comment").equals("duplicate")))    {
                            PrefsUtil.getInstance(context).setValue(xpubs[i], true);
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
            }
        }

    }

    private synchronized JSONObject getXPUB(String[] xpubs) {

        JSONObject jsonObject  = null;

        for(int i = 0; i < xpubs.length; i++)   {
            try {
                StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
                url.append("multiaddr?active=");
                url.append(xpubs[i]);
//                Log.i("APIFactory", "XPUB:" + url.toString());
                String response = WebUtil.getInstance(null).getURL(url.toString());
//                Log.i("APIFactory", "XPUB response:" + response);
                try {
                    jsonObject = new JSONObject(response);
                    if(!HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr().equals(xpubs[i]))    {
                        xpub_txs.put(xpubs[i], new ArrayList<Tx>());
                    }
                    parseXPUB(jsonObject);
                    if(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr().equals(xpubs[i]))    {
                        long amount0 = xpub_amounts.get(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr());
                        xpub_amounts.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), amount0 + bip47_balance);
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
        }

        for(String xpub : xpub_amounts.keySet())   {
            xpub_balance += xpub_amounts.get(xpub);
        }

        return jsonObject;
    }

    private synchronized void parseXPUB(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {
/*
            if(jsonObject.has("wallet"))  {
                JSONObject walletObj = (JSONObject)jsonObject.get("wallet");
                if(walletObj.has("final_balance"))  {
                    xpub_balance = walletObj.getLong("final_balance");
                }
            }
*/
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
                    if(i == 1 && addrObj.has("n_tx") && addrObj.getInt("n_tx") > 0)  {
                        hasShuffled = true;
                    }
                    if(addrObj.has("final_balance") && addrObj.has("address"))  {
                        xpub_amounts.put((String)addrObj.get("address"), addrObj.getLong("final_balance"));
                        AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.getInt("account_index"));
                        AddressFactory.getInstance().setHighestTxChangeIdx(AddressFactory.getInstance().xpub2account().get((String)addrObj.get("address")), addrObj.getInt("change_index"));
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
                    boolean hasBIP47Input = false;

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
                                else if(prevOutObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr("addr") != null)  {
                                    hasBIP47Input = true;
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

                                path = (String)xpubObj.get("path");
                                if(outObj.has("spent"))  {
                                    if(outObj.getBoolean("spent") == false && outObj.has("addr"))  {
                                        if(!haveUnspentOuts.containsKey(addr))  {
                                            List<String> addrs = new ArrayList<String>();
                                            haveUnspentOuts.put(addr, addrs);
                                        }
                                        String data = path + "," + (String)outObj.get("addr");
                                        if(!haveUnspentOuts.get(addr).contains(data))  {
                                            haveUnspentOuts.get(addr).add(data);
                                        }
                                    }
                                }
                                String[] s = path.split("/");
                                if(s[1].equals("1") && hasBIP47Input)    {
                                    continue;
                                }
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

        }

    }

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
            url.append(StringUtils.join(xpubs, "|"));
//            Log.i("APIFactory", "unspent outputs:" + url.toString());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            Log.i("APIFactory", "unspent outputs response:" + response);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public synchronized JSONObject getAddressInfo(String addr) {

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
            url.append("address/");
            url.append(addr);
            url.append("?format=json");

            String response = WebUtil.getInstance(null).getURL(url.toString());
            jsonObject = new JSONObject(response);
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
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

    public synchronized void initWalletAmounts() {

        APIFactory.getInstance(context).reset();

        //
        // bip47 balance and tx
        //
        try {
            xpub_txs.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), new ArrayList<Tx>());

            APIFactory.getInstance(context).getBIP47(BIP47Meta.getInstance().getIncomingAddresses(), false);

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
            APIFactory.getInstance(context).preloadXPUB(HD_WalletFactory.getInstance(context).get().getXPUBs());
            APIFactory.getInstance(context).getXPUB(HD_WalletFactory.getInstance(context).get().getXPUBs());
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

        Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
        intent.putExtra("notfTx", false);
        intent.putExtra("fetch", false);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);


    }

    private synchronized JSONObject getBIP47(String[] addresses, boolean simple) {

        JSONObject jsonObject  = null;

        StringBuilder args = new StringBuilder();
        args.append("active=");
        args.append(StringUtils.join(addresses, "|"));
        if(simple) {
            args.append("&simple=true");
        }
        else {
            args.append("&symbol_btc=" + "BTC" + "&symbol_local=" + "USD");
        }

        try {
//            Log.i("APIFactory", "BIP47 multiaddr:" + args.toString());
            String response = WebUtil.getInstance(null).postURL(WebUtil.BLOCKCHAIN_DOMAIN + "multiaddr?", args.toString());
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

        StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
        url.append("multiaddr?active=");
        url.append(StringUtils.join(addresses, "|"));

        try {
//            Log.i("APIFactory", "BIP47 multiaddr:" + url.toString());
            String response = WebUtil.getInstance(null).getURL(url.toString());
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

        StringBuilder url = new StringBuilder(WebUtil.BLOCKCHAIN_DOMAIN);
        url.append("multiaddr?active=");
        url.append(StringUtils.join(addresses, "|"));

        try {
//            Log.i("APIFactory", "BIP47 multiaddr:" + url.toString());
            String response = WebUtil.getInstance(null).getURL(url.toString());
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

                    if(txObj.has("inputs"))  {
                        JSONArray inputArray = (JSONArray)txObj.get("inputs");
                        JSONObject inputObj = null;
                        for(int j = 0; j < inputArray.length(); j++)  {
                            inputObj = (JSONObject)inputArray.get(j);
                            if(inputObj.has("prev_out"))  {
                                JSONObject prevOutObj = (JSONObject)inputObj.get("prev_out");
                                if(prevOutObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr(prevOutObj.getString("addr")) != null)   {
//                                    Log.i("APIFactory", "found input:" + prevOutObj.getString("addr"));
                                    addr = prevOutObj.getString("addr");
                                    amount -= prevOutObj.getLong("value");
                                    hasBIP47Input = true;
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
                                String path = (String)xpubObj.get("path");
                                String[] s = path.split("/");
                                if(s[1].equals("1") && hasBIP47Input)    {
                                    amount += outObj.getLong("value");
                                }
                                //
                                // collect unspent outputs for each xpub
                                // store path info in order to generate private key later on
                                //
                                if(outObj.has("spent"))  {
                                    if(outObj.getBoolean("spent") == false && outObj.has("addr"))  {
                                        if(!haveUnspentOuts.containsKey(addr))  {
                                            List<String> addrs = new ArrayList<String>();
                                            haveUnspentOuts.put(addr, addrs);
                                        }
                                        String data = path + "," + (String)outObj.get("addr");
                                        if(!haveUnspentOuts.get(addr).contains(data))  {
                                            haveUnspentOuts.get(addr).add(data);
                                        }
                                    }
                                }
                            }
                            else if(outObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr(outObj.getString("addr")) != null)   {
//                                Log.i("APIFactory", "found output:" + outObj.getString("addr"));
                                addr = outObj.getString("addr");
                                amount += outObj.getLong("value");
                                hasBIP47Output = true;
                            }
                            else    {
                                ;
                            }
                        }
                    }

                    if(addr != null)  {

//                        Log.i("APIFactory", "found BIP47 tx, value:" + amount + "," + addr);

                        if((hasBIP47Output || hasBIP47Input) && !seenBIP47Tx.containsKey(hash))    {
                            Tx tx = new Tx(hash, addr, amount, ts, (latest_block > 0L && height > 0L) ? (latest_block - height) + 1 : 0);
                            if(!xpub_txs.containsKey(account0_xpub))  {
                                xpub_txs.put(account0_xpub, new ArrayList<Tx>());
                            }
                            if(hasBIP47Input || hasBIP47Output && (BIP47Meta.getInstance().getPCode4Addr(addr) != null))    {
                                tx.setPaymentCode(BIP47Meta.getInstance().getPCode4Addr(addr));
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

    public HashMap<String,List<String>> getUnspentOuts()  {
        return haveUnspentOuts;
    }

    public boolean hasShuffled() {
        return hasShuffled;
    }

    public void setHasShuffled(boolean shuffled) {
        hasShuffled = shuffled;
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
