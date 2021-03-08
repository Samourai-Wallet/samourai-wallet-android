package com.samourai.wallet.send.batch;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.R
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.util.BatchSendUtil
import com.samourai.wallet.util.FormatsUtil
import kotlinx.android.synthetic.main.batch_spend_compose.*

class ComposeFragment : Fragment() {

        private val viewModel: BatchSpendViewModel by activityViewModels()
        private val batchListAdapter = BatchListAdapter()
        private lateinit var batchRecyclerView: RecyclerView;
        private var reviewButton: MaterialButton? = null
        private var onListItemClick: ((item: BatchSendUtil.BatchSend) -> Unit)? = null;
        private var onReviewClick: View.OnClickListener? = null;

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            batchRecyclerView = composeView.findViewById(R.id.batchListRecyclerView)
            reviewButton = composeView.findViewById(R.id.reviewButtonBatch)
            batchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            batchRecyclerView.adapter = batchListAdapter
            reviewButton?.setOnClickListener {
                this.onReviewClick?.onClick(it)
            }
            enableReview(false)
            viewModel.getBatchListLive().observe(viewLifecycleOwner, {
                batchListAdapter.submitList(it)
                enableReview(it.size != 0)
            })
            batchListAdapter.setOnDeleteClick {
                viewModel.remove(it)
            }
            batchListAdapter.setViewOnClick {
                onListItemClick?.invoke(it)
            }
        }

        fun setOnItemClickListener(listener: ((batch: BatchSendUtil.BatchSend) -> Unit)) {
            this.onListItemClick = listener
        }

        fun setOnReviewClickListener(listener: View.OnClickListener) {
            this.onReviewClick = listener
        }

        private fun enableReview(enable: Boolean) {
            reviewButton?.isEnabled = enable;
            if (enable) {
                reviewButton?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_ui_2))
            } else {
                reviewButton?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.disabled_grey))
            }
        }

        fun getReviewButton(): MaterialButton? {
            return reviewButton
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.batch_spend_compose, container, false)
        }


        class BatchListAdapter : RecyclerView.Adapter<BatchListAdapter.BatchViewHolder>() {

            private val mDiffer: AsyncListDiffer<BatchSendUtil.BatchSend> = AsyncListDiffer(this, callBack)

            private var viewOnClick: ((batch: BatchSendUtil.BatchSend) -> Unit)? = null
            private var onDeleteClick: ((batch: BatchSendUtil.BatchSend) -> Unit)? = null

            fun setViewOnClick(listener: ((batch: BatchSendUtil.BatchSend) -> Unit)) {
                viewOnClick = listener
            }

            fun setOnDeleteClick(listener: (batch: BatchSendUtil.BatchSend) -> Unit) {
                onDeleteClick = listener
            }

            data class BatchViewHolder(val v: View,
                                       val amount: TextView,
                                       val to: TextView,
                                       val deleteButton: MaterialButton) : RecyclerView.ViewHolder(v)


            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_batch_spend, parent, false)
                return BatchViewHolder(view, amount = view.findViewById(R.id.batchItemAmount),
                        deleteButton = view.findViewById(R.id.batchDeleteBtn),
                        to = view.findViewById(R.id.batchItemToAddress));
            }

            override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
                val item = mDiffer.currentList[position]
                holder.itemView.setOnClickListener {
                    viewOnClick?.invoke(item)
                }
                holder.itemView.setOnLongClickListener {
                    MaterialAlertDialogBuilder(holder.itemView.context)
                            .setTitle("Batch Item Details")
                            .setMessage("Address: ${item.addr}\n" +
                                    "Amount: ${FormatsUtil.getBTCDecimalFormat(item.amount)} BTC\n" +
                                    "PayNym: ${if (item.pcode != null) BIP47Meta.getInstance().getDisplayLabel(item.pcode) else ""}")
                            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .show()
                    true
                }
                holder.amount.text = "${FormatsUtil.getBTCDecimalFormat(item.amount)} BTC"
                holder.to.text = item.addr
                if (item.pcode != null) {
                    holder.to.text = BIP47Meta.getInstance().getDisplayLabel(item.pcode);
                }
                holder.deleteButton.setOnClickListener {
                    onDeleteClick?.invoke(item)
                }
            }

            override fun getItemCount(): Int {
                return mDiffer.currentList.size
            }

            fun submitList(list: List<BatchSendUtil.BatchSend>) {
                mDiffer.submitList(list)
            }

            companion object {
                val callBack = object : DiffUtil.ItemCallback<BatchSendUtil.BatchSend>() {
                    override fun areItemsTheSame(oldItem: BatchSendUtil.BatchSend, newItem: BatchSendUtil.BatchSend): Boolean {
                        return oldItem.UUID == newItem.UUID
                    }

                    override fun areContentsTheSame(oldItem: BatchSendUtil.BatchSend, newItem: BatchSendUtil.BatchSend): Boolean {
                        return newItem.addr == oldItem.addr
                                && newItem.amount == oldItem.amount
                                && newItem.UUID == oldItem.UUID
                                && newItem.pcode == oldItem.pcode
                    }
                }
            }

        }

    }