package com.samourai.wallet.tor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.matthewnelson.topl_service_base.ServiceUtilities
import io.matthewnelson.topl_service_base.TorPortInfo
import io.matthewnelson.topl_service_base.TorServiceEventBroadcaster

class TorEventBroadcaster : TorServiceEventBroadcaster() {


    ///////////////////
    /// TorPortInfo ///
    ///////////////////
    private val _liveTorPortInfo = MutableLiveData<TorPortInfo>()
    val liveTorPortInfo: LiveData<TorPortInfo> = _liveTorPortInfo
    private val liveTorLogs: MutableLiveData<String> = MutableLiveData()

    override fun broadcastPortInformation(torPortInfo: TorPortInfo) {
        _liveTorPortInfo.value = torPortInfo
    }


    /////////////////
    /// Bandwidth ///
    /////////////////
    private var lastDownload = "0"
    private var lastUpload = "0"

    private val _liveBandwidth = MutableLiveData<String>()
    private val _liveProgress = MutableLiveData<Int>()

    val liveBandwidth: LiveData<String> = _liveBandwidth

    override fun broadcastBandwidth(bytesRead: String, bytesWritten: String) {
        if (bytesRead == lastDownload && bytesWritten == lastUpload) return

        lastDownload = bytesRead
        lastUpload = bytesWritten
        if (!liveBandwidth.hasActiveObservers()) return

        _liveBandwidth.value = ServiceUtilities.getFormattedBandwidthString(
                bytesRead.toLong(), bytesWritten.toLong()
        )
    }

    override fun broadcastDebug(msg: String) {
        broadcastLogMessage(msg)
    }

    override fun broadcastException(msg: String?, e: Exception) {
        if (msg.isNullOrEmpty()) return

        broadcastLogMessage(msg)
        e.printStackTrace()
    }


    ////////////////////
    /// Log Messages ///
    ////////////////////
    override fun broadcastLogMessage(logMessage: String?) {
        if (logMessage.isNullOrEmpty()) return

        val splitMsg = logMessage.split("|")
        if (splitMsg.size < 3) return
        liveTorLogs.postValue("${splitMsg[0]} | ${splitMsg[1]} | ${splitMsg[2]}")
        if (logMessage == "Bootstrapped 100%") {
            _liveProgress.postValue(100)
        } else {
            val progress: Int? = try {
                logMessage.split(" ")[1].split("%")[0].toInt()
            } catch (e: Exception) {
                null
            }
            progress?.let {
                _liveProgress.postValue(progress)
            }
        }
    }

    override fun broadcastNotice(msg: String) {
        broadcastLogMessage(msg)

    }

    ///////////////////
    /// Tor's State ///
    ///////////////////
    inner class TorStateData(val state: String, val networkState: String)

    private var lastState = TorState.OFF
    private var lastNetworkState = TorNetworkState.DISABLED

    private val _liveTorState = MutableLiveData<TorStateData>()

    val liveTorState: LiveData<TorStateData> = _liveTorState

    val torLogs: LiveData<String>
        get() = liveTorLogs

    val torPortInfo: LiveData<TorPortInfo>
        get() = _liveTorPortInfo

    val torBootStrapProgress: LiveData<Int>
        get() = _liveProgress

    override fun broadcastTorState(@TorState state: String, @TorNetworkState networkState: String) {
        if (state == lastState && networkState == lastNetworkState) return

        lastState = state
        lastNetworkState = networkState
        _liveTorState.value = TorStateData(state, networkState)
    }
}

