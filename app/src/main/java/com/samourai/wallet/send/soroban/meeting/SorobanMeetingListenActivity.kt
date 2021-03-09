package com.samourai.wallet.send.soroban.meeting

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.soroban.client.meeting.SorobanMeetingService
import com.samourai.soroban.client.meeting.SorobanRequestMessage
import com.samourai.soroban.client.meeting.SorobanResponseMessage
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.cahoots.AndroidSorobanCahootsService
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.PrefsUtil
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class SorobanMeetingListenActivity : SamouraiActivity() {


    private var sorobanMeetingService: SorobanMeetingService? = null
    private var sorobanDisposable: Disposable? = null
    private var menu: Menu? = null;
    private lateinit var progressLoader: ProgressBar
    lateinit var loaderText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soroban_meeting_listen)
        setSupportActionBar(findViewById(R.id.toolbar_soroban_meeting))
        progressLoader = findViewById(R.id.loader)
        loaderText = findViewById(R.id.loader_text)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        sorobanMeetingService = AndroidSorobanCahootsService.getInstance(applicationContext).sorobanMeetingService
        startListen()
    }

    private fun startListen() {

        progressLoader.visibility = View.VISIBLE
        loaderText.text = getString(R.string.waiting_for_cahoots_request)
        menu?.findItem(R.id.action_refresh)?.isVisible = false
        sorobanDisposable = sorobanMeetingService!!.receiveMeetingRequest(TIMEOUT_MS.toLong())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cahootsRequest: SorobanRequestMessage ->
                    showSorobanRequest(cahootsRequest)
                }) { error: Throwable ->
                    Log.i(TAG, "Error: " + error.message)
                    error.message?.let {
                        if (it.contains("aborting")) {
                            menu?.findItem(R.id.action_refresh)?.isVisible = true
                            progressLoader.visibility = View.GONE
                            loaderText.text = getString(R.string.no_cahoots_detected)
                        }
                    }
                    Toast.makeText(this, "${error.message}", Toast.LENGTH_SHORT).show()
                }
    }

    override fun onResume() {
        super.onResume()
        AppUtil.getInstance(this).setIsInForeground(true)
        AppUtil.getInstance(this).checkTimeOut()
    }

    private fun clearDisposable() {
        if (sorobanDisposable != null && !sorobanDisposable!!.isDisposed) {
            sorobanDisposable!!.dispose()
            sorobanDisposable = null
        }
    }

    private fun showSorobanRequest(cahootsRequest: SorobanRequestMessage?) {
        // trusted paynyms only
        val item = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater;
        val view = item.inflate(R.layout.dialog_soroban_request, null)


        val paynymImage = view.findViewById<ImageView>(R.id.paynym_user_avatar_soroban);
        val paynym = view.findViewById<TextView>(R.id.paynym_id_soroban);
        val message = view.findViewById<TextView>(R.id.soroban_message);
        val sorobanType = view.findViewById<TextView>(R.id.soroban_type_text);
        val sorobanFee = view.findViewById<TextView>(R.id.soroban_tx_fee);

        cahootsRequest?.apply {

            if (!bip47Meta.exists(this.sender, false)) {
                val senderDisplayLabel = bip47Meta.getDisplayLabel(this.sender)
                Toast.makeText(this@SorobanMeetingListenActivity, "Ignored Cahoots request from unknown sender: $senderDisplayLabel", Toast.LENGTH_LONG).show()
                return
            }

            val senderPaymentCode = PaymentCode(this.sender)

            Picasso.get()
                    .load(WebUtil.PAYNYM_API + this.sender + "/avatar")
                    .into(paynymImage)

            paynym.text = bip47Meta.getDisplayLabel(cahootsRequest.sender)
            if (this.type == CahootsType.STOWAWAY) {
                sorobanType.text = "Stowaway"
            } else {
                sorobanType.text = "STONEWALLx2"
            }

            message.text = "${paynym.text} wants to CAHOOT with you using a ${sorobanType.text} CoinJoin"
            if (this.type.isMinerFeeShared) {
                sorobanFee.text = "You pay half of the miner fee"
            } else {
                sorobanFee.text = "None"
            }

            MaterialAlertDialogBuilder(this@SorobanMeetingListenActivity, R.style.Theme_Samourai_MaterialDialog_Rounded)
                    .setView(view)
                    .setPositiveButton(R.string.yes) { dialog, whichwhich ->
                        try {
                            Toast.makeText(this@SorobanMeetingListenActivity, "Accepting Cahoots request...", Toast.LENGTH_SHORT).show()
                            sorobanMeetingService!!.sendMeetingResponse(senderPaymentCode, cahootsRequest, true).subscribe { sorobanResponseMessage: SorobanResponseMessage? ->
                                val intent = SorobanCahootsActivity.createIntentCounterparty(applicationContext, account, cahootsRequest.type, cahootsRequest.sender)
                                startActivity(intent)
                                finish()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@SorobanMeetingListenActivity, "Error: " + e.message, Toast.LENGTH_SHORT).show()
                        }

                    }
                    .setNegativeButton(R.string.no) { dialog, whichwhich ->
                        try {
                            Toast.makeText(this@SorobanMeetingListenActivity, "Refusing Cahoots request...", Toast.LENGTH_SHORT).show()
                            sorobanMeetingService!!.sendMeetingResponse(senderPaymentCode, cahootsRequest, false).subscribe {

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@SorobanMeetingListenActivity, "Error: " + e.message, Toast.LENGTH_SHORT).show()
                        }

                    }
                    .show()

        }

    }

    override fun finish() {
        clearDisposable()
        super.finish()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.soroban_meeting_listen_activity_menu, menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_refresh) {
            startListen()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() { // cancel cahoots request
        clearDisposable()
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "SorobanMeetingListen"
        private val bip47Meta = BIP47Meta.getInstance()
        private const val TIMEOUT_MS = 60000
    }
}