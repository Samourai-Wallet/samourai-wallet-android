package com.samourai.wallet.widgets;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Group;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.samourai.wallet.R;


public class TransactionProgressView extends FrameLayout {

    private ArcProgress mArcProgress;
    private ConstraintLayout mTransactionProgressContainer;
    private ImageView mCheckMark;
    private Button mTennaBroadCastBtn;
    private Button mShowQRBtn;
    private TextView txProgressText, txSubText;

    public TransactionProgressView(@NonNull Context context) {
        super(context);
        init();

    }

    public TransactionProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public TransactionProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

    }

    public TransactionProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.transaction_progress_view, null);
        mArcProgress = view.findViewById(R.id.arc_progress);
        mTransactionProgressContainer = view.findViewById(R.id.TransactionProgressContainer);
        mCheckMark = view.findViewById(R.id.check_vd);
        mTennaBroadCastBtn = view.findViewById(R.id.tx_broadcast_tenna_btn);
        mShowQRBtn = view.findViewById(R.id.tx_show_qr_btn);
        txProgressText = view.findViewById(R.id.tx_progress_text);
        txSubText = view.findViewById(R.id.tx_progress_sub_text);
        addView(view);
    }

    public ArcProgress getmArcProgress() {
        return mArcProgress;
    }

    public void offlineMode(int duration) {
        int colorFrom = getResources().getColor(R.color.tx_broadcast_normal_bg);
        int colorTo = getResources().getColor(R.color.tx_broadcast_offline_bg);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(duration); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                mTransactionProgressContainer.setBackgroundColor((int) animator.getAnimatedValue());
            }

        });
        colorAnimation.start();

    }

    public void showCheck() {

        ((Animatable) mCheckMark.getDrawable()).start();
    }

    public void setTxStatusMessage(int resId) {
        txProgressText.setText(resId);
    }

    public void setTxStatusMessage(String message) {
        txProgressText.setText(message);
    }

    public void setTxSubText(int resId) {
        txSubText.setText(resId);
    }

    public void setTxSubText(String message) {
        txSubText.setText(message);
    }

    public void toggleOfflineButton() {
        int visibility = mTennaBroadCastBtn.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        mTennaBroadCastBtn.setVisibility(visibility);
        mShowQRBtn.setVisibility(visibility);

    }

    public void onlineMode() {

    }

    public void setmArcProgress(ArcProgress mArcProgress) {
        this.mArcProgress = mArcProgress;
    }

    public void reset() {
        init();
    }

    public Button getTxTennaButton() {
        return mTennaBroadCastBtn;
    }

    public Button getShowQRButton() {
        return mShowQRBtn;
    }

}
