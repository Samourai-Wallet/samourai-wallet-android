package com.samourai.wallet.paynym;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.payload.PayloadUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

        disposables.add(disposable);
    }

    public void setPaynymPayload(JSONObject jsonObject) {
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
                }
                this.followingList.postValue(followings);
            }
            if (jsonObject.has("followers")) {
                JSONArray _following = jsonObject.getJSONArray("followers");
                for (int i = 0; i < _following.length(); i++) {
                    followers.add(((JSONObject) _following.get(i)).getString("code"));
                    if (((JSONObject) _following.get(i)).has("segwit")) {
                        BIP47Meta.getInstance().setSegwit(((JSONObject) _following.get(i)).getString("code"), ((JSONObject) _following.get(i)).getBoolean("segwit"));
                    }
                }
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

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
