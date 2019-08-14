package com.samourai.wallet.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.Fade;
import android.support.transition.Slide;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.samourai.boltzmann.processor.TxProcessorResult;
import com.samourai.wallet.R;

import java.text.DecimalFormat;

/**
 * A CustomView for showing and hiding transaction and transactionReview
 * Two layouts will be inflated dynamically and added to FrameLayout
 */
public class SendTransactionDetailsView extends FrameLayout {


    private View transactionView, transactionReview;
    private ViewGroup ricochetHopsReview, stoneWallReview;
    private boolean reviewActive = false;
    private ViewGroup stowawayLayout, stoneWallX2Layout, stoneWallLayout;
    private EntropyBar entropyBarStoneWallX2, entropyBarStoneWallX1;
    private Switch stoneWallx1Switch;
    private TextView stowawayMixingParticipant, entropyValueX1,entropyValueX2, stowawayMethod, stoneWallx2Fee, stoneWallx2mixingParticipant;


    public SendTransactionDetailsView(@NonNull Context context) {
        super(context);
        init();
    }

    public SendTransactionDetailsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SendTransactionDetailsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        transactionView = inflate(getContext(), R.layout.send_transaction_main_segment, null);
        transactionReview = inflate(getContext(), R.layout.send_transaction_review, null);
        ricochetHopsReview = transactionReview.findViewById(R.id.ricochet_hops_layout);
        stowawayLayout = transactionReview.findViewById(R.id.stowaway_layout);
        stoneWallX2Layout = transactionReview.findViewById(R.id.stonewallx2_review_layout);
        stoneWallLayout = transactionReview.findViewById(R.id.stonewallx1_layout);
//        entropyBarStoneWallX2 = transactionReview.findViewById(R.id.cahoots_entropy_bar);
        entropyBarStoneWallX1 = transactionReview.findViewById(R.id.entropy_bar_stonewallx1);
        stoneWallx1Switch = transactionReview.findViewById(R.id.stonewallx1_switch);
        stowawayMixingParticipant = transactionReview.findViewById(R.id.stowaway_mixing_participant);
        stowawayMethod = transactionReview.findViewById(R.id.stowaway_method);
        stoneWallx2Fee = transactionReview.findViewById(R.id.extra_stonewall_fee);
        stoneWallx2mixingParticipant = transactionReview.findViewById(R.id.mixing_partner_txtview);
        entropyValueX1 = transactionReview.findViewById(R.id.entropy_value_stonewallx1);
//        entropyValueX2 = transactionReview.findViewById(R.id.entropy_value_stonewallx2);

        entropyBarStoneWallX1.setMaxBars(4);
//        entropyBarStoneWallX2.setMaxBars(3);

        addView(transactionView);

    }

    public View getTransactionReview() {
        return transactionReview;
    }

    public View getTransactionView() {
        return transactionView;
    }
    public void enableForRicochet(boolean enable){
        stoneWallLayout.setVisibility(enable ? INVISIBLE : VISIBLE);
    }

    public void showStonewallX2Layout(String participant, long fee) {

        stoneWallLayout.getRootView().post(() -> {
            stoneWallX2Layout.setVisibility(VISIBLE);
            stowawayLayout.setVisibility(GONE);
            stoneWallLayout.setVisibility(GONE);
        });
        stoneWallx2mixingParticipant.setText(participant);

    }

    public void showStowawayLayout(String participant, TxProcessorResult entropy, long fee) {
        stoneWallLayout.getRootView().post(() -> {
            stowawayLayout.setVisibility(VISIBLE);
            stoneWallLayout.setVisibility(GONE);
            stoneWallX2Layout.setVisibility(GONE);
        });

        stowawayMixingParticipant.setText("____");

    }

    public void showStonewallx1Layout(TxProcessorResult entropyResult) {
        stoneWallLayout.getRootView().post(() -> {
            stoneWallLayout.setVisibility(VISIBLE);
            stowawayLayout.setVisibility(GONE);
            stoneWallX2Layout.setVisibility(GONE);
        });

    }

    public void enableStonewall(boolean enable) {
        for (int i = 0; i < stoneWallLayout.getChildCount(); i++) {
            stoneWallLayout.getChildAt(i).setAlpha(enable ? 1f : 0.6f);
        }
    }


    public Switch getStoneWallSwitch() {
        return stoneWallx1Switch;
    }

    /**
     * Shows review layout with transition
     *
     * @param ricochet will be used to show and hide ricochet hops slider
     */
    public void showReview(boolean ricochet) {

        TransitionSet set = new TransitionSet();

        set.setOrdering(TransitionSet.ORDERING_TOGETHER);

        set.addTransition(new Fade())
                .addTarget(transactionView)
                .addTransition(new Slide(Gravity.END))
                .addTarget(transactionReview);
//
//        if (ricochet) {
////            ricochetHopsReview.setVisibility(View.VISIBLE);
//            stoneWallReview.setVisibility(View.GONE);
//        } else {
//            ricochetHopsReview.setVisibility(View.GONE);
//            stoneWallReview.setVisibility(View.VISIBLE);
//        }

        TransitionManager.beginDelayedTransition(this, set);
        addView(transactionReview);
        reviewActive = true;
        removeView(transactionView);
    }

    public void setEntropyBarStoneWallX1(TxProcessorResult entropy) {
        if(entropy == null){
            entropyBarStoneWallX1.disable();
            entropyValueX1.setText(R.string.not_available);
        }else {
            entropyBarStoneWallX1.setRange(entropy);
            DecimalFormat decimalFormat = new DecimalFormat("##.00");
            entropyValueX1.setText(decimalFormat.format(entropy.getEntropy()).concat(" bits"));
        }
    }

    public EntropyBar getEntropyBarStoneWallX1() {
        return entropyBarStoneWallX1;
    }

    public void setEntropyBarStoneWallX1ZeroBits() {
        entropyBarStoneWallX1.setRange(0);
        entropyValueX1.setText(R.string.zero_bits);
    }

    public void setEntropyBarStoneWallX2(TxProcessorResult entropy) {
        if(entropy == null){
            entropyBarStoneWallX2.disable();
            entropyValueX2.setText(R.string.not_available);

        }else {
            entropyBarStoneWallX2.setRange(entropy);
            DecimalFormat decimalFormat = new DecimalFormat("##.00");
            entropyValueX2.setText(decimalFormat.format(entropy.getEntropy()).concat(" bits"));
        }
    }
    public void setEntropyBarStoneWallX2(int entropy) {
        entropyBarStoneWallX2.setRange(entropy);
        entropyValueX2.setText(R.string.zero_bits);
    }



    public void showTransaction() {
        TransitionSet set = new TransitionSet();

        set.setOrdering(TransitionSet.ORDERING_TOGETHER);

        set.addTransition(new Fade())
                .addTarget(transactionReview)
                .addTransition(new Slide(Gravity.START))
                .addTarget(transactionView);

        TransitionManager.beginDelayedTransition(this, set);
        addView(transactionView);
        reviewActive = false;
        removeView(transactionReview);
    }

    public boolean isReview() {
        return reviewActive;
    }
}
