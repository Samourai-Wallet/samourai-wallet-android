package com.samourai.wallet.whirlpool.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.samourai.wallet.R
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.databinding.WhirlpoolIntroViewBinding
import com.samourai.wallet.home.BalanceActivity
import com.samourai.wallet.home.BalanceViewModel
import com.samourai.wallet.home.adapters.TxAdapter
import com.samourai.wallet.service.JobRefreshService
import com.samourai.wallet.tx.TxDetailsActivity
import com.samourai.wallet.whirlpool.WhirlPoolHomeViewModel
import com.samourai.wallet.whirlpool.WhirlpoolHome.Companion.NEWPOOL_REQ_CODE
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionsFragment : Fragment() {

    private val viewModel: BalanceViewModel by viewModels()
    private val whirlPoolHomeViewModel: WhirlPoolHomeViewModel by activityViewModels()
    lateinit var adapter: TxAdapter
    lateinit var recyclerView: RecyclerView
    lateinit var containerLayout: FrameLayout
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (BalanceActivity.DISPLAY_INTENT == intent.action) {
                    if (swipeRefreshLayout != null) {
                        if (swipeRefreshLayout!!.isRefreshing) {
                            swipeRefreshLayout?.isRefreshing = false
                            loadFromRepo()
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setAccount(WhirlpoolAccount.POSTMIX.accountIndex);
        val filter = IntentFilter(BalanceActivity.DISPLAY_INTENT)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadCastReceiver, filter)
        viewModel.loadOfflineData()
        whirlPoolHomeViewModel.onboardStatus.observe(viewLifecycleOwner, { showOnBoarding ->
            if (showOnBoarding) {
                showIntroView()
            } else {
                adapter = TxAdapter(
                    view.context,
                    listOf(
                    ),
                    WhirlpoolAccount.POSTMIX.accountIndex
                )
                adapter.setClickListener { _, tx ->
                    val txIntent = Intent(requireContext(), TxDetailsActivity::class.java)
                    txIntent.putExtra("TX", tx.toJSON().toString())
                    txIntent.putExtra("_account", WhirlpoolAccount.POSTMIX.accountIndex)
                    startActivity(txIntent)
                }
                recyclerView.adapter = adapter
                viewModel.txs.observe(viewLifecycleOwner, {
                    adapter.setTxes(it)
                })
            }
        })

        loadFromRepo()
    }

    private fun showIntroView() {
        val binding =
            WhirlpoolIntroViewBinding.inflate(layoutInflater, containerLayout, true)
        binding.whirlpoolIntroTopText.text = getString(R.string.whirlpool_completely_breaks_the)
        binding.whirlpoolIntroSubText.text = getString(R.string.whirlpool_is_entirely)
        binding.whirlpoolIntroImage.setImageResource(R.drawable.ic_nue_transactions_graphic)
        binding.whirlpoolIntroGetStarted.setOnClickListener {
            activity?.startActivityForResult(
                Intent(activity, NewPoolActivity::class.java),
                NEWPOOL_REQ_CODE
            )
        }
    }

    private fun loadFromRepo() {
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val postMixList = APIFactory.getInstance(requireContext()).allPostMixTxs
                if (postMixList != null) {
                    withContext(Dispatchers.Main) {
                        viewModel.setTx(postMixList)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        containerLayout = FrameLayout(container!!.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val drawable = ContextCompat.getDrawable(container.context, R.drawable.divider_grey)
        recyclerView = RecyclerView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            layoutManager = LinearLayoutManager(container.context)
            addItemDecoration(
                DividerItemDecoration(
                    container.context,
                    LinearLayoutManager(container.context).orientation
                ).apply {
                    drawable?.let { this.setDrawable(it) }
                })
        }

        swipeRefreshLayout = SwipeRefreshLayout(container.context)
            .apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                whirlPoolHomeViewModel.refresh()
                this.addView(recyclerView)
                setOnRefreshListener {
                    val intent = Intent(requireContext(), JobRefreshService::class.java)
                    intent.putExtra("notifTx", false)
                    intent.putExtra("dragged", true)
                    intent.putExtra("launch", false)
                    JobRefreshService.enqueueWork(requireContext(), intent)
                }
            }
        containerLayout.addView(swipeRefreshLayout)
        return containerLayout
    }

    companion object {

        @JvmStatic
        fun newInstance(): TransactionsFragment {
            return TransactionsFragment().apply {
                arguments = Bundle().apply {
                }
            }
        }
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadCastReceiver)
        super.onDestroyView()
    }
}