package com.samourai.wallet.whirlpool.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samourai.wallet.R
import com.samourai.wallet.databinding.ItemMixUtxoBinding
import com.samourai.wallet.util.FormatsUtil
import com.samourai.whirlpool.client.wallet.beans.MixableStatus
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus
import kotlinx.coroutines.*


class MixListAdapter : RecyclerView.Adapter<MixListAdapter.ViewHolder>() {

    private lateinit var itemMixUtxoBinding: ItemMixUtxoBinding
    var onClick: (utxo: WhirlpoolUtxo) -> Unit = {}
    private val mDiffer = AsyncListDiffer(this, DIFF_CALLBACK)
    private val scope = CoroutineScope(Dispatchers.Default) + SupervisorJob()

    class ViewHolder(private val viewBinding: ItemMixUtxoBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(utxo: WhirlpoolUtxo) {
            val utxoState = utxo.utxoState
            val output = utxo.utxo

            val progressbar = viewBinding.mixProgressBar
            viewBinding.mixAmount.text = FormatsUtil.formatBTC(output.value)
            if (utxoState != null && utxoState.mixProgress != null) {
                viewBinding.mixProgressMessage.visibility = View.VISIBLE
                viewBinding.mixProgressMessage.text = utxoState.mixProgress.mixStep.message
                progressbar.visibility = View.VISIBLE
                progressbar.progress = utxoState.mixProgress.progressPercent
            } else {
                progressbar.visibility = View.GONE
                viewBinding.mixProgressMessage.visibility = View.GONE
            }
            viewBinding.mixStatus.text = "${utxo.mixsDone} mixes complete"

            when (utxoState.status) {
                WhirlpoolUtxoStatus.READY -> {

                }
                WhirlpoolUtxoStatus.STOP -> {
                }
                WhirlpoolUtxoStatus.TX0 -> {
                }
                WhirlpoolUtxoStatus.TX0_FAILED -> {
                }
                WhirlpoolUtxoStatus.TX0_SUCCESS -> {

                }
                WhirlpoolUtxoStatus.MIX_QUEUE -> {
                    viewBinding.mixingStatusIcon.setImageResource(R.drawable.ic_timer_white_24dp)
                }
                WhirlpoolUtxoStatus.MIX_STARTED -> {
                    viewBinding.mixingStatusIcon.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                }
                WhirlpoolUtxoStatus.MIX_SUCCESS -> {
                    progressbar.setProgressCompat(100, true)
                }
                WhirlpoolUtxoStatus.MIX_FAILED -> {
                    viewBinding.mixingStatusIcon.setImageResource(R.drawable.ic_baseline_problem_24)
                }
                else -> {

                }
            }

        }
    }


    fun updateList(utxos: List<WhirlpoolUtxo>) {
        scope.launch {
            try {
                val list = mutableListOf<WhirlpoolUtxo>()
                list.addAll(utxos.filter {
                    it.utxoState.mixProgress != null && it.utxoState.mixableStatus == MixableStatus.MIXABLE
                })
                list.addAll(utxos.filter {
                    it.utxoState.mixProgress != null && it.utxoState.mixableStatus == MixableStatus.UNCONFIRMED
                })
                list.addAll(utxos.filter {
                    it.utxoState.mixProgress == null
                })
                withContext(Dispatchers.Main) {
                    mDiffer.submitList(list)
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        itemMixUtxoBinding =
            ItemMixUtxoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemMixUtxoBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val whirlpoolUtxo = mDiffer.currentList[position];
            holder.bind(whirlpoolUtxo)
            utxoStateMap["${whirlpoolUtxo.utxo.tx_hash}:${whirlpoolUtxo.utxo.tx_output_n}"] =
                whirlpoolUtxo.utxoState.toString()
        } catch (e: Exception) {
        }
        holder.itemView.setOnClickListener {
            this.onClick.invoke(mDiffer.currentList[position])
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        utxoStateMap.clear()
        scope.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun setOnClickListener(handler: (utxo: WhirlpoolUtxo) -> Unit) {
        this.onClick = handler;
    }

    override fun getItemCount(): Int {
        return mDiffer.currentList.size
    }

    companion object {
        private var utxoStateMap = hashMapOf<String, String>()

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WhirlpoolUtxo>() {
            override fun areItemsTheSame(oldItem: WhirlpoolUtxo, newItem: WhirlpoolUtxo): Boolean {
                return oldItem.utxo.tx_hash == newItem.utxo.tx_hash &&
                        oldItem.utxo.tx_output_n == newItem.utxo.tx_output_n
            }

            /**
             * [WhirlpoolUtxo] is not an immutable object. change detection is not possible
             * [utxoStateMap] will map viewHolder utxo progress,
             * this will be used for change detection
             */
            override fun areContentsTheSame(
                oldItem: WhirlpoolUtxo,
                newItem: WhirlpoolUtxo
            ): Boolean {
                return try {
                    val key = "${newItem.utxo.tx_hash}:${newItem.utxo.tx_output_n}"
                    if (utxoStateMap.containsKey(key)) {
                        return utxoStateMap[key] == newItem.utxoState.toString();
                    }
                    return false
                } catch (ex: Exception) {
                    false;
                }
            }

        }
    }
}