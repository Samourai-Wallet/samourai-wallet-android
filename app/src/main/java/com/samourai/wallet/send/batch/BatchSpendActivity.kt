package com.samourai.wallet.send.batch

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.*
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.google.common.base.Splitter
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.TxAnimUIActivity
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.cahoots.Cahoots
import com.samourai.wallet.cahoots.psbt.PSBTUtil
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.fragments.PaynymSelectModalFragment
import com.samourai.wallet.fragments.PaynymSelectModalFragment.Companion.newInstance
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.paynym.paynymDetails.PayNymDetailsActivity
import com.samourai.wallet.segwit.BIP84Util
import com.samourai.wallet.segwit.SegwitAddress
import com.samourai.wallet.segwit.bech32.Bech32Util
import com.samourai.wallet.send.*
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.UTXO.UTXOComparator
import com.samourai.wallet.send.cahoots.ManualCahootsActivity
import com.samourai.wallet.util.*
import com.samourai.wallet.utxos.UTXOSActivity
import com.samourai.wallet.whirlpool.WhirlpoolConst
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_batch_spend.*
import kotlinx.android.synthetic.main.batch_spend_compose.*
import kotlinx.android.synthetic.main.batch_spend_review.*
import kotlinx.android.synthetic.main.fee_selector.*
import kotlinx.android.synthetic.main.item_send_review_details.*
import kotlinx.coroutines.*
import org.bitcoinj.core.Transaction
import org.bouncycastle.util.encoders.Hex
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.net.URLDecoder
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.*


class BatchSpendActivity : SamouraiActivity() {


    private var strPCode: String? = null
    private var tx: Transaction? = null
    private lateinit var totalBTC: TextView
    private lateinit var satEditText: EditText
    private lateinit var btcEditText: EditText
    private lateinit var toAddressEditText: EditText
    private lateinit var toAddressEditTextLayout: TextInputLayout
    private var destAddress: String? = null
    private var amount = 0L
    private var menu: Menu? = null
    private val viewModel: BatchSpendViewModel by viewModels()
    private val reviewFragment = ReviewFragment()
    private val composeFragment = ComposeFragment()
    private var selectedId: Long? = null
    private var balance = 0L
    private var isInReviewMode = false;
    private var changeAmount: Long = 0L
    private var fee: BigInteger = BigInteger.ZERO
    private var change_idx: Int = 0
    private var change_address: String? = null
    private var composeJob:Job? = null
    private var outpoints: MutableList<MyTransactionOutPoint> = mutableListOf()
    private var receivers: HashMap<String, BigInteger> = hashMapOf()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_spend)
        setSupportActionBar(findViewById(R.id.toolbar_batch_send))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toAddressEditText = findViewById(R.id.edt_send_to)
        toAddressEditTextLayout = findViewById(R.id.to_edt_layout)
        btcEditText = findViewById(R.id.amountBTC)
        totalBTC = findViewById(R.id.totalBTC)
        satEditText = findViewById(R.id.amountSat)

        satEditText.addTextChangedListener(satWatcher)
        toAddressEditText.addTextChangedListener(addressWatcher)
        btcEditText.addTextChangedListener(btcWatcher)

        btcEditText.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8, 8))

        listenBalance()

        viewModel.setBalance(applicationContext,account)

        showCompose()

        toAddressEditTextLayout.setEndIconOnClickListener {
            if (toAddressEditText.text.isEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val string = if (clipboard.hasPrimaryClip()) {
                    val item = clipboard.primaryClip?.getItemAt(0)
                    item?.text.toString()
                } else {
                    ""
                }
                toAddressEditText.setText(string)
                validate()
            } else {
                toAddressEditText.setText("")
                validate()
            }
        }

        addToBatch.setOnClickListener {
            this.hidekeyboard()
            if (validate()) {
                val batchItem = BatchSendUtil.BatchSend().apply {
                    this.amount = this@BatchSpendActivity.amount.toLong()
                    this.addr = destAddress
                    if (strPCode != null) {
                        this.pcode = strPCode;
                    }
                    this.UUID = selectedId ?: System.currentTimeMillis()
                }
                selectedId = null
                viewModel.add(batchItem)
                strPCode = null
                destAddress = null
                setToAddress("")
                btcEditText.setText("0")
                btcEditText.setSelection(btcEditText.text.length)
            }
        }

        viewModel.getBatchListLive().observe(this, {
            to_address_review.text = "${it.size} ${getString(R.string.recipients)}"
            send_review_amount.text = "${FormatsUtil.getBTCDecimalFormat(viewModel.getBatchAmount())} BTC"
        })

        val disposable = APIFactory.getInstance(applicationContext)
                .walletBalanceObserver
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewModel.setBalance(applicationContext,account)
                }) { obj: Throwable -> obj.printStackTrace() }
        compositeDisposable.add(disposable)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.send_menu, menu)
        menu.findItem(R.id.action_batch).isVisible = false
        menu.findItem(R.id.action_clear_batch).isVisible = true
        menu.findItem(R.id.action_ricochet).isVisible = false
        menu.findItem(R.id.action_empty_ricochet).isVisible = false
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }


    private fun validate(): Boolean {
        var isValid = false
        var insufficientFunds = false
        var btcAmount = 0.0

        val strBTCAddress = destAddress ?: ""

        if (strBTCAddress.startsWith("bitcoin:")) {
            setToAddress(strBTCAddress.substring(8))
        }
        if (strPCode == null)
            setToAddress(strBTCAddress)

        btcAmount = try {
            NumberFormat.getInstance(Locale.US).parse(btcEditText.text.toString()).toDouble()
        } catch (nfe: java.lang.NumberFormatException) {
            0.0
        } catch (pe: ParseException) {
            0.0
        }
        val dAmount: Double = btcAmount
        val amountRounded = (dAmount * 1e8)
        val walletBalance =  viewModel.totalWalletBalance() ?: 0
        if (amountRounded > walletBalance) {
            insufficientFunds = true
        }

        isValid = if (amountRounded >= SamouraiWallet.bDust.toLong() && FormatsUtil.getInstance().isValidBitcoinAddress(strBTCAddress)) {
            true
        } else amountRounded >= SamouraiWallet.bDust.toLong() && FormatsUtil.getInstance().isValidBitcoinAddress(strBTCAddress)

        if (isValid && !insufficientFunds) {
            amount = amountRounded.toLong()
        }

        addToBatch.isEnabled = isValid && !insufficientFunds

        addToBatch.text = if (this.selectedId == null) getString(R.string.add_to_batch) else getString(R.string.update_to_batch)
        return isValid && !insufficientFunds;

    }

    @SuppressLint("SetTextI18n")
    private fun listenBalance() {
        viewModel.getBalance().observe(this, {
            it?.let{
                balance = it
                totalBTC.text = "${FormatsUtil.getBTCDecimalFormat(it)} BTC"
                batchCurrentAmount.text = getString(R.string.current_batch) + " (${FormatsUtil.getBTCDecimalFormat(viewModel.getBatchAmount())} BTC)"
            }
        })
    }

    override fun onDestroy() {
        if(!compositeDisposable.isDisposed){
            compositeDisposable.dispose()
        }
        try {
            PayloadUtil.getInstance(applicationContext)
                    .saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(applicationContext).guid + AccessFactory.getInstance(applicationContext).pin))
        } catch (e: Exception) {
        }
        composeJob?.let {
            if(it.isActive)
                it.cancel()
        }
        super.onDestroy()
    }

    private val btcWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {
            var editable = editable
            satEditText.removeTextChangedListener(satWatcher)
            btcEditText.removeTextChangedListener(this)
            try {
                if (editable.toString().isEmpty()) {
                    satEditText.setText("0")
                    btcEditText.setText("")
                    satEditText.setSelection(satEditText.getText().length)
                    satEditText.addTextChangedListener(satWatcher)
                    btcEditText.addTextChangedListener(this)
                    return
                }
                var btc = editable.toString().toDouble()
                if (btc > 21000000.0) {
                    btcEditText.setText("0.00")
                    btcEditText.setSelection(btcEditText.getText().length)
                    satEditText.setText("0")
                    satEditText.setSelection(satEditText.getText().length)
                    Toast.makeText(this@BatchSpendActivity, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                } else {
                    val format = DecimalFormat.getInstance(Locale.US) as DecimalFormat
                    val symbols = format.decimalFormatSymbols
                    val defaultSeparator = Character.toString(symbols.decimalSeparator)
                    val max_len = 8
                    val btcFormat = NumberFormat.getInstance(Locale.US)
                    btcFormat.maximumFractionDigits = max_len + 1
                    try {
                        val d = NumberFormat.getInstance(Locale.US).parse(editable.toString()).toDouble()
                        val s1 = btcFormat.format(d)
                        if (s1.indexOf(defaultSeparator) != -1) {
                            var dec = s1.substring(s1.indexOf(defaultSeparator))
                            if (dec.isNotEmpty()) {
                                dec = dec.substring(1)
                                if (dec.length > max_len) {
                                    btcEditText.setText(s1.substring(0, s1.length - 1))
                                    btcEditText.setSelection(btcEditText.text.length)
                                    editable = btcEditText.editableText
                                    btc = btcEditText.text.toString().toDouble()
                                }
                            }
                        }
                    } catch (nfe: java.lang.NumberFormatException) {
                    } catch (pe: ParseException) {
                    }
                    val sats: Double = getSatValue(java.lang.Double.valueOf(btc))
                    satEditText.setText(formattedSatValue(sats))
                }

//
            } catch (e: java.lang.NumberFormatException) {
                e.printStackTrace()
            }
            satEditText.addTextChangedListener(satWatcher)
            btcEditText.addTextChangedListener(this)
            validate()
        }
    }

    private val addressWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {
            if (editable.toString().isNotEmpty()) {
                if (FormatsUtil.getInstance().isValidBitcoinAddress(editable.toString())) {
                    destAddress = editable.toString();
                }
            }
        }
    }

    private val satWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {
            satEditText.removeTextChangedListener(this)
            btcEditText.removeTextChangedListener(btcWatcher)
            try {
                if (editable.toString().isEmpty()) {
                    btcEditText.setText("0.00")
                    satEditText.setText("")
                    satEditText.addTextChangedListener(this)
                    btcEditText.addTextChangedListener(btcWatcher)
                    return
                }
                val spaceRemoved = editable.toString().replace(" ", "")
                val sats = spaceRemoved.toDouble()
                val btc: Double = getBtcValue(sats)
                val formatted: String? = formattedSatValue(sats)
                satEditText.setText(formatted)
                formatted?.length?.let { satEditText.setSelection(it) }
                btcEditText.setText(String.format(Locale.ENGLISH, "%.8f", btc))
                if (btc > 21000000.0) {
                    btcEditText.setText("0.00")
                    btcEditText.setSelection(btcEditText.text.length)
                    satEditText.setText("0")
                    satEditText.setSelection(satEditText.text.length)
                    Toast.makeText(this@BatchSpendActivity, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.NumberFormatException) {
                e.printStackTrace()
            }
            satEditText.addTextChangedListener(this)
            btcEditText.addTextChangedListener(btcWatcher)
            validate()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        if (item.itemId == R.id.action_clear_batch) {
         MaterialAlertDialogBuilder(this)
                  .setMessage(getString(R.string.confirm_batch_list_clear))
                  .setPositiveButton(R.string.ok) { _, _ ->
                          viewModel.clearBatch()
                  }.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.show()
            return true
        }
        if (item.itemId == R.id.select_paynym) {
            val paynymSelectModalFragment = newInstance(selectListener = object : PaynymSelectModalFragment.Listener {
                override fun onPaynymSelectItemClicked(code: String?) {
                    code?.let { processPCode(code, null) }
                }

            }, getString(R.string.paynym), false)
            paynymSelectModalFragment.show(supportFragmentManager, "paynym_select")
            return true
        }

        // noinspection SimplifiableIfStatement
        when (id) {
            R.id.action_scan_qr -> {
                doScan()
            }
            R.id.action_fees -> {
                doFees()
            }
            R.id.action_support -> {
                doSupport()
            }
            R.id.action_utxo -> {
                doUTXO()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun doUTXO() {
        val intent = Intent(this, UTXOSActivity::class.java)
        if (account != 0) {
            intent.putExtra("_account", account)
        }
        startActivity(intent)
    }

    private fun doSupport() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/section/8-sending-bitcoin")))
    }


    private fun doFees() {
        val highFee = FeeUtil.getInstance().highFee
        val normalFee = FeeUtil.getInstance().normalFee
        val lowFee = FeeUtil.getInstance().lowFee
        var message = getText(R.string.current_fee_selection).toString() + " " + FeeUtil.getInstance().suggestedFee.defaultPerKB.toLong() / 1000L + " " + getText(R.string.slash_sat)
        message += "\n"
        message += getText(R.string.current_hi_fee_value).toString() + " " + highFee.defaultPerKB.toLong() / 1000L + " " + getText(R.string.slash_sat)
        message += "\n"
        message += getText(R.string.current_mid_fee_value).toString() + " " + normalFee.defaultPerKB.toLong() / 1000L + " " + getText(R.string.slash_sat)
        message += "\n"
        message += getText(R.string.current_lo_fee_value).toString() + " " + lowFee.defaultPerKB.toLong() / 1000L + " " + getText(R.string.slash_sat)
        val dlg = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        if (!isFinishing) {
            dlg.show()
        }
    }

    private fun doScan() {
        val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
        cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)

        cameraFragmentBottomSheet.setQrCodeScanListener { code: String? ->
            cameraFragmentBottomSheet.dismissAllowingStateLoss()
            code?.let { processScan(it) }
        }
    }

    private fun processScan(code: String) {
        strPCode = null
        var data = code;
        if (data.contains("https://bitpay.com")) {
            val dlg = MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.no_bitpay)
                    .setCancelable(false)
                    .setPositiveButton(R.string.learn_more) { dialog, whichButton ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://blog.samouraiwallet.com/post/169222582782/bitpay-qr-codes-are-no-longer-valid-important"))
                        startActivity(intent)
                    }.setNegativeButton(R.string.close) { dialog, whichButton -> dialog.dismiss() }
            if (!isFinishing) {
                dlg.show()
            }
            return
        }

        if (Cahoots.isCahoots(data.trim { it <= ' ' })) {
            try {
                val cahootsIntent = ManualCahootsActivity.createIntentResume(this, account, data.trim { it <= ' ' })
                startActivity(cahootsIntent)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.cannot_process_cahoots, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
            return
        }
        if (FormatsUtil.getInstance().isPSBT(data.trim { it <= ' ' })) {
            try {
                PSBTUtil.getInstance(this).doPSBT(data.trim { it <= ' ' })
            } catch (e: Exception) {
            }
            return
        }

        if (FormatsUtil.getInstance().isValidPaymentCode(data)) {
            processPCode(data, null)
            return
        }

        if (data.startsWith("BITCOIN:")) {
            data = "bitcoin:" + data.substring(8)
        }

        if (FormatsUtil.getInstance().isBitcoinUri(data)) {
            val address = FormatsUtil.getInstance().getBitcoinAddress(data)
            val amount = FormatsUtil.getInstance().getBitcoinAmount(data)
            setToAddress(address)
            if (amount != null) {
                try {
                    val btcFormat = NumberFormat.getInstance(Locale.US)
                    btcFormat.maximumFractionDigits = 8
                    btcFormat.minimumFractionDigits = 1
                } catch (nfe: NumberFormatException) {
//                    setToAddress("0.0");
                }
            }
            val strAmount: String
            val nf = NumberFormat.getInstance(Locale.US)
            nf.minimumIntegerDigits = 1
            nf.minimumFractionDigits = 1
            nf.maximumFractionDigits = 8
            try {
                if (amount != null && amount.toDouble() != 0.0) {
                    //                    selectPaynymBtn.setEnabled(false);
//                    selectPaynymBtn.setAlpha(0.5f);
                    //                    Toast.makeText(this, R.string.no_edit_BIP21_scan, Toast.LENGTH_SHORT).show();
                }
            } catch (nfe: NumberFormatException) {
//                enableAmount(true)
            }
        } else if (FormatsUtil.getInstance().isValidBitcoinAddress(data)) {
            if (FormatsUtil.getInstance().isValidBech32(data)) {
                setToAddress(data.toLowerCase())
            } else {
                setToAddress(data)
            }
        } else if (data.contains("?")) {
            var pcode: String = data.substring(0, data.indexOf("?"))
            // not valid BIP21 but seen often enough
            if (pcode.startsWith("bitcoin://")) {
                pcode = pcode.substring(10)
            }
            if (pcode.startsWith("bitcoin:")) {
                pcode = pcode.substring(8)
            }
            if (FormatsUtil.getInstance().isValidPaymentCode(pcode)) {
                processPCode(pcode, data.substring(data.indexOf("?")))
            }
        } else {
            Toast.makeText(this, R.string.scan_error, Toast.LENGTH_SHORT).show()
        }

    }

    private fun processPCode(pcode: String, meta: String?) {
        var meta: String? = meta

        if (FormatsUtil.getInstance().isValidPaymentCode(pcode)) {
            if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_CFM) {
                try {
                    val _pcode = PaymentCode(pcode)
                    val paymentAddress = BIP47Util.getInstance(this).getSendAddress(_pcode, BIP47Meta.getInstance().getOutgoingIdx(pcode))
                    destAddress = if (BIP47Meta.getInstance().getSegwit(pcode)) {
                        val segwitAddress = SegwitAddress(paymentAddress.sendECKey, SamouraiWallet.getInstance().currentNetworkParams)
                        segwitAddress.bech32AsString
                    } else {
                        paymentAddress.sendECKey.toAddress(SamouraiWallet.getInstance().currentNetworkParams).toString()
                    }
                    strPCode = _pcode.toString()
                    setToAddress(BIP47Meta.getInstance().getDisplayLabel(strPCode))
                } catch (e: java.lang.Exception) {
                    Toast.makeText(this, R.string.error_payment_code, Toast.LENGTH_SHORT).show()
                }
            } else {
                val intent = Intent(this, PayNymDetailsActivity::class.java)
                intent.putExtra("pcode", pcode)
                intent.putExtra("label", "")
                if (meta != null && meta.startsWith("?") && meta.length > 1) {
                    meta = meta.substring(1)
                    if (meta.isNotEmpty()) {
                        var _meta: String? = null
                        var map: Map<String?, String> = HashMap()
                        meta.length
                        try {
                            _meta = URLDecoder.decode(meta, "UTF-8")
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        }
                        map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(_meta)
                        intent.putExtra("label", if (map.containsKey("title")) map["title"]!!.trim { it <= ' ' } else "")
                    }
                }
            }
        } else {
            Toast.makeText(this, R.string.invalid_payment_code, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setToAddress(displayLabel: String?) {
        displayLabel?.let {
            toAddressEditText.setText(it)
        }
    }

    private fun getBtcValue(sats: Double): Double {
        return (sats / 1e8)
    }

    private fun formattedSatValue(number: Any): String? {
        val nFormat = NumberFormat.getNumberInstance(Locale.US)
        val decimalFormat = nFormat as DecimalFormat
        decimalFormat.applyPattern("#,###")
        return decimalFormat.format(number).replace(",", " ")
    }

    private fun getSatValue(btc: Double): Double {
        return if (btc == 0.0) {
            0.toDouble()
        } else btc * 1e8
    }

    private fun showReview() {
        val sharedAxis = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        TransitionManager.beginDelayedTransition(batchDetailContainer, sharedAxis)
        reviewFragment.enterTransition = sharedAxis
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.batchDetailContainer, reviewFragment)
                .commit()
        isInReviewMode = true

        reviewFragment.setOnFeeChangeListener {
            composeJob =  viewModel.viewModelScope.launch(Dispatchers.Default) {
                delay(300)
                withContext(Dispatchers.Main){
                    prepareSpend()
                }
            }
        }
        reviewFragment.setOnClickListener {
            this.initiateSpend()
        }

        val sharedAxis2 = MaterialSharedAxis(MaterialSharedAxis.Y, true)
                .apply {
                    addTarget(reviewForm)
                }
        TransitionManager.beginDelayedTransition(appBarLayoutBatch, sharedAxis2)
        sendForm.visibility = View.GONE
        reviewForm.visibility = View.VISIBLE
        addToBatch.visibility = View.INVISIBLE
        this.menu?.findItem(R.id.select_paynym)?.isVisible = false
        this.menu?.findItem(R.id.action_scan_qr)?.isVisible = false
    }

    private fun showCompose() {
        val sharedAxis = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
            addTarget(sendForm)
        }
        TransitionManager.beginDelayedTransition(batchDetailContainer, sharedAxis)
        composeFragment.enterTransition = sharedAxis
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.batchDetailContainer, composeFragment)
                .commit()
        isInReviewMode = false

        composeFragment.setOnReviewClickListener {
            showReview()
        }
        composeFragment.setOnItemClickListener {
            if (it.pcode == null) {
                this.destAddress = it.addr
                this.setToAddress(it.addr)
            } else {
                this.processPCode(it.pcode, null)
            }
            this.satEditText.setText("${it.amount}")
            this.selectedId = it.UUID
            validate()
        }
        val sharedAxis2 = MaterialSharedAxis(MaterialSharedAxis.Y, false)
                .apply {
                    addTarget(sendForm)
                }
        TransitionManager.beginDelayedTransition(appBarLayoutBatch, sharedAxis2)
        sendForm.visibility = View.VISIBLE
        addToBatch.visibility = View.VISIBLE
        reviewForm.visibility = View.INVISIBLE

        this.menu?.findItem(R.id.select_paynym)?.isVisible = true
        this.menu?.findItem(R.id.action_scan_qr)?.isVisible = true

    }

    @Synchronized
    fun prepareSpend() {
        //Resets current receivers,outpoints etc..
        this.reset()
        for (_data in viewModel.getBatchList()) {
              LogUtil.debug("BatchSendActivity", "output:" + _data.amount)
              LogUtil.debug("BatchSendActivity", "output:" + _data.addr)
              LogUtil.debug("BatchSendActivity", "output:" + _data.pcode)
            amount += _data.amount
            if (receivers.containsKey(_data.addr)) {
                val _amount = receivers[_data.addr]
                receivers[_data.addr] = _amount!!.add(BigInteger.valueOf(_data.amount))
            } else {
                receivers[_data.addr] = BigInteger.valueOf(_data.amount)
            }
        }

        var utxos: List<UTXO> = arrayListOf();
        if (account == 0) {
            utxos = APIFactory.getInstance(applicationContext).getUtxos(true)
        } else if (account == WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT) {
            utxos = APIFactory.getInstance(applicationContext).getUtxosPostMix(true)
        }

        Collections.sort(utxos, UTXOComparator())

        val selectedUTXO: MutableList<UTXO> = ArrayList()
        var p2pkh = 0
        var p2sh_p2wpkh = 0
        var p2wpkh = 0
        var totalValueSelected = 0L
        var totalSelected = 0

        for (utxo in utxos) {
            LogUtil.debug("BatchSendActivity", "utxo value:" + utxo.value)
            selectedUTXO.add(utxo)
            totalValueSelected += utxo.value
            totalSelected += utxo.outpoints.size
            val outpointTypes = FeeUtil.getInstance().getOutpointCount(Vector(utxo.outpoints))
            p2pkh += outpointTypes.left
            p2sh_p2wpkh += outpointTypes.middle
            p2wpkh += outpointTypes.right
            if (totalValueSelected >= amount + SamouraiWallet.bDust.toLong() + FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, receivers.size + 1).toLong()) {
                break
            }
        }

          LogUtil.debug("BatchSendActivity", "totalSelected:$totalSelected")
          LogUtil.debug("BatchSendActivity", "totalValueSelected:$totalValueSelected")

        for (utxo in selectedUTXO) {
            outpoints.addAll(utxo.outpoints)
            for (out in utxo.outpoints) {
                  LogUtil.debug("BatchSendActivity", "outpoint hash:" + out.txHash.toString())
                  LogUtil.debug("BatchSendActivity", "outpoint idx:" + out.txOutputN)
                  LogUtil.debug("BatchSendActivity", "outpoint address:" + out.address)
            }
        }
        val outpointTypes = FeeUtil.getInstance().getOutpointCount(Vector(outpoints))
        fee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.left, outpointTypes.middle, outpointTypes.right, receivers.size + 1)
        val walletBalance =  viewModel.totalWalletBalance() ?: 0L
        if (amount + fee.toLong() > walletBalance) {
            reviewFragment.setTotalMinerFees(BigInteger.ZERO)
            Snackbar
                    .make(appBarLayoutBatch.rootView,R.string.insufficient_funds, Snackbar.LENGTH_SHORT)
                    .setAnchorView(reviewFragment.getSendButton())
                    .show()
            reviewFragment.enableSendButton(false)
            return
        }
        reviewFragment.enableSendButton(true)

        val changeAmount: Long = totalValueSelected - (amount + fee.toLong())
        change_idx = 0
        if (changeAmount > 0L) {
            change_idx = BIP84Util.getInstance(applicationContext).wallet.getAccount(0).change.addrIdx
            change_address = BIP84Util.getInstance(applicationContext).getAddressAt(AddressFactory.CHANGE_CHAIN, change_idx).bech32AsString
            receivers[change_address!!] = BigInteger.valueOf(changeAmount!!)
              LogUtil.debug("BatchSendActivity", "change output:$changeAmount")
              LogUtil.debug("BatchSendActivity", "change output:$change_address")
        }
        else    {
            reviewFragment.setTotalMinerFees(BigInteger.ZERO)
            Toast.makeText(applicationContext, R.string.error_change_output, Toast.LENGTH_SHORT).show()
            return
        }

        tx = SendFactory.getInstance(applicationContext).makeTransaction(0, outpoints, receivers)

        if (tx != null) {
            val rbf: RBFSpend?
            if (PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.RBF_OPT_IN, false)) {
                rbf = RBFSpend()
                for (input in tx!!.inputs) {
                    var _isBIP49 = false
                    var _isBIP84 = false
                    var _addr: String? = null
                    val script = Hex.toHexString(input.connectedOutput!!.getScriptBytes())
                    if (Bech32Util.getInstance().isBech32Script(script)) {
                        try {
                            _addr = Bech32Util.getInstance().getAddressFromScript(script)
                            _isBIP84 = true
                        } catch (e: java.lang.Exception) {
                        }
                    } else {
                        val _address = input.connectedOutput!!.getAddressFromP2SH(SamouraiWallet.getInstance().currentNetworkParams)
                        if (_address != null) {
                            _addr = _address.toString()
                            _isBIP49 = true
                        }
                    }
                    if (_addr == null) {
                        _addr = input.connectedOutput!!.getAddressFromP2PKHScript(SamouraiWallet.getInstance().currentNetworkParams).toString()
                    }
                    val path = APIFactory.getInstance(applicationContext).unspentPaths[_addr]
                    if (path != null) {
                        when {
                            _isBIP84 -> {
                                rbf.addKey(input.outpoint.toString(), "$path/84")
                            }
                            _isBIP49 -> {
                                rbf.addKey(input.outpoint.toString(), "$path/49")
                            }
                            else -> {
                                rbf.addKey(input.outpoint.toString(), path)
                            }
                        }
                    } else {
                        val pcode = BIP47Meta.getInstance().getPCode4Addr(_addr)
                        val idx = BIP47Meta.getInstance().getIdx4Addr(_addr)
                        rbf.addKey(input.outpoint.toString(), "$pcode/$idx")
                    }
                }
            } else {
                rbf = null
            }
            val signedTx = SendFactory.getInstance(application).signTransaction(tx, account)
            reviewFragment.setFeeRate(fee.toDouble() / signedTx.virtualTransactionSize)
            reviewFragment.setTotalMinerFees(fee)
        }
    }


    private fun initiateSpend() {
        val strMessage = "Send ${FormatsUtil.getBTCDecimalFormat(amount.toLong())} BTC. (fee: ${FormatsUtil.getBTCDecimalFormat(fee.toLong())})"

        val _change_idx = change_idx
        val _amount = amount
        var SignedTx = SendFactory.getInstance(applicationContext).signTransaction(tx, 0)
        val hexTx = String(Hex.encode(SignedTx.bitcoinSerialize()))

        val dlg = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(strMessage)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { dialog, whichButton ->
                    dialog.dismiss()
                    if (!PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.BROADCAST_TX, true)) {
//                        doShowTx(hexTx, strTxHash)
                        val dialog = QRBottomSheetDialog(
                                qrData = hexTx,
                                title = "",
                                clipboardLabel = "dialogTitle"
                        );
                        dialog.show(supportFragmentManager, dialog.tag)
                        return@OnClickListener
                    }
                    SendParams.getInstance().setParams(outpoints,
                            receivers,
                            viewModel.getBatchList(),
                            SendActivity.SPEND_SIMPLE,
                            changeAmount,
                            84,
                            0,
                            "",
                            false,
                            false,
                            _amount.toLong(),
                            _change_idx
                    )
                    val intent = Intent(this, TxAnimUIActivity::class.java)
                    startActivity(intent)
                }).setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
        if (!isFinishing) {
            dlg.show()
        }
    }

    private fun reset() {
        changeAmount = 0L
        amount = 0L
        fee = BigInteger.ZERO
        change_idx = 0
        change_address = null
        outpoints = mutableListOf()
        receivers = hashMapOf()
    }

    override fun onBackPressed() {
        if (isInReviewMode) {
            showCompose()
        } else {
            super.onBackPressed()
        }
    }


}