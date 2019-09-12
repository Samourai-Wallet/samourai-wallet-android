package com.samourai.wallet.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.auth0.android.jwt.JWT;
import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.JSONRPC.JSONRPC;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.util.UTXOUtil;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import io.reactivex.subjects.BehaviorSubject;

import static com.samourai.wallet.util.LogUtil.debug;
import static com.samourai.wallet.util.LogUtil.info;

public class APIFactory	{

    private static String APP_TOKEN = null;         // API app token
    private static String ACCESS_TOKEN = null;      // API access token
    private static long ACCESS_TOKEN_REFRESH = 300L;  // in seconds

    private static long xpub_balance = 0L;
    private static long xpub_postmix_balance = 0L;
    private static HashMap<String, Long> xpub_amounts = null;
    private static HashMap<String,List<Tx>> xpub_txs = null;
    private static HashMap<String,Integer> unspentAccounts = null;
    private static HashMap<String,Integer> unspentBIP49 = null;
    private static HashMap<String,Integer> unspentBIP84 = null;
    private static HashMap<String,Integer> unspentBIP84PostMix = null;
    private static HashMap<String,String> unspentPaths = null;
    private static HashMap<String,UTXO> utxos = null;
    private static HashMap<String,UTXO> utxosPostMix = null;

    private static JSONObject utxoObj0 = null;
    private static JSONObject utxoObj1 = null;

    private static HashMap<String, Long> bip47_amounts = null;
    public boolean walletInit = false;
    public BehaviorSubject<Long> walletBalanceObserver = BehaviorSubject.create();
    private static long latest_block_height = -1L;
    private static String latest_block_hash = null;

    private static APIFactory instance = null;

    private static Context context = null;

    private static AlertDialog alertDialog = null;

    private APIFactory()	{
        walletBalanceObserver.onNext(0L);
    }

    public static APIFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            xpub_amounts = new HashMap<String, Long>();
            xpub_txs = new HashMap<String,List<Tx>>();
            xpub_balance = 0L;
            bip47_amounts = new HashMap<String, Long>();
            unspentPaths = new HashMap<String, String>();
            unspentAccounts = new HashMap<String, Integer>();
            unspentBIP49 = new HashMap<String, Integer>();
            unspentBIP84 = new HashMap<String, Integer>();
            unspentBIP84PostMix = new HashMap<String, Integer>();
            utxos = new HashMap<String, UTXO>();
            utxosPostMix = new HashMap<String, UTXO>();
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
        unspentBIP49 = new HashMap<String, Integer>();
        unspentBIP84 = new HashMap<String, Integer>();
        unspentBIP84PostMix = new HashMap<String, Integer>();
        utxos = new HashMap<String, UTXO>();
        utxosPostMix = new HashMap<String, UTXO>();

        UTXOFactory.getInstance().clear();
    }

    public String getAccessToken() {
        if(ACCESS_TOKEN == null && APIFactory.getInstance(context).APITokenRequired())    {
            getToken(true);
        }
        return DojoUtil.getInstance(context).getDojoParams() == null ? "" : ACCESS_TOKEN;
    }

    public void setAccessToken(String accessToken) {
        ACCESS_TOKEN = accessToken;
    }

    public void setAppToken(String token)   {
        APP_TOKEN = token;
    }

    public String getAppToken()  {

        if(APP_TOKEN != null)    {
            return APP_TOKEN;
        }
        else    {
            return new String(getXORKey());
        }

    }

    public byte[] getXORKey() {

        if(APP_TOKEN != null)    {
            return APP_TOKEN.getBytes();
        }

        if(BuildConfig.XOR_1.length() > 0 && BuildConfig.XOR_2.length() > 0)    {
            byte[] xorSegments0 = Base64.decode(BuildConfig.XOR_1);
            byte[] xorSegments1 = Base64.decode(BuildConfig.XOR_2);
            return xor(xorSegments0, xorSegments1);
        }
        else    {
            return null;
        }
    }

    private byte[] xor(byte[] b0, byte[] b1) {

        byte[] ret = new byte[b0.length];

        for(int i = 0; i < b0.length; i++){
            ret[i] = (byte)(b0[i] ^ b1[i]);
        }

        return ret;

    }

    public long getAccessTokenRefresh() {
        return ACCESS_TOKEN_REFRESH;
    }

    public boolean stayingAlive()   {

        if(!AppUtil.getInstance(context).isOfflineMode() && APITokenRequired())    {

            if(APIFactory.getInstance(context).getAccessToken() == null)    {
                APIFactory.getInstance(context).getToken(false);
            }

            if(APIFactory.getInstance(context).getAccessToken() != null)    {
                JWT jwt = new JWT(APIFactory.getInstance(context).getAccessToken());
                if(jwt != null && jwt.isExpired(APIFactory.getInstance(context).getAccessTokenRefresh()))    {
                    if(APIFactory.getInstance(context).getToken(false))  {
                        return true;
                    }
                    else    {
                        return false;
                    }
                }
            }

            return false;

        }
        else    {
            return true;
        }

    }

    public synchronized boolean APITokenRequired()  {
        return DojoUtil.getInstance(context).getDojoParams() == null ? false : true;
    }

    public synchronized boolean getToken(boolean setupDojo) {

        if(!APITokenRequired())    {
            return true;
        }

        String _url = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET : WebUtil.SAMOURAI_API2;

        if(DojoUtil.getInstance(context).getDojoParams() != null || setupDojo)    {
            _url = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET_TOR :  WebUtil.SAMOURAI_API2_TOR;
        }

        debug("APIFactory", "getToken() url:" + _url);

        JSONObject jsonObject  = null;

        try {

            String response = null;

            if(AppUtil.getInstance(context).isOfflineMode())    {
                return true;
            }
            else if(!TorManager.getInstance(context).isRequired())    {
                // use POST
                StringBuilder args = new StringBuilder();
                args.append("apikey=");
                args.append(new String(getXORKey()));
                response = WebUtil.getInstance(context).postURL(_url + "auth/login?", args.toString());
                info("APIFactory", "API token response:" + response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("apikey", new String(getXORKey()));
                info("APIFactory", "API key (XOR):" + new String(getXORKey()));
                info("APIFactory", "API key url:" + _url);
                response = WebUtil.getInstance(context).tor_postURL(_url + "auth/login", args);
                info("APIFactory", "API token response:" + response);
            }

            try {
                jsonObject = new JSONObject(response);
                if(jsonObject != null && jsonObject.has("authorizations"))    {
                    JSONObject authObj = jsonObject.getJSONObject("authorizations");
                    if(authObj.has("access_token"))    {
                        info("APIFactory", "setting access token:" + authObj.getString("access_token"));
                        setAccessToken(authObj.getString("access_token"));
                        return true;
                    }
                }
            }
            catch(JSONException je) {
                je.printStackTrace();
                jsonObject = null;
                return false;
            }
        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private synchronized JSONObject getXPUB(String[] xpubs, boolean parse) {

        String _url = WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;

        try {

            String response = null;

            if(AppUtil.getInstance(context).isOfflineMode())    {
                response = PayloadUtil.getInstance(context).deserializeMultiAddr().toString();
            }
            else if(!TorManager.getInstance(context).isRequired())    {
                // use POST
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
                info("APIFactory", "XPUB:" + args.toString());
                args.append("&at=");
                args.append(getAccessToken());
                response = WebUtil.getInstance(context).postURL(_url + "multiaddr?", args.toString());
                info("APIFactory", "XPUB response:" + response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", StringUtils.join(xpubs, "|"));
                info("APIFactory", "XPUB:" + args.toString());
                args.put("at", getAccessToken());
                info("APIFactory", "XPUB access token:" + getAccessToken());
                response = WebUtil.getInstance(context).tor_postURL(_url + "multiaddr", args);
                info("APIFactory", "XPUB response:" + response);
            }

            try {
                jsonObject = new JSONObject(response);
                if(!parse)    {
                    return jsonObject;
                }
                xpub_txs.put(xpubs[0], new ArrayList<Tx>());
                parseXPUB(jsonObject);
                xpub_amounts.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), xpub_balance - BlockedUTXO.getInstance().getTotalValueBlocked0());
                walletBalanceObserver.onNext( xpub_balance - BlockedUTXO.getInstance().getTotalValueBlocked0());
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

    private synchronized JSONObject registerXPUB(String xpub, int purpose, String tag) {

        String _url = WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;

        try {

            String response = null;

            if(!TorManager.getInstance(context).isRequired())    {
                // use POST
                StringBuilder args = new StringBuilder();
                args.append("xpub=");
                args.append(xpub);
                args.append("&type=");
                if(PrefsUtil.getInstance(context).getValue(PrefsUtil.IS_RESTORE, false) == true)    {
                    args.append("restore");
                }
                else    {
                    args.append("new");
                }
                if(purpose == 49)    {
                    args.append("&segwit=");
                    args.append("bip49");
                }
                else if(purpose == 84)   {
                    args.append("&segwit=");
                    args.append("bip84");
                }
                else    {
                    ;
                }
                info("APIFactory", "XPUB:" + args.toString());
                args.append("&at=");
                args.append(getAccessToken());
                response = WebUtil.getInstance(context).postURL(_url + "xpub?", args.toString());
                info("APIFactory", "XPUB response:" + response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("xpub", xpub);
                if(PrefsUtil.getInstance(context).getValue(PrefsUtil.IS_RESTORE, false) == true)    {
                    args.put("type", "restore");
                }
                else    {
                    args.put("type", "new");
                }
                if(purpose == 49)    {
                    args.put("segwit", "bip49");
                }
                else if(purpose == 84)   {
                    args.put("segwit", "bip84");
                }
                else    {
                    ;
                }
                info("APIFactory", "XPUB:" + args.toString());
                args.put("at", getAccessToken());
                response = WebUtil.getInstance(context).tor_postURL(_url + "xpub", args);
                info("APIFactory", "XPUB response:" + response);
            }

            try {
                jsonObject = new JSONObject(response);
                info("APIFactory", "XPUB response:" + jsonObject.toString());
                if(jsonObject.has("status") && jsonObject.getString("status").equals("ok"))    {
                    if(tag != null)    {
                        PrefsUtil.getInstance(context).setValue(tag, true);
                        if(tag.equals(PrefsUtil.XPUBPOSTREG))    {
                            PrefsUtil.getInstance(context).removeValue(PrefsUtil.IS_RESTORE);
                        }
                    }
                    else if(purpose == 44)    {
                        PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB44REG, true);
                    }
                    else if(purpose == 49)    {
                        PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB49REG, true);
                    }
                    else if(purpose == 84)    {
                        PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB84REG, true);
                    }
                    else    {
                        ;
                    }

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

    private synchronized boolean parseXPUB(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {

            HashMap<String,Integer> pubkeys = new HashMap<String,Integer>();

            if(jsonObject.has("wallet"))  {
                JSONObject walletObj = (JSONObject)jsonObject.get("wallet");
                if(walletObj.has("final_balance"))  {
                    xpub_balance = walletObj.getLong("final_balance");
                    debug("APIFactory", "xpub_balance:" + xpub_balance);
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

                            if(addrObj.getString("address").equals(BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr()) ||
                                    addrObj.getString("address").equals(BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr()))    {
                                AddressFactory.getInstance().setHighestBIP84ReceiveIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                AddressFactory.getInstance().setHighestBIP84ChangeIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                BIP84Util.getInstance(context).getWallet().getAccount(0).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                BIP84Util.getInstance(context).getWallet().getAccount(0).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                            }
                            else if(addrObj.getString("address").equals(BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr()) ||
                                    addrObj.getString("address").equals(BIP49Util.getInstance(context).getWallet().getAccount(0).ypubstr()))    {
                                AddressFactory.getInstance().setHighestBIP49ReceiveIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                AddressFactory.getInstance().setHighestBIP49ChangeIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                BIP49Util.getInstance(context).getWallet().getAccount(0).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                BIP49Util.getInstance(context).getWallet().getAccount(0).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                            }
                            else if(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")) != null)    {
                                AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                AddressFactory.getInstance().setHighestTxChangeIdx(AddressFactory.getInstance().xpub2account().get((String)addrObj.get("address")), addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);

                                try {
                                    HD_WalletFactory.getInstance(context).get().getAccount(0).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                    HD_WalletFactory.getInstance(context).get().getAccount(0).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                }
                                catch(IOException | MnemonicException.MnemonicLengthException e) {
                                    ;
                                }
                            }
                            else    {
                                ;
                            }
                        }
                        else    {
                            long amount = 0L;
                            String addr = null;
                            addr = (String)addrObj.get("address");
                            amount = addrObj.getLong("final_balance");
                            String pcode = BIP47Meta.getInstance().getPCode4Addr(addr);

                            if(addrObj.has("pubkey"))    {
                                bip47Lookahead(pcode, addrObj.getString("pubkey"));
                            }

                            if(addr != null && addr.length() > 0 && pcode != null && pcode.length() > 0 && BIP47Meta.getInstance().getIdx4Addr(addr) != null)    {
                                int idx = BIP47Meta.getInstance().getIdx4Addr(addr);
                                if(amount > 0L)    {
                                    BIP47Meta.getInstance().addUnspent(pcode, idx);
                                    if(idx > BIP47Meta.getInstance().getIncomingIdx(pcode))    {
                                        BIP47Meta.getInstance().setIncomingIdx(pcode, idx);
                                    }
                                }
                                else    {
                                    if(addrObj.has("pubkey"))    {
                                        String pubkey = addrObj.getString("pubkey");
                                        if(pubkeys.containsKey(pubkey))    {
                                            int count = pubkeys.get(pubkey);
                                            count++;
                                            if(count == BIP47Meta.INCOMING_LOOKAHEAD)    {
                                                BIP47Meta.getInstance().removeUnspent(pcode, Integer.valueOf(idx));
                                            }
                                            else    {
                                                pubkeys.put(pubkey, count + 1);
                                            }
                                        }
                                        else    {
                                            pubkeys.put(pubkey, 1);
                                        }
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
            }

            if(jsonObject.has("txs"))  {

                List<String> seenHashes = new ArrayList<String>();

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

                    if(!seenHashes.contains(hash))  {
                        seenHashes.add(hash);
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
                        if(SentToFromBIP47Util.getInstance().getByHash(hash) != null)    {
                            tx.setPaymentCode(SentToFromBIP47Util.getInstance().getByHash(hash));
                        }
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

                List<String> hashesSentToViaBIP47 = SentToFromBIP47Util.getInstance().getAllHashes();
                if(hashesSentToViaBIP47.size() > 0)    {
                    for(String s : hashesSentToViaBIP47)    {
                        if(!seenHashes.contains(s)) {
                            SentToFromBIP47Util.getInstance().removeHash(s);
                        }
                    }
                }

            }

            try {
                PayloadUtil.getInstance(context).serializeMultiAddr(jsonObject);
            }
            catch(IOException | DecryptionException e) {
                ;
            }

            return true;

        }

        return false;

    }

    private synchronized void bip47Lookahead(String pcode, String addr)  {
        debug("APIFactory", "bip47Lookahead():" + addr);
        debug("APIFactory", "bip47Lookahead():" + pcode);
        debug("APIFactory", "bip47Lookahead():" + BIP47Meta.getInstance().getPCode4Addr(addr));
        int idx = BIP47Meta.getInstance().getIdx4Addr(addr);
        debug("APIFactory", "bip47Lookahead():" + idx);
        try {
            idx++;
            for (int i = idx; i < (idx + BIP47Meta.INCOMING_LOOKAHEAD); i++) {
                info("APIFactory", "receive from " + i + ":" + BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i));
                BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), i);
                BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), pcode.toString());
//                    _addrs.add(BIP47Util.getInstance(RefreshService.this).getReceivePubKey(new PaymentCode(pcode), i));
            }

            idx--;
            if (idx >= 2) {
                for (int i = idx; i >= (idx - (BIP47Meta.INCOMING_LOOKAHEAD - 1)); i--) {
                    info("APIFactory", "receive from " + i + ":" + BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i));
                    BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), i);
                    BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), pcode.toString());
//                        _addrs.add(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i));
                }
            }

//                addrs = _addrs.toArray(new String[_addrs.size()]);
        } catch (NullPointerException | NotSecp256k1Exception | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            ;
        }
    }

    /*
        public synchronized JSONObject deleteXPUB(String xpub, boolean bip49) {

            String _url = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET : WebUtil.SAMOURAI_API2;

            JSONObject jsonObject  = null;

            try {

                String response = null;
                ECKey ecKey = null;

                if(AddressFactory.getInstance(context).xpub2account().get(xpub) != null || xpub.equals(BIP49Util.getInstance(context).getWallet().getAccount(0).ypubstr()))    {

                    HD_Address addr = null;
                    if(bip49)    {
                        addr = BIP49Util.getInstance(context).getWallet().getAccountAt(0).getChange().getAddressAt(0);
                    }
                    else    {
                        addr = HD_WalletFactory.getInstance(context).get().getAccount(0).getChain(AddressFactory.CHANGE_CHAIN).getAddressAt(0);
                    }
                    ecKey = addr.getECKey();

                    if(ecKey != null && ecKey.hasPrivKey())    {

                        String sig = ecKey.signMessage(xpub);
                        String address = null;
                        if(bip49)    {
                            SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                            address = segwitAddress.getAddressAsString();
                        }
                        else    {
                            address = ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                        }

                        if(!TorUtil.getInstance(context).statusFromBroadcast())    {
                            StringBuilder args = new StringBuilder();
                            args.append("message=");
                            args.append(xpub);
                            args.append("address=");
                            args.append(address);
                            args.append("&signature=");
                            args.append(Uri.encode(sig));
                            info("APIFactory", "delete XPUB:" + args.toString());
                            response = WebUtil.getInstance(context).deleteURL(_url + "delete/" + xpub, args.toString());
                            info("APIFactory", "delete XPUB response:" + response);
                        }
                        else    {
                            HashMap<String,String> args = new HashMap<String,String>();
                            args.put("message", xpub);
                            args.put("address", address);
                            args.put("signature", Uri.encode(sig));
                            info("APIFactory", "delete XPUB:" + args.toString());
                            response = WebUtil.getInstance(context).tor_deleteURL(_url + "delete", args);
                            info("APIFactory", "delete XPUB response:" + response);
                        }

                        try {
                            jsonObject = new JSONObject(response);

                            if(jsonObject.has("status") && jsonObject.getString("status").equals("ok"))    {
                                ;
                            }

                        }
                        catch(JSONException je) {
                            je.printStackTrace();
                            jsonObject = null;
                        }

                    }
                }

            }
            catch(Exception e) {
                jsonObject = null;
                e.printStackTrace();
            }

            return jsonObject;
        }
    */
    public synchronized JSONObject lockXPUB(String xpub, int purpose, String tag) {

        String _url =  WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;

        try {

            String response = null;
            ECKey ecKey = null;

            if(AddressFactory.getInstance(context).xpub2account().get(xpub) != null ||
                    xpub.equals(BIP49Util.getInstance(context).getWallet().getAccount(0).ypubstr()) ||
                    xpub.equals(BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr()) ||
                    xpub.equals(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).zpubstr()) ||
                    xpub.equals(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).zpubstr())
            )    {

                HD_Address addr = null;
                switch(purpose)    {
                    case 49:
                        addr = BIP49Util.getInstance(context).getWallet().getAccountAt(0).getChange().getAddressAt(0);
                        break;
                    case 84:
                        if(tag != null && tag.equals(PrefsUtil.XPUBPRELOCK))    {
                            addr = BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).getChange().getAddressAt(0);
                        }
                        else if(tag != null && tag.equals(PrefsUtil.XPUBPOSTLOCK))   {
                            addr = BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).getChange().getAddressAt(0);
                        }
                        else    {
                            addr = BIP84Util.getInstance(context).getWallet().getAccountAt(0).getChange().getAddressAt(0);
                        }
                        break;
                    default:
                        addr = HD_WalletFactory.getInstance(context).get().getAccount(0).getChain(AddressFactory.CHANGE_CHAIN).getAddressAt(0);
                        break;
                }
                ecKey = addr.getECKey();

                if(ecKey != null && ecKey.hasPrivKey())    {

                    String sig = ecKey.signMessage("lock");
                    String address = null;
                    switch(purpose)    {
                        case 49:
                            SegwitAddress p2shp2wpkh = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                            address = p2shp2wpkh.getAddressAsString();
                            break;
                        case 84:
                            SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                            address = segwitAddress.getBech32AsString();
                            break;
                        default:
                            address = ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                            break;
                    }

                    if(!TorManager.getInstance(context).isRequired())    {
                        StringBuilder args = new StringBuilder();
                        args.append("address=");
                        args.append(address);
                        args.append("&signature=");
                        args.append(Uri.encode(sig));
                        args.append("&message=");
                        args.append("lock");
//                        info("APIFactory", "lock XPUB:" + args.toString());
                        args.append("&at=");
                        args.append(getAccessToken());
                        response = WebUtil.getInstance(context).postURL(_url + "xpub/" + xpub + "/lock/", args.toString());
//                        info("APIFactory", "lock XPUB response:" + response);
                    }
                    else    {
                        HashMap<String,String> args = new HashMap<String,String>();
                        args.put("address", address);
//                        args.put("signature", Uri.encode(sig));
                        args.put("signature", sig);
                        args.put("message", "lock");
                        args.put("at", getAccessToken());
                        info("APIFactory", "lock XPUB:" + _url);
                        info("APIFactory", "lock XPUB:" + args.toString());
                        response = WebUtil.getInstance(context).tor_postURL(_url + "xpub/" + xpub + "/lock/", args);
                        info("APIFactory", "lock XPUB response:" + response);
                    }

                    try {
                        jsonObject = new JSONObject(response);

                        if(jsonObject.has("status") && jsonObject.getString("status").equals("ok"))    {

                            if(tag != null)    {
                                PrefsUtil.getInstance(context).setValue(tag, true);
                            }
                            else    {
                                switch(purpose)    {
                                    case 49:
                                        PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB49LOCK, true);
                                        break;
                                    case 84:
                                        PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB84LOCK, true);
                                        break;
                                    default:
                                        PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB44LOCK, true);
                                        break;
                                }
                            }

                        }

                    }
                    catch(JSONException je) {
                        je.printStackTrace();
                        jsonObject = null;
                    }

                }
            }

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        return jsonObject;
    }

    public long getLatestBlockHeight()  {
        return latest_block_height;
    }

    public String getLatestBlockHash()  {
        return latest_block_hash;
    }

    public JSONObject getNotifTx(String hash, String addr) {

        String _url =  WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(_url);
            url.append("tx/");
            url.append(hash);
            url.append("?fees=1");
//            info("APIFactory", "Notif tx:" + url.toString());
            url.append("&at=");
            url.append(getAccessToken());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            info("APIFactory", "Notif tx:" + response);
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

        String _url = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET : WebUtil.SAMOURAI_API2;

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(_url);
            url.append("multiaddr?active=");
            url.append(addr);
//            info("APIFactory", "Notif address:" + url.toString());
            url.append("&at=");
            url.append(getAccessToken());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            info("APIFactory", "Notif address:" + response);
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

        info("APIFactory", "notif address:" + addr);
        info("APIFactory", "hash:" + hash);

        if(jsonObject != null)  {

            byte[] mask = null;
            byte[] payload = null;
            PaymentCode pcode = null;

            if(jsonObject.has("inputs"))    {

                JSONArray inArray = (JSONArray)jsonObject.get("inputs");

                if(inArray.length() > 0)    {
                    JSONObject objInput = (JSONObject)inArray.get(0);
                    byte[] pubkey = null;
                    String strScript = objInput.getString("sig");
                    info("APIFactory", "scriptsig:" + strScript);
                    if((strScript == null || strScript.length() == 0 || strScript.startsWith("160014")) && objInput.has("witness"))    {
                        JSONArray witnessArray = (JSONArray)objInput.get("witness");
                        if(witnessArray.length() == 2)    {
                            pubkey = Hex.decode((String)witnessArray.get(1));
                        }
                    }
                    else    {
                        Script script = new Script(Hex.decode(strScript));
                        info("APIFactory", "pubkey from script:" + Hex.toHexString(script.getPubKey()));
                        pubkey = script.getPubKey();
                    }
                    ECKey pKey = new ECKey(null, pubkey, true);
                    info("APIFactory", "address from script:" + pKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
//                        info("APIFactory", "uncompressed public key from script:" + Hex.toHexString(pKey.decompress().getPubKey()));

                    if(((JSONObject)inArray.get(0)).has("outpoint"))    {
                        JSONObject received_from = ((JSONObject) inArray.get(0)).getJSONObject("outpoint");

                        String strHash = received_from.getString("txid");
                        int idx = received_from.getInt("vout");

                        byte[] hashBytes = Hex.decode(strHash);
                        Sha256Hash txHash = new Sha256Hash(hashBytes);
                        TransactionOutPoint outPoint = new TransactionOutPoint(SamouraiWallet.getInstance().getCurrentNetworkParams(), idx, txHash);
                        byte[] outpoint = outPoint.bitcoinSerialize();
                        info("APIFactory", "outpoint:" + Hex.toHexString(outpoint));

                        try {
                            mask = BIP47Util.getInstance(context).getIncomingMask(pubkey, outpoint);
                            info("APIFactory", "mask:" + Hex.toHexString(mask));
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
                    info("APIFactory", "xlat_payload:" + Hex.toHexString(xlat_payload));

                    pcode = new PaymentCode(xlat_payload);
                    info("APIFactory", "incoming payment code:" + pcode.toString());

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
                    for(int i = 0; i < BIP47Meta.INCOMING_LOOKAHEAD; i++)   {
                        info("APIFactory", "receive from " + i + ":" + BIP47Util.getInstance(context).getReceivePubKey(pcode, i));
                        BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(pcode, i), i);
                        BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(pcode, i), pcode.toString());
                    }
                }
                catch(Exception e) {
                    ;
                }
            }

        }

    }

    public synchronized int getNotifTxConfirmations(String hash) {

        String _url =  WebUtil.getAPIUrl(context);

//        info("APIFactory", "Notif tx:" + hash);

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(_url);
            url.append("tx/");
            url.append(hash);
            url.append("?fees=1");
//            info("APIFactory", "Notif tx:" + url.toString());
            url.append("&at=");
            url.append(getAccessToken());
            String response = WebUtil.getInstance(null).getURL(url.toString());
//            info("APIFactory", "Notif tx:" + response);
            jsonObject = new JSONObject(response);
//            info("APIFactory", "Notif tx json:" + jsonObject.toString());

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

        String _url =  WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;
        String response = null;

        try {

            if(AppUtil.getInstance(context).isOfflineMode())    {
                response = PayloadUtil.getInstance(context).deserializeUTXO().toString();
            }
            else if(!TorManager.getInstance(context).isRequired())    {
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
                debug("APIFactory", "UTXO args:" + args.toString());
                args.append("&at=");
                args.append(getAccessToken());
                response = WebUtil.getInstance(context).postURL(_url + "unspent?", args.toString());
                debug("APIFactory", "UTXO:" + response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", StringUtils.join(xpubs, "|"));
                args.put("at", getAccessToken());
                response = WebUtil.getInstance(context).tor_postURL(_url + "unspent", args);
            }

            parseUnspentOutputs(response);

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        if(!AppUtil.getInstance(context).isOfflineMode())    {
            try {
                jsonObject = new JSONObject(response);
            }
            catch(JSONException je) {
                ;
            }
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
                    String path = null;

                    try {
                        String address = null;
                        if(Bech32Util.getInstance().isBech32Script(script))    {
                            address = Bech32Util.getInstance().getAddressFromScript(script);
                        }
                        else    {
                            address = new Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                        }

                        if(outDict.has("xpub"))    {
                            JSONObject xpubObj = (JSONObject)outDict.get("xpub");
                            path = (String)xpubObj.get("path");
                            String m = (String)xpubObj.get("m");
                            unspentPaths.put(address, path);
                            if(m.equals(BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr()))    {
                                unspentBIP49.put(address, 0);   // assume account 0
                            }
                            else if(m.equals(BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr()))    {
                                unspentBIP84.put(address, 0);   // assume account 0
                            }
                            else    {
                                unspentAccounts.put(address, AddressFactory.getInstance(context).xpub2account().get(m));
                            }
                        }
                        else if(outDict.has("pubkey"))    {
                            int idx = BIP47Meta.getInstance().getIdx4AddrLookup().get(outDict.getString("pubkey"));
                            BIP47Meta.getInstance().getIdx4AddrLookup().put(address, idx);
                            String pcode = BIP47Meta.getInstance().getPCode4AddrLookup().get(outDict.getString("pubkey"));
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(address, pcode);

                            debug("APIFactory", outDict.getString("pubkey") + "," + pcode);
                            debug("APIFactory", outDict.getString("pubkey") + "," + idx);
                        }
                        else    {
                            ;
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
                            utxo.setPath(path);
                            utxos.put(script, utxo);
                        }

                        if(!BlockedUTXO.getInstance().contains(txHash.toString(), txOutputN))    {

                            if(Bech32Util.getInstance().isBech32Script(script))    {
                                UTXOFactory.getInstance().addP2WPKH(script, utxos.get(script));
                            }
                            else if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress())    {
                                UTXOFactory.getInstance().addP2SH_P2WPKH(script, utxos.get(script));
                            }
                            else    {
                                UTXOFactory.getInstance().addP2PKH(script, utxos.get(script));
                            }

                        }

                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }

                }

                long amount = 0L;
                for(String key : utxos.keySet())   {
                    for(MyTransactionOutPoint out : utxos.get(key).getOutpoints())    {
                        debug("APIFactory", "utxo:" + out.getAddress() + "," + out.getValue());
                        debug("APIFactory", "utxo:" + utxos.get(key).getPath());
                        amount += out.getValue().longValue();
                    }
                }
                debug("APIFactory", "utxos by value (post-parse):" + amount);

                return true;

            }
            catch(JSONException je) {
                ;
            }

        }

        return false;

    }

    public synchronized JSONObject getAddressInfo(String addr) {

        return getXPUB(new String[] { addr }, false);

    }

    public synchronized JSONObject getTxInfo(String hash) {

        String _url = WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(_url);
            url.append("tx/");
            url.append(hash);
            url.append("?fees=true");
            url.append("&at=");
            url.append(getAccessToken());

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

        String _url =  WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;

        try {
            StringBuilder url = new StringBuilder(_url);
            url.append("header/");
            url.append(hash);
            url.append("?at=");
            url.append(getAccessToken());

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
            if(sel == 1)    {

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
                String _url =  WebUtil.getAPIUrl(context);
//            info("APIFactory", "Dynamic fees:" + url.toString());
                String response = null;
                if(!AppUtil.getInstance(context).isOfflineMode())    {
                    response = WebUtil.getInstance(null).getURL(_url + "fees" + "?at=" + getAccessToken());
                }
                else    {
                    response = PayloadUtil.getInstance(context).deserializeFees().toString();
                }
//            info("APIFactory", "Dynamic fees response:" + response);
                try {
                    jsonObject = new JSONObject(response);
                    parseDynamicFees_bitcoind(jsonObject);
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

//                debug("APIFactory", "high fee:" + FeeUtil.getInstance().getHighFee().getDefaultPerKB().toString());
//                debug("APIFactory", "suggested fee:" + FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().toString());
//                debug("APIFactory", "low fee:" + FeeUtil.getInstance().getLowFee().getDefaultPerKB().toString());
            }

            try {
                PayloadUtil.getInstance(context).serializeFees(jsonObject);
            }
            catch(IOException | DecryptionException e) {
                ;
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

                if(!AppUtil.getInstance(context).isOfflineMode()) {

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

        info("APIFactory", "initWallet()");

        initWalletAmounts();

    }

    private synchronized void initWalletAmounts() {

        APIFactory.getInstance(context).reset();

        List<String> addressStrings = new ArrayList<String>();
        String[] s = null;

        try {
            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB44REG, false) == false)    {
                registerXPUB(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), 44, null);
            }
            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB49REG, false) == false)    {
                registerXPUB(BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr(), 49, null);
            }
            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB84REG, false) == false)    {
                registerXPUB(BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr(), 84, null);
            }
            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBPREREG, false) == false)    {
                registerXPUB(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).xpubstr(), 84, PrefsUtil.XPUBPREREG);
            }
            if(PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBPOSTREG, false) == false)    {
                registerXPUB(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).xpubstr(), 84, PrefsUtil.XPUBPOSTREG);
            }

            xpub_txs.put(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr(), new ArrayList<Tx>());

            addressStrings.addAll(Arrays.asList(BIP47Meta.getInstance().getIncomingAddresses(false)));
            for(String _s : Arrays.asList(BIP47Meta.getInstance().getIncomingLookAhead(context)))   {
                if(!addressStrings.contains(_s))    {
                    addressStrings.add(_s);
                }
            }
            for(String pcode : BIP47Meta.getInstance().getUnspentProviders())   {
                for(String addr : BIP47Meta.getInstance().getUnspentAddresses(context, pcode))   {
                    if(!addressStrings.contains(addr))    {
                        addressStrings.add(addr);
                    }
                }
                List<Integer> idxs = BIP47Meta.getInstance().getUnspent(pcode);
                for(Integer idx : idxs)   {
                    String receivePubKey = BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), idx);
                    BIP47Meta.getInstance().getIdx4AddrLookup().put(receivePubKey, idx);
                    BIP47Meta.getInstance().getPCode4AddrLookup().put(receivePubKey, pcode.toString());
                    if(!addressStrings.contains(receivePubKey))    {
                        addressStrings.add(receivePubKey);
                    }
                }
            }
            if(addressStrings.size() > 0)    {
                s = addressStrings.toArray(new String[0]);
//                info("APIFactory", addressStrings.toString());
                utxoObj0 = getUnspentOutputs(s);
            }

            debug("APIFactory", "addresses:" + addressStrings.toString());

            HD_Wallet hdw = HD_WalletFactory.getInstance(context).get();
            if(hdw != null && hdw.getXPUBs() != null)    {
                String[] all = null;
                if(s != null && s.length > 0)    {
                    all = new String[hdw.getXPUBs().length + 2 + s.length];
                    all[0] = BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr();
                    all[1] = BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr();
                    System.arraycopy(hdw.getXPUBs(), 0, all, 2, hdw.getXPUBs().length);
                    System.arraycopy(s, 0, all, hdw.getXPUBs().length + 2, s.length);
                }
                else    {
                    all = new String[hdw.getXPUBs().length + 2];
                    all[0] = BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr();
                    all[1] = BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr();
                    System.arraycopy(hdw.getXPUBs(), 0, all, 2, hdw.getXPUBs().length);
                }
                APIFactory.getInstance(context).getXPUB(all, true);
                String[] xs = new String[4];
                xs[0] = HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr();
                xs[1] = HD_WalletFactory.getInstance(context).get().getAccount(1).xpubstr();
                xs[2] = BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr();
                xs[3] = BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr();
                utxoObj1 = getUnspentOutputs(xs);
                getDynamicFees();
            }

            try {
                List<JSONObject> utxoObjs = new ArrayList<JSONObject>();
                if(utxoObj0 != null)    {
                    utxoObjs.add(utxoObj0);
                }
                if(utxoObj1 != null)    {
                    utxoObjs.add(utxoObj1);
                }
                PayloadUtil.getInstance(context).serializeUTXO(utxoObjs);
            }
            catch(IOException | DecryptionException e) {
                ;
            }

            //
            //
            //
            List<String> seenOutputs = new ArrayList<String>();
            List<UTXO> _utxos = getUtxos(false);
            for(UTXO _u : _utxos)   {
                for(MyTransactionOutPoint _o : _u.getOutpoints())   {
                    seenOutputs.add(_o.getTxHash().toString() + "-" + _o.getTxOutputN());
                }
            }
            for(String _s : BlockedUTXO.getInstance().getNotDustedUTXO())   {
//                debug("APIFactory", "not dusted:" + _s);
                if(!seenOutputs.contains(_s))    {
                    BlockedUTXO.getInstance().removeNotDusted(_s);
//                    debug("APIFactory", "not dusted removed:" + _s);
                }
            }
            for(String _s : BlockedUTXO.getInstance().getBlockedUTXO().keySet())   {
//                debug("APIFactory", "blocked:" + _s);
                if(!seenOutputs.contains(_s))    {
                    BlockedUTXO.getInstance().remove(_s);
//                    debug("APIFactory", "blocked removed:" + _s);
                }
            }

            //            String strPreMix = BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).xpubstr();
            String strPostMix = BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).xpubstr();
//            JSONObject preMultiAddrObj = getRawXPUB(new String[] { strPreMix });
//            JSONObject preUnspentObj = getRawUnspentOutputs(new String[] { strPreMix });
//            debug("APIFactory", "pre-mix multi:" + preMultiAddrObj.toString());
//            debug("APIFactory", "pre-mix unspent:" + preUnspentObj.toString());
//            boolean parsedPreMultiAddr = parseMixXPUB(preMultiAddrObj);
//            boolean parsedPreUnspent = parsePostMixUnspentOutputs(preUnspentObj.toString());
            JSONObject postMultiAddrObj = getRawXPUB(new String[] { strPostMix });
            JSONObject postUnspentObj = getRawUnspentOutputs(new String[] { strPostMix });
            debug("APIFactory", "post-mix multi:" + postMultiAddrObj.toString());
            debug("APIFactory", "post-mix unspent:" + postUnspentObj.toString());
            boolean parsedPostMultiAddr = parseMixXPUB(postMultiAddrObj);
            boolean parsedPostUnspent = parsePostMixUnspentOutputs(postUnspentObj.toString());
//            debug("APIFactory", "post-mix multi:" + parsedPostMultiAddr);
//            debug("APIFactory", "post-mix unspent:" + parsedPostUnspent);
//            debug("APIFactory", "post-mix multi:" + getXpubPostMixBalance());
//            debug("APIFactory", "post-mix unspent:" + getUtxosPostMix().size());

            //
            //
            //
            List<String> seenOutputsPostMix = new ArrayList<String>();
            List<UTXO> _utxosPostMix = getUtxosPostMix(false);
            for(UTXO _u : _utxosPostMix)   {
                for(MyTransactionOutPoint _o : _u.getOutpoints())   {
                    seenOutputsPostMix.add(_o.getTxHash().toString() + "-" + _o.getTxOutputN());
                }
            }

            for(String _s : UTXOUtil.getInstance().getTags().keySet())   {
                if(!seenOutputsPostMix.contains(_s) && !seenOutputs.contains(_s))    {
                    UTXOUtil.getInstance().remove(_s);
                }
            }

            /*
            for(String _s : BlockedUTXO.getInstance().getNotDustedUTXO())   {
//                debug("APIFactory", "not dusted:" + _s);
                if(!seenOutputsPostMix.contains(_s))    {
                    BlockedUTXO.getInstance().removeNotDusted(_s);
//                    debug("APIFactory", "not dusted removed:" + _s);
                }
            }
            */
            for(String _s : BlockedUTXO.getInstance().getBlockedUTXOPostMix().keySet())   {
                debug("APIFactory", "blocked post-mix:" + _s);
                if(!seenOutputsPostMix.contains(_s))    {
                    BlockedUTXO.getInstance().removePostMix(_s);
                    debug("APIFactory", "blocked removed:" + _s);
                }
            }

        }
        catch (IndexOutOfBoundsException ioobe) {
            ioobe.printStackTrace();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        walletInit = true;

    }

    public synchronized int syncBIP47Incoming(String[] addresses) {

        JSONObject jsonObject = getXPUB(addresses, false);
        debug("APIFactory", "sync BIP47 incoming:" + jsonObject.toString());
        int ret = 0;

        try {

            if(jsonObject != null && jsonObject.has("addresses"))  {

                HashMap<String,Integer> pubkeys = new HashMap<String,Integer>();

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

                        if(addrObj.has("pubkey"))    {
                            addr = (String)addrObj.get("pubkey");
                            pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                            idx = BIP47Meta.getInstance().getIdx4Addr(addr);

                            BIP47Meta.getInstance().getIdx4AddrLookup().put(addrObj.getString("address"), idx);
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(addrObj.getString("address"), pcode);
                        }
                        else    {
                            addr = (String)addrObj.get("address");
                            pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                            idx = BIP47Meta.getInstance().getIdx4Addr(addr);
                        }

                        if(addrObj.has("final_balance"))  {
                            amount = addrObj.getLong("final_balance");
                            if(amount > 0L)    {
                                BIP47Meta.getInstance().addUnspent(pcode, idx);
                                info("APIFactory", "BIP47 incoming amount:" + idx + ", " + addr + ", " + amount);
                            }
                            else    {
                                if(addrObj.has("pubkey"))    {
                                    String pubkey = addrObj.getString("pubkey");
                                    if(pubkeys.containsKey(pubkey))    {
                                        int count = pubkeys.get(pubkey);
                                        count++;
                                        if(count == 3)    {
                                            BIP47Meta.getInstance().removeUnspent(pcode, Integer.valueOf(idx));
                                            info("APIFactory", "BIP47 remove unspent:" + pcode + ":" + idx);
                                        }
                                        else    {
                                            pubkeys.put(pubkey, count + 1);
                                        }
                                    }
                                    else    {
                                        pubkeys.put(pubkey, 1);
                                    }
                                }
                                else    {
                                    BIP47Meta.getInstance().removeUnspent(pcode, Integer.valueOf(idx));
                                }
                            }
                        }
                        if(addrObj.has("n_tx"))  {
                            nbTx = addrObj.getInt("n_tx");
                            if(nbTx > 0)    {
                                if(idx > BIP47Meta.getInstance().getIncomingIdx(pcode))    {
                                    BIP47Meta.getInstance().setIncomingIdx(pcode, idx);
                                }
                                info("APIFactory", "sync receive idx:" + idx + ", " + addr);
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

        JSONObject jsonObject = getXPUB(addresses, false);
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
                    info("APIFactory", "address object:" + addrObj.toString());

                    if(addrObj.has("pubkey"))    {
                        addr = (String)addrObj.get("pubkey");
                        pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                        idx = BIP47Meta.getInstance().getIdx4Addr(addr);

                        BIP47Meta.getInstance().getIdx4AddrLookup().put(addrObj.getString("address"), idx);
                        BIP47Meta.getInstance().getPCode4AddrLookup().put(addrObj.getString("address"), pcode);
                    }
                    else    {
                        addr = (String)addrObj.get("address");
                        pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                        idx = BIP47Meta.getInstance().getIdx4Addr(addr);
                    }

                    if(addrObj.has("n_tx"))  {
                        nbTx = addrObj.getInt("n_tx");
                        if(nbTx > 0)    {
                            if(idx >= BIP47Meta.getInstance().getOutgoingIdx(pcode))    {
                                info("APIFactory", "sync send idx:" + idx + ", " + addr);
                                BIP47Meta.getInstance().setOutgoingIdx(pcode, idx + 1);
                            }
                            ret++;
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
        return xpub_balance - BlockedUTXO.getInstance().getTotalValueBlocked0();
    }

    public void setXpubBalance(long value)  {
        xpub_balance = value;
        walletBalanceObserver.onNext(value);
    }

    public long getXpubPostMixBalance()  {
        return xpub_postmix_balance - BlockedUTXO.getInstance().getTotalValueBlockedPostMix();
    }

    public void setXpubPostMixBalance(long value)  {
        xpub_postmix_balance = value;
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

    public HashMap<String, Integer> getUnspentBIP49() {
        return unspentBIP49;
    }

    public HashMap<String, Integer> getUnspentBIP84() {
        return unspentBIP84;
    }

    public List<UTXO> getUtxos(boolean filter) {

        long amount = 0L;
        for(String key : utxos.keySet())   {
            for(MyTransactionOutPoint out : utxos.get(key).getOutpoints())    {
                debug("APIFactory", "utxo:" + out.getAddress() + "," + out.getValue());
                debug("APIFactory", "utxo:" + utxos.get(key).getPath());
                amount += out.getValue().longValue();
            }
        }
        debug("APIFactory", "utxos by value:" + amount);

        List<UTXO> unspents = new ArrayList<UTXO>();

        if(filter)    {
            for(String key : utxos.keySet())   {
                UTXO u = new UTXO();
                for(MyTransactionOutPoint out : utxos.get(key).getOutpoints())    {
                    if(!BlockedUTXO.getInstance().contains(out.getTxHash().toString(), out.getTxOutputN()))    {
                        u.getOutpoints().add(out);
                    }
                }
                if(u.getOutpoints().size() > 0)    {
                    unspents.add(u);
                }
            }
        }
        else    {
            unspents.addAll(utxos.values());
        }

        return unspents;
    }
    public List<UTXO> getUtxosWithLocalCache(boolean filter,boolean useLocalCache) {

        List<UTXO> unspents = new ArrayList<UTXO>();
        if(utxos.isEmpty() && useLocalCache){
            try {
                String response  = PayloadUtil.getInstance(context).deserializeUTXO().toString();
                parseUnspentOutputs(response);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        if(filter)    {
            for(String key : utxos.keySet())   {
                UTXO u = new UTXO();
                for(MyTransactionOutPoint out : utxos.get(key).getOutpoints())    {
                    if(!BlockedUTXO.getInstance().contains(out.getTxHash().toString(), out.getTxOutputN()))    {
                        u.getOutpoints().add(out);
                    }
                }
                if(u.getOutpoints().size() > 0)    {
                    unspents.add(u);
                }
            }
        }
        else    {
            unspents.addAll(utxos.values());
        }

        return unspents;
    }

    public List<UTXO> getUtxosPostMix(boolean filter) {

        List<UTXO> unspents = new ArrayList<UTXO>();

        if(filter)    {
            for(String key : utxosPostMix.keySet())   {
                UTXO u = new UTXO();
                for(MyTransactionOutPoint out : utxosPostMix.get(key).getOutpoints())    {
                    if(!BlockedUTXO.getInstance().containsPostMix(out.getTxHash().toString(), out.getTxOutputN()))    {
                        u.getOutpoints().add(out);
                    }
                }
                if(u.getOutpoints().size() > 0)    {
                    unspents.add(u);
                }
            }
        }
        else    {
            unspents.addAll(utxosPostMix.values());
        }

        return unspents;
    }

    public void setUtxos(HashMap<String, UTXO> utxos) {
        APIFactory.utxos = utxos;
    }

    public void setUtxosPostMix(HashMap<String, UTXO> utxos) {
        APIFactory.utxosPostMix = utxos;
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

        String _url =  WebUtil.getAPIUrl(context);

        try {

            String response = null;

            if(!TorManager.getInstance(context).isRequired())    {
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(address);
                args.append("&at=");
                args.append(getAccessToken());
//                debug("APIFactory", args.toString());
                response = WebUtil.getInstance(context).postURL(_url + "unspent?", args.toString());
//                debug("APIFactory", response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", address);
                args.put("at", getAccessToken());
//                debug("APIFactory", args.toString());
                response = WebUtil.getInstance(context).tor_postURL(_url + "unspent", args);
//                debug("APIFactory", response);
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

//            debug("APIFactory", "unspents found:" + outputsRoot.size());

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
                        String address = null;

                        if(Bech32Util.getInstance().isBech32Script(script))    {
                            address = Bech32Util.getInstance().getAddressFromScript(script);
                            debug("address parsed:", address);
                        }
                        else    {
                            address = new Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                        }

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

    //
    // use for post-mix
    //
    private synchronized JSONObject getRawXPUB(String[] xpubs) {

        String _url = WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;

        try {

            String response = null;

            if(AppUtil.getInstance(context).isOfflineMode())    {
                response = PayloadUtil.getInstance(context).deserializeMultiAddrPost().toString();
            }
            else if(!TorManager.getInstance(context).isRequired())    {
                // use POST
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
                info("APIFactory", "XPUB:" + args.toString());
                args.append("&at=");
                args.append(getAccessToken());
                response = WebUtil.getInstance(context).postURL(_url + "multiaddr?", args.toString());
                info("APIFactory", "XPUB response:" + response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", StringUtils.join(xpubs, "|"));
                info("APIFactory", "XPUB:" + args.toString());
                args.put("at", getAccessToken());
                response = WebUtil.getInstance(context).tor_postURL(_url + "multiaddr", args);
                info("APIFactory", "XPUB response:" + response);
            }

            try {
                jsonObject = new JSONObject(response);
                return jsonObject;
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

    public synchronized JSONObject getRawUnspentOutputs(String[] xpubs) {

        String _url =  WebUtil.getAPIUrl(context);

        JSONObject jsonObject  = null;
        String response = null;

        try {

            if(AppUtil.getInstance(context).isOfflineMode())    {
                response = PayloadUtil.getInstance(context).deserializeUTXOPost().toString();
            }
            else if(!TorManager.getInstance(context).isRequired())    {
                StringBuilder args = new StringBuilder();
                args.append("active=");
                args.append(StringUtils.join(xpubs, URLEncoder.encode("|", "UTF-8")));
                debug("APIFactory", "UTXO args:" + args.toString());
                args.append("&at=");
                args.append(getAccessToken());
                response = WebUtil.getInstance(context).postURL(_url + "unspent?", args.toString());
                debug("APIFactory", "UTXO:" + response);
            }
            else    {
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("active", StringUtils.join(xpubs, "|"));
                args.put("at", getAccessToken());
                response = WebUtil.getInstance(context).tor_postURL(_url + "unspent", args);
            }

        }
        catch(Exception e) {
            jsonObject = null;
            e.printStackTrace();
        }

        if(!AppUtil.getInstance(context).isOfflineMode())    {
            try {
                jsonObject = new JSONObject(response);
            }
            catch(JSONException je) {
                ;
            }
        }

        return jsonObject;
    }

    private synchronized boolean parseMixXPUB(JSONObject jsonObject) throws JSONException  {

        if(jsonObject != null)  {

            if(jsonObject.has("wallet"))  {
                JSONObject walletObj = (JSONObject)jsonObject.get("wallet");
                if(walletObj.has("final_balance"))  {
                    xpub_postmix_balance = walletObj.getLong("final_balance");
                    debug("APIFactory", "xpub_postmix_balance:" + xpub_postmix_balance);
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
//                            xpub_amounts.put((String)addrObj.get("address"), addrObj.getLong("final_balance"));
                            if(addrObj.getString("address").equals(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).xpubstr()) ||
                                    addrObj.getString("address").equals(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).zpubstr()))    {
                                AddressFactory.getInstance().setHighestPostReceiveIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                AddressFactory.getInstance().setHighestPostChangeIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                            }
                            else if(addrObj.getString("address").equals(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).xpubstr()) ||
                                    addrObj.getString("address").equals(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).zpubstr()))    {
                                AddressFactory.getInstance().setHighestPreReceiveIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                AddressFactory.getInstance().setHighestPreChangeIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                            }
                            else    {
                                ;
                            }
                        }
                    }
                }
            }

            try {
                PayloadUtil.getInstance(context).serializeMultiAddrPost(jsonObject);
            }
            catch(IOException | DecryptionException e) {
                ;
            }

            return true;

        }

        return false;

    }

    private synchronized boolean parsePostMixUnspentOutputs(String unspents)   {

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
                    String path = null;

                    try {
                        String address = Bech32Util.getInstance().getAddressFromScript(script);

                        if(outDict.has("xpub"))    {
                            JSONObject xpubObj = (JSONObject)outDict.get("xpub");
                            path = (String)xpubObj.get("path");
                            String m = (String)xpubObj.get("m");
                            unspentPaths.put(address, path);
                            if(m.equals(BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).xpubstr()))    {
                                unspentBIP84PostMix.put(address, WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix());
                            }
                        }
                        else    {
                            ;
                        }

                        // Construct the output
                        MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes, address);
                        outPoint.setConfirmations(confirmations);

                        if(utxosPostMix.containsKey(script))    {
                            utxosPostMix.get(script).getOutpoints().add(outPoint);
                        }
                        else    {
                            UTXO utxo = new UTXO();
                            utxo.getOutpoints().add(outPoint);
                            utxo.setPath(path);
                            utxosPostMix.put(script, utxo);
                        }

                        if(!BlockedUTXO.getInstance().containsPostMix(txHash.toString(), txOutputN))    {

                            UTXOFactory.getInstance().addPostMix(script, utxosPostMix.get(script));

                        }

                    }
                    catch(Exception e) {
                        ;
                    }

                }

                PayloadUtil.getInstance(context).serializeUTXOPost(jsonObj);

                return true;

            }
            catch(Exception j) {
                ;
            }

        }

        return false;

    }

}
