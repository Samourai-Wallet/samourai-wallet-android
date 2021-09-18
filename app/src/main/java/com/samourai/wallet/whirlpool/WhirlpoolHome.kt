package com.samourai.wallet.whirlpool

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.cahoots.Cahoots
import com.samourai.wallet.cahoots.psbt.PSBTUtil
import com.samourai.wallet.databinding.ActivityWhirlpoolHomeBinding
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.home.BalanceActivity
import com.samourai.wallet.network.NetworkDashboard
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.SendActivity
import com.samourai.wallet.send.cahoots.ManualCahootsActivity
import com.samourai.wallet.service.JobRefreshService
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.util.LogUtil
import com.samourai.wallet.utxos.PreSelectUtil
import com.samourai.wallet.utxos.UTXOSActivity
import com.samourai.wallet.whirlpool.fragments.SectionsPagerAdapter
import com.samourai.wallet.whirlpool.fragments.WhirlPoolLoaderDialog
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity
import com.samourai.wallet.whirlpool.newPool.WhirlpoolDialog
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhirlpoolHome : SamouraiActivity() {

    private lateinit var binding: ActivityWhirlpoolHomeBinding
    private val whirlPoolHomeViewModel by viewModels<WhirlPoolHomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhirlpoolHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.loading)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }
        val whirlPoolLoaderDialog = WhirlPoolLoaderDialog()
        whirlPoolLoaderDialog.setOnInitComplete {
            initPager()
        }
        val wallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWalletOrNull
        if (wallet == null) {
            whirlPoolLoaderDialog.show(supportFragmentManager, whirlPoolLoaderDialog.tag)
        } else {
            if (!wallet.isStarted) {
                whirlPoolLoaderDialog.show(supportFragmentManager, whirlPoolLoaderDialog.tag)
            } else {
                initPager()
            }
        }
        binding.fab.setOnClickListener {
            val whirlpoolDialog = WhirlpoolDialog()
            whirlpoolDialog.show(supportFragmentManager, whirlpoolDialog.tag)
        }
        binding.viewPager.addOnPageChangeListener(pageListener)
        val filterDisplay = IntentFilter(BalanceActivity.DISPLAY_INTENT)
        LocalBroadcastManager.getInstance(this@WhirlpoolHome)
            .registerReceiver(receiver, filterDisplay)

    }

    //Checks if there is any previous postmix or premix activities
    private fun checkOnboardStatus() {
        whirlPoolHomeViewModel.viewModelScope
            .launch(Dispatchers.Default) {
                val postmix = APIFactory.getInstance(applicationContext).allPostMixTxs.size == 0
                val premix = APIFactory.getInstance(applicationContext).premixXpubTxs.isEmpty()
                var hasMixUtxos = false;
                if (AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.isPresent) {
                    hasMixUtxos =
                        AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.get().utxoSupplier.utxos.isEmpty()
                }
                LogUtil.debug(TAG, "checkOnboardStatus:  ${postmix && premix && hasMixUtxos}")
                withContext(Dispatchers.Main) {
                    if (whirlPoolHomeViewModel.onboardStatus.value != postmix && premix && hasMixUtxos) {
                        whirlPoolHomeViewModel.setOnBoardingStatus(postmix && premix && hasMixUtxos)
                    }
                }
            }
    }

    private fun initPager() {
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
        whirlPoolHomeViewModel.remixBalance.observe(this, {
            if (viewPager.currentItem == 3) {
                binding.whirlpoolToolbar.title = FormatsUtil.formatBTC(it)
            }
        })

        whirlPoolHomeViewModel.totalBalance.observe(this, {
            if (viewPager.currentItem == 0) {
                binding.whirlpoolToolbar.title = FormatsUtil.formatBTC(it)
            }
        })

        whirlPoolHomeViewModel.mixingBalance.observe(this, {
            if (viewPager.currentItem == 1) {
                binding.whirlpoolToolbar.title = FormatsUtil.formatBTC(it)
            }
        })
        try {
            validateIntentAndStartNewPool()
        } catch (ex: Exception) {

        }
        checkOnboardStatus()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkOnboardStatus()
        }
    }

    private val pageListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            var balance = 0L;
            when (position) {
                0 -> {
                    binding.whirlpoolAmountCaption.text = "Total Balance"
                    whirlPoolHomeViewModel.totalBalance.value?.let {
                        balance = it
                    }
                }
                1 -> {
                    binding.whirlpoolAmountCaption.text = "Total in Progress Balance "
                    whirlPoolHomeViewModel.mixingBalance.value?.let {
                        balance = it
                    }
                }
                2 -> {
                    binding.whirlpoolAmountCaption.text = "Total Remixable Balance "
                    whirlPoolHomeViewModel.remixBalance.value?.let {
                        balance = it
                    }
                }
            }
            binding.whirlpoolToolbar.title = FormatsUtil.formatBTC(balance)
            binding.toolbar.title = FormatsUtil.formatBTC(balance)
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEWPOOL_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.isPresent) {
                initPager()
                val intent = Intent(this, JobRefreshService::class.java)
                intent.putExtra("notifTx", false)
                intent.putExtra("dragged", true)
                intent.putExtra("launch", false)
                JobRefreshService.enqueueWork(applicationContext, intent)
            }
        }
    }


    private fun validateIntentAndStartNewPool() {
        if (intent.extras != null && intent.extras!!.containsKey("preselected")) {
            val intent = Intent(applicationContext, NewPoolActivity::class.java)
            val account = getIntent().extras!!.getInt("_account")
            intent.putExtra("_account", getIntent().extras!!.getInt("_account"))
            intent.putExtra("preselected", getIntent().extras!!.getString("preselected"))
            if (account == WhirlpoolMeta.getInstance(application).whirlpoolPostmix) {
                val coins = PreSelectUtil.getInstance().getPreSelected(
                    getIntent().extras!!.getString("preselected")
                )
                val mediumFee = FeeUtil.getInstance().normalFee.defaultPerKB.toLong() / 1000L
                val tx0 = WhirlpoolTx0(
                    WhirlpoolMeta.getInstance(this@WhirlpoolHome).minimumPoolDenomination,
                    mediumFee,
                    1,
                    coins
                )
                try {
                    tx0.make()
                } catch (ex: Exception) {
                    Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
                    ex.printStackTrace()
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setMessage("Tx0 is not possible with selected utxo.")
                        .setCancelable(true)
                    builder.setPositiveButton(R.string.ok) { dialogInterface, i -> dialogInterface.dismiss() }
                    builder.create().show()
                    return
                }
                if (tx0.tx0 == null) {
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setMessage("Tx0 is not possible with selected utxo.")
                        .setCancelable(true)
                    builder.setPositiveButton(R.string.ok) { dialogInterface, i -> dialogInterface.dismiss() }
                    builder.create().show()
                } else {
                    startActivityForResult(intent, NEWPOOL_REQ_CODE)
                }
            } else {
                startActivityForResult(intent, NEWPOOL_REQ_CODE)
            }
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this@WhirlpoolHome)
            .unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.whirlpool_main, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) finish()
        if (id == R.id.action_utxo) {
            val intent = Intent(this@WhirlpoolHome, UTXOSActivity::class.java)
            intent.putExtra(
                "_account",
                WhirlpoolMeta.getInstance(this@WhirlpoolHome).whirlpoolPostmix
            )
            startActivity(intent)
        } else if (id == R.id.action_menu_view_post_mix) {
            val intent = Intent(this@WhirlpoolHome, BalanceActivity::class.java)
            intent.putExtra(
                "_account",
                WhirlpoolMeta.getInstance(this@WhirlpoolHome).whirlpoolPostmix
            )
            startActivity(intent)
        } else if (id == R.id.action_scode) {
            doSCODE()
        } else if (id == R.id.action_scan_qr) {
            val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
            cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
            cameraFragmentBottomSheet.setQrCodeScanListener { code: String ->
                cameraFragmentBottomSheet.dismissAllowingStateLoss()
                try {
                    if (Cahoots.isCahoots(code.trim { it <= ' ' })) {
                        val cahootIntent = ManualCahootsActivity.createIntentResume(
                            this,
                            WhirlpoolMeta.getInstance(application).whirlpoolPostmix,
                            code.trim { it <= ' ' })
                        startActivity(cahootIntent)
                    } else if (FormatsUtil.getInstance().isPSBT(code.trim { it <= ' ' })) {
                        PSBTUtil.getInstance(application).doPSBT(code.trim { it <= ' ' })
                    } else if (DojoUtil.getInstance(application)
                            .isValidPairingPayload(code.trim { it <= ' ' })
                    ) {
                        val intent = Intent(application, NetworkDashboard::class.java)
                        intent.putExtra("params", code.trim { it <= ' ' })
                        startActivity(intent)
                    } else {
                        val intent = Intent(application, SendActivity::class.java)
                        intent.putExtra("uri", code.trim { it <= ' ' })
                        intent.putExtra(
                            "_account",
                            WhirlpoolMeta.getInstance(application).whirlpoolPostmix
                        )
                        startActivity(intent)
                    }
                } catch (e: java.lang.Exception) {
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun doSCODE() {
        val scode = EditText(this@WhirlpoolHome)

        val strCurrentCode = WhirlpoolMeta.getInstance(this@WhirlpoolHome).scode
        if (strCurrentCode != null && strCurrentCode.length > 0) {
            scode!!.setText(strCurrentCode)
        }

        val layout = LinearLayout(this@WhirlpoolHome)
        scode.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        layout.addView(scode)
        MaterialAlertDialogBuilder(this@WhirlpoolHome)
            .setTitle(R.string.app_name)
            .setMessage(R.string.enter_scode)
            .setView(layout)
            .setNeutralButton("Remove SCODE") { dialog: DialogInterface?, which: Int ->
                WhirlpoolMeta.getInstance(this@WhirlpoolHome).scode = ""
                WhirlpoolNotificationService.stopService(applicationContext)
                saveState()
                whirlPoolHomeViewModel.viewModelScope.launch(Dispatchers.Default) {
                    delay(1000)
                    withContext(Dispatchers.Main) {
                        val _intent = Intent(this@WhirlpoolHome, BalanceActivity::class.java)
                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(_intent)
                    }
                }
            }
            .setPositiveButton(R.string.ok) { dialog, whichButton ->

                val strSCODE = scode.text.toString().trim { it <= ' ' }
                if (strSCODE.isNotBlank()) {
                    WhirlpoolMeta.getInstance(this@WhirlpoolHome).scode = strSCODE
                    WhirlpoolNotificationService.stopService(applicationContext)
                    saveState()
                    whirlPoolHomeViewModel.viewModelScope.launch(Dispatchers.Default) {
                        delay(1000)
                        withContext(Dispatchers.Main) {
                            val _intent = Intent(this@WhirlpoolHome, BalanceActivity::class.java)
                            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(_intent)
                        }
                    }
                }
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.show()

    }


    companion object {
        const val NEWPOOL_REQ_CODE = 6102
        private const val TAG = "WhirlpoolHome"
     }

}