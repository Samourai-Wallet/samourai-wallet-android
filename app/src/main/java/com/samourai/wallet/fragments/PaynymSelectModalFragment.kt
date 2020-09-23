package com.samourai.wallet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.samourai.wallet.R
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.PaynymModel
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.paynym.PayNymHome
import com.samourai.wallet.util.fromJSON
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import org.json.JSONObject

class PaynymSelectModalFragment : BottomSheetDialogFragment() {
    var selectListener: Listener? = null
    private var job: Job? = null
    private var paymentCodes: ArrayList<PaynymModel> = arrayListOf()
    private var loadFromNetwork = false
    lateinit var recyclerView: RecyclerView
    lateinit var emptyview: LinearLayout
    lateinit var loadingView: LinearLayout
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_paynymselectmodal_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.list)
        emptyview = view.findViewById(R.id.empty_paynym)
        loadingView = view.findViewById(R.id.paynym_loading)
        if (!loadFromNetwork) {
            paymentCodes = ArrayList(BIP47Meta.getInstance().getSortedByLabels(false, true).map {
                PaynymModel(code = it, "", nymName = BIP47Meta.getInstance().getDisplayLabel(it))
            }.toMutableList())
            setAdapter()
        }
        if (loadFromNetwork) getFromNetwork()

    }

    private fun setAdapter() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = PaynymSelectModalAdapter()
        if (paymentCodes.size == 0) {
            recyclerView.visibility = View.GONE
            emptyview.visibility = View.VISIBLE
            emptyview.findViewById<View>(R.id.paynym_add_btn).setOnClickListener { view1: View? ->
                startActivity(Intent(context, PayNymHome::class.java))
                dismiss()
            }
        }
    }

    private fun getFromNetwork() {
        loadingView.visibility = View.VISIBLE
        job = CoroutineScope(Dispatchers.Main).launch(Dispatchers.IO) {
            val strPaymentCode = BIP47Util.getInstance(requireContext().applicationContext).paymentCode.toString()
            val obj = JSONObject()
            obj.put("nym", strPaymentCode)
            val res = WebUtil.getInstance(requireContext().applicationContext).postURL("application/json", null, WebUtil.PAYNYM_API + "api/v1/nym", obj.toString())
            val json = JSONObject(res)
            val mutableCollection = mutableListOf<PaynymModel>()

            if (json.has("following")) {
                repeat(json.getJSONArray("following").length()) {
                    val item = fromJSON<PaynymModel>(json.getJSONArray("following").getJSONObject(it).toString());
                    item?.let { it1 -> mutableCollection.add(it1) }
                }
            }

            if (json.has("followers")) {
                repeat(json.getJSONArray("followers").length()) { position ->
                    val item = fromJSON<PaynymModel>(json.getJSONArray("followers").getJSONObject(position).toString());
                    mutableCollection.find {  it.code == item?.code }.let {
                        if(it == null){
                            item?.let { it1 -> mutableCollection.add(it1) }
                        }
                    }
                }
            }
            paymentCodes = ArrayList(mutableCollection)

            withContext(Dispatchers.Main) {
                loadingView.visibility = View.GONE
                setAdapter()
            }
        }
        job?.invokeOnCompletion {
            if (it == null) {

            } else {
                if (it !is CancellationException)
                    Toast.makeText(requireContext(), "Error ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        job?.cancel("OnDestroy")
        super.onDestroyView()
    }

    override fun onDetach() {
        selectListener = null
        super.onDetach()
    }

    interface Listener {
        fun onPaynymSelectItemClicked(code: String?)
    }

    private inner class ViewHolder(inflater: LayoutInflater, parent: ViewGroup?) : RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_paynymselectmodal_list_item, parent, false)) {
        var avatar: ImageView = itemView.findViewById(R.id.img_paynym_avatar_select)
        var displayName: TextView = itemView.findViewById(R.id.paynym_display_name)
        var rootLayout: ConstraintLayout = itemView.findViewById(R.id.paynym_select_root)

        init {
            rootLayout.setOnClickListener {
                selectListener!!.onPaynymSelectItemClicked(paymentCodes[position].code)
                dismiss()
            }
        }
    }

    private inner class PaynymSelectModalAdapter internal constructor() : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val code = paymentCodes[position].code
            var label = paymentCodes[position].nymName
            var metaLabel = BIP47Meta.getInstance().getLabel(paymentCodes[position].code)
            if(! metaLabel.isNullOrBlank()){
                label = metaLabel
            }
            if (BIP47Meta.getInstance().getArchived(code)) {
                label += " (archived)"
            }
            if (BIP47Meta.getInstance().getOutgoingStatus(code) == BIP47Meta.STATUS_NOT_SENT) {
                label += " (not followed)"
            }
            if (BIP47Meta.getInstance().getOutgoingStatus(code) == BIP47Meta.STATUS_SENT_NO_CFM) {
                label += " (not confirmed)"
            }
            holder.displayName.text = label
            Picasso.with(context)
                    .load("${WebUtil.PAYNYM_API}${code}/avatar")
                    .into(holder.avatar)
        }

        override fun getItemCount(): Int {
            return paymentCodes.size
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(selectListener: Listener?, loadFromNetwork: Boolean): PaynymSelectModalFragment {
            val fragment = PaynymSelectModalFragment()
            fragment.selectListener = selectListener
            fragment.loadFromNetwork = loadFromNetwork
            return fragment
        }
    }
}