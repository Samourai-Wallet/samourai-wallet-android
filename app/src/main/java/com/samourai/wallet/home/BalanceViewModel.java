package com.samourai.wallet.home;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BalanceViewModel extends AndroidViewModel {

    private static final String TAG = "BalanceViewModel";
    private MutableLiveData<List<Tx>> txs = new MutableLiveData<>();
    private MutableLiveData<Long> balance = new MutableLiveData<>();
    private long latest_block_height = -1L;
    private long xpub_balance = 0L;
    private HashMap<String, Long> xpub_amounts = new HashMap<>();
    private String latest_block_hash = null;
    private HashMap<String, Long> bip47_amounts = null;
    private HashMap<String, List<Tx>> xpub_txs = new HashMap<>();

    private MutableLiveData<Boolean> toggleSat = new MutableLiveData<>();
    private CompositeDisposable compositeDisposables = new CompositeDisposable();

    public BalanceViewModel(@NonNull Application application) {
        super(application);
        toggleSat.setValue(false);

        try {
            JSONObject response = PayloadUtil.getInstance(application).deserializeMultiAddr();
            if (response != null) {
                Disposable disposable = parseXPUB(new JSONObject(response.toString()))
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(boo -> {
                            List<Tx> txes = new ArrayList<Tx>();
                            for (String key : xpub_txs.keySet()) {
                                List<Tx> txs = xpub_txs.get(key);
                                txes.addAll(txs);
                            }

                            Collections.sort(txes, new APIFactory.TxMostRecentDateComparator());
                            txs.postValue(txes);
                            toggleSat.setValue(false);
                            balance.postValue(xpub_balance- BlockedUTXO.getInstance().getTotalValueBlocked0());

                        }, error -> {
                            txs.postValue(new ArrayList<>());
                            toggleSat.setValue(false);
                            balance.postValue(0L);
                        });

                compositeDisposables.add(disposable);

            }
//            Log.i(TAG, "BalanceViewModel: ".concat(response));


        } catch (IOException | JSONException e) {
            e.printStackTrace();
            txs.setValue(new ArrayList<>());
            toggleSat.setValue(false);
            balance.setValue(0L);
        }
    }

    public LiveData<List<Tx>> getTxs() {

        return this.txs;
    }

    @Override
    protected void onCleared() {
        compositeDisposables.dispose();
        super.onCleared();
    }

    public LiveData<Long> getBalance() {
        return balance;
    }

    MutableLiveData<Boolean> getSatState() {
        return toggleSat;
    }

    void toggleSat() {

        if (toggleSat.getValue() == null) {
            this.toggleSat.setValue(false);
        } else {
            this.toggleSat.setValue(!toggleSat.getValue());
        }

    }

    public void setTx(List<Tx> txes) {
        if (txes == null || txes.size() == 0) {
            return;
        }
        this.txs.postValue(txes);
    }

    public void setBalance(Long balance) {
        this.balance.postValue(balance);
    }

    private Observable<Boolean> parseXPUB(JSONObject jsonObject) throws JSONException {

        return Observable.fromCallable(() -> {
            if (jsonObject != null) {

                HashMap<String, Integer> pubkeys = new HashMap<String, Integer>();

                if (jsonObject.has("wallet")) {
                    JSONObject walletObj = (JSONObject) jsonObject.get("wallet");
                    if (walletObj.has("final_balance")) {
                        xpub_balance = walletObj.getLong("final_balance");
                        Log.d("APIFactory", "xpub_balance:" + xpub_balance);
                    }
                }

                if (jsonObject.has("info")) {
                    JSONObject infoObj = (JSONObject) jsonObject.get("info");
                    if (infoObj.has("latest_block")) {
                        JSONObject blockObj = (JSONObject) infoObj.get("latest_block");
                        if (blockObj.has("height")) {
                            latest_block_height = blockObj.getLong("height");
                        }
                        if (blockObj.has("hash")) {
                            latest_block_hash = blockObj.getString("hash");
                        }
                    }
                }

                if (jsonObject.has("addresses")) {

                    JSONArray addressesArray = (JSONArray) jsonObject.get("addresses");
                    JSONObject addrObj = null;
                    for (int i = 0; i < addressesArray.length(); i++) {
                        addrObj = (JSONObject) addressesArray.get(i);
                        if (addrObj != null && addrObj.has("final_balance") && addrObj.has("address")) {
                            if (FormatsUtil.getInstance().isValidXpub((String) addrObj.get("address"))) {
                                xpub_amounts.put((String) addrObj.get("address"), addrObj.getLong("final_balance"));

                                if (addrObj.getString("address").equals(BIP84Util.getInstance(getApplication()).getWallet().getAccount(0).xpubstr()) ||
                                        addrObj.getString("address").equals(BIP84Util.getInstance(getApplication()).getWallet().getAccount(0).zpubstr())) {
                                    AddressFactory.getInstance().setHighestBIP84ReceiveIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                    AddressFactory.getInstance().setHighestBIP84ChangeIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                    BIP84Util.getInstance(getApplication()).getWallet().getAccount(0).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                    BIP84Util.getInstance(getApplication()).getWallet().getAccount(0).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                }
                                else if (addrObj.getString("address").equals(BIP49Util.getInstance(getApplication()).getWallet().getAccount(0).xpubstr()) ||
                                        addrObj.getString("address").equals(BIP49Util.getInstance(getApplication()).getWallet().getAccount(0).ypubstr())) {
                                    AddressFactory.getInstance().setHighestBIP49ReceiveIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                    AddressFactory.getInstance().setHighestBIP49ChangeIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                    BIP49Util.getInstance(getApplication()).getWallet().getAccount(0).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                    BIP49Util.getInstance(getApplication()).getWallet().getAccount(0).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                }
                                else if (AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")) != null) {
                                    AddressFactory.getInstance().setHighestTxReceiveIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                    AddressFactory.getInstance().setHighestTxChangeIdx(AddressFactory.getInstance().xpub2account().get((String) addrObj.get("address")), addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);

                                    try {
                                        HD_WalletFactory.getInstance(getApplication()).get().getAccount(0).getChain(0).setAddrIdx(addrObj.has("account_index") ? addrObj.getInt("account_index") : 0);
                                        HD_WalletFactory.getInstance(getApplication()).get().getAccount(0).getChain(1).setAddrIdx(addrObj.has("change_index") ? addrObj.getInt("change_index") : 0);
                                    } catch (IOException | MnemonicException.MnemonicLengthException e) {
                                        ;
                                    }
                                } else {
                                    ;
                                }
                            } else {
                                long amount = 0L;
                                String addr = null;
                                addr = (String) addrObj.get("address");
                                amount = addrObj.getLong("final_balance");
                                String pcode = BIP47Meta.getInstance().getPCode4Addr(addr);
                                if (addr != null && addr.length() > 0 && pcode != null && pcode.length() > 0 && BIP47Meta.getInstance().getIdx4Addr(addr) != null) {
                                    int idx = BIP47Meta.getInstance().getIdx4Addr(addr);
                                    if (amount > 0L) {
                                        BIP47Meta.getInstance().addUnspent(pcode, idx);
                                        if (idx > BIP47Meta.getInstance().getIncomingIdx(pcode)) {
                                            BIP47Meta.getInstance().setIncomingIdx(pcode, idx);
                                        }
                                    } else {
                                        if (addrObj.has("pubkey")) {
                                            String pubkey = addrObj.getString("pubkey");
                                            if (pubkeys.containsKey(pubkey)) {
                                                int count = pubkeys.get(pubkey);
                                                count++;
                                                if (count == 3) {
                                                    BIP47Meta.getInstance().removeUnspent(pcode, Integer.valueOf(idx));
                                                } else {
                                                    pubkeys.put(pubkey, count + 1);
                                                }
                                            } else {
                                                pubkeys.put(pubkey, 1);
                                            }
                                        } else {
                                            BIP47Meta.getInstance().removeUnspent(pcode, Integer.valueOf(idx));
                                        }
                                    }
                                    if (addr != null) {
                                        bip47_amounts.put(addr, amount);
                                    }
                                }

                            }
                        }
                    }
                }

                if (jsonObject.has("txs")) {

                    List<String> seenHashes = new ArrayList<String>();

                    JSONArray txArray = (JSONArray) jsonObject.get("txs");
                    JSONObject txObj = null;
                    for (int i = 0; i < txArray.length(); i++) {

                        txObj = (JSONObject) txArray.get(i);
                        long height = 0L;
                        long amount = 0L;
                        long ts = 0L;
                        String hash = null;
                        String addr = null;
                        String _addr = null;

                        if (txObj.has("block_height")) {
                            height = txObj.getLong("block_height");
                        } else {
                            height = -1L;  // 0 confirmations
                        }
                        if (txObj.has("hash")) {
                            hash = (String) txObj.get("hash");
                        }
                        if (txObj.has("result")) {
                            amount = txObj.getLong("result");
                        }
                        if (txObj.has("time")) {
                            ts = txObj.getLong("time");
                        }

                        if (!seenHashes.contains(hash)) {
                            seenHashes.add(hash);
                        }

                        if (txObj.has("inputs")) {
                            JSONArray inputArray = (JSONArray) txObj.get("inputs");
                            JSONObject inputObj = null;
                            for (int j = 0; j < inputArray.length(); j++) {
                                inputObj = (JSONObject) inputArray.get(j);
                                if (inputObj.has("prev_out")) {
                                    JSONObject prevOutObj = (JSONObject) inputObj.get("prev_out");
                                    if (prevOutObj.has("xpub")) {
                                        JSONObject xpubObj = (JSONObject) prevOutObj.get("xpub");
                                        addr = (String) xpubObj.get("m");
                                    } else if (prevOutObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr((String) prevOutObj.get("addr")) != null) {
                                        _addr = (String) prevOutObj.get("addr");
                                    } else {
                                        _addr = (String) prevOutObj.get("addr");
                                    }
                                }
                            }
                        }

                        if (txObj.has("out")) {
                            JSONArray outArray = (JSONArray) txObj.get("out");
                            JSONObject outObj = null;
                            for (int j = 0; j < outArray.length(); j++) {
                                outObj = (JSONObject) outArray.get(j);
                                if (outObj.has("xpub")) {
                                    JSONObject xpubObj = (JSONObject) outObj.get("xpub");
                                    addr = (String) xpubObj.get("m");
                                } else {
                                    _addr = (String) outObj.get("addr");
                                }
                            }
                        }

                        if (addr != null || _addr != null) {

                            if (addr == null) {
                                addr = _addr;
                            }

                            Tx tx = new Tx(hash, addr, amount, ts, (latest_block_height > 0L && height > 0L) ? (latest_block_height - height) + 1 : 0);
                            if (SentToFromBIP47Util.getInstance().getByHash(hash) != null) {
                                tx.setPaymentCode(SentToFromBIP47Util.getInstance().getByHash(hash));
                            }
                            if (BIP47Meta.getInstance().getPCode4Addr(addr) != null) {
                                tx.setPaymentCode(BIP47Meta.getInstance().getPCode4Addr(addr));
                            }
                            if (!xpub_txs.containsKey(addr)) {
                                xpub_txs.put(addr, new ArrayList<Tx>());
                            }
                            if (FormatsUtil.getInstance().isValidXpub(addr)) {
                                xpub_txs.get(addr).add(tx);
                            } else {
//                            xpub_txs.get(AddressFactory.getInstance().account2xpub().get(0)).add(tx);
                            }

                            if (height > 0L) {
                                RBFUtil.getInstance().remove(hash);
                            }

                        }
                    }

                    List<String> hashesSentToViaBIP47 = SentToFromBIP47Util.getInstance().getAllHashes();
                    if (hashesSentToViaBIP47.size() > 0) {
                        for (String s : hashesSentToViaBIP47) {
                            if (!seenHashes.contains(s)) {
                                SentToFromBIP47Util.getInstance().removeHash(s);
                            }
                        }
                    }

                }

                try {
                    PayloadUtil.getInstance(getApplication()).serializeMultiAddr(jsonObject);
                } catch (IOException | DecryptionException e) {
                    ;
                }

                return true;

            }

            return false;
        });

    }
}
