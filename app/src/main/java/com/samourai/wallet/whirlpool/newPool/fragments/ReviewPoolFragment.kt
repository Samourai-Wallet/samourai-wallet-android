package com.samourai.wallet.whirlpool.newPool.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.samourai.wallet.R
import com.samourai.wallet.api.backend.beans.UnspentOutput
import com.samourai.wallet.api.backend.beans.UnspentOutput.Xpub
import com.samourai.wallet.send.SendFactory
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.whirlpool.WhirlpoolTx0
import com.samourai.wallet.widgets.EntropyBar
import com.samourai.whirlpool.client.tx0.Tx0Preview
import com.samourai.whirlpool.client.tx0.UnspentOutputWithKey
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount
import com.samourai.whirlpool.client.whirlpool.beans.Pool
import kotlinx.android.synthetic.main.fragment_whirlpool_review.*
import kotlinx.coroutines.*
import org.bouncycastle.util.encoders.Hex
import java.util.*

class ReviewPoolFragment : Fragment() {


    private val coroutineContext = CoroutineScope(Dispatchers.IO)

    private val account = 0
    private var tx0: WhirlpoolTx0? = null;
    private var tx0FeeTarget: Tx0FeeTarget? = null
    private var mixFeeTarget: Tx0FeeTarget? = null
    private var pool: Pool? = null
    private var onLoading: (Boolean, Exception?) -> Unit = { _: Boolean, _: Exception? -> }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entropyBar?.setMaxBars(4)
        entropyBar?.setRange(4)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_whirlpool_review, container, false)
    }

    fun showProgress(show: Boolean) {
        progressBar!!.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setLoadingListener(listener: (Boolean, Exception?) -> Unit) {
        this.onLoading = listener
    }

    fun setTx0(tx0: WhirlpoolTx0, tx0FeeTarget: Tx0FeeTarget?, mixFeeTarget: Tx0FeeTarget?, pool: Pool?) {
        this.pool = pool
        this.tx0 = tx0;
        this.tx0FeeTarget = tx0FeeTarget
        this.mixFeeTarget = mixFeeTarget
        makeTxoPreview(tx0)
        minerFees.text = ""
        feePerUtxo.text = ""
        poolFees.text = ""
        uncycledAmount.text = ""
        amountToCycle.text = ""
        poolTotalFees.text = ""
        totalPoolAmount.text = ""
        poolAmount.text = getBTCDisplayAmount(tx0.pool)
        totalUtxoCreated.text = "${tx0.premixRequested}";
    }

    private fun makeTxoPreview(tx0: WhirlpoolTx0) {
        showLoadingProgress(true)
        onLoading.invoke(true, null)
        val whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWalletOrNull
        val spendFroms: MutableCollection<UnspentOutputWithKey> = ArrayList()
        for (outPoint in tx0.outpoints) {
            val unspentOutput = UnspentOutput()
            unspentOutput.addr = outPoint.address
            unspentOutput.script = Hex.toHexString(outPoint.scriptBytes)
            unspentOutput.confirmations = outPoint.confirmations
            unspentOutput.tx_hash = outPoint.txHash.toString()
            unspentOutput.tx_output_n = outPoint.txOutputN
            unspentOutput.value = outPoint.value.value
            unspentOutput.xpub = Xpub()
            unspentOutput.xpub.path = "M/0/0"
            val eckey = SendFactory.getPrivKey(outPoint.address, account)
            val spendFrom = UnspentOutputWithKey(unspentOutput, eckey.privKeyBytes)
            spendFroms.add(spendFrom)
        }
        coroutineContext.launch(Dispatchers.IO) {
            val tx0Config = whirlpoolWallet.tx0Config
            tx0Config.changeWallet = WhirlpoolAccount.DEPOSIT
            try {
                val poolSelected: Pool = whirlpoolWallet.poolSupplier.findPoolById(pool?.poolId)
                val tx0Preview = whirlpoolWallet.tx0Preview(poolSelected, spendFroms, tx0Config, tx0FeeTarget, mixFeeTarget)
                withContext(Dispatchers.Main) {
                    showLoadingProgress(false)
                    setFees(tx0Preview);
                    onLoading.invoke(false, null);
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onLoading.invoke(false, e);
                    showLoadingProgress(false)
                }
            }
        }
    }

    private fun showLoadingProgress(loading: Boolean) {
        if (loading) {
            loadingFeeDetails.show()
        } else {
            loadingFeeDetails.hide()
        }
    }

    private fun setFees(tx0Preview: Tx0Preview?) {
        tx0Preview?.let {
            TransitionManager.beginDelayedTransition(reviewLayout, Fade())
            val embeddedFees = tx0Preview.premixValue.minus(tx0!!.pool);
            val embeddedTotalFees =  (embeddedFees * it.nbPremix)
            minerFees.text = getBTCDisplayAmount(it.tx0MinerFee);
            val totalFees =embeddedTotalFees + tx0Preview.feeValue + tx0Preview.tx0MinerFee;
            poolTotalFees.text = getBTCDisplayAmount(totalFees);
            poolFees.text = getBTCDisplayAmount(tx0Preview.feeValue)
            if (it.feeDiscountPercent != 0) {
                val scodeMessage = "SCODE Applied, ${it.feeDiscountPercent}% Discount"
                discountText.text = scodeMessage
            }
            this.tx0?.let { tx0 ->
                totalPoolAmount.text = "${getBTCDisplayAmount(tx0.amountSelected)}"
                amountToCycle.text = getBTCDisplayAmount((tx0.amountSelected - totalFees ) - tx0Preview.changeValue )
            }
            uncycledAmount?.text = getBTCDisplayAmount((tx0Preview.changeValue));
            feePerUtxo.text = getBTCDisplayAmount(embeddedTotalFees);
            totalUtxoCreated.text = "${it.nbPremix}";
        }
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SelectPoolFragment"
    }


    private fun getBTCDisplayAmount(value: Long): String? {
        return "${FormatsUtil.getBTCDecimalFormat(value)} BTC"
    }

}