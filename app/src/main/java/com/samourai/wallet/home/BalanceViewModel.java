package com.samourai.wallet.home;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

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
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.PrefsUtil;
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

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BalanceViewModel extends AndroidViewModel {

    private static final String TAG = "BalanceViewModel";
    private MutableLiveData<List<Tx>> txs = new MutableLiveData<>();
    private MutableLiveData<Long> balance = new MutableLiveData<>();
    private int account = 0;

    Context mContext = this.getApplication();
    SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "com.samourai.wallet_preferences" ;
    public static final String IS_SAT_KEY = "IS_SAT";

    private MutableLiveData<Boolean> toggleSat = new MutableLiveData<>();
    private CompositeDisposable compositeDisposables = new CompositeDisposable();

    public BalanceViewModel(@NonNull Application application) {
        super(application);
        boolean is_sat_prefs = PrefsUtil.getInstance(this.mContext).getValue(PrefsUtil.IS_SAT, true);
        toggleSat.setValue(is_sat_prefs);
    }

    public LiveData<List<Tx>> getTxs() {
        return this.txs;
    }

   public void loadOfflineData() {
        boolean is_sat_prefs = PrefsUtil.getInstance(this.mContext).getValue(PrefsUtil.IS_SAT, true);
        try {
            JSONObject response = new JSONObject("{}");

            if (account == 0) {
                response = PayloadUtil.getInstance(getApplication()).deserializeMultiAddr();

            }
            if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix()) {
                response = PayloadUtil.getInstance(getApplication()).deserializeMultiAddrMix();
            }

            if (response != null) {

                Observable<Pair<List<Tx>, Long>> parser = account == 0 ? APIFactory.getInstance(getApplication()).parseXPUBObservable(new JSONObject(response.toString())) : APIFactory.getInstance(getApplication()).parseMixXPUBObservable(new JSONObject(response.toString()));
                Disposable disposable = parser
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(pairValues -> {
                            List<Tx> txes = pairValues.first;
                            Long xpub_balance = pairValues.second;
                            Collections.sort(txes, new APIFactory.TxMostRecentDateComparator());
                            txs.postValue(txes);
                            toggleSat.setValue(is_sat_prefs);
                            if (account == 0) {
                                balance.postValue(xpub_balance - BlockedUTXO.getInstance().getTotalValueBlocked0());
                            }
                            if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix()) {
                                balance.postValue(xpub_balance - BlockedUTXO.getInstance().getTotalValuePostMix());
                            }
                            if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolBadBank()) {
                                balance.postValue(xpub_balance - BlockedUTXO.getInstance().getTotalValueBadBank());
                            }
                        }, error -> {
                            LogUtil.info(TAG,error.getMessage());
                            txs.postValue(new ArrayList<>());
                            toggleSat.setValue(is_sat_prefs);
                            balance.postValue(0L);
                        });

                compositeDisposables.add(disposable);

            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            txs.setValue(new ArrayList<>());
            toggleSat.setValue(is_sat_prefs);
            balance.setValue(0L);
        }
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

    boolean toggleSat() {
        sharedpreferences = mContext.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        boolean is_sat_prefs = PrefsUtil.getInstance(this.mContext).getValue(PrefsUtil.IS_SAT, true);
        if (toggleSat.getValue() == null) {
            this.toggleSat.setValue(is_sat_prefs);
            return false;
        } else {
            this.toggleSat.setValue(!toggleSat.getValue());
            return (toggleSat.getValue());
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

    public void setAccount(int account) {
        this.account = account;
    }
}
