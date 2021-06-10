package com.samourai.wallet.onboard;

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.wallet.R
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.util.PrefsUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class SetUpWalletViewModel : ViewModel() {
        enum class DojoStatus{
            CONNECTED,
            NOT_CONFIGURED,
            ERROR,
            CONNECTING
        }
        private val compositeDisposable = CompositeDisposable()
        private val _errors: MutableLiveData<String?> = MutableLiveData(null)
        private val _apiEndpoint = MutableLiveData("")
        private val _dojoStatus = MutableLiveData(DojoStatus.NOT_CONFIGURED)
        private val _dojoApiVersion = MutableLiveData("1.4.5")
        private val _apiKey = MutableLiveData("")


        val errorsLiveData: LiveData<String?> get() = _errors
        val apiEndpoint: LiveData<String> get() = _apiEndpoint
        val apiKey: LiveData<String> get() = _apiKey
        val dojoStatus: MutableLiveData<DojoStatus> get() = _dojoStatus
        val dojoApiVersion: LiveData<String> get() = _dojoApiVersion


        fun setDojoParams(code: String, applicationContext: Context) {
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    if (DojoUtil.getInstance(applicationContext).isValidPairingPayload(code.trim { it <= ' ' })) {
                        val dojoPayload = JSONObject(code).getJSONObject("pairing");
                        if (dojoPayload.has("apikey")) {
                            _apiKey.postValue(dojoPayload.getString("apikey"))
                        }
                        if (dojoPayload.has("url")) {
                            _apiEndpoint.postValue(dojoPayload.getString("url"))
                        }
                        if (dojoPayload.has("version")) {
                            _dojoApiVersion.postValue(dojoPayload.getString("version"))
                        }
                    } else {
                        _errors.postValue(applicationContext.getString(R.string.invalid_dojo_payload))
                    }
                } catch (ex: Exception) {
                    _errors.postValue(ex.message)
                }
            }
        }

        fun connectToDojo(applicationContext: Context) {
            PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.ENABLE_TOR, true);

            val paring = JSONObject().apply {
                put("type", "dojo.api")
                put("apikey", _apiKey.value)
                put("url", _apiEndpoint.value)
                put("version", _dojoApiVersion.value)
            }
            val dojoParams = JSONObject().apply {
                put("pairing", paring)
            }
            _dojoStatus.postValue(DojoStatus.CONNECTING)
            val disposable = DojoUtil.getInstance(applicationContext)
                    .setDojoParams(dojoParams.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        if (it == true) {
                            _dojoStatus.postValue(DojoStatus.CONNECTED)
                        } else {
                             _errors.postValue(applicationContext.getString(R.string.dojo_connection_error))
                            _dojoStatus.postValue(DojoStatus.ERROR)
                            DojoUtil.getInstance(applicationContext).removeDojoParams()
                        }
                    }, {
                        _dojoStatus.postValue(DojoStatus.ERROR)
                        it?.let {
                            _errors.postValue(it.message)
                        }
                        DojoUtil.getInstance(applicationContext).removeDojoParams()
                    })
            compositeDisposable.add(disposable)
        }

        override fun onCleared() {
            compositeDisposable.dispose()
            super.onCleared()
        }

        fun setApiUrl(apiUrl: String) {
            this._apiEndpoint.postValue(apiUrl)
            this._apiEndpoint.value = apiUrl
        }

        fun setApiKey(key: String) {
            this._apiKey.postValue(key)
            this._apiKey.value = key
        }

    fun unPairDojo(applicationContext: Context?) {
        DojoUtil.getInstance(applicationContext).removeDojoParams()
        this.setApiKey("")
        this.setApiUrl("")
        _dojoStatus.postValue(DojoStatus.NOT_CONFIGURED)
    }

}