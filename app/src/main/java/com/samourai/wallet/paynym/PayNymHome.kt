package com.samourai.wallet.paynym

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.common.base.Splitter
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.crypto.DecryptionException
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.paynym.addPaynym.AddPaynymActivity
import com.samourai.wallet.paynym.fragments.PaynymListFragment
import com.samourai.wallet.paynym.fragments.ShowPayNymQRBottomSheet
import com.samourai.wallet.paynym.paynymDetails.PayNymDetailsActivity
import com.samourai.wallet.util.*
import com.samourai.wallet.widgets.ViewPager
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.spec.InvalidKeySpecException
import java.util.*
import java.util.concurrent.TimeUnit

class PayNymHome : SamouraiActivity() {

    private var paynymTabLayout: TabLayout? = null
    private var payNymViewPager: ViewPager? = null
    private val compositeDisposable: CompositeDisposable? = CompositeDisposable()

    //    private var payNymHomeViewModel: PayNymHomeViewModel? = null
    private var paynymSync: ProgressBar? = null
    private var paynym: TextView? = null
    private var paynymCode: TextView? = null
    private var paymentCodeSyncMessage: TextView? = null
    private var userAvatar: ImageView? = null
    private var paynymFab: FloatingActionButton? = null
    private var followersFragment: PaynymListFragment? = null
    private var followingFragment: PaynymListFragment? = null
    private var pcode: String? = null
    private val tabTitle = arrayOf("Following", "Followers")
    private var followers = ArrayList<String>()
    private var pcodeSyncLayout: ConstraintLayout? = null
    var swipeToRefreshPaynym: SwipeRefreshLayout? = null
    val payNymHomeViewModel: PayNymHomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_nym_home)
        setSupportActionBar(findViewById(R.id.toolbar_paynym))
        paynymTabLayout = findViewById(R.id.paynym_tabs)
        payNymViewPager = findViewById(R.id.paynym_viewpager)
        paynymTabLayout?.setupWithViewPager(payNymViewPager)
        paynym = findViewById(R.id.txtview_paynym)
        paynymCode = findViewById(R.id.paynym_payment_code)
        userAvatar = findViewById(R.id.paybyn_user_avatar)
        paynymFab = findViewById(R.id.paynym_fab)
        payNymViewPager?.enableSwipe(true)
        paymentCodeSyncMessage = findViewById(R.id.payment_code_sync_message)
        paynymSync = findViewById(R.id.progressbar_payment_code_sync)
        pcodeSyncLayout = findViewById(R.id.payment_code_sync_layout)
        swipeToRefreshPaynym = findViewById(R.id.swipeToRefreshPaynym)
        val adapter = ViewPagerAdapter(supportFragmentManager)
        payNymViewPager?.adapter = adapter
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        pcode = BIP47Util.getInstance(this).paymentCode.toString()
        paynymCode?.text = BIP47Meta.getInstance().getDisplayLabel(pcode)
        followersFragment = PaynymListFragment.newInstance()
        followingFragment = PaynymListFragment.newInstance()
        Picasso.with(applicationContext).load(WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar)
        paynymFab?.setOnClickListener {
//            startActivity(Intent(this, AddPaynymActivity::class.java))
            Snackbar.make(paynym!!,"Error : LFSLKFLSKF",Snackbar.LENGTH_LONG).show()

        }
        payNymHomeViewModel.pcode.observe(this, { paymentCode: String? -> paynym?.text = paymentCode })
        payNymHomeViewModel.loaderLiveData.observe(this, {
            Log.i(TAG, "onCreate: ${it}")
            swipeToRefreshPaynym?.isRefreshing = it
        })

        payNymHomeViewModel.errorsLiveData.observe(this, {
            Snackbar.make(paynym!!,"Error : ${it}",Snackbar.LENGTH_LONG).show()
        })
        payNymHomeViewModel.followers.observe(this, { followersList: ArrayList<String>? ->
            if (followersList == null || followersList.size == 0) {
                return@observe
            }
            val filtered = filterArchived(followersList)
            tabTitle[1] = "Followers " + " (" + filtered.size.toString() + ")"
            followersFragment?.addPcodes(followersList)
            adapter.notifyDataSetChanged()
            followers = followersList
        })
        payNymHomeViewModel.following.observe(this, { followingList: ArrayList<String>? ->
            if (followingList == null || followingList.size == 0) {
                return@observe
            }
            val filtered = filterArchived(followingList)
            followingFragment?.addPcodes(filtered)
            tabTitle[0] = "Following " + " (" + filtered.size.toString() + ")"
            adapter.notifyDataSetChanged()
        })
        if (!PrefsUtil.getInstance(this).getValue(PrefsUtil.PAYNYM_CLAIMED, false)) {
            doClaimPayNym()
        }
        swipeToRefreshPaynym?.setOnRefreshListener {
            swipeToRefreshPaynym?.isRefreshing = false
            payNymHomeViewModel.refreshPayNym()
        }

        payNymHomeViewModel.refreshTaskProgressLiveData.observe(this, {
            if (it.first != 0 || it.second != 0) {
                paynymSync?.progress = it.first
                paynymSync?.max = it.second
                pcodeSyncLayout?.visibility = View.VISIBLE
                paymentCodeSyncMessage?.text = this.getString(R.string.sycing_pcodes) + " " + paynymSync!!.progress.toString() + "/" + it.second.toString()
                if (paynymSync?.progress == paynymSync?.max) {
                    pcodeSyncLayout?.visibility = View.GONE
                    Snackbar.make(pcodeSyncLayout!!.rootView, this.getString(R.string.sync_complete), Snackbar.LENGTH_SHORT).show()
                }
            }
        })
        RxJavaPlugins.setErrorHandler { throwable: Throwable? ->
            if (throwable is UndeliverableException) {
                Log.i(TAG, "onCreate: Thread interrupted")
            }
        }
    }

    private fun doClaimPayNym() {
        val intent = Intent(this, ClaimPayNymActivity::class.java)
        startActivityForResult(intent, CLAIM_PAYNYM)
    }

    override fun onDestroy() {
        try {
            PayloadUtil.getInstance(applicationContext).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(applicationContext).guid + AccessFactory.getInstance(applicationContext).pin))
        } catch (e: MnemonicLengthException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: DecryptionException) {
            e.printStackTrace()
        }
        super.onDestroy()
        if (compositeDisposable != null && !compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.action_support -> {
                doSupport()
            }
            R.id.action_scan_qr -> {
                val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
                cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
                cameraFragmentBottomSheet.setQrCodeScanLisenter { code: String ->
                    cameraFragmentBottomSheet.dismissAllowingStateLoss()
                    processScan(code)
                }
            }
            R.id.action_unarchive -> {
                doUnArchive()
            }
            R.id.action_sync_all -> {
                if (!AppUtil.getInstance(this).isOfflineMode) {
                    doSyncAll()
                } else {
                    Toast.makeText(this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show()
                }
            }
            R.id.sign -> {
                doSign()
            }
            R.id.action_paynym_share_qr -> {
                val bundle = Bundle()
                bundle.putString("pcode", pcode)
                val showPayNymQRBottomSheet = ShowPayNymQRBottomSheet()
                showPayNymQRBottomSheet.arguments = bundle
                showPayNymQRBottomSheet.show(supportFragmentManager, showPayNymQRBottomSheet.tag)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bip47_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun doSign() {
        MessageSignUtil.getInstance(this).doSign(this.getString(R.string.bip47_sign),
                this.getString(R.string.bip47_sign_text1),
                this.getString(R.string.bip47_sign_text2),
                BIP47Util.getInstance(this).notificationAddress.ecKey)
    }

    private fun doSupport() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/section/32-paynym"))
        startActivity(intent)
    }

    private fun doUnArchive() {
        val _pcodes = BIP47Meta.getInstance().getSortedByLabels(true)
        //
        // check for own payment code
        //
        try {
            if (_pcodes.contains(BIP47Util.getInstance(this).paymentCode.toString())) {
                _pcodes.remove(BIP47Util.getInstance(this).paymentCode.toString())
                BIP47Meta.getInstance().remove(BIP47Util.getInstance(this).paymentCode.toString())
            }
        } catch (afe: AddressFormatException) {
        }
        for (pcode in _pcodes) {
            BIP47Meta.getInstance().setArchived(pcode, false)
        }
        val pcodes = arrayOfNulls<String>(_pcodes.size)
        for ((i, pcode) in _pcodes.withIndex()) {
            pcodes[i] = pcode
        }
        payNymHomeViewModel.refreshPayNym()
        //
    }

    private fun filterArchived(list: ArrayList<String>?): ArrayList<String> {
        val filtered = ArrayList<String>()
        if (list != null) {
            for (item in list) {
                if (!BIP47Meta.getInstance().getArchived(item)) {
                    filtered.add(item)
                }
            }
        }
        return filtered
    }

    private fun doSyncAll() {
        payNymHomeViewModel.doSyncAll()

    }

    private fun processScan(data: String) {
        var data = data
        if (data.startsWith("bitcoin://") && data.length > 10) {
            data = data.substring(10)
        }
        if (data.startsWith("bitcoin:") && data.length > 8) {
            data = data.substring(8)
        }
        if (FormatsUtil.getInstance().isValidPaymentCode(data)) {
            try {
                if (data == BIP47Util.getInstance(this).paymentCode.toString()) {
                    Toast.makeText(this, R.string.bip47_cannot_scan_own_pcode, Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (afe: AddressFormatException) {
            }
            val intent = Intent(this, PayNymDetailsActivity::class.java)
            intent.putExtra("pcode", data)
            startActivityForResult(intent, EDIT_PCODE)
        } else if (data.contains("?") && data.length >= data.indexOf("?")) {
            val meta = data.substring(data.indexOf("?") + 1)
            var _meta: String? = null
            try {
                var map: Map<String?, String> = HashMap()
                if (meta != null && meta.length > 0) {
                    _meta = URLDecoder.decode(meta, "UTF-8")
                    map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(_meta)
                }
                val intent = Intent(this, AddPaynymActivity::class.java)
                intent.putExtra("pcode", data.substring(0, data.indexOf("?")))
                intent.putExtra("label", if (map.containsKey("title")) map["title"]!!.trim { it <= ' ' } else "")
                startActivityForResult(intent, EDIT_PCODE)
            } catch (uee: UnsupportedEncodingException) {
            } catch (e: Exception) {
            }
        } else {
            Toast.makeText(this, R.string.scan_error, Toast.LENGTH_SHORT).show()
        }
    }

    internal inner class ViewPagerAdapter(manager: FragmentManager?) : FragmentPagerAdapter(manager!!) {
        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                followingFragment!!
            } else {
                followersFragment!!
            }
        }

        override fun getCount(): Int {
            return tabTitle.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return tabTitle[position]
        }
    }

    companion object {
        private const val EDIT_PCODE = 2000
        private const val CLAIM_PAYNYM = 107
        private const val TAG = "PayNymHome"
    }
}