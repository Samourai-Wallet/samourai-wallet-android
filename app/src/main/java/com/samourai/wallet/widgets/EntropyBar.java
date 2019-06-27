package com.samourai.wallet.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.samourai.boltzmann.processor.TxProcessorResult;
import com.samourai.wallet.R;


public class EntropyBar extends View {

    private Canvas mCanvas;
    private Bitmap mBitmap;
    private Paint mBarPaintActive, mBarPaintDisabled, mBarPaintRed;
    private int maxBars = 3;
    private int enabledBars = maxBars;
    private int mBarWidth = 0;
    private int mBarHeight = 0;
    private boolean disable = false;

    public EntropyBar(Context context) {
        super(context);
        init();
    }

    public EntropyBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EntropyBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBarPaintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaintActive.setColor(ContextCompat.getColor(getContext(), R.color.green_ui_2));
        mBarPaintActive.setStyle(Paint.Style.FILL);
        mBarPaintActive.setStrokeCap(Paint.Cap.BUTT);

        mBarPaintDisabled = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaintDisabled.setColor(ContextCompat.getColor(getContext(), R.color.disabled_grey));
        mBarPaintDisabled.setStyle(Paint.Style.FILL);
        mBarPaintDisabled.setStrokeCap(Paint.Cap.BUTT);

        mBarPaintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaintRed.setColor(ContextCompat.getColor(getContext(), R.color.red));
        mBarPaintRed.setStyle(Paint.Style.FILL);
        mBarPaintRed.setStrokeCap(Paint.Cap.BUTT);
        mBarWidth = (getWidth() / maxBars) - (mBarWidth * maxBars);
        mBarHeight = (getHeight() / maxBars);
    }

    public void disable() {
        enabledBars = maxBars;
        disable = true;
        invalidate();
    }

    public void enable() {
        disable = false;
        invalidate();
    }

    public void setRange(int bars) {
        this.enabledBars = bars;
        this.enable();
    }

    public void setRange(TxProcessorResult result) {
        int percentage = (int) (result.getNRatioDL() * 100);

        if (percentage == 0) {
            this.disable();
            return;
        }
        if (percentage <= 25) {
            setRange(1);
            return;
        }

        if (percentage <= 50) {
            setRange(2);
            return;
        }
        if (percentage <= 75) {
            setRange(3);
            return;
        }
        if (percentage <= 100) {
            setRange(3);
        }

    }

    public void setMaxBars(int maxBars) {
        this.maxBars = maxBars;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        if (w != oldw || h != oldh) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mBitmap.eraseColor(Color.TRANSPARENT);
            mCanvas = new Canvas(mBitmap);
        }
        mBarWidth = (getWidth() / maxBars) - (mBarWidth * maxBars);
        mBarHeight = (getHeight() / maxBars);

        super.onSizeChanged(w, h, oldw, oldh);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint;
        for (int i = 0; i < maxBars; i++) {
            int mBarMargin = 4;
            int left = getWidth() - ((mBarWidth * i) + mBarMargin);
            int right = getWidth() - (mBarWidth * (i + 1));
            int bottom = !disable ? (mBarHeight * i) + 6 : getHeight() - 2;
            int disabledTrack = this.maxBars - this.enabledBars;


            if (i < disabledTrack) {
                paint = mBarPaintDisabled;
            } else {
                paint = mBarPaintActive;
            }
            if (disable) {
                paint = mBarPaintRed;
            }
            canvas.drawRoundRect(left, getHeight(), right, bottom,9f,9f, paint);

        }
    }
}
