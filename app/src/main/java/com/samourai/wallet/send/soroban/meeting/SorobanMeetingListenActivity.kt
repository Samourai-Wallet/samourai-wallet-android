package com.samourai.wallet.send.soroban.meeting

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.samourai.soroban.client.meeting.SorobanMeetingService
import com.samourai.soroban.client.meeting.SorobanRequestMessage
import com.samourai.soroban.client.meeting.SorobanResponseMessage
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.cahoots.AndroidSorobanCahootsService
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity
import com.samourai.wallet.send.soroban.meeting.SorobanMeetingListenActivity
import com.samourai.wallet.util.AppUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class SorobanMeetingListenActivity : SamouraiActivity() {
    private var sorobanMeetingService: SorobanMeetingService? = null
    private var sorobanDisposable: Disposable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soroban_meeting_listen)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        sorobanMeetingService = AndroidSorobanCahootsService.getInstance(applicationContext).sorobanMeetingService
        try {
            startListen()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Cahoots error: " + e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            finish()
        }
    }

    @Throws(Exception::class)
    private fun startListen() {
        Toast.makeText(this, "Waiting for online Cahoots requests...", Toast.LENGTH_SHORT).show()
        sorobanDisposable = sorobanMeetingService!!.receiveMeetingRequest(TIMEOUT_MS.toLong())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cahootsRequest: SorobanRequestMessage ->

                    // trusted paynyms only
                    if (!bip47Meta.exists(cahootsRequest.sender, false)) {
                        val senderDisplayLabel = bip47Meta.getDisplayLabel(cahootsRequest.sender)
                        Toast.makeText(this, "Ignored Cahoots request from unknown sender: $senderDisplayLabel", Toast.LENGTH_LONG).show()
                        return@subscribe
                    }
                    val senderDisplayLabel = bip47Meta.getDisplayLabel(cahootsRequest.sender)
                    val senderPaymentCode = PaymentCode(cahootsRequest.sender)
                    var alert = """
                From: $senderDisplayLabel
                Type: ${cahootsRequest.type.label}
                Miner fee: ${if (cahootsRequest.type.isMinerFeeShared) "shared" else "paid by sender"}

                """.trimIndent()
                    alert += "Do you want to collaborate?"
                    AlertDialog.Builder(this@SorobanMeetingListenActivity)
                            .setTitle("Cahoots request received!")
                            .setMessage(alert)
                            .setCancelable(true)
                            .setPositiveButton(R.string.yes) { dialog: DialogInterface?, whichButton: Int ->
                                try {
                                    Toast.makeText(this, "Accepting Cahoots request...", Toast.LENGTH_SHORT).show()
                                    sorobanMeetingService!!.sendMeetingResponse(senderPaymentCode, cahootsRequest, true).subscribe { sorobanResponseMessage: SorobanResponseMessage? ->
                                        val intent = SorobanCahootsActivity.createIntentCounterparty(applicationContext, account, cahootsRequest.type, cahootsRequest.sender)
                                        startActivity(intent)
                                        finish()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_SHORT).show()
                                }
                            }.setNegativeButton(R.string.no) { dialog: DialogInterface?, whichButton: Int ->
                                try {
                                    Toast.makeText(this, "Refusing Cahoots request...", Toast.LENGTH_SHORT).show()
                                    sorobanMeetingService!!.sendMeetingResponse(senderPaymentCode, cahootsRequest, false).subscribe { sorobanResponseMessage: SorobanResponseMessage? -> finish() }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_SHORT).show()
                                }
                            }.show()
                }) { error: Throwable ->
                    Log.i(TAG, "Error: " + error.message)
                    Toast.makeText(this, "Error: " + error.message, Toast.LENGTH_SHORT).show()
                    finish()
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

    override fun finish() {
        clearDisposable()
        super.finish()
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