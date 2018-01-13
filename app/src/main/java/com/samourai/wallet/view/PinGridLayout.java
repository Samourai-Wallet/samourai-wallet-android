package com.samourai.wallet.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridLayout;


public class PinGridLayout extends GridLayout {


    public PinGridLayout(Context context) {
        this(context, null);
    }

    public PinGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PinGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PinGridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


}
