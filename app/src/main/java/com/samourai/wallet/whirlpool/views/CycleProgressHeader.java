package com.samourai.wallet.whirlpool.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.samourai.wallet.R;


public class CycleProgressHeader extends FrameLayout {

    View progressHeaderSection, HeaderSection;

    ProgressBar progressBar;

    public CycleProgressHeader(Context context) {
        this(context, null);
        init();
    }

    public CycleProgressHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }


    public CycleProgressHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        progressHeaderSection = inflate(getContext(), R.layout.cycle_progress_header, null);
        HeaderSection = inflate(getContext(), R.layout.cycle_header, null);
        addView(progressHeaderSection);
        progressBar = progressHeaderSection.findViewById(R.id.cycleProgressHeader_progress);
    }

    public void setProgress(int progress, int duration) {
        ObjectAnimator progressAnimator;
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), progress);
        progressAnimator.setDuration(duration);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.start();
    }

}