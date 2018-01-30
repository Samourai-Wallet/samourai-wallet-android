package com.samourai.wallet.ui.custom;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * This TabLayout will only indicate the current selected tab, but will ignore clicks.
 */
public class IndicatorTabLayout extends TabLayout {


    public IndicatorTabLayout(Context context) {
        this(context, null);
    }

    public IndicatorTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // We consume all intercepted touch events to ignore Tab clicks
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We want to intercept all touch event
        return true;
    }
}
