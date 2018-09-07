package com.samourai.wallet.whirlpool.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import com.samourai.wallet.R;

import java.util.concurrent.TimeUnit;

public class CycleProgressHeader extends FrameLayout {

    View progressHeaderSection, HeaderSection;

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
    }


}