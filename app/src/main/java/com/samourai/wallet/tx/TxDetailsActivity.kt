package com.samourai.wallet.tx

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.MainActivity2
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.api.Tx
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.send.RBFUtil
import com.samourai.wallet.send.SendActivity
import com.samourai.wallet.send.boost.CPFPTask
import com.samourai.wallet.send.boost.RBFTask
import com.samourai.wallet.util.DateUtil
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.widgets.CircleImageView
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.bitcoinj.core.Coin
import org.json.JSONException
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class TxDetailsActivity : SamouraiActivity() {
    private var payNymAvatar: CircleImageView? = null
    private var payNymUsername: TextView? = null
    private var amount: TextView? = null
    private var txStatus: TextView? = null
    private var txId: TextView? = null
    private var txDate: TextView? = null
    private var bottomButton: MaterialButton? = null
    private var minerFee: TextView? = null
    private var minerFeeRate: TextView? = null
    private var tx: Tx? = null
    private var BTCDisplayAmount: String? = null
    private var SatDisplayAmount: String? = null
    private var paynymDisplayName: String? = null
    private var rbfTask: RBFTask? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tx)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (intent.hasExtra("TX")) {
            try {
                val TxJsonObject = JSONObject(intent.getStringExtra("TX"))
                tx = Tx(TxJsonObject)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        payNymUsername = findViewById(R.id.tx_paynym_username)
        amount = findViewById(R.id.tx_amount)
        payNymAvatar = findViewById(R.id.img_paynym_avatar)
        txId = findViewById(R.id.transaction_id)
        txStatus = findViewById(R.id.tx_status)
        txDate = findViewById(R.id.tx_date)
        bottomButton = findViewById(R.id.btn_bottom_button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progressBar = findViewById(R.id.progressBar)
        minerFee = findViewById(R.id.tx_miner_fee_paid)
        minerFeeRate = findViewById(R.id.tx_miner_fee_rate)
        amount?.setOnClickListener { toggleUnits() }
        setTx()
        bottomButton?.setOnClickListener {
            if (isBoostingAvailable) {
                doBoosting()
            } else {
                refundOrPayAgain()
            }
        }
        txId?.setOnClickListener { view: View ->
            MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.txid_to_clipboard)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                        val clipboard = this@TxDetailsActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData = ClipData.newPlainText("tx id", (view as TextView).text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this@TxDetailsActivity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    }.setNegativeButton(R.string.no) { _: DialogInterface?, _: Int -> }.show()
        }
    }

    private fun refundOrPayAgain() {
        val intent = Intent(this, SendActivity::class.java)
        intent.putExtra("pcode", tx!!.paymentCode)
        if (!isSpend) {
            intent.putExtra("amount", tx!!.amount)
        }
        startActivity(intent)
    }

    private fun setTx() {
        calculateBTCDisplayAmount(tx!!.amount.toLong())
        calculateSatoshiDisplayAmount(tx!!.amount.toLong())
        amount?.text = BTCDisplayAmount
        bottomButton!!.visibility = View.GONE
        if (tx!!.confirmations <= 3) {
            txStatus!!.setTextColor(ContextCompat.getColor(this, R.color.tx_broadcast_offline_bg))
            val txConfirmation = getString(R.string.unconfirmed) +
                    " (" +
                    tx!!.confirmations +
                    "/3)"
            txStatus!!.text = txConfirmation
        }
        if (tx!!.confirmations > 3) {
            val txConfirmation = tx!!.confirmations.toString() +
                    " " +
                    getString(R.string.confirmation)
            txStatus!!.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            txStatus!!.text = txConfirmation
            bottomButton?.visibility = View.GONE;
        }
        txId!!.text = tx!!.hash
        txDate!!.text = DateUtil.getInstance(this).formatted(tx!!.ts)
        if (tx!!.paymentCode != null) {
            bottomButton!!.visibility = View.VISIBLE
            paynymDisplayName = BIP47Meta.getInstance().getDisplayLabel(tx!!.paymentCode)
            showPaynym()
            if (isSpend) {
                bottomButton!!.setText(R.string.pay_again)
            } else {
                bottomButton!!.setText(R.string.refund)
            }
        }
        if (isBoostingAvailable) {
            bottomButton!!.visibility = View.VISIBLE
            bottomButton!!.setText(R.string.boost_transaction_fee)
        }
        fetchTxDetails()
    }

    private fun doBoosting() {
        val message = getString(R.string.options_unconfirmed_tx)
        if (isRBFPossible) {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle(R.string.app_name)
            builder.setMessage(message)
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.options_bump_fee) { dialog: DialogInterface?, whichButton: Int -> RBFBoost() }
            builder.setNegativeButton(R.string.cancel) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
            builder.create().show()
            return
        } else {
            if (isCPFPPossible) {
                val builder = MaterialAlertDialogBuilder(this@TxDetailsActivity)
                builder.setTitle(R.string.app_name)
                builder.setMessage(message)
                builder.setCancelable(true)
                builder.setPositiveButton(R.string.options_bump_fee) { dialog: DialogInterface?, whichButton: Int -> CPFBoost() }
                builder.setNegativeButton(R.string.cancel) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                builder.create().show()
            }
        }
    }

    private fun CPFBoost() {
        progressBar?.visibility = View.VISIBLE
       CoroutineScope(Dispatchers.IO).launch {
            val cpfp = CPFPTask(applicationContext, tx!!.hash)
            try {
                val message = cpfp.checkCPFP()
                withContext(Dispatchers.Main) {
                    val dlg: MaterialAlertDialogBuilder= MaterialAlertDialogBuilder(this@TxDetailsActivity)
                            .setTitle(R.string.app_name)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                doCPFPSpend(cpfp)
                            }.setNegativeButton(R.string.cancel) { _, _ ->
                                cpfp.reset()
                            }
                    if (!isFinishing) {
                        dlg.show()
                    }
                }
            } catch (e: CPFPTask.CPFPException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                }
            }

        }
               .invokeOnCompletion {
                   if(it != null){
                   }
                   runBlocking {
                       withContext(Dispatchers.Main){
                           progressBar?.visibility = View.GONE
                       }
                   }
               }
    }

    private fun doCPFPSpend(cpfpTaskKt: CPFPTask) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                progressBar?.visibility = View.VISIBLE
            }
            try {
                cpfpTaskKt.doCPFP()
            } catch (ex: Exception) {
                throw CancellationException(ex.message)
            }
        }.invokeOnCompletion {
            runBlocking {
                withContext(Dispatchers.Main){
                    progressBar?.visibility = View.GONE
                    if (it != null) {
                        Toast.makeText(this@TxDetailsActivity, it.message, Toast.LENGTH_SHORT).show()
                    } else {
                        bottomButton?.visibility = View.GONE
                        Toast.makeText(this@TxDetailsActivity, R.string.cpfp_spent, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@TxDetailsActivity, MainActivity2::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        this@TxDetailsActivity.startActivity(intent)
                    }
                }
            }

        }
    }

    private fun RBFBoost() {
        if (rbfTask == null || rbfTask!!.status == AsyncTask.Status.FINISHED) {
            rbfTask = RBFTask(this)
            rbfTask!!.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tx!!.hash)
        }
    }

    private val isBoostingAvailable: Boolean
        private get() = tx!!.confirmations < 1
    private val isSpend: Boolean
        private get() = tx!!.amount < 0

    private fun fetchTxDetails() {
        toggleProgress(View.VISIBLE)
        makeTxNetworkRequest()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<JSONObject> {
                    override fun onSubscribe(d: Disposable) {}
                    override fun onNext(jsonObject: JSONObject) {
                        toggleProgress(View.INVISIBLE)
                        try {
                            setFeeInfo(jsonObject)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        toggleProgress(View.INVISIBLE)
                    }

                    override fun onComplete() {}
                })
    }

    /**
     * @param jsonObject
     * @throws JSONException
     */
    @Throws(JSONException::class)
    private fun setFeeInfo(jsonObject: JSONObject) {
        if (jsonObject.has("fees")) {
            minerFee!!.text = jsonObject.getString("fees") + " sats"
        }
        if (jsonObject.has("feerate")) {
            minerFeeRate!!.text = jsonObject.getString("vfeerate") + " sats"
        }
    }

    private fun makeTxNetworkRequest(): Observable<JSONObject> {
        return Observable.create { emitter: ObservableEmitter<JSONObject> -> emitter.onNext(APIFactory.getInstance(this@TxDetailsActivity).getTxInfo(tx!!.hash)) }
    }

    private fun calculateBTCDisplayAmount(value: Long) {
        BTCDisplayAmount = FormatsUtil.formatBTC(value)
    }

    private fun toggleProgress(Visibility: Int) {
        progressBar!!.visibility = Visibility
    }

    private fun toggleUnits() {
        TransitionManager.beginDelayedTransition(amount!!.rootView.rootView as ViewGroup, AutoTransition())
        if (amount?.text?.contains("BTC")!!) {
            amount!!.text = SatDisplayAmount
        } else {
            amount!!.text = BTCDisplayAmount
        }
    }

    private fun calculateSatoshiDisplayAmount(value: Long) {
        SatDisplayAmount = FormatsUtil.formatSats(value)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_block_explore -> {
                doExplorerView()
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Opens external BlockExplorer
     */
    private fun doExplorerView() {
        var blockExplorer = "https://m.oxt.me/transaction/"
        if (SamouraiWallet.getInstance().isTestNet) {
            blockExplorer = "https://blockstream.info/testnet/"
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + tx!!.hash))
        startActivity(browserIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tx_details_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun showPaynym() {
        TransitionManager.beginDelayedTransition(payNymAvatar!!.rootView.rootView as ViewGroup, AutoTransition())
        payNymUsername!!.visibility = View.VISIBLE
        payNymAvatar!!.visibility = View.VISIBLE
        payNymUsername!!.text = paynymDisplayName
        Picasso.get()
                .load(WebUtil.PAYNYM_API + tx!!.paymentCode + "/avatar")
                .into(payNymAvatar)
    }

    /***
     * checks tx can be boosted using
     * Replace-by-fee method
     * @return boolean
     */
    private val isRBFPossible: Boolean
        private get() = tx!!.confirmations < 1 && tx!!.amount < 0.0 && RBFUtil.getInstance().contains(tx!!.hash)

    /***
     * checks tx can be boosted using
     * child pays for parent method
     * @return boolean
     */
    private val isCPFPPossible: Boolean
        private get() {
            val a = tx!!.confirmations < 1 && tx!!.amount >= 0.0
            val b = tx!!.confirmations < 1 && tx!!.amount < 0.0
            return a || b
        }

    companion object {
        private const val TAG = "TxDetailsActivity"
    }
}