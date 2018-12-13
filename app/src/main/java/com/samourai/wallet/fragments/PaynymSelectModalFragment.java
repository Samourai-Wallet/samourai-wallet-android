package com.samourai.wallet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class PaynymSelectModalFragment extends BottomSheetDialogFragment {

    public Listener selectListener;
    private ArrayList<String> paymentCodes;

    public static PaynymSelectModalFragment newInstance(Listener selectListener) {
        PaynymSelectModalFragment fragment = new PaynymSelectModalFragment();
        fragment.selectListener = selectListener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paynymselectmodal_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = view.findViewById(R.id.list);
        final LinearLayout emptyview = view.findViewById(R.id.empty_paynym);

        paymentCodes = new ArrayList<>(BIP47Meta.getInstance().getSortedByLabels(false, true));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new PaynymSelectModalAdapter());

        if (paymentCodes.size() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyview.setVisibility(View.VISIBLE);
            emptyview.findViewById(R.id.paynym_add_btn).setOnClickListener(view1 -> {
                startActivity(new Intent(getContext(), BIP47Activity.class));
                dismiss();
            });
        }
    }


    @Override
    public void onDetach() {
        this.selectListener = null;
        super.onDetach();
    }


    public interface Listener {
        void onPaynymSelectItemClicked(String code);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView displayName;
        ConstraintLayout rootLayout;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.fragment_paynymselectmodal_list_item, parent, false));
            avatar = itemView.findViewById(R.id.img_paynym_avatar_select);
            displayName = itemView.findViewById(R.id.paynym_display_name);
            rootLayout = itemView.findViewById(R.id.paynym_select_root);
            rootLayout.setOnClickListener(view -> {
                selectListener.onPaynymSelectItemClicked(paymentCodes.get(getAdapterPosition()));
                dismiss();
            });
        }

    }

    private class PaynymSelectModalAdapter extends RecyclerView.Adapter<ViewHolder> {


        PaynymSelectModalAdapter() {
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {


            holder.displayName.setText(BIP47Meta.getInstance().getDisplayLabel(paymentCodes.get(position)));
            Picasso.with(getContext())
                    .load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + paymentCodes.get(position) + "/avatar")
                    .into(holder.avatar);


        }

        @Override
        public int getItemCount() {
            return paymentCodes.size();
        }

    }

}
