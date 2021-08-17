package com.samourai.wallet.paynym.paynymDetails

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.samourai.http.client.AndroidHttpClient
import com.samourai.http.client.IHttpClient
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.api.Tx
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.SendNotifTxFactory
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.bip47.rpc.SecretPoint
import com.samourai.wallet.crypto.DecryptionException
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.paynym.PayNymViewModel
import com.samourai.wallet.paynym.fragments.EditPaynymBottomSheet
import com.samourai.wallet.paynym.fragments.ShowPayNymQRBottomSheet
import com.samourai.wallet.segwit.BIP49Util
import com.samourai.wallet.segwit.BIP84Util
import com.samourai.wallet.segwit.bech32.Bech32Util
import com.samourai.wallet.send.*
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.UTXO.UTXOComparator
import com.samourai.wallet.tor.TorManager
import com.samourai.wallet.util.*
import com.samourai.wallet.widgets.ItemDividerDecorator
import com.samourai.xmanager.client.XManagerClient
import com.samourai.xmanager.protocol.XManagerService
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_paynym_details.*
import kotlinx.coroutines.*
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException
import org.bitcoinj.script.Script
import org.bouncycastle.util.encoders.DecoderException
import org.bouncycastle.util.encoders.Hex
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.spec.InvalidKeySpecException
import java.util.*

class PayNymDetailsActivity : SamouraiActivity() {

    private var pcode: String? = null
    private var label: String? = null
    private val txesList: MutableList<Tx> = ArrayList()
    private lateinit var paynymTxListAdapter: PaynymTxListAdapter
    private val disposables = CompositeDisposable()
    private var menu: Menu? = null
    private val payNymViewModel: PayNymViewModel by viewModels()
    private var following = false
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paynym_details)
        setSupportActionBar(findViewById(R.id.toolbar_paynym))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        historyRecyclerView.isNestedScrollingEnabled = true
        if (intent.hasExtra("pcode")) {
            pcode = intent.getStringExtra("pcode")
        } else {
            finish()
        }
        if (intent.hasExtra("label")) {
            label = intent.getStringExtra("label")
        }
        paynymTxListAdapter = PaynymTxListAdapter(txesList, this)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = paynymTxListAdapter
        val drawable = ContextCompat.getDrawable(this, R.drawable.divider_grey)
        historyRecyclerView.addItemDecoration(ItemDividerDecorator(drawable))
        setPayNym()
        loadTxes()
        payNymViewModel.followers.observe(this, {
            setPayNym()
        })
        payNymViewModel.errorsLiveData.observe(this, {
            Snackbar.make(paynymCode, "$it", Snackbar.LENGTH_LONG).show()
        })
        payNymViewModel.loaderLiveData.observe(this, {
            progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        })
        followBtn.setOnClickListener { followPaynym() }

    }

    private fun setPayNym() {
        followMessage.text = resources.getString(R.string.follow) + " " + getLabel() + " " + resources.getText(R.string.paynym_follow_message_2).toString()
        if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT) {
            showFollow()
        } else {
            showHistory()
        }
        if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_NO_CFM) {
            showWaitingForConfirm()
        }
        if (BIP47Meta.getInstance().getIncomingIdx(pcode) >= 0) {
            historyLayout!!.visibility = View.VISIBLE
        }
        if (BIP47Meta.getInstance().isFollowing(pcode)) {
            followBtn.text = getString(R.string.connect)
            feeMessage.text = getString(R.string.connect_paynym_fee)
            followMessage.text = "${getString(R.string.blockchain_connect_with)} ${getLabel()} ${resources.getText(R.string.paynym_connect_message)}"
            if (!following)
                addChip(getString(R.string.following))
            following = true
        } else {
            if (!BIP47Meta.getInstance().exists(pcode, true)) {
                feeMessage.text = getString(R.string.follow_paynym_fee_free)
            }
        }
        paynymCode.text = BIP47Meta.getInstance().getAbbreviatedPcode(pcode)
        title = getLabel()
        paynymAvatarProgress.visibility = View.VISIBLE
        Picasso.get()
                .load(WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar, object : Callback {
                    override fun onSuccess() {
                        paynymAvatarProgress.visibility = View.GONE
                    }

                    override fun onError(e: Exception) {
                        paynymAvatarProgress.visibility = View.GONE
                        Toast.makeText(this@PayNymDetailsActivity, "Unable to load avatar", Toast.LENGTH_SHORT).show()
                    }
                })
        if (menu != null) {
            menu!!.findItem(R.id.retry_notiftx).isVisible = BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_NO_CFM
            menu!!.findItem(R.id.action_unfollow).isVisible = BIP47Meta.getInstance().exists(pcode, true)
        }
    }

    private fun addChip(chipText: String) {
        val scale = resources.displayMetrics.density

        if (paynymChipLayout.childCount == 2) {
            return
        }
        paynymChipLayout.addView(Chip(this).apply {
            text = chipText
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply {
                        marginStart = (12 * scale + 0.5f).toInt()
                    }
            setChipBackgroundColorResource(R.color.white)
            setTextColor(ContextCompat.getColor(this@PayNymDetailsActivity, R.color.darkgrey))
        })
    }

    private fun showWaitingForConfirm() {
        historyLayout!!.visibility = View.VISIBLE
        feeMessage!!.visibility = View.GONE
        followLayout!!.visibility = View.VISIBLE
        confirmMessage!!.visibility = View.VISIBLE
        followBtn!!.visibility = View.GONE
        followMessage!!.visibility = View.GONE
    }

    private fun showHistory() {
        addChip("Connected")
        historyLayout!!.visibility = View.VISIBLE
        followLayout!!.visibility = View.GONE
        confirmMessage!!.visibility = View.GONE
    }

    private fun showFollow() {
        historyLayout!!.visibility = View.GONE
        followBtn!!.visibility = View.VISIBLE
        followLayout!!.visibility = View.VISIBLE
        confirmMessage!!.visibility = View.GONE
        followMessage!!.visibility = View.VISIBLE
    }

    private fun showFollowAlert(strAmount: String, onClickListener: View.OnClickListener?) {
        try {
            val dialog = Dialog(this, android.R.style.Theme_Dialog)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.paynym_follow_dialog)
            dialog.setCanceledOnTouchOutside(true)
            if (dialog.window != null) dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val title = dialog.findViewById<TextView>(R.id.follow_title_paynym_dialog)
            val oneTimeFeeMessage = dialog.findViewById<TextView>(R.id.one_time_fee_message)
            title.text = (resources.getText(R.string.follow).toString()
                    + " " + BIP47Meta.getInstance().getLabel(pcode))
            val followBtn = dialog.findViewById<Button>(R.id.follow_paynym_btn)
            val message = resources.getText(R.string.paynym_follow_fee_message).toString()
            val part1 = message.substring(0, 28)
            val part2 = message.substring(29)
            oneTimeFeeMessage.text = "$part1 $strAmount $part2"
            followBtn.setOnClickListener { view: View? ->
                dialog.dismiss()
                onClickListener?.onClick(view)
            }
            dialog.findViewById<View>(R.id.cancel_btn).setOnClickListener { view: View? -> dialog.dismiss() }
            dialog.setCanceledOnTouchOutside(false)
            dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun followPaynym() {
        if (BIP47Meta.getInstance().isFollowing(pcode)) {
                doNotifTx()
        } else {
            MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm)
                    .setMessage("Are you sure want to follow ${getLabel()} ?")
                    .setPositiveButton("Yes") { _, _ ->
                        pcode?.let {
                            pcode?.let { payNymViewModel.doFollow(it) };
                        }
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .show()
        }

    }

    private fun getLabel(): String {
        return if (label == null) BIP47Meta.getInstance().getDisplayLabel(pcode) else label!!
    }

    private fun loadTxes() {
        val disposable = Observable.fromCallable<List<Tx>> {
            val txesListSelected: MutableList<Tx> = ArrayList()
            val txs = APIFactory.getInstance(this).allXpubTxs
            APIFactory.getInstance(applicationContext).xpubAmounts[HD_WalletFactory.getInstance(applicationContext).get().getAccount(0).xpubstr()]
            if (txs != null) for (tx: Tx in txs) {
                if (tx.paymentCode != null) {
                    if ((tx.paymentCode == pcode)) {
                        txesListSelected.add(tx)
                    }
                }
                val hashes = SentToFromBIP47Util.getInstance()[pcode]
                if (hashes != null) for (hash: String in hashes) {
                    if ((hash == tx.hash)) {
                        if (!txesListSelected.contains(tx)) {
                            txesListSelected.add(tx)
                        }
                    }
                }
            }
            txesListSelected
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { txes: List<Tx>? ->
                    txesList.clear()
                    txesList.addAll((txes)!!)
                    paynymTxListAdapter.notifyDataSetChanged()
                }
        disposables.add(disposable)
    }

    override fun onDestroy() {
        disposables.dispose()
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
            }
            R.id.send_menu_paynym_details -> {
                if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_CFM) {
                    val intent = Intent(this, SendActivity::class.java)
                    intent.putExtra("pcode", pcode)
                    startActivity(intent)
                } else {
                    if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT) {
                        followPaynym()
                    } else {
                        Snackbar.make(historyLayout!!.rootView, "Follow transaction is still pending", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            R.id.edit_menu_paynym_details -> {
                val bundle = Bundle()
                bundle.putString("label", getLabel())
                bundle.putString("pcode", pcode)
                bundle.putString("buttonText", "Save")
                val editPaynymBottomSheet = EditPaynymBottomSheet()
                editPaynymBottomSheet.arguments = bundle
                editPaynymBottomSheet.show(supportFragmentManager, editPaynymBottomSheet.tag)
                editPaynymBottomSheet.setSaveButtonListener { view: View? ->
                    updatePaynym(editPaynymBottomSheet.label, editPaynymBottomSheet.pcode)
                    setPayNym()
                }
            }
            R.id.archive_paynym -> {
                BIP47Meta.getInstance().setArchived(pcode, true)
                finish()
            }
            R.id.resync_menu_paynym_details -> {
                doSync()
            }
            R.id.view_code_paynym_details -> {
                val bundle = Bundle()
                bundle.putString("pcode", pcode)
                val showPayNymQRBottomSheet = ShowPayNymQRBottomSheet()
                showPayNymQRBottomSheet.arguments = bundle
                showPayNymQRBottomSheet.show(supportFragmentManager, showPayNymQRBottomSheet.tag)
            }
            R.id.retry_notiftx -> {
                doNotifTx()
            }
            R.id.action_unfollow -> {
                MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.confirm)
                        .setMessage("Are you sure want to unfollow ${getLabel()} ?")
                        .setPositiveButton("Yes") { _, _ ->
                            pcode?.let {
                                payNymViewModel.doUnFollow(it).invokeOnCompletion {
                                    finish()
                                }
                            }
                        }
                        .setNegativeButton("No", { _, _ -> })
                        .show()
            }
            R.id.paynym_indexes -> {
                val outgoing = BIP47Meta.getInstance().getOutgoingIdx(pcode)
                val incoming = BIP47Meta.getInstance().getIncomingIdx(pcode)
                Toast.makeText(this@PayNymDetailsActivity, "Incoming index:$incoming, Outgoing index:$outgoing", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updatePaynym(label: String?, pcode: String?) {
        if (pcode == null || pcode.length < 1 || !FormatsUtil.getInstance().isValidPaymentCode(pcode)) {
            Snackbar.make(userAvatar!!.rootView, R.string.invalid_payment_code, Snackbar.LENGTH_SHORT).show()
        } else if (label == null || label.length < 1) {
            Snackbar.make(userAvatar!!.rootView, R.string.bip47_no_label_error, Snackbar.LENGTH_SHORT).show()
        } else {
            BIP47Meta.getInstance().setLabel(pcode, label)
            Thread {
                Looper.prepare()
                try {
                    PayloadUtil.getInstance(this@PayNymDetailsActivity).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(this@PayNymDetailsActivity).guid + AccessFactory.getInstance().pin))
                } catch (mle: MnemonicLengthException) {
                    mle.printStackTrace()
                    Toast.makeText(this@PayNymDetailsActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                } catch (de: DecoderException) {
                    de.printStackTrace()
                    Toast.makeText(this@PayNymDetailsActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                } catch (je: JSONException) {
                    je.printStackTrace()
                    Toast.makeText(this@PayNymDetailsActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                    Toast.makeText(this@PayNymDetailsActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                } catch (npe: NullPointerException) {
                    npe.printStackTrace()
                    Toast.makeText(this@PayNymDetailsActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                } catch (de: DecryptionException) {
                    de.printStackTrace()
                    Toast.makeText(this@PayNymDetailsActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                } finally {
                }
                Looper.loop()
                doUpdatePayNymInfo(pcode)
            }.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.paynym_details_menu, menu)
        if (pcode != null) {
            menu.findItem(R.id.retry_notiftx).isVisible = BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_NO_CFM
            menu.findItem(R.id.retry_notiftx).isVisible = BIP47Meta.getInstance().exists(pcode, true)
        }
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PCODE) {
            setPayNym()
        }
    }


    private fun doNotifTx() {
        progressBar.visibility = View.VISIBLE
        scope.launch(Dispatchers.IO) {
            val torManager = TorManager
            val httpClient: IHttpClient = AndroidHttpClient(com.samourai.wallet.util.WebUtil.getInstance(applicationContext), torManager)
            val xManagerClient = XManagerClient(SamouraiWallet.getInstance().isTestNet, torManager.isConnected(), httpClient)
            val address = xManagerClient.getAddressOrDefault(XManagerService.BIP47)
            SendNotifTxFactory.getInstance().setAddress(address)
            //
            // get wallet balance
            //
            var balance = 0L
            balance = try {
                APIFactory.getInstance(this@PayNymDetailsActivity).xpubAmounts[HD_WalletFactory.getInstance(this@PayNymDetailsActivity).get().getAccount(0).xpubstr()]!!
            } catch (npe: NullPointerException) {
                0L
            }
            val selectedUTXO: MutableList<UTXO?> = ArrayList()
            var totalValueSelected = 0L
            //        long change = 0L;
            var fee: BigInteger? = null
            //
            // spend dust threshold amount to notification address
            //
            var amount = SendNotifTxFactory._bNotifTxValue.toLong()

            //
            // add Samourai Wallet fee to total amount
            //
            amount += SendNotifTxFactory._bSWFee.toLong()

            //
            // get unspents
            //
            var utxos: MutableList<UTXO?>? = null
            if (UTXOFactory.getInstance().totalP2SH_P2WPKH > amount + FeeUtil.getInstance().estimatedFeeSegwit(0, 1, 0, 4).toLong()) {
                utxos = ArrayList()
                utxos.addAll(UTXOFactory.getInstance().allP2SH_P2WPKH.values)
            } else {
                utxos = APIFactory.getInstance(this@PayNymDetailsActivity).getUtxos(true)
            }

            // sort in ascending order by value
            val _utxos: List<UTXO?>? = utxos
            Collections.sort(_utxos, UTXOComparator())
            Collections.reverse(_utxos)

            //
            // get smallest 1 UTXO > than spend + fee + sw fee + dust
            //
            for (u in _utxos!!) {
                if (u!!.value >= amount + SamouraiWallet.bDust.toLong() + FeeUtil.getInstance().estimatedFee(1, 4).toLong()) {
                    selectedUTXO.add(u)
                    totalValueSelected += u.value
                    Log.d("PayNymDetailsActivity", "value selected:" + u.value)
                    Log.d("PayNymDetailsActivity", "total value selected:$totalValueSelected")
                    Log.d("PayNymDetailsActivity", "nb inputs:" + u.outpoints.size)
                    break
                }
            }

            //
            // use normal fee settings
            //
            val suggestedFee = FeeUtil.getInstance().suggestedFee
            val lo = FeeUtil.getInstance().lowFee.defaultPerKB.toLong() / 1000L
            val mi = FeeUtil.getInstance().normalFee.defaultPerKB.toLong() / 1000L
            val hi = FeeUtil.getInstance().highFee.defaultPerKB.toLong() / 1000L
            if (lo == mi && mi == hi) {
                val hi_sf = SuggestedFee()
                hi_sf.defaultPerKB = BigInteger.valueOf((hi * 1.15 * 1000.0).toLong())
                FeeUtil.getInstance().suggestedFee = hi_sf
            } else if (lo == mi) {
                FeeUtil.getInstance().suggestedFee = FeeUtil.getInstance().highFee
            } else {
                FeeUtil.getInstance().suggestedFee = FeeUtil.getInstance().normalFee
            }
            if (selectedUTXO.size == 0) {
                // sort in descending order by value
                Collections.sort(_utxos, UTXOComparator())
                var selected = 0

                // get largest UTXOs > than spend + fee + dust
                for (u in _utxos) {
                    selectedUTXO.add(u)
                    totalValueSelected += u!!.value
                    selected += u.outpoints.size
                    if (totalValueSelected >= amount + SamouraiWallet.bDust.toLong() + FeeUtil.getInstance().estimatedFee(selected, 4).toLong()) {
                        Log.d("PayNymDetailsActivity", "multiple outputs")
                        Log.d("PayNymDetailsActivity", "total value selected:$totalValueSelected")
                        Log.d("PayNymDetailsActivity", "nb inputs:" + u.outpoints.size)
                        break
                    }
                }

//            fee = FeeUtil.getInstance().estimatedFee(selected, 4);
                fee = FeeUtil.getInstance().estimatedFee(selected, 7)
            } else {
//            fee = FeeUtil.getInstance().estimatedFee(1, 4);
                fee = FeeUtil.getInstance().estimatedFee(1, 7)
            }

            //
            // reset fee to previous setting
            //
            FeeUtil.getInstance().suggestedFee = suggestedFee

            //
            // total amount to spend including fee
            //
            if (amount + fee.toLong() >= balance) {
                scope.launch(Dispatchers.Main) {
                    progressBar.visibility = View.INVISIBLE
                    var message: String? = getText(R.string.bip47_notif_tx_insufficient_funds_1).toString() + " "
                    val biAmount = SendNotifTxFactory._bSWFee.add(SendNotifTxFactory._bNotifTxValue.add(FeeUtil.getInstance().estimatedFee(1, 4, FeeUtil.getInstance().lowFee.defaultPerKB)))
                    val strAmount = FormatsUtil.formatBTC(biAmount.toLong());
                    message += strAmount
                    message += " " + getText(R.string.bip47_notif_tx_insufficient_funds_2)
                    val dlg = MaterialAlertDialogBuilder(this@PayNymDetailsActivity)
                            .setTitle(R.string.app_name)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.help) { _, _ ->
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.samourai.io/wallet/usage#follow-paynyms"))
                                startActivity(browserIntent)
                            }
                            .setNegativeButton(R.string.close) { dialog, _ -> dialog.dismiss() }
                    if (!isFinishing) {
                        dlg.show()
                    }
                }
                return@launch
            }

            //
            // payment code to be notified
            //
            val payment_code: PaymentCode?
            payment_code = try {
                PaymentCode(pcode)
            } catch (afe: AddressFormatException) {
                null
            }
            if (payment_code == null) {
                return@launch
            }

            //
            // create outpoints for spend later
            //
            val outpoints: MutableList<MyTransactionOutPoint> = ArrayList()
            for (u in selectedUTXO) {
                outpoints.addAll(u!!.outpoints)
            }
            //
            // create inputs from outpoints
            //
            val inputs: MutableList<MyTransactionInput> = ArrayList()
            for (o in outpoints) {
                val script = Script(o.scriptBytes)
                if (script.scriptType == Script.ScriptType.NO_TYPE) {
                    continue
                }
                val input = MyTransactionInput(SamouraiWallet.getInstance().currentNetworkParams, null, ByteArray(0), o, o.txHash.toString(), o.txOutputN)
                inputs.add(input)
            }
            //
            // sort inputs
            //
            Collections.sort(inputs, SendFactory.BIP69InputComparator())
            //
            // find outpoint that corresponds to 0th input
            //
            var outPoint: MyTransactionOutPoint? = null
            for (o in outpoints) {
                if (o.txHash.toString() == inputs[0].getTxHash() && o.txOutputN == inputs[0].getTxPos()) {
                    outPoint = o
                    break
                }
            }
            if (outPoint == null) {
                throw Exception(getString(R.string.bip47_cannot_identify_outpoint))
            }
            var op_return: ByteArray? = null
            //
            // get private key corresponding to outpoint
            //
            try {
//            Script inputScript = new Script(outPoint.getConnectedPubKeyScript());
                val scriptBytes = outPoint?.connectedPubKeyScript
                var address: String? = null
                address = if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(scriptBytes))) {
                    Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(scriptBytes))
                } else {
                    Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().currentNetworkParams).toString()
                }
                //            String address = inputScript.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                val ecKey = SendFactory.getPrivKey(address, 0)
                if (ecKey == null || !ecKey.hasPrivKey()) {
                    throw Exception(getString(R.string.bip47_cannot_compose_notif_tx))
                }

                //
                // use outpoint for payload masking
                //
                val privkey = ecKey.privKeyBytes
                val pubkey = payment_code.notificationAddress(SamouraiWallet.getInstance().currentNetworkParams).pubKey
                val outpoint = outPoint?.bitcoinSerialize()
                //                Log.i("PayNymDetailsActivity", "outpoint:" + Hex.toHexString(outpoint));
//                Log.i("PayNymDetailsActivity", "payer shared secret:" + Hex.toHexString(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes()));
                val mask = PaymentCode.getMask(SecretPoint(privkey, pubkey).ECDHSecretAsBytes(), outpoint)
                //                Log.i("PayNymDetailsActivity", "mask:" + Hex.toHexString(mask));
//                Log.i("PayNymDetailsActivity", "mask length:" + mask.length);
//                Log.i("PayNymDetailsActivity", "payload0:" + Hex.toHexString(BIP47Util.getInstance(context).getPaymentCode().getPayload()));
                op_return = PaymentCode.blind(BIP47Util.getInstance(this@PayNymDetailsActivity).paymentCode.payload, mask)
                //                Log.i("PayNymDetailsActivity", "payload1:" + Hex.toHexString(op_return));
            } catch (ike: InvalidKeyException) {
                throw ike
            } catch (ikse: InvalidKeySpecException) {
                throw ikse
            } catch (nsae: NoSuchAlgorithmException) {
                throw nsae
            } catch (nspe: NoSuchProviderException) {
                throw nspe
            } catch (e: Exception) {
                throw e
            }
            val receivers = HashMap<String, BigInteger>()
            receivers[Hex.toHexString(op_return)] = BigInteger.ZERO
            receivers[payment_code.notificationAddress(SamouraiWallet.getInstance().currentNetworkParams).addressString] = SendNotifTxFactory._bNotifTxValue
            receivers[if (SamouraiWallet.getInstance().isTestNet) SendNotifTxFactory.getInstance().TESTNET_SAMOURAI_NOTIF_TX_FEE_ADDRESS else SendNotifTxFactory.getInstance().SAMOURAI_NOTIF_TX_FEE_ADDRESS] = SendNotifTxFactory._bSWFee
            val change = totalValueSelected - (amount + fee.toLong())
            if (change > 0L) {
                val change_address = BIP84Util.getInstance(this@PayNymDetailsActivity).getAddressAt(AddressFactory.CHANGE_CHAIN, BIP84Util.getInstance(this@PayNymDetailsActivity).wallet.getAccount(0).change.addrIdx).bech32AsString
                receivers[change_address] = BigInteger.valueOf(change)
            }
            Log.d("PayNymDetailsActivity", "outpoints:" + outpoints.size)
            Log.d("PayNymDetailsActivity", "totalValueSelected:" + BigInteger.valueOf(totalValueSelected).toString())
            Log.d("PayNymDetailsActivity", "amount:" + BigInteger.valueOf(amount).toString())
            Log.d("PayNymDetailsActivity", "change:" + BigInteger.valueOf(change).toString())
            Log.d("PayNymDetailsActivity", "fee:$fee")
            if (change < 0L) {
                throw Exception(getString(R.string.bip47_cannot_compose_notif_tx))
            }
            val _outPoint: MyTransactionOutPoint = outPoint!!
            var strNotifTxMsg = getText(R.string.bip47_setup4_text1).toString() + " "
            val notifAmount = amount
            val strAmount = MonetaryUtil.getInstance().btcFormat.format((notifAmount.toDouble() + fee.toLong()) / 1e8) + " BTC "
            strNotifTxMsg += strAmount + getText(R.string.bip47_setup4_text2)

            scope.launch(Dispatchers.Main) {
                progressBar.visibility = View.INVISIBLE

                showFollowAlert(strAmount) { view: View? ->
                    progressBar.visibility = View.VISIBLE

                    val job = scope.launch(Dispatchers.IO) {
                        var tx = SendFactory.getInstance(this@PayNymDetailsActivity).makeTransaction(0, outpoints, receivers)
                        if (tx != null) {
                            val input0hash = tx.getInput(0L).outpoint.hash.toString()
                            val input0index = tx.getInput(0L).outpoint.index.toInt()
                            if (input0hash != _outPoint.txHash.toString() || input0index != _outPoint.txOutputN) {
                                throw  Exception(getString(R.string.bip47_cannot_compose_notif_tx))
                            }
                            tx = SendFactory.getInstance(this@PayNymDetailsActivity).signTransaction(tx, 0)
                            val hexTx = String(Hex.encode(tx.bitcoinSerialize()))
                            var isOK = false
                            var response: String? = null
                            try {
                                response = PushTx.getInstance(this@PayNymDetailsActivity).samourai(hexTx, null)
                                Log.d("SendActivity", "pushTx:$response")
                                if (response != null) {
                                    val jsonObject = JSONObject(response)
                                    if (jsonObject.has("status")) {
                                        if ((jsonObject.getString("status") == "ok")) {
                                            isOK = true
                                        }
                                    }
                                } else {
                                    throw  Exception(getString(R.string.pushtx_returns_null))
                                }
                                scope.launch(Dispatchers.Main) {
                                    progressBar.visibility = View.INVISIBLE

                                    if (isOK) {

                                        Toast.makeText(this@PayNymDetailsActivity, R.string.payment_channel_init, Toast.LENGTH_SHORT).show()
                                        //
                                        // set outgoing index for payment code to 0
                                        //
                                        BIP47Meta.getInstance().setOutgoingIdx(pcode, 0)
                                        //                        Log.i("SendNotifTxFactory", "tx hash:" + tx.getHashAsString());
                                        //
                                        // status to NO_CFM
                                        //
                                        BIP47Meta.getInstance().setOutgoingStatus(pcode, tx.hashAsString, BIP47Meta.STATUS_SENT_NO_CFM)

                                        //
                                        // increment change index
                                        //
                                        if (change > 0L) {
                                            BIP49Util.getInstance(this@PayNymDetailsActivity).wallet.getAccount(0).change.incAddrIdx()
                                        }
                                        savePayLoad()
                                    } else {
                                        Toast.makeText(this@PayNymDetailsActivity, R.string.tx_failed, Toast.LENGTH_SHORT).show()
                                    }
                                    scope.launch(Dispatchers.Main) {
                                        setPayNym()
                                    }
                                }
                            } catch (je: JSONException) {
                                throw  Exception("pushTx:" + je.message)
                            } catch (mle: MnemonicLengthException) {
                                throw  Exception("pushTx:" + mle.message)
                            } catch (de: DecoderException) {
                                throw  Exception("pushTx:" + de.message)
                            } catch (ioe: IOException) {
                                throw  Exception("pushTx:" + ioe.message)
                            } catch (de: DecryptionException) {
                                throw  Exception("pushTx:" + de.message)
                            }
                        }
                    }

                    job.invokeOnCompletion {
                        if (it != null) {
                            progressBar.visibility = View.INVISIBLE
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(this@PayNymDetailsActivity, it.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }.invokeOnCompletion {
                if (it != null) {
                    scope.launch(Dispatchers.Main) {
                        progressBar.visibility = View.INVISIBLE
                        Toast.makeText(this@PayNymDetailsActivity, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.invokeOnCompletion {
            if (it != null) {
                scope.launch(Dispatchers.Main) {
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this@PayNymDetailsActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    @Throws(MnemonicLengthException::class, DecryptionException::class, JSONException::class, IOException::class)
    private fun savePayLoad() {
        PayloadUtil.getInstance(this@PayNymDetailsActivity).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(this@PayNymDetailsActivity).guid + AccessFactory.getInstance(this@PayNymDetailsActivity).pin))
    }

    private fun doUpdatePayNymInfo(pcode: String?) {
        val disposable = Observable.fromCallable {
            val obj = JSONObject()
            obj.put("nym", pcode)
            val res = WebUtil.getInstance(this).postURL("application/json", null, WebUtil.PAYNYM_API + "api/v1/nym", obj.toString())
            //                    Log.d("PayNymDetailsActivity", res);
            val responseObj = JSONObject(res)
            if (responseObj.has("nymName")) {
                val strNymName = responseObj.getString("nymName")
                if (FormatsUtil.getInstance().isValidPaymentCode(BIP47Meta.getInstance().getLabel(pcode))) {
                    BIP47Meta.getInstance().setLabel(pcode, strNymName)
                }
            }
            if (responseObj.has("segwit") && responseObj.getBoolean("segwit")) {
                BIP47Meta.getInstance().setSegwit(pcode, true)
            }
            true
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ success: Boolean ->
                    setPayNym()
                    if (success) {
                        savePayLoad()
                    }
                }) { errror: Throwable ->
                    Toast.makeText(this, "Unable to update paynym", Toast.LENGTH_SHORT).show()
                }
        disposables.add(disposable)
    }

    private fun doSync() {
        progressBar!!.visibility = View.VISIBLE
        val disposable = Observable.fromCallable {
            val payment_code = PaymentCode(pcode)
            var idx = 0
            var loop = true
            val addrs = ArrayList<String>()
            while (loop) {
                addrs.clear()
                for (i in idx until (idx + 20)) {
//                            Log.i("PayNymDetailsActivity", "sync receive from " + i + ":" + BIP47Util.getInstance(PayNymDetailsActivity.this).getReceivePubKey(payment_code, i));
                    BIP47Meta.getInstance().idx4AddrLookup[BIP47Util.getInstance(this).getReceivePubKey(payment_code, i)] = i
                    BIP47Meta.getInstance().pCode4AddrLookup[BIP47Util.getInstance(this).getReceivePubKey(payment_code, i)] = payment_code.toString()
                    addrs.add(BIP47Util.getInstance(this).getReceivePubKey(payment_code, i))
                    //                            Log.i("PayNymDetailsActivity", "p2pkh " + i + ":" + BIP47Util.getInstance(PayNymDetailsActivity.this).getReceiveAddress(payment_code, i).getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                }
                val s = addrs.toTypedArray()
                val nb = APIFactory.getInstance(this).syncBIP47Incoming(s)
                //                        Log.i("PayNymDetailsActivity", "sync receive idx:" + idx + ", nb == " + nb);
                if (nb == 0) {
                    loop = false
                }
                idx += 20
            }
            idx = 0
            loop = true
            BIP47Meta.getInstance().setOutgoingIdx(pcode, 0)
            while (loop) {
                addrs.clear()
                for (i in idx until (idx + 20)) {
                    val sendAddress = BIP47Util.getInstance(this).getSendAddress(payment_code, i)
                    //                            Log.i("PayNymDetailsActivity", "sync send to " + i + ":" + sendAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
//                            BIP47Meta.getInstance().setOutgoingIdx(payment_code.toString(), i);
                    BIP47Meta.getInstance().idx4AddrLookup[BIP47Util.getInstance(this).getSendPubKey(payment_code, i)] = i
                    BIP47Meta.getInstance().pCode4AddrLookup[BIP47Util.getInstance(this).getSendPubKey(payment_code, i)] = payment_code.toString()
                    addrs.add(BIP47Util.getInstance(this).getSendPubKey(payment_code, i))
                }
                val s = addrs.toTypedArray()
                val nb = APIFactory.getInstance(this).syncBIP47Outgoing(s)
                //                        Log.i("PayNymDetailsActivity", "sync send idx:" + idx + ", nb == " + nb);
                if (nb == 0) {
                    loop = false
                }
                idx += 20
            }
            BIP47Meta.getInstance().pruneIncoming()
            PayloadUtil.getInstance(this).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(this).guid + AccessFactory.getInstance(this).pin))
            true
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ aBoolean: Boolean? ->
                    progressBar!!.visibility = View.INVISIBLE
                    setPayNym()
                }) { error: Throwable ->
                    error.printStackTrace()
                    progressBar!!.visibility = View.INVISIBLE
                }
        disposables.add(disposable)
    }

    companion object {
        private const val EDIT_PCODE = 2000
        private const val RECOMMENDED_PCODE = 2001
        private const val SCAN_PCODE = 2077
        private const val TAG = "PayNymDetailsActivity"
    }
}