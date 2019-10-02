package com.samourai.wallet.whirlpool.newPool.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.WhirlpoolTx0;
import com.samourai.wallet.widgets.EntropyBar;

import java.text.DecimalFormat;


public class ReviewPoolFragment extends Fragment {

    private static final String TAG = "SelectPoolFragment";

    private EntropyBar entropyBar;

    private TextView deterMinisticLinksPerTx,
            totalTxs,
            poolAmount,
            combinationPerTxs,
            poolTotalFees,
            minerFees,
            totalPoolAmount,
            amountToCycle,
            uncycledAmount,
            poolFees,
            entropyPerTxs;


    public ReviewPoolFragment() {
    }

    public void setOnPoolSelectionComplete() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        entropyBar = view.findViewById(R.id.pool_review_entropy_bar);
        entropyPerTxs = view.findViewById(R.id.pool_review_entropy_txt);
        deterMinisticLinksPerTx = view.findViewById(R.id.pool_review_deterministic_links_per_tx);
        totalTxs = view.findViewById(R.id.pool_review_total_txes);
        poolAmount = view.findViewById(R.id.pool_review_amount);
        poolFees = view.findViewById(R.id.pool_review_pool_fee);
        minerFees = view.findViewById(R.id.pool_review_miner_fee);
        uncycledAmount = view.findViewById(R.id.pool_review_uncycled_amount);
        amountToCycle = view.findViewById(R.id.pool_review_amount_to_cycle);
        poolTotalFees = view.findViewById(R.id.pool_review_total_fees);
        combinationPerTxs = view.findViewById(R.id.pool_review_combination_per_tx);
        totalPoolAmount = view.findViewById(R.id.pool_review_total_pool_amount);


        entropyBar.setMaxBars(4);
        entropyBar.setRange(3);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_whirlpool_review, container, false);
    }


    @Override
    public void onAttach
            (Context
                     context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    public void setTx0(WhirlpoolTx0 tx0) {
        totalPoolAmount.setText(String.valueOf(tx0.getAmountSelected() / 1e8));
        poolAmount.setText(String.valueOf(tx0.getPool() / 1e8));
        poolFees.setText(String.valueOf(new DecimalFormat("0.########").format(tx0.getFeeSamourai() / 1e8)));
        minerFees.setText(String.valueOf(new DecimalFormat("0.########").format(tx0.getFee() / 1e8)));
        amountToCycle.setText(String.valueOf(tx0.getAmountAfterWhirlpoolFee() / 1e8));
        poolTotalFees.setText(String.valueOf(new DecimalFormat("0.########").format((tx0.getFeeSamourai() + tx0.getFee() ) / 1e8)));
        uncycledAmount.setText(" "+ (tx0.getChange() /1e8));
        totalTxs.setText(String.valueOf(tx0.getPremixRequested()));

    }
}
