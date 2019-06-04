package com.samourai.wallet.paynym.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.paynym.paynymDetails.PayNymDetailsActivity;
import com.samourai.wallet.widgets.CircleImageView;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class PaynymListFragment extends Fragment {

    private PaynymListFragmentViewModel mViewModel;
    private RecyclerView list;
    private static final String TAG = "PaynymListFragment";
    private PaynymAdapter paynymAdapter;

    public static PaynymListFragment newInstance() {
        return new PaynymListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.paynym_account_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = view.findViewById(R.id.paynym_accounts_rv);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        list.addItemDecoration(new ItemDividerDecorator(drawable));
        list.setLayoutManager(new LinearLayoutManager(this.getContext()));
        list.setNestedScrollingEnabled(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(PaynymListFragmentViewModel.class);
        paynymAdapter = new PaynymAdapter();
        list.setAdapter(paynymAdapter);
        mViewModel.pcodes.observe(this, paynymAdapter::setPcodes);

    }

    public void addPcodes(ArrayList<String> list) {
        if (mViewModel == null) {
            mViewModel = ViewModelProviders.of(this).get(PaynymListFragmentViewModel.class);
        }
        mViewModel.addPcodes(list);
    }

    private ArrayList<String> filterArchived(ArrayList<String> list) {
        ArrayList<String> filtered = new ArrayList<>();

        for (String item : list) {
            if (!BIP47Meta.getInstance().getArchived(item)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public void onPayNymItemClick(String pcode, PaynymAdapter.ViewHolder holder) {
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(getActivity(), holder.avatar, "profile");


        startActivity(new Intent(getActivity(), PayNymDetailsActivity.class).putExtra("pcode", pcode),options.toBundle());
    }


    class PaynymAdapter extends RecyclerView.Adapter<PaynymAdapter.ViewHolder> {
        private ArrayList<String> pcodes = new ArrayList<>();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.paynym_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            String strPaymentCode = pcodes.get(position);
            Picasso.with(getContext()).load(WebUtil.PAYNYM_API + strPaymentCode + "/avatar")
                    .into(holder.avatar);
            holder.paynymCode.setText(BIP47Meta.getInstance().getDisplayLabel(strPaymentCode));
            holder.avatar.getRootView().setOnClickListener(view -> onPayNymItemClick(pcodes.get(position),holder));
        }

        @Override
        public int getItemCount() {
            return pcodes.size();
        }

        public void setPcodes(ArrayList<String> list) {
            pcodes.clear();
            pcodes.addAll(list);
            this.notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            CircleImageView avatar;
            TextView paynymCode;

            ViewHolder(View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.paynym_avatar);
                paynymCode = itemView.findViewById(R.id.paynym_code);
            }
        }
    }
}
