package com.samourai.wallet.onboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.samourai.wallet.CreateWalletActivity
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.permissions.PermissionsUtil
import com.samourai.wallet.tor.TorManager
import io.matthewnelson.topl_service.TorServiceController
import kotlinx.android.synthetic.main.activity_set_up_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class SetUpWalletActivity : AppCompatActivity() {

    private var activeColor = 0
    private var disabledColor: Int = 0
    private var waiting: Int = 0
    private var storagePermGranted = false

    private val setUpWalletViewModel: SetUpWalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_up_wallet)
        window.statusBarColor = ContextCompat.getColor(this, R.color.window)

        activeColor = ContextCompat.getColor(this, R.color.green_ui_2)
        disabledColor = ContextCompat.getColor(this, R.color.disabledRed)
        waiting = ContextCompat.getColor(this, R.color.warning_yellow)


        if (!PermissionsUtil.getInstance(applicationContext).hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || !PermissionsUtil.getInstance(applicationContext).hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            askPermission()
        } else {
            storagePermGranted = true;
        }

        TorManager.getTorStateLiveData().observe(this, {
            setTorState(it)
        })

        setTorState(TorManager.torState)

        setUpWalletTorSwitch.setOnClickListener {
            if (setUpWalletTorSwitch.isChecked) {
                TorServiceController.startTor()
            } else {
                if (DojoUtil.getInstance(applicationContext).dojoParams != null) {
                    setUpWalletTorSwitch.isChecked = true
                    MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.cannot_disable_tor_dojo)
                        .setPositiveButton(R.string.ok) { dialog,
                                                          _ ->
                            dialog.dismiss()
                        }
                        .show()
                    return@setOnClickListener
                }
                TorServiceController.stopTor()
            }
        }
        setUpWalletViewModel.errorsLiveData.observe(this, {
            it?.let {
                Snackbar.make(setUpWalletContainer, "Error: $it", Snackbar.LENGTH_LONG)
                    .show()
            }
        })
        setUpWalletViewModel.apiEndpoint.observe(this, {
            if (setUpWalletAddressInput.text.toString() != it)
                setUpWalletAddressInput.setText(it)
        })
        setUpWalletViewModel.apiKey.observe(this, {
            if (setUpWalletApiKeyInput.text.toString() != it)
                setUpWalletApiKeyInput.setText(it)
        })
        setUpWalletViewModel.dojoStatus.observe(this, {
            val defaultColor = MaterialColors.getColor(applicationContext, R.attr.colorPrimary, ContextCompat.getColor(this, R.color.primary_light))
            onBoardingDojoStatus.setTextColor(defaultColor)
            when (it) {
                SetUpWalletViewModel.DojoStatus.CONNECTED -> {
                    disableInputs(false)
                    onBoardingDojoStatus.text = getString(R.string.connected)
                    onBoardingDojoStatus.setTextColor(activeColor)
                    setUpWalletDojoSwitch.visibility = View.VISIBLE
                    setUpWalletDojoProgress.visibility = View.GONE
                }
                SetUpWalletViewModel.DojoStatus.NOT_CONFIGURED -> {
                    disableInputs(false)
                    onBoardingDojoStatus.text = getString(R.string.not_configured)
                }

                SetUpWalletViewModel.DojoStatus.CONNECTING -> {
                    disableInputs(true)
                    onBoardingDojoStatus.text = getString(R.string.connecting)
                    onBoardingDojoStatus.setTextColor(waiting)
                    setUpWalletDojoProgress.visibility = View.VISIBLE
                    setUpWalletDojoSwitch.visibility = View.INVISIBLE
                }
                SetUpWalletViewModel.DojoStatus.ERROR -> {
                    disableInputs(false)
                    onBoardingDojoStatus.text = "Error"
                    setUpWalletDojoProgress.visibility = View.GONE
                    setUpWalletDojoSwitch.visibility = View.VISIBLE
                }
                else -> {

                }
            }

        })
        setUpDojoLayout()
        setUpWalletConnectDojo.setOnClickListener {
            connectDojo()
        }
        setUpWalletCreateNewWallet.setOnClickListener {
            if (!storagePermGranted) {
                askPermission()
                return@setOnClickListener
            }
            val intent = Intent(this, CreateWalletActivity::class.java)
            startActivity(intent)
        }
        setUpWalletRestoreButton.setOnClickListener {
            if (!storagePermGranted) {
                askPermission()
                return@setOnClickListener
            }
            val intent = Intent(this, RestoreOptionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun askPermission() {

        val title = getString(R.string.permission_alert_dialog_title_external)
        val message = getString(R.string.permission_dialog_message_external)

        val permissionDialog = MaterialAlertDialogBuilder(this)
        permissionDialog.setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                run {
                    dialog.dismiss()
                }
                }

        var openSettings = false;
        if (!shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                !shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            openSettings = true;
        }
        if(!openSettings){
            permissionDialog .setPositiveButton(R.string.action_settings) { dialog, _ ->
                run {
                    dialog.dismiss()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:" + applicationContext.packageName)
                    startActivity(intent)
                }
            }
        }else{
            permissionDialog .setPositiveButton(R.string.ok) { dialog, _ ->
                run {
                    dialog.dismiss()
                    ActivityCompat
                            .requestPermissions(this@SetUpWalletActivity,
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                    PermissionsUtil.READ_WRITE_EXTERNAL_PERMISSION_CODE)
                }
            }
        }
        permissionDialog.show()
    }

    private fun connectDojo() {
        val urlPattern =
            Pattern.compile("^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
        var apiUrl = setUpWalletAddressInput.text.toString()
        if (!apiUrl.startsWith("http")) {
            apiUrl = "http://$apiUrl"
        }
        if (apiUrl.endsWith(".onion") || apiUrl.endsWith(".onion/")) {
            apiUrl = if(!SamouraiWallet.getInstance().isTestNet){
                if (apiUrl.last() == '/') "${apiUrl}v2" else "$apiUrl/v2/"
            }else{
                if (apiUrl.last() == '/') "${apiUrl}test/v2" else "$apiUrl/test/v2"
            }
        }
        if (!urlPattern.matcher(apiUrl).matches()) {
            setUpWalletAddressInput.error = getString(R.string.invalid_api_endpoint)
            setUpWalletAddressInput.requestFocus()
            return
        }
        if (setUpWalletApiKeyInput.text.toString().isEmpty()) {
            setUpWalletApiKeyInput.error = getString(R.string.api_key_blank_error)
            setUpWalletApiKeyInput.requestFocus()
            return
        }

        setUpWalletViewModel.setApiUrl(apiUrl)
        setUpWalletViewModel.setApiKey(setUpWalletApiKeyInput.text.toString())

        if (TorManager.isConnected()) {
            setUpWalletViewModel.connectToDojo(applicationContext)
        } else {
            TorServiceController.startTor()
            TorManager.getTorStateLiveData().observe(this, Observer {
                if (it == TorManager.TorState.ON) {
                  setUpWalletViewModel.viewModelScope.launch(Dispatchers.Default) {
                      delay(600)
                      withContext(Dispatchers.Main){
                          setUpWalletViewModel.connectToDojo(applicationContext)
                      }
                  }
                }
            })
        }
    }

    private fun setUpDojoLayout() {

        setUpWalletScanDojo.setOnClickListener {
            val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
            cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
            cameraFragmentBottomSheet.setQrCodeScanListener { code: String ->
                cameraFragmentBottomSheet.dismissAllowingStateLoss()

                try {
                    setUpWalletViewModel.viewModelScope.launch(Dispatchers.Default) {
                        withContext(Dispatchers.Main){
                            setUpWalletViewModel.setDojoParams(code, applicationContext)
                        }
                        delay(600)
                        withContext(Dispatchers.Main){
                            connectDojo()
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }

        setUpWalletDojoSwitch.setOnCheckedChangeListener { buttonView,
                                                           isChecked ->
            run {
                if (!isChecked && DojoUtil.getInstance(applicationContext).dojoParams != null) {
                    setUpWalletDojoSwitch.isChecked = true
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.confirm)
                        .setMessage(getString(R.string.do_you_want_to_unpair))
                        .setPositiveButton(R.string.ok) { _,
                                                          _ ->
                            run {
                                setUpWalletViewModel.unPairDojo(applicationContext)
                                setUpWalletDojoSwitch.isChecked = false
                            }
                        }.setNegativeButton(R.string.cancel) { dialog,
                                                               _ ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    showDojoInputLayout(isChecked)
                }
            }
        }

        setUpWalletDojoMessage.setOnClickListener {
            setUpWalletDojoSwitch.isChecked = !setUpWalletDojoSwitch.isChecked
        }

        slideDown(setUpWalletDojoInputGroup)
    }

    private fun showDojoInputLayout(checked: Boolean) {
        if (checked) {
            slideUp(setUpWalletDojoInputGroup)
        } else {
            slideDown(setUpWalletDojoInputGroup)
        }
    }

    private fun setTorState(state: TorManager.TorState) {
        when (state) {
            TorManager.TorState.WAITING -> {
                onBoardingTorStatus.text = getString(R.string.tor_initializing)
                onBoardingTorStatus.setTextColor(waiting)
                makeViewTransition(setUpWalletTorProgress, setUpWalletTorSwitch)
            }
            TorManager.TorState.ON -> {
                disableInputs(false)
                onBoardingTorStatus.text = getString(R.string.active)
                onBoardingTorStatus.setTextColor(activeColor)
                setUpWalletTorSwitch.isChecked = true
                makeViewTransition(setUpWalletTorSwitch, setUpWalletTorProgress)
            }
            TorManager.TorState.OFF -> {
                setUpWalletTorSwitch.isChecked = false
                onBoardingTorStatus.text = getString(R.string.off)
                onBoardingTorStatus.setTextColor(disabledColor)
                makeViewTransition(setUpWalletTorSwitch, setUpWalletTorProgress)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            storagePermGranted = true;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun makeViewTransition(entering: View?, leaving: View?) {
        TransitionManager.beginDelayedTransition(setUpWalletContainer, Fade())
        leaving?.let {
            it.visibility = View.INVISIBLE
        }
        entering?.let {
            it.visibility = View.VISIBLE
        }
    }

    private fun disableInputs(enable: Boolean) {
        setUpWalletRestoreButton.isEnabled = !enable
        setUpWalletAddressInput.isEnabled = !enable
        setUpWalletScanDojo.isEnabled = !enable
        setUpWalletTorSwitch.isEnabled = !enable
        setUpWalletApiKeyInput.isEnabled = !enable
        setUpWalletCreateNewWallet.isEnabled = !enable
        setUpWalletConnectDojo.isEnabled = !enable
    }

    private fun slideDown(view: View): ViewPropertyAnimator? {
        return view.animate()
            .translationY(100F)
            .alpha(0f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(200)
            .withEndAction {
                view.visibility = View.GONE
            }.apply {
                this.start()
            }

    }

    private fun slideUp(view: View): ViewPropertyAnimator? {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        return view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .apply {
                this.start()
            }
    }

}
