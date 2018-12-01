package com.masanari.wallet.spend.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.masanari.wallet.R;


public class EntropyBar extends View {

    private Canvas mCanvas;
    private Bitmap mBitmap;
    private Paint mBarPaint;
    private int maxBars = 3;
    private int totalBars = 2;
    private int mBarWidth = 0;
    private int mBarHeight = 0;
    private int mBarMargin = 4;
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
        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaint.setColor(ContextCompat.getColor(getContext(), R.color.green_ui_2));
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setStrokeCap(Paint.Cap.BUTT);
        mBarWidth = (getWidth() / maxBars) - (mBarWidth * maxBars);
        mBarHeight = (getHeight() / maxBars);
    }

    public void disable() {
        disable = true;
        totalBars = maxBars;
        mBarPaint.setColor(ContextCompat.getColor(getContext(), R.color.red));
        invalidate();
    }

    public void enable() {
        disable = false;
        mBarPaint.setColor(ContextCompat.getColor(getContext(), R.color.green_ui_2));
        invalidate();
    }

    public void setRange(int bars) {
        this.totalBars = bars;
        this.enable();
        invalidate();
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
        for (int i = 0; i < totalBars; i++) {
            int left = getWidth() - ((mBarWidth * i) + mBarMargin);
            int right = getWidth() - (mBarWidth * (i + 1));
            int bottom = !disable ? (mBarHeight * i) + 6 : getHeight() - 2;
            canvas.drawRect(left, getHeight(), right, bottom, mBarPaint);
        }
    }
}

