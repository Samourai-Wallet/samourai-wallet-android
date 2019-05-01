package com.samourai.wallet.paynym;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.payload.PayloadUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class PayNymHomeViewModel extends AndroidViewModel {

    protected MutableLiveData<String> paymentCode = new MutableLiveData<>();
    protected MutableLiveData<ArrayList<String>> followersList = new MutableLiveData<>();
    protected MutableLiveData<ArrayList<String>> followingList = new MutableLiveData<>();

    CompositeDisposable disposables = new CompositeDisposable();
    private static HashMap<String, String> meta = null;

    public PayNymHomeViewModel(@NonNull Application application) {
        super(application);

        this.paymentCode.postValue("");
        this.followingList.postValue(new ArrayList<>());
        this.followersList.postValue(new ArrayList<>());

        Disposable disposable = this.loadOfflineData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setPaynymPayload, error -> {
                });

        Disposable disposableFollowers = this.getFollowingPcodes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    this.followingList.postValue(list);
                }, error -> {
                });

        disposables.add(disposable);
        disposables.add(disposableFollowers);
    }

    public void setPaynymPayload(JSONObject jsonObject) {
        Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels(false);

        JSONArray array = new JSONArray();
        ArrayList<String> followings = new ArrayList<>();
        ArrayList<String> followers = new ArrayList<>();
        try {
            array = jsonObject.getJSONArray("codes");

            if (array.getJSONObject(0).has("claimed") && array.getJSONObject(0).getBoolean("claimed")) {
                final String strNymName = jsonObject.getString("nymName");
                paymentCode.postValue(strNymName);
            }
            if (jsonObject.has("following")) {
                JSONArray _following = jsonObject.getJSONArray("following");
                for (int i = 0; i < _following.length(); i++) {
                    followings.add(((JSONObject) _following.get(i)).getString("code"));
                    if (((JSONObject) _following.get(i)).has("segwit")) {
                        BIP47Meta.getInstance().setSegwit(((JSONObject) _following.get(i)).getString("code"), ((JSONObject) _following.get(i)).getBoolean("segwit"));
                    }
                    JSONObject paynym = (JSONObject) _following.get(i);
                    if (BIP47Meta.getInstance().getDisplayLabel(paynym.getString("code")).contains(paynym.getString("code").substring(0, 4))) {
                        BIP47Meta.getInstance().setLabel(paynym.getString("code"), paynym.getString("nymName"));
                    }

                }
                for (String pcode : _pcodes) {
                    if (!followings.contains(pcode)) {
                        followings.add(pcode);
                    }
                }
                sortByLabel(followings);

                this.followingList.postValue(followings);
            }
            if (jsonObject.has("followers")) {
                JSONArray _follower = jsonObject.getJSONArray("followers");
                for (int i = 0; i < _follower.length(); i++) {
                    followers.add(((JSONObject) _follower.get(i)).getString("code"));
                    if (((JSONObject) _follower.get(i)).has("segwit")) {
                        BIP47Meta.getInstance().setSegwit(((JSONObject) _follower.get(i)).getString("code"), ((JSONObject) _follower.get(i)).getBoolean("segwit"));
                    }
                    JSONObject paynym = (JSONObject) _follower.get(i);
                    if (BIP47Meta.getInstance().getDisplayLabel(paynym.getString("code")).contains(paynym.getString("code").substring(0, 4))) {
                        BIP47Meta.getInstance().setLabel(paynym.getString("code"), paynym.getString("nymName"));
                    }
                }

                sortByLabel(followers);
                this.followersList.postValue(followers);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Observable<JSONObject> loadOfflineData() {
        return Observable.fromCallable(() -> {
            String res = PayloadUtil.getInstance(this.getApplication()).deserializePayNyms().toString();
            JSONObject responseObj = new JSONObject();
            if (res != null) {
                responseObj = new JSONObject(res);

            }
            return responseObj;
        });

    }

    private Observable<ArrayList<String>> getFollowingPcodes() {
        return Observable.fromCallable(() -> {
            Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels(false);
            ArrayList<String> list = new ArrayList();
            for (String pcode : _pcodes) {
                list.add(pcode);
            }
            sortByLabel(list);

            return list;
        });

    }

    private void sortByLabel(ArrayList<String> list) {
        Collections.sort(list, (pcode1, pcode2) -> {
            int res = String.CASE_INSENSITIVE_ORDER.compare(BIP47Meta.getInstance().getDisplayLabel(pcode1), BIP47Meta.getInstance().getDisplayLabel(pcode2));
            if (res == 0) {
                res = BIP47Meta.getInstance().getDisplayLabel(pcode1).compareTo(BIP47Meta.getInstance().getDisplayLabel(pcode2));
            }
            return res;
        });
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
