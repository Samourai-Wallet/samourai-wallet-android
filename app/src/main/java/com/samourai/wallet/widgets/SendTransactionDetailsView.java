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
import android.widget.FrameLayout;

import com.samourai.wallet.R;

public class SendTransactionDetailsView extends FrameLayout {


    private View transactionView, transactionReview;

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
        addView(transactionView);
    }

    public void showReview(boolean recochet) {
        TransitionSet set = new TransitionSet();
        set.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
        set.addTransition(new Fade())
                .addTarget(transactionView)
                .addTransition(new Slide(Gravity.END))
                .addTarget(transactionReview);
        TransitionManager.beginDelayedTransition(this, set);
        addView(transactionReview);
        removeView(transactionView);
    }
}
