package com.samourai.wallet.settings

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.*
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.cahoots.psbt.PSBTUtil
import com.samourai.wallet.crypto.AESUtil
import com.samourai.wallet.crypto.DecryptionException
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.payload.ExternalBackupManager
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.ricochet.RicochetMeta
import com.samourai.wallet.segwit.BIP49Util
import com.samourai.wallet.segwit.BIP84Util
import com.samourai.wallet.send.PushTx
import com.samourai.wallet.send.RBFUtil
import com.samourai.wallet.tor.TorManager
import com.samourai.wallet.util.*
import com.samourai.wallet.whirlpool.WhirlpoolMeta
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo
import io.matthewnelson.topl_service.TorServiceController
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException
import org.bouncycastle.util.encoders.Hex
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.util.concurrent.CancellationException


class SettingsDetailsFragment(private val key: String?) : PreferenceFragmentCompat() {

    public var targetTransition: Transition? = null
    private var progress: ProgressDialog? = null
    private val scope = CoroutineScope(Dispatchers.IO) + SupervisorJob();

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        targetTransition?.addTarget(view)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        when (key) {
            "wallet" -> {
                setPreferencesFromResource(R.xml.settings_wallet, rootKey)
                activity?.title = "Settings | Wallet"
                walletSettings()
            }
            "txs" -> {
                activity?.title = "Settings | Transactions"
                setPreferencesFromResource(R.xml.settings_txs, rootKey)
                transactionsSettings()
            }
            "troubleshoot" -> {
                activity?.title = "Settings | Troubleshoot"
                setPreferencesFromResource(R.xml.settings_troubleshoot, rootKey)
                troubleShootSettings()
            }
            "other" -> {
                activity?.title = "Settings | Other"
                setPreferencesFromResource(R.xml.settings_other, rootKey)
                otherSettings()
            }
        }
    }

    private fun walletSettings() {

        val mnemonicPref = findPreference("mnemonic") as Preference?
        mnemonicPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getHDSeed(true)
            true
        }

        val xpubPref = findPreference("xpub") as Preference?
        xpubPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(44, 0)
            true
        }

        val ypubPref = findPreference("ypub") as Preference?
        ypubPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(49, 0)
            true
        }

        val zpubPref = findPreference("zpub") as Preference?
        zpubPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(84, 0)
            true
        }

        val zpubPrePref = findPreference("zpub_pre") as Preference?
        zpubPrePref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(84, WhirlpoolMeta.getInstance(requireContext()).whirlpoolPremixAccount)
            true
        }

        val zpubPostPref = findPreference("zpub_post") as Preference?
        zpubPostPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(84, WhirlpoolMeta.getInstance(requireContext()).whirlpoolPostmix)
            true
        }

        val zpubPostXPref = findPreference("zpub_post_x") as Preference?
        zpubPostXPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(44, WhirlpoolMeta.getInstance(requireContext()).whirlpoolPostmix)
            true
        }

        val zpubPostYPref = findPreference("zpub_post_y") as Preference?
        zpubPostYPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(49, WhirlpoolMeta.getInstance(requireContext()).whirlpoolPostmix)
            true
        }

        val zpubBadBankPref = findPreference("zpub_badbank") as Preference?
        zpubBadBankPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getXPUB(84, WhirlpoolMeta.getInstance(requireContext()).whirlpoolBadBank)
            true
        }

        val wipePref = findPreference("wipe") as Preference?
        wipePref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {

            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.sure_to_erase)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialog, whichButton ->
                        val progress = ProgressDialog(requireContext())
                        progress.setTitle(R.string.app_name)
                        progress.setMessage(requireContext().resources.getString(R.string.securely_wiping_wait))
                        progress.setCancelable(false)
                        progress.show()

                        WhirlpoolMeta.getInstance(requireContext().applicationContext).scode = null
                        WhirlpoolNotificationService.stopService(requireContext().applicationContext)
                        if (TorManager.isConnected()) {
                            TorServiceController.startTor()
                        }
                        scope.launch {
                            AppUtil.getInstance(requireContext()).wipeApp()

                            delay(500)
                            val walletDir = requireContext().getDir("wallet", Context.MODE_PRIVATE)
                            val filesDir = requireContext().filesDir
                            val cacheDir = requireContext().cacheDir

                            if (walletDir.exists()) {
                                FileUtils.deleteDirectory(walletDir);
                            }
                            if (filesDir.exists()) {
                                FileUtils.deleteDirectory(filesDir);
                            }
                            if (cacheDir.exists()) {
                                FileUtils.deleteDirectory(cacheDir);

                            }
                        }.invokeOnCompletion {
                            scope.launch(Dispatchers.Main) {
                                if (it == null) {
                                    if (progress.isShowing) {
                                        progress.dismiss();
                                    }
                                    Toast.makeText(requireContext(), R.string.wallet_erased, Toast.LENGTH_SHORT).show()
                                    AppUtil.getInstance(requireContext()).restartApp()
                                } else {
                                    if (progress.isShowing) {
                                        progress.dismiss();
                                    }
                                    Toast.makeText(requireContext(), "Error ${it.message}", Toast.LENGTH_SHORT).show()
                                    if (BuildConfig.DEBUG) {
                                        it.printStackTrace();
                                    }
                                }
                            }
                        }

                    }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }.show()
            true
        }

        val cbPref5 = findPreference("scramblePin") as CheckBoxPreference?
        cbPref5!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (cbPref5.isChecked) {
                PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.SCRAMBLE_PIN, false)
            } else {
                PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.SCRAMBLE_PIN, true)
            }
            true
        }

        val cbPref11 = findPreference("haptic") as CheckBoxPreference?
        cbPref11!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (cbPref11.isChecked) {
                PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.HAPTIC_PIN, false)
            } else {
                PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.HAPTIC_PIN, true)
            }
            true
        }

        val changePinPref = findPreference("change_pin") as Preference?
        changePinPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.confirm_change_pin)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { dialog, whichButton ->
                        val pin = EditText(requireContext())
                        pin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                        MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.pin_enter)
                                .setView(pin)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok) { dialog, whichButton ->
                                    val _pin = pin.text.toString()
                                    if (_pin != null && _pin.length >= AccessFactory.MIN_PIN_LENGTH && _pin.length <= AccessFactory.MAX_PIN_LENGTH) {
                                        val hash = PrefsUtil.getInstance(requireContext()).getValue(PrefsUtil.ACCESS_HASH, "")
                                        if (AccessFactory.getInstance(requireContext()).validateHash(hash, AccessFactory.getInstance(requireContext()).guid, CharSequenceX(_pin), AESUtil.DefaultPBKDF2Iterations)) {
                                            val pin = EditText(requireContext())
                                            pin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                                            AlertDialog.Builder(requireContext())
                                                    .setTitle(R.string.app_name)
                                                    .setMessage(R.string.pin_5_8)
                                                    .setView(pin)
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.ok) { dialog, whichButton ->
                                                        val _pin = pin.text.toString()
                                                        if (_pin != null && _pin.length >= AccessFactory.MIN_PIN_LENGTH && _pin.length <= AccessFactory.MAX_PIN_LENGTH) {
                                                            val pin2 = EditText(requireContext())
                                                            pin2.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                                                            MaterialAlertDialogBuilder(requireContext())
                                                                    .setTitle(R.string.app_name)
                                                                    .setMessage(R.string.pin_5_8_confirm)
                                                                    .setView(pin2)
                                                                    .setCancelable(false)
                                                                    .setPositiveButton(R.string.ok) { dialog, whichButton ->
                                                                        val _pin2 = pin2.text.toString()
                                                                        if (_pin2 != null && _pin2 == _pin) {
                                                                            val accessHash = PrefsUtil.getInstance(requireContext()).getValue(PrefsUtil.ACCESS_HASH, "")
                                                                            val accessHash2 = PrefsUtil.getInstance(requireContext()).getValue(PrefsUtil.ACCESS_HASH2, "")
                                                                            val hash = AccessFactory.getInstance(requireContext()).getHash(AccessFactory.getInstance(requireContext()).guid, CharSequenceX(_pin), AESUtil.DefaultPBKDF2Iterations)
                                                                            PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.ACCESS_HASH, hash)
                                                                            if (accessHash == accessHash2) {
                                                                                PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.ACCESS_HASH2, hash)
                                                                            }
                                                                            AccessFactory.getInstance(requireContext()).pin = _pin2
                                                                            try {
                                                                                PayloadUtil.getInstance(requireContext()).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(requireContext()).guid + _pin))
                                                                            } catch (je: JSONException) {
                                                                                je.printStackTrace()
                                                                            } catch (ioe: IOException) {
                                                                                ioe.printStackTrace()
                                                                            } catch (mle: MnemonicLengthException) {
                                                                                mle.printStackTrace()
                                                                            } catch (de: DecryptionException) {
                                                                                de.printStackTrace()
                                                                            } finally {
                                                                                Toast.makeText(requireContext().getApplicationContext(), R.string.success_change_pin, Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        }
                                                                    }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }.show()
                                                        }
                                                    }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }.show()
                                        } else {
                                            Toast.makeText(requireContext().getApplicationContext(), R.string.pin_error, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(requireContext().getApplicationContext(), R.string.pin_error, Toast.LENGTH_SHORT).show()
                                    }
                                }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }.show()
                    }.setNegativeButton(R.string.no) { dialog, whichButton -> }.show()
            true
        }

        val cbPref6 = findPreference("autoBackup") as CheckBoxPreference?
        if (!SamouraiWallet.getInstance().hasPassphrase(requireContext())) {
            cbPref6!!.isChecked = false
            cbPref6.isEnabled = false
        } else {
            cbPref6!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                if (cbPref6.isChecked) {
                    PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.AUTO_BACKUP, false)
                } else {
                    if(!ExternalBackupManager.hasPermissions()){
                        ExternalBackupManager.askPermission(requireActivity())
                    }
                    PrefsUtil.getInstance(requireContext()).setValue(PrefsUtil.AUTO_BACKUP, true)
                }
                true
            }
        }
    }

    private fun transactionsSettings() {

        val cbPref0 = findPreference("segwit") as CheckBoxPreference?
        cbPref0?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (cbPref0!!.isChecked) {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.USE_SEGWIT, false)
            } else {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.USE_SEGWIT, true)
            }
            true
        }

        val cbPref15 = findPreference("likeTypedChange") as CheckBoxPreference?
        cbPref15?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (cbPref15!!.isChecked) {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, false)
            } else {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true)
            }
            true
        }

        val cbPref9 = findPreference("rbf") as CheckBoxPreference?
        cbPref9?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (cbPref9!!.isChecked) {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.RBF_OPT_IN, false)
            } else {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.RBF_OPT_IN, true)
            }
            true
        }

        val cbPref10 = findPreference("broadcastTx") as CheckBoxPreference?
        cbPref10?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (cbPref10!!.isChecked) {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.BROADCAST_TX, false)
            } else {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.BROADCAST_TX, true)
            }
            true
        }

        val broadcastHexPref = findPreference("broadcastHex") as Preference?
        broadcastHexPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doBroadcastHex()
            true
        }

        val cbPref11 = findPreference("strictOutputs") as CheckBoxPreference?
        cbPref11?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (cbPref11!!.isChecked) {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.STRICT_OUTPUTS, false)
            } else {
                PrefsUtil.getInstance(activity).setValue(PrefsUtil.STRICT_OUTPUTS, true)
            }
            true
        }


        val psbtPref = findPreference("psbt") as Preference?
        psbtPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doPSBT()
            true
        }

        val whirlpoolGUIPref = findPreference("whirlpool_gui") as Preference?
        whirlpoolGUIPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doWhirlpoolGUIPairing()
            true
        }

    }

    private fun troubleShootSettings() {
        val troubleshootPref = findPreference("troubleshoot") as Preference?
        troubleshootPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doTroubleshoot()
            true
        }

        val sendBackupPref = findPreference("send_backup_support") as Preference?
        sendBackupPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.prompt_send_backup_to_support)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { dialog, whichButton -> doSendBackup() }.setNegativeButton(R.string.no) { dialog, whichButton -> }.show()
            true
        }

        val prunePref = findPreference("prune") as Preference?
        prunePref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doPrune()
            true
        }

        val idxPref = findPreference("idx") as Preference?
        idxPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doIndexes()
            true
        }

        val addressCalcPref = findPreference("acalc") as Preference?
        addressCalcPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doAddressCalc()
            true
        }

        val paynymCalcPref = findPreference("pcalc") as Preference?
        paynymCalcPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doPayNymCalc()
            true
        }

        val wpStatePref = findPreference("wpstate") as Preference?
        wpStatePref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            doWhirlpoolState()
            true
        }

    }

    private fun otherSettings() {
        val aboutPref = findPreference("about") as Preference?
        aboutPref?.summary = "Samourai," + " " + resources.getString(R.string.version_name)
        aboutPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(activity, AboutActivity::class.java)
            startActivity(intent)
            true
        }
    }

    private fun getHDSeed(mnemonic: Boolean) {
        var seed: String? = null
        try {
            seed = if (mnemonic) {
                HD_WalletFactory.getInstance(requireContext()).get().mnemonic
            } else {
                HD_WalletFactory.getInstance(requireContext()).get().seedHex
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
        } catch (mle: MnemonicLengthException) {
            mle.printStackTrace()
            Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
        }
        val showText = TextView(requireContext())
        showText.text = seed
        showText.setTextIsSelectable(true)
        showText.setPadding(40, 10, 40, 10)
        showText.textSize = 18.0f
        activity?.getWindow()?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setView(showText)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, whichButton -> }.show()
    }

    private fun getXPUB(purpose: Int, account: Int) {
        var xpub = ""
        if((purpose == 44 || purpose == 49) && account == WhirlpoolMeta.getInstance(context).whirlpoolPostmix) {

            var vpub = BIP84Util.getInstance(requireContext()).wallet.getAccountAt(WhirlpoolMeta.getInstance(context).whirlpoolPostmix).zpubstr()

            if(purpose == 49) {
                xpub = FormatsUtil.xlatXPUB(vpub, true);
            }
            else {
                xpub = FormatsUtil.xlatXPUB(vpub, false);
            }

        }
        else {
            when (purpose) {
                49 -> xpub = BIP49Util.getInstance(requireContext()).wallet.getAccount(account).ypubstr()
                84 -> xpub = BIP84Util.getInstance(requireContext()).wallet.getAccountAt(account).zpubstr()
                else -> try {
                    xpub = HD_WalletFactory.getInstance(requireContext()).get().getAccount(account).xpubstr()
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                    Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
                } catch (mle: MnemonicLengthException) {
                    mle.printStackTrace()
                    Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        var dialogTitle = when (purpose) {
            44 -> "BIP44"
            84 -> "Segwit ZPUB"
            49 -> "Segwit YPUB"
            else -> "XPUB"
        }

        when (account) {
            WhirlpoolAccount.POSTMIX.accountIndex -> {
                if(purpose == 49){
                    dialogTitle = "Whirlpool Post-mix YPUB"
                }
                else if(purpose == 44){
                    dialogTitle = "Whirlpool Post-mix XPUB"
                }
                else{
                    dialogTitle = "Whirlpool Post-mix ZPUB"
                }
            }
            WhirlpoolAccount.PREMIX.accountIndex -> {
                dialogTitle = "Whirlpool Pre-mix ZPUB"
            }
            WhirlpoolAccount.BADBANK.accountIndex -> {
                dialogTitle = "Whirlpool Bad bank ZPUB"
            }
            else -> dialogTitle
        }
        val dialog = QRBottomSheetDialog(
                qrData = xpub,
                dialogTitle, clipboardLabel = dialogTitle
        );
        dialog.show(requireActivity().supportFragmentManager, dialog.tag)
    }

    private fun doTroubleshoot() {
        try {
            if(!ExternalBackupManager.backupAvailable()){
                Toast.makeText(context, "Backup file is not available. please enable auto-backup to continue", Toast.LENGTH_SHORT).show()
                return
            }
            val strExpected = HD_WalletFactory.getInstance(requireContext()).get().passphrase
            val view = layoutInflater.inflate(R.layout.password_input_dialog_layout, null)
            val password = view.findViewById<EditText>(R.id.restore_dialog_password_edittext)
            val message = view.findViewById<TextView>(R.id.dialogMessage)
            message.text = getString(R.string.wallet_passphrase);
            val dlg = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_name)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialog, whichButton ->
                        val _passphrase39 = password.text.toString()
                        if (_passphrase39 == strExpected) {
                            Toast.makeText(requireContext(), R.string.bip39_match, Toast.LENGTH_SHORT).show()
                            if (ExternalBackupManager.backupAvailable()) {
                                MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.bip39_decrypt_test)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.yes) { dialog, whichButton ->
                                            scope.launch(Dispatchers.IO) {
                                               val data =  ExternalBackupManager.read()
                                                val decrypted = PayloadUtil.getInstance(requireContext()).getDecryptedBackupPayload(data, CharSequenceX(_passphrase39))
                                                withContext(Dispatchers.Main){
                                                    if (decrypted == null || decrypted.isEmpty()) {
                                                        Toast.makeText(requireContext(), R.string.backup_read_error, Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(requireContext(), R.string.backup_read_ok, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }

                                        }.setNegativeButton(R.string.no) { dialog, whichButton -> }.show()
                            }
                        } else {
                            Toast.makeText(requireContext(), R.string.invalid_passphrase, Toast.LENGTH_SHORT).show()
                        }
                    }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }
            if (!requireActivity().isFinishing()) {
                dlg.show()
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
        } catch (mle: MnemonicLengthException) {
            mle.printStackTrace()
            Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doPrune() {
        val dlg = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.prune_backup)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, whichButton ->
                    try {

//                            BIP47Meta.getInstance().pruneIncoming();
                        SendAddressUtil.getInstance().reset()
                        RicochetMeta.getInstance(requireContext()).empty()
                        BatchSendUtil.getInstance().clear()
                        RBFUtil.getInstance().clear()
                        PayloadUtil.getInstance(requireContext()).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(requireContext()).guid + AccessFactory.getInstance(requireContext()).pin))
                    } catch (je: JSONException) {
                        je.printStackTrace()
                        Toast.makeText(requireContext(), R.string.error_reading_payload, Toast.LENGTH_SHORT).show()
                    } catch (mle: MnemonicLengthException) {
                    } catch (ioe: IOException) {
                    } catch (de: DecryptionException) {
                    }
                }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }
        if (!requireActivity().isFinishing()) {
            dlg.show()
        }
    }

    private fun doSendBackup() {
        try {
            val jsonObject = PayloadUtil.getInstance(requireContext()).payload
            jsonObject.getJSONObject("wallet").remove("seed")
            jsonObject.getJSONObject("wallet").remove("passphrase")
            if (jsonObject.has("meta") && jsonObject.getJSONObject("meta").has("trusted_node")) {
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("password")
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("node")
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("port")
                jsonObject.getJSONObject("meta").getJSONObject("trusted_node").remove("user")
            }
            val email = Intent(Intent.ACTION_SEND)
            email.putExtra(Intent.EXTRA_EMAIL, arrayOf("help@samourai.support"))
            email.putExtra(Intent.EXTRA_SUBJECT, "Samourai Wallet support backup")
            email.putExtra(Intent.EXTRA_TEXT, jsonObject.toString())
            email.type = "message/rfc822"
            startActivity(Intent.createChooser(email, requireContext().getText(R.string.choose_email_client)))
        } catch (je: JSONException) {
            je.printStackTrace()
            Toast.makeText(requireContext(), R.string.error_reading_payload, Toast.LENGTH_SHORT).show()
        }
    }

    private fun doScanHexTx() {

        val cameraFragmentBottomSheet =  CameraFragmentBottomSheet()
        cameraFragmentBottomSheet.show(requireActivity().supportFragmentManager, cameraFragmentBottomSheet.tag)

        cameraFragmentBottomSheet.setQrCodeScanListener { code: String? ->
            cameraFragmentBottomSheet.dismissAllowingStateLoss()
            code?.let {
                doBroadcastHex(it)
            }
        }
    }

    private fun doIndexes() {
        val builder = StringBuilder()
        var idxBIP84External = 0
        var idxBIP84Internal = 0
        var idxBIP49External = 0
        var idxBIP49Internal = 0
        var idxBIP44External = 0
        var idxBIP44Internal = 0
        var idxPremixExternal = 0
        //        int idxPremixInternal = 0;
        var idxPostmixExternal = 0
        var idxPostmixInternal = 0
        idxBIP84External = BIP84Util.getInstance(requireContext()).wallet.getAccount(0).receive.addrIdx
        idxBIP84Internal = BIP84Util.getInstance(requireContext()).wallet.getAccount(0).change.addrIdx
        idxBIP49External = BIP49Util.getInstance(requireContext()).wallet.getAccount(0).receive.addrIdx
        idxBIP49Internal = BIP49Util.getInstance(requireContext()).wallet.getAccount(0).change.addrIdx
        //        idxPremixExternal = BIP49Util.getInstance(SettingsActivity2.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(SettingsActivity2.this).getWhirlpoolPremixAccount()).getReceive().getAddrIdx();
//        idxPremixInternal = BIP49Util.getInstance(SettingsActivity2.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(SettingsActivity2.this).getWhirlpoolPremixAccount()).getChange().getAddrIdx();
        idxPremixExternal = AddressFactory.getInstance(requireContext()).highestPreReceiveIdx
        //        idxPremixInternal = AddressFactory.getInstance(SettingsActivity2.this).getHighestPreChangeIdx();
//        idxPostmixExternal = BIP49Util.getInstance(SettingsActivity2.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(SettingsActivity2.this).getWhirlpoolPostmix()).getReceive().getAddrIdx();
//        idxPostmixInternal = BIP49Util.getInstance(SettingsActivity2.this).getWallet().getAccountAt(WhirlpoolMeta.getInstance(SettingsActivity2.this).getWhirlpoolPostmix()).getChange().getAddrIdx();
        idxPostmixExternal = AddressFactory.getInstance(requireContext()).highestPostReceiveIdx
        idxPostmixInternal = AddressFactory.getInstance(requireContext()).highestPostChangeIdx
        try {
            idxBIP44External = HD_WalletFactory.getInstance(requireContext()).get().getAccount(0).receive.addrIdx
            idxBIP44Internal = HD_WalletFactory.getInstance(requireContext()).get().getAccount(0).change.addrIdx
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
        } catch (mle: MnemonicLengthException) {
            mle.printStackTrace()
            Toast.makeText(requireContext(), "HD wallet error", Toast.LENGTH_SHORT).show()
        }
        builder.append("44 receive: $idxBIP44External\n")
        builder.append("44 change: $idxBIP44Internal\n")
        builder.append("49 receive: $idxBIP49External\n")
        builder.append("49 change: $idxBIP49Internal\n")
        builder.append("84 receive :$idxBIP84External\n")
        builder.append("84 change :$idxBIP84Internal\n")
        builder.append("""
    Ricochet :${RicochetMeta.getInstance(requireContext()).index}
    
    """.trimIndent())
        builder.append("Premix receive :$idxPremixExternal\n")
        //        builder.append("Premix change :" + idxPremixInternal + "\n");
        builder.append("Postmix receive :$idxPostmixExternal\n")
        builder.append("Postmix change :$idxPostmixInternal\n")
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(builder.toString())
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, whichButton -> dialog.dismiss() }
                .show()
    }

    private fun doAddressCalc() {
        val intent = Intent(requireContext(), AddressCalcActivity::class.java)
        startActivity(intent)
    }

    private fun doPayNymCalc() {
        val intent = Intent(requireContext(), PayNymCalcActivity::class.java)
        startActivity(intent)
    }

    private fun utxoToString(whirlpoolUtxo: WhirlpoolUtxo): String {
        val builder = StringBuilder()
        val utxo = whirlpoolUtxo.utxo
        builder.append("[").append(utxo.tx_hash).append(":").append(utxo.tx_output_n).append("] ").append(utxo.value.toString() + "sats").append(", ").append(utxo.confirmations).append("confs")
        builder.append(", ").append(if (whirlpoolUtxo.poolId != null) whirlpoolUtxo.poolId else "no pool")
        builder.append(", ").append(whirlpoolUtxo.mixsDone.toString()).append(" mixed")
        builder.append(", ").append(whirlpoolUtxo.account).append(", ").append(whirlpoolUtxo.utxo.path)
        builder.append(", ").append(whirlpoolUtxo.utxoState)
        return builder.toString()
    }

    private fun doWhirlpoolState() {
        val whirlpoolWalletService = AndroidWhirlpoolWalletService.getInstance()
        val whirlpoolWallet = whirlpoolWalletService.whirlpoolWalletOrNull
        val builder = StringBuilder()

        // whirlpool wallet status
        if (whirlpoolWallet == null) {
            builder.append("WHIRLPOOL IS CLOSED\n")
        } else {
            val SEPARATOR = "---------------------------\n"
            val mixingState = whirlpoolWallet.mixingState
            builder.append("""
    WHIRLPOOL IS ${if (mixingState.isStarted) "RUNNING" else "STOPPED"}
    
    
    """.trimIndent())
            builder.append("""${mixingState.nbQueued} QUEUED (${mixingState.nbQueuedMustMix}+${mixingState.nbQueuedLiquidity})
""")

            // mixing threads
            builder.append("\n")
            builder.append("""${mixingState.nbMixing} MIXING (${mixingState.nbMixingMustMix}+${mixingState.nbMixingLiquidity})
""")
            builder.append(SEPARATOR)
            for (whirlpoolUtxo in mixingState.utxosMixing) {
                val mixProgress = whirlpoolUtxo.utxoState.mixProgress
                if (mixProgress != null) {
                    builder.append("* ").append("""
    ${utxoToString(whirlpoolUtxo)}
    
    """.trimIndent())
                }
            }

            // wallet state
            builder.append("WALLET STATE\n");
            builder.append(whirlpoolWallet.walletSupplier.walletStateSupplier.value.toString()+"\n")

            // premix
            builder.append("\n")
            val premixs = whirlpoolWallet.utxoSupplier.findUtxos(WhirlpoolAccount.PREMIX)
            builder.append("""${premixs.size} PREMIXS
""")
            builder.append(SEPARATOR)
            for (whirlpoolUtxo in premixs) {
                builder.append("* ").append("""
    ${utxoToString(whirlpoolUtxo)}
    
    """.trimIndent())
            }

            // postmix
            builder.append("\n")
            val postmixs = whirlpoolWallet.utxoSupplier.findUtxos(WhirlpoolAccount.POSTMIX)
            builder.append("""${postmixs.size} POSTMIXS
""")
            builder.append(SEPARATOR)
            for (whirlpoolUtxo in postmixs) {
                builder.append("* ").append("""
    ${utxoToString(whirlpoolUtxo)}
    
    """.trimIndent())
            }
        }
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(builder.toString())
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, whichButton -> dialog.dismiss() }
                .show()
    }

    private fun doBroadcastHex() {
        val dlg = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.tx_hex)
                .setCancelable(true)
                .setPositiveButton(R.string.enter_tx_hex) { dialog, whichButton ->
                    val edHexTx = EditText(requireContext())
                    edHexTx.isSingleLine = false
                    edHexTx.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    edHexTx.setLines(10)
                    edHexTx.setHint(R.string.tx_hex)
                    edHexTx.gravity = Gravity.START
                    val textWatcher: TextWatcher = object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {
                            edHexTx.setSelection(0)
                        }

                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                        }

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        }
                    }
                    edHexTx.addTextChangedListener(textWatcher)
                    val dlg = AlertDialog.Builder(requireContext())
                            .setTitle(R.string.app_name)
                            .setView(edHexTx)
                            .setMessage(R.string.enter_tx_hex)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok) { dialog, whichButton ->
                                val strHexTx = edHexTx.text.toString().trim { it <= ' ' }
                                doBroadcastHex(strHexTx)
                            }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }
                    if (!requireActivity().isFinishing()) {
                        dlg.show()
                    }
                }.setNegativeButton(R.string.scan) { dialog, whichButton -> doScanHexTx() }
        if (!requireActivity().isFinishing()) {
            dlg.show()
        }
    }

    private fun doBroadcastHex(strHexTx: String) {
        val tx = Transaction(SamouraiWallet.getInstance().currentNetworkParams, Hex.decode(strHexTx))
        val msg: String = requireContext().getString(R.string.broadcast).toString() + ":" + tx.hashAsString + " ?"
        val dlg = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, whichButton ->
                    if (progress != null && progress!!.isShowing()) {
                        progress!!.dismiss()
                        progress = null
                    }
                    progress = ProgressDialog(requireContext())
                    progress!!.setCancelable(false)
                    progress!!.setTitle(R.string.app_name)
                    progress!!.setMessage(getString(R.string.please_wait))
                    progress!!.show()
                    Thread {
                        Looper.prepare()
                        PushTx.getInstance(requireContext()).pushTx(strHexTx)
                        progress!!.dismiss()
                        Looper.loop()
                    }.start()
                }.setNegativeButton(R.string.cancel) { dialog, whichButton -> }
        if (!requireActivity().isFinishing()) {
            dlg.show()
        }
    }

    private fun doPSBT() {
        val edPSBT = EditText(requireContext())
        edPSBT.isSingleLine = false
        edPSBT.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        edPSBT.setLines(10)
        edPSBT.setHint(R.string.PSBT)
        edPSBT.gravity = Gravity.START
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                edPSBT.setSelection(0)
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        }
        edPSBT.addTextChangedListener(textWatcher)
        val dlg = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setView(edPSBT)
                .setMessage(R.string.enter_psbt)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, whichButton ->
                    dialog.dismiss()
                    val strPSBT = edPSBT.text.toString().replace(" ".toRegex(), "").trim { it <= ' ' }
                    try {
                        PSBTUtil.getInstance(requireContext()).doPSBT(strPSBT)
                    } catch (e: Exception) {
                    }
                }.setNegativeButton(R.string.cancel) { dialog, whichButton -> dialog.dismiss() }
        if (!requireActivity().isFinishing()) {
            dlg.show()
        }
    }


    private fun doWhirlpoolGUIPairing() {

        fun showQR(pairingObj: JSONObject) {
            val dialog = QRBottomSheetDialog(
                    qrData = pairingObj.toString(),
                    getString(R.string.whirlpool_pairing), clipboardLabel = getString(R.string.whirlpool_pairing)
            );
            dialog.show(requireActivity().supportFragmentManager, dialog.tag)
        }

        val pairingObj = JSONObject()
        val jsonObj = JSONObject()
        val dojoObj = JSONObject()
        try {
            if (DojoUtil.getInstance(requireContext()).dojoParams != null) {
                val params = DojoUtil.getInstance(requireContext()).dojoParams
                val url = DojoUtil.getInstance(requireContext()).getUrl(params)
                val apiKey = DojoUtil.getInstance(requireContext()).getApiKey(params)
                if (url != null && apiKey != null && url.isNotEmpty() && apiKey.isNotEmpty()) {
                    dojoObj.put("apikey", apiKey)
                    dojoObj.put("url", url)
                }
            }
            jsonObj.put("type", "whirlpool.gui")
            jsonObj.put("version", "3.0.0")
            jsonObj.put("network", if (SamouraiWallet.getInstance().isTestNet) "testnet" else "mainnet")
            val mnemonic = HD_WalletFactory.getInstance(requireContext()).get().mnemonic
            if (SamouraiWallet.getInstance().hasPassphrase(requireContext())) {
                val encrypted = AESUtil.encrypt(mnemonic, CharSequenceX(HD_WalletFactory.getInstance(requireContext()).get().passphrase), AESUtil.DefaultPBKDF2Iterations)
                jsonObj.put("passphrase", true)
                jsonObj.put("mnemonic", encrypted)
                pairingObj.put("pairing", jsonObj)
                if (dojoObj.has("url") && dojoObj.has("apikey")) {
                    val apiKey = dojoObj.getString("apikey")
                    val encryptedApiKey = AESUtil.encrypt(apiKey, CharSequenceX(HD_WalletFactory.getInstance(requireContext()).get().passphrase))
                    dojoObj.put("apikey", encryptedApiKey)
                    pairingObj.put("dojo", dojoObj)
                }
                showQR(pairingObj)
            } else {

                val builder = MaterialAlertDialogBuilder(requireContext())
                builder.setTitle(getString(R.string.enter_pairing_password))
                val inflater = layoutInflater
                val view = inflater.inflate(R.layout.password_input_dialog_layout, null)
                val password = view.findViewById<EditText>(R.id.restore_dialog_password_edittext)
                val message = view.findViewById<TextView>(R.id.dialogMessage)
                message.setText(R.string.pairing_password)
                builder.setPositiveButton(R.string.confirm) { dialog: DialogInterface, which: Int ->
                    val pw = password.text.toString()
                    if (pw.length >= AppUtil.MIN_BACKUP_PW_LENGTH && pw.length <= AppUtil.MAX_BACKUP_PW_LENGTH) {
                        val encrypted = AESUtil.encrypt(mnemonic, CharSequenceX(pw), AESUtil.DefaultPBKDF2Iterations)
                        jsonObj.put("passphrase", false)
                        jsonObj.put("mnemonic", encrypted)
                        if (dojoObj.has("url") && dojoObj.has("apikey")) {
                            val apiKey = dojoObj.getString("apikey")
                            val encryptedApiKey = AESUtil.encrypt(apiKey, CharSequenceX(pw))
                            dojoObj.put("apikey", encryptedApiKey)
                            pairingObj.put("dojo", dojoObj)
                        }
                        pairingObj.put("pairing", jsonObj)
                        showQR(pairingObj)
                    }else{
                        Toast.makeText(requireContext(), R.string.password_error, Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                builder.setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
                builder.setView(view)
                builder.show()
            }

        } catch (Er: Exception) {

        }
    }


    override fun onDestroy() {
        if (scope.isActive) {
            scope.cancel(CancellationException())
        }
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ExternalBackupManager.onActivityResult(requestCode, resultCode, data, requireActivity().application)
        super.onActivityResult(requestCode, resultCode, data)

    }
}