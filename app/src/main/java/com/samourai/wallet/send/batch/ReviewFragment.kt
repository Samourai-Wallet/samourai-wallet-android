package com.samourai.wallet.send.batch;


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.samourai.wallet.R
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.SuggestedFee
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.util.PrefsUtil
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

class ReviewFragment : Fragment() {

    private val viewModel: BatchSpendViewModel by activityViewModels()
    private val FEE_LOW = 0
    private val FEE_NORMAL = 1
    private val FEE_PRIORITY = 2
    private val FEE_CUSTOM = 3
    private var FEE_TYPE = FEE_LOW
    private var feeLow: Long = 0L
    private var feeMed: Long = 0L
    private var feeHigh: Long = 0

    private lateinit var feeSeekBar: SeekBar
    private lateinit var tvSelectedFeeRateLayman: TextView
    private lateinit var tvEstimatedBlockWait: TextView
    private lateinit var tvTotalFee: TextView
    private lateinit var tvSelectedFeeRate: TextView
    private var sendButtonBatch: MaterialButton? = null;
    private var onFeeChangeListener: (() -> Unit)? = null
    private var onClickListener: (View.OnClickListener)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feeSeekBar = view.findViewById(R.id.fee_seekbar)
        tvSelectedFeeRate = view.findViewById(R.id.selected_fee_rate)
        tvSelectedFeeRateLayman = view.findViewById(R.id.selected_fee_rate_in_layman)
        tvEstimatedBlockWait = view.findViewById(R.id.est_block_time)
        sendButtonBatch = view.findViewById(R.id.sendButtonBatch)
        tvTotalFee = view.findViewById(R.id.total_fee)
        sendButtonBatch?.setOnClickListener { onClickListener?.onClick(it) }
        setUpFee()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.batch_spend_review, container, false)
    }

    private fun setUpFee() {
        FEE_TYPE = PrefsUtil.getInstance(requireContext()).getValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL)
        feeLow = FeeUtil.getInstance().lowFee.defaultPerKB.toLong() / 1000L
        feeMed = FeeUtil.getInstance().normalFee.defaultPerKB.toLong() / 1000L
        feeHigh = FeeUtil.getInstance().highFee.defaultPerKB.toLong() / 1000L
        val high = feeHigh.toFloat() / 2 + feeHigh.toFloat()
        val feeHighSliderValue = (high * 10000).toInt()
        val feeMedSliderValue = (feeMed * 10000).toInt()
        feeSeekBar.max = feeHighSliderValue - 10000
        if (feeLow == feeMed && feeMed == feeHigh) {
            feeLow = (feeMed.toDouble() * 0.85).toLong()
            feeHigh = (feeMed.toDouble() * 1.15).toLong()
            val lo_sf = SuggestedFee()
            lo_sf.defaultPerKB = BigInteger.valueOf(feeLow * 1000L)
            FeeUtil.getInstance().lowFee = lo_sf
            val hi_sf = SuggestedFee()
            hi_sf.defaultPerKB = BigInteger.valueOf(feeHigh * 1000L)
            FeeUtil.getInstance().highFee = hi_sf
        } else if (feeLow == feeMed || feeMed == feeMed) {
            feeMed = (feeLow + feeHigh) / 2L
            val mi_sf = SuggestedFee()
            mi_sf.defaultPerKB = BigInteger.valueOf(feeHigh * 1000L)
            FeeUtil.getInstance().normalFee = mi_sf
        } else {
        }
        if (feeLow < 1L) {
            feeLow = 1L
            val lo_sf = SuggestedFee()
            lo_sf.defaultPerKB = BigInteger.valueOf(feeLow * 1000L)
            FeeUtil.getInstance().lowFee = lo_sf
        }
        if (feeMed < 1L) {
            feeMed = 1L
            val mi_sf = SuggestedFee()
            mi_sf.defaultPerKB = BigInteger.valueOf(feeMed * 1000L)
            FeeUtil.getInstance().normalFee = mi_sf
        }
        if (feeHigh < 1L) {
            feeHigh = 1L
            val hi_sf = SuggestedFee()
            hi_sf.defaultPerKB = BigInteger.valueOf(feeHigh * 1000L)
            FeeUtil.getInstance().highFee = hi_sf
        }
        //        tvEstimatedBlockWait.setText("6 blocks");
        tvSelectedFeeRateLayman.text = getString(R.string.normal)
        FeeUtil.getInstance().sanitizeFee()
        tvSelectedFeeRate.text = ("${feeMed}")
        feeSeekBar.progress = feeMedSliderValue - 10000 + 1
        val decimalFormat = DecimalFormat("##.00")
        setFeeLabels()
        feeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                var value = (i.toDouble() + 10000) / 10000.toDouble()
                tvSelectedFeeRate.text = decimalFormat.format(value).toString()
                if (value == 0.0) {
                    value = 1.0
                }
                var pct = 0.0
                var nbBlocks = 6
                if (value <= feeLow.toDouble()) {
                    pct = feeLow.toDouble() / value
                    nbBlocks = Math.ceil(pct * 24.0).toInt()
                } else if (value >= feeHigh.toDouble()) {
                    pct = feeHigh.toDouble() / value
                    nbBlocks = Math.ceil(pct * 2.0).toInt()
                    if (nbBlocks < 1) {
                        nbBlocks = 1
                    }
                } else {
                    pct = feeMed.toDouble() / value
                    nbBlocks = Math.ceil(pct * 6.0).toInt()
                }
                tvEstimatedBlockWait.text = "$nbBlocks blocks"
                setFee(value)
                setFeeLabels()
//                    restoreChangeIndexes()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                    restoreChangeIndexes()
            }
        })
        when (FEE_TYPE) {
            FEE_LOW -> {
                FeeUtil.getInstance().suggestedFee = FeeUtil.getInstance().lowFee
                FeeUtil.getInstance().sanitizeFee()
            }
            FEE_PRIORITY -> {
                FeeUtil.getInstance().suggestedFee = FeeUtil.getInstance().highFee
                FeeUtil.getInstance().sanitizeFee()
            }
            else -> {
                FeeUtil.getInstance().suggestedFee = FeeUtil.getInstance().normalFee
                FeeUtil.getInstance().sanitizeFee()
            }
        }
        setFee(((feeMedSliderValue-10000) /10000).toDouble())
    }

    private fun setFeeLabels() {
        val sliderValue: Float = feeSeekBar.progress.toFloat() / feeSeekBar.max
        val sliderInPercentage = sliderValue * 100
        if (sliderInPercentage < 33) {
            tvSelectedFeeRateLayman.setText(R.string.low)
        } else if (sliderInPercentage > 33 && sliderInPercentage < 66) {
            tvSelectedFeeRateLayman.setText(R.string.normal)
        } else if (sliderInPercentage > 66) {
            tvSelectedFeeRateLayman.setText(R.string.urgent)
        }
    }

    private fun setFee(fee: Double) {
        val sanitySat = FeeUtil.getInstance().highFee.defaultPerKB.toDouble() / 1000.0
        val sanityValue: Long
        sanityValue = if (sanitySat < 10.0) {
            15L
        } else {
            (sanitySat * 1.5).toLong()
        }
        //        String val  = null;
        val d = FeeUtil.getInstance().suggestedFee.defaultPerKB.toDouble() / 1000.0
        val decFormat = NumberFormat.getInstance(Locale.US)
        decFormat.maximumFractionDigits = 3
        decFormat.minimumFractionDigits = 0
        var customValue = 0.0
        customValue = try {
            fee
        } catch (e: java.lang.Exception) {
            Toast.makeText(requireContext(), R.string.custom_fee_too_low, Toast.LENGTH_SHORT).show()
            return
        }
        val suggestedFee = SuggestedFee()
        suggestedFee.isStressed = false
        suggestedFee.isOK = true
        suggestedFee.defaultPerKB = BigInteger.valueOf((customValue * 1000.0).toLong())
        FeeUtil.getInstance().suggestedFee = suggestedFee
        this.onFeeChangeListener?.invoke()
    }

    fun setOnFeeChangeListener(onFeeChangeListener: (() -> Unit)) {
        this.onFeeChangeListener = onFeeChangeListener
    }

    fun setTotalMinerFees(fee: BigInteger?) {
        if (isAdded) {
            tvTotalFee.text = "${FormatsUtil.getBTCDecimalFormat(fee?.toLong())} BTC"
        }
    }

    fun setOnClickListener(onClickListener: View.OnClickListener) {
        this.onClickListener = onClickListener
    }
}