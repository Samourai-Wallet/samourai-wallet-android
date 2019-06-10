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

import com.samourai.wallet.R;

/**
 * A CustomView for showing and hiding transaction and transactionReview
 * Two layouts will be inflated dynamically and added to FrameLayout
 */
public class SendTransactionDetailsView extends FrameLayout {


    private View transactionView, transactionReview;
    private ViewGroup ricochetHopsReview, stoneWallReview;
    private boolean reviewActive = false;

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
        stoneWallReview = transactionReview.findViewById(R.id.stone_wall_review_layout);
        addView(transactionView);
    }

    public View getTransactionReview() {
        return transactionReview;
    }

    public View getTransactionView() {
        return transactionView;
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

        if (ricochet) {
//            ricochetHopsReview.setVisibility(View.VISIBLE);
            stoneWallReview.setVisibility(View.GONE);
        } else {
            ricochetHopsReview.setVisibility(View.GONE);
            stoneWallReview.setVisibility(View.VISIBLE);
        }

        TransitionManager.beginDelayedTransition(this, set);
        addView(transactionReview);
        reviewActive = true;
        removeView(transactionView);
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
