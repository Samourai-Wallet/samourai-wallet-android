package com.samourai.wallet.tor

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.MainActivity2
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiApplication
import com.samourai.wallet.util.PrefsUtil
import io.matthewnelson.topl_core_base.TorConfigFiles
import io.matthewnelson.topl_service.TorServiceController
import io.matthewnelson.topl_service.TorServiceController.*
import io.matthewnelson.topl_service.lifecycle.BackgroundManager
import io.matthewnelson.topl_service.notification.ServiceNotification
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * samourai-wallet-android
 *
 */

object TorManager {
    lateinit var stopTorDelaySettingAtAppStartup: String
        private set

    private var proxy: Proxy? = null

    enum class TorState {
        WAITING,
        ON,
        OFF
    }

    var appContext: SamouraiApplication? = null
    private val torStateLiveData: MutableLiveData<TorState> = MutableLiveData()
    private val torProgress: MutableLiveData<Int> = MutableLiveData()


    var torState: TorState = TorState.OFF
        set(value) {
            field = value
            torStateLiveData.postValue(value)
        }


    fun startTor() {
        TorServiceController.startTor()
    }

    private fun generateTorServiceNotificationBuilder(
    ): ServiceNotification.Builder {
        var contentIntent: PendingIntent? = null

        appContext?.packageManager?.getLaunchIntentForPackage(appContext!!.packageName)?.let { intent ->
            contentIntent = PendingIntent.getActivity(
                    appContext,
                    0,
                    intent,
                    0
            )
        }

        return ServiceNotification.Builder(
                channelName = "Tor service",
                channelDescription = "Tor foreground service notification",
                channelID = SamouraiApplication.TOR_CHANNEL_ID,
                notificationID = 12
        )
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setImageTorNetworkingEnabled(R.drawable.ic_samourai_tor_enabled)
                .setImageTorDataTransfer(R.drawable.ic_samourai_tor_data_transfer)
                .setImageTorNetworkingDisabled(R.drawable.ic_samourai_tor_idle)
                .setCustomColor(R.color.green_ui_2)
                .enableTorRestartButton(true)
                .enableTorStopButton(false)
                .showNotification(true)
                .also { builder ->
                    contentIntent?.let {
                        builder.setContentIntent(it)
                    }
                }
    }

    /**
     * for a cleaner
     * */
    private fun generateBackgroundManagerPolicy(
    ): BackgroundManager.Builder.Policy {
        val builder = BackgroundManager.Builder()
        return builder.runServiceInForeground(true)
    }

    private fun setupTorServices(
            application: Application,
            serviceNotificationBuilder: ServiceNotification.Builder,
    ): Builder {

        val installDir = File(application.applicationInfo.nativeLibraryDir)

        val configDir = application.getDir("torservice", Context.MODE_PRIVATE)

        val builder = TorConfigFiles.Builder(installDir, configDir)
        builder.torExecutable(File(installDir, "libTor.so"))
        return Builder(
                application = application,
                torServiceNotificationBuilder = serviceNotificationBuilder,
                backgroundManagerPolicy = generateBackgroundManagerPolicy(),
                buildConfigVersionCode = BuildConfig.VERSION_CODE,
                defaultTorSettings = TorSettings(),
                geoipAssetPath = "common/geoip",
                geoip6AssetPath = "common/geoip6",

                )
                .useCustomTorConfigFiles(builder.build())
                .setBuildConfigDebug(BuildConfig.DEBUG)
                .setEventBroadcaster(eventBroadcaster = TorEventBroadcaster())
    }

    fun isRequired(): Boolean {
        return PrefsUtil.getInstance(appContext).getValue(PrefsUtil.ENABLE_TOR, false);
    }

    fun isConnected(): Boolean {
        return getTorStateLiveData().value == TorState.ON
    }

    fun getTorStateLiveData(): LiveData<TorState> {
        return torStateLiveData
    }

    fun getTorBootstrapProgress(): LiveData<Int> {
        return torProgress
    }

    fun getProxy(): Proxy? {
        return proxy;
    }

    fun setUp(context: SamouraiApplication) {
        appContext = context
        val builder = setupTorServices(
                context,
                generateTorServiceNotificationBuilder(),
        )

        try {
            builder.build()
        } catch (e: Exception) {
            e.message?.let {

            }
        }
        TorServiceController.appEventBroadcaster?.let {

            (it as TorEventBroadcaster).liveTorState.observeForever { torEventState ->
                when (torEventState.state) {
                    "Tor: Off" -> {
                        torState = TorState.OFF
                    }
                    "Tor: Starting" -> {
                        torState = TorState.WAITING
                    }
                    "Tor: Stopping" -> {
                        torState = TorState.WAITING
                    }
                }
            }

            it.torLogs.observeForever { log ->
                if (log.contains("Bootstrapped 100%")) {
                    torState = TorState.ON
                }
                if (log.contains("NEWNYM")) {
                    val message = log.substring(log.lastIndexOf("|"), log.length)
                    Toast.makeText(appContext, message.replace("|", ""), Toast.LENGTH_SHORT).show()
                }
            }
            it.torBootStrapProgress.observeForever { log ->
                torProgress.postValue(log)
            }
            it.torPortInfo.observeForever { torInfo ->
                torInfo.socksPort?.let { port ->
                    createProxy(port)
                }
            }
        }
    }

    private fun createProxy(proxyUrl: String) {
        val host = proxyUrl.split(":")[0].trim()
        val port = proxyUrl.split(":")[1]
        proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(
                host, port.trim().toInt()))
    }

    fun toJSON(): JSONObject {
        val jsonPayload = JSONObject();

        try {
                jsonPayload.put("active", PrefsUtil.getInstance(appContext).getValue(PrefsUtil.ENABLE_TOR,false));
        } catch (ex: JSONException) {
//            throw  RuntimeException(ex);
        } catch (ex: ClassCastException) {
//            throw  RuntimeException(ex);
        }

        return jsonPayload
    }

    fun fromJSON(jsonPayload: JSONObject) {
        try {
            if (jsonPayload.has("active")) {
                PrefsUtil.getInstance(appContext).setValue(PrefsUtil.ENABLE_TOR, jsonPayload.getBoolean("active"));
            }
        } catch (ex: JSONException) {
        }
    }
}
