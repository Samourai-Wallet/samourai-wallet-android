package com.samourai.wallet.send.batch

import androidx.lifecycle.*
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.util.BatchSendUtil
import com.samourai.wallet.util.FeeUtil
import com.samourai.wallet.whirlpool.WhirlpoolConst

class BatchSpendViewModel() : ViewModel() {


    private val batchList: MutableLiveData<ArrayList<BatchSendUtil.BatchSend>> = MutableLiveData()
    private val balance: MutableLiveData<Long> = MutableLiveData()
    private val feeLivedata: MutableLiveData<Long> by lazy {
        MutableLiveData(0L)
    }

    init {
        BatchSendUtil.getInstance().sends?.let {
            if (it.size != 0) {
                batchList.postValue(ArrayList(it))
            }
        }
    }

    fun getBatchListLive(): LiveData<ArrayList<BatchSendUtil.BatchSend>> {
        return batchList
    }

    fun getBatchList():   ArrayList<BatchSendUtil.BatchSend> {
        return batchList.value ?: arrayListOf()
    }

    fun getFee(): LiveData<Long> {
        return feeLivedata
    }

    fun setFee(fee: Long) {
        feeLivedata.postValue(fee)
    }

    //A livedata instance that returns balance
    //balance will be recalculated when batch list changed
    fun getBalance(): LiveData<Long> {
        return MediatorLiveData<Long>().apply {
            fun update() {
                val totalBatchAmount = getBatchAmount()
                value = balance.value?.minus(totalBatchAmount)
            }
            addSource(batchList) { update() }
            addSource(balance) { update() }
        }
    }

    fun getBatchAmount(): Long {
        return  batchList.value?.map { it.amount }
                .takeIf { it?.isNotEmpty() ?: false }
                ?.reduce { acc, l -> acc + l }
                ?: 0L
    }

    fun add(batchItem: BatchSendUtil.BatchSend) {
        val list = ArrayList<BatchSendUtil.BatchSend>().apply { batchList.value?.let { addAll(it) } }
        val exist = list.find { it.UUID==batchItem.UUID }
        if(exist==null){
            list.add(batchItem)
        }else{
            list[list.indexOf(exist)] = batchItem
        }
        list.sortByDescending { it.UUID }
        list.let {
            batchList.postValue(list)
        }
    }

    fun remove(it: BatchSendUtil.BatchSend) {
        val list = ArrayList<BatchSendUtil.BatchSend>().apply { batchList.value?.let { addAll(it) } }
        list.remove(it)
        batchList.postValue(list)
    }

    fun setBalance(balance: Long) {
        this.balance.postValue(balance)
    }
}
