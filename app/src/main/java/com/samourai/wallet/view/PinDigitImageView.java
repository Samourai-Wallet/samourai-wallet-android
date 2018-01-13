package com.samourai.wallet.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.samourai.wallet.R;
import com.samourai.wallet.util.LogUtil;

public class PinDigitImageView extends android.support.v7.widget.AppCompatImageView {
    private static final String TAG = LogUtil.getTag();

    private boolean mIsSetState = false;

    public PinDigitImageView(Context context) {
        this(context, null);
    }

    public PinDigitImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PinDigitImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setStateImage(false);
    }

    public void setState(boolean isSet) {
        if (isSet != mIsSetState) {
            if (LogUtil.DEBUG) Log.d(TAG, "state changed to - " + isSet);
            setStateImage(isSet);
        }
        mIsSetState = isSet;
    }

    public boolean isSet() {
        return mIsSetState;
    }

    private void setStateImage(boolean isSet) {
        if (isSet) {
            setImageResource(R.drawable.pin_digit_set);
        } else {
            setImageResource(R.drawable.pin_digit_not_set);
        }
    }
}
