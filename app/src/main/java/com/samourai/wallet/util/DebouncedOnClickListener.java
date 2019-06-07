package com.samourai.wallet.util;


import android.os.SystemClock;
import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Debounced OnClickListener
 * Rejects clicks that are too close together in time.
 * This class is safe to use as an OnClickListener for multiple views, and will debounce each one separately.
 */
public abstract class DebouncedOnClickListener implements View.OnClickListener {

    private final long mMinimumInterval;
    private Map<View, Long> mLastClickMap;

    /**
     * Implement this in your subclass instead of onClick
     *
     * @param v The view that was clicked
     */
    public abstract void onDebouncedClick(View v);

    /**
     * The one and only constructor
     *
     * @param minimumIntervalMsec The minimum allowed time between clicks - any click sooner than this after a previous click will be rejected
     */
    public DebouncedOnClickListener(long minimumIntervalMsec) {
        this.mMinimumInterval = minimumIntervalMsec;
        this.mLastClickMap = new WeakHashMap<View, Long>();
    }

    @Override
    public void onClick(View clickedView) {
        Long previousClickTimestamp = mLastClickMap.get(clickedView);
        long currentTimestamp = SystemClock.uptimeMillis();

        mLastClickMap.put(clickedView, currentTimestamp);
        if (previousClickTimestamp == null || Math.abs(currentTimestamp - previousClickTimestamp.longValue()) > mMinimumInterval) {
            onDebouncedClick(clickedView);
        }
    }
}