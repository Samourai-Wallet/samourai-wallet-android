package com.samourai.wallet.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.samourai.wallet.R;


public class TransactionProgressView extends FrameLayout {

    private ArcProgress mArcProgress;

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
        addView(view);
    }

    public ArcProgress getmArcProgress() {
        return mArcProgress;
    }

    public void setmArcProgress(ArcProgress mArcProgress) {
        this.mArcProgress = mArcProgress;
    }
}
