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
import com.samourai.wallet.widgets.EntropyBar;


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
        uncycledAmount = view.findViewById(R.id.pool_review_uncyled_amount);
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


}
