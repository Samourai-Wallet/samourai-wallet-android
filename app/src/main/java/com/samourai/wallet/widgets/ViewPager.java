package com.samourai.wallet.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class ViewPager extends android.support.v4.view.ViewPager {

    private boolean swipeEnabled;

    public ViewPager(Context context) {
        super(context);
    }

    public ViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void enableSwipe(boolean swipeEnabled) {
        this.swipeEnabled = swipeEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.swipeEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.swipeEnabled && super.onInterceptTouchEvent(event);
    }

}
