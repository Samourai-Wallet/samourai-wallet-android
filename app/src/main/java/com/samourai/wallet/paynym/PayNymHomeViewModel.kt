package com.samourai.wallet.paynym

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.paynym.api.PayNymApiService
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.LogUtil
import com.samourai.wallet.util.MessageSignUtil
import com.samourai.wallet.util.PrefsUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import okhttp3.Response
import org.bitcoinj.core.AddressFormatException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class PayNymHomeViewModel(application: Application) : AndroidViewModel(application) {

    private var paymentCode = MutableLiveData<String>()
    private var errors = MutableLiveData<String>()
    private var loader = MutableLiveData(false)
    private var refreshTaskProgress = MutableLiveData(Pair(0, 0))
    private var followersList = MutableLiveData<ArrayList<String>>()
    private var followingList = MutableLiveData<ArrayList<String>>()
    private val TAG = "PayNymHomeViewModel"
    private var refreshJob: Job = Job()

    val followers: LiveData<ArrayList<String>>
        get() = followersList

    val errorsLiveData: LiveData<String>
        get() = errors

    val pcode: LiveData<String>
        get() = paymentCode

    val following: LiveData<ArrayList<String>>
        get() = followingList

    val loaderLiveData: LiveData<Boolean>
        get() = loader


    val refreshTaskProgressLiveData: LiveData<Pair<Int, Int>>
        get() = refreshTaskProgress


    private suspend fun setPaynymPayload(jsonObject: JSONObject) = withContext(Dispatchers.IO) {
        val _pcodes = BIP47Meta.getInstance().getSortedByLabels(false)
        var array = JSONArray()
        val followings = ArrayList<String>()
        val followers = ArrayList<String>()
        try {
            array = jsonObject.getJSONArray("codes")
            if (array.getJSONObject(0).has("claimed") && array.getJSONObject(0).getBoolean("claimed")) {
                val strNymName = jsonObject.getString("nymName")
                viewModelScope.launch(Dispatchers.Main) {
                    paymentCode.postValue(strNymName)
                }
            }
            if (jsonObject.has("following")) {
                val _following = jsonObject.getJSONArray("following")
                for (i in 0 until _following.length()) {
                    followings.add((_following[i] as JSONObject).getString("code"))
                    if ((_following[i] as JSONObject).has("segwit")) {
                        BIP47Meta.getInstance().setSegwit((_following[i] as JSONObject).getString("code"), (_following[i] as JSONObject).getBoolean("segwit"))
                    }
                    val paynym = _following[i] as JSONObject
                    if (BIP47Meta.getInstance().getDisplayLabel(paynym.getString("code")).contains(paynym.getString("code").substring(0, 4))) {
                        BIP47Meta.getInstance().setLabel(paynym.getString("code"), paynym.getString("nymName"))
                    }
                }
                for (pcode in _pcodes) {
                    if (!followings.contains(pcode)) {
                        followings.add(pcode)
                    }
                }
                sortByLabel(followings)
                viewModelScope.launch(Dispatchers.Main) {
                    followingList.postValue(followings)
                }
            }
            if (jsonObject.has("followers")) {
                val _follower = jsonObject.getJSONArray("followers")
                for (i in 0 until _follower.length()) {
                    followers.add((_follower[i] as JSONObject).getString("code"))
                    if ((_follower[i] as JSONObject).has("segwit")) {
                        BIP47Meta.getInstance().setSegwit((_follower[i] as JSONObject).getString("code"), (_follower[i] as JSONObject).getBoolean("segwit"))
                    }
                    val paynym = _follower[i] as JSONObject
                    if (BIP47Meta.getInstance().getDisplayLabel(paynym.getString("code")).contains(paynym.getString("code").substring(0, 4))) {
                        BIP47Meta.getInstance().setLabel(paynym.getString("code"), paynym.getString("nymName"))
                    }
                }
                sortByLabel(followers)
                viewModelScope.launch(Dispatchers.Main) {
                    followersList.postValue(followers)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun sortByLabel(list: ArrayList<String>) {
        list.sortWith { pcode1: String?, pcode2: String? ->
            var res = java.lang.String.CASE_INSENSITIVE_ORDER.compare(BIP47Meta.getInstance().getDisplayLabel(pcode1), BIP47Meta.getInstance().getDisplayLabel(pcode2))
            if (res == 0) {
                res = BIP47Meta.getInstance().getDisplayLabel(pcode1).compareTo(BIP47Meta.getInstance().getDisplayLabel(pcode2))
            }
            res
        }
    }

    private suspend fun getPayNymData() = withContext(Dispatchers.IO) {
        if (AppUtil.getInstance(getApplication()).isOfflineMode) {
            return@withContext
        }
        val strPaymentCode = BIP47Util.getInstance(getApplication()).paymentCode.toString()
        val apiService = PayNymApiService.getInstance(strPaymentCode, getApplication())
        try {
            val response = apiService.getNymInfo()
            withContext(Dispatchers.Main) {
                loader.postValue(false)
            }
            if (response.isSuccessful) {
                val responseJson = response.body()?.string()
                if (responseJson != null)
                    setPaynymPayload(JSONObject(responseJson))
                else
                    throw Exception("Invalid response ")
            }
            if (!BIP47Meta.directoryTaskCompleted) {
                directoryTask()
            }
        } catch (ex: Exception) {
            LogUtil.error(TAG, ex)
            throw CancellationException(ex.message)
        }
    }

    fun refreshPayNym() {
        if (refreshJob.isActive) {
            refreshJob.cancel("")
        }
        refreshJob = viewModelScope.launch(Dispatchers.Main) {
            loader.postValue(true)
            withContext(Dispatchers.IO) {
                try {
                    async { getPayNymData() }.apply {
                        invokeOnCompletion {
                            loader.postValue(false)
                        }
                    }.await()
                } catch (error: Exception) {

                }
            }
        }
    }

    fun doSyncAll(silentSync: Boolean = false) {

        val strPaymentCode = BIP47Util.getInstance(getApplication()).paymentCode.toString()
        val apiService = PayNymApiService.getInstance(strPaymentCode, getApplication())

        val _pcodes = BIP47Meta.getInstance().getSortedByLabels(false)
        if (_pcodes.size == 0) {
            return
        }
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_pcodes.contains(BIP47Util.getInstance(getApplication()).paymentCode.toString())) {
                    _pcodes.remove(BIP47Util.getInstance(getApplication()).paymentCode.toString())
                    BIP47Meta.getInstance().remove(BIP47Util.getInstance(getApplication()).paymentCode.toString())
                }
            } catch (afe: AddressFormatException) {
                afe.printStackTrace()
            }
            var progress = 0;
            val jobs = arrayListOf<Deferred<Unit>>()
            if (!silentSync)
                refreshTaskProgress.postValue(Pair(0, _pcodes.size))
            _pcodes.forEachIndexed { _, pcode ->
                val job = async(Dispatchers.IO) { apiService.syncPcode(pcode) }
                jobs.add(job)
                job.invokeOnCompletion {
                    if (it == null) {
                        progress++
                        if (!silentSync)
                            refreshTaskProgress.postValue(Pair(progress, _pcodes.size))
                    } else {
                        it.printStackTrace()
                    }
                }
            }
            jobs.awaitAll()
        }
    }

    private fun directoryTask() {
        val strPaymentCode = BIP47Util.getInstance(getApplication()).paymentCode.toString()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pcodes = BIP47Meta.getInstance().getSortedByLabels(true)
                val apiService = PayNymApiService.getInstance(strPaymentCode, getApplication())
                val jobs = arrayListOf<Deferred<Response>>()

                pcodes.forEach {
                    val job = async { apiService.follow(it) }
                    jobs.add(job)
                    job.invokeOnCompletion {
                        if (it == null) {
                            BIP47Meta.directoryTaskCompleted = true
                        }
                    }
                }
                jobs.awaitAll()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }


    }

    fun init() {
        paymentCode.postValue("")
        //Load offline
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val _pcodes = BIP47Meta.getInstance().getSortedByLabels(false)
                    val list: ArrayList<String> = ArrayList<String>()
                    for (pcode in _pcodes) {
                        list.add(pcode)
                    }
                    sortByLabel(list)
                    viewModelScope.launch(Dispatchers.Main) {
                        followingList.postValue(list)
                    }
                    val res = PayloadUtil.getInstance(getApplication()).deserializePayNyms().toString()
                    setPaynymPayload(JSONObject(res))
                } catch (ex: Exception) {
                    throw CancellationException(ex.message)
                }
            }
        }
        //Load Online
        refreshPayNym()
    }

    init {
        if (PrefsUtil.getInstance(getApplication()).getValue(PrefsUtil.PAYNYM_CLAIMED, false)) {
            init()
        }
    }
}