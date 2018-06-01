package com.samourai.wallet.widgets;

import android.animation.Animator;
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

import java.util.concurrent.TimeUnit;

public class ArcProgress extends View {

    private static final int ARC_FIRST_ARC_ANGLE = 150;
    private static final int ARC_SECOND_ARC_ANGLE = 70;
    private static final int ARC_THIRD_ARC_ANGLE = 70;


    private static final int ARC_FIRST_ARC_START_ANGLE = 195;
    private static final int ARC_SECOND_ARC_START_ANGLE = 10;
    private static final int ARC_THIRD_ARC_START_ANGLE = 100;

    private static final float THICKNESS_SCALE = 0.009f;

    private float firstAngle = 0f;
    private float secondAngle = 0f;
    private float thirdAngle = 0f;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private RectF mCircleOuterBounds;
    private RectF mCircleInnerBounds;

    private Paint mCirclePaint;
    private Paint mEraserPaint;

    private ValueAnimator mTimerAnimator;

    public ArcProgress(Context context) {
        this(context, null);
        init();
    }

    public ArcProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public ArcProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void  init(){

        int circleColor = Color.WHITE;

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStrokeCap(Paint.Cap.ROUND);
        mCirclePaint.setColor(circleColor);

        mEraserPaint = new Paint();
        mEraserPaint.setAntiAlias(true);
        mEraserPaint.setColor(Color.TRANSPARENT);
        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mBitmap.eraseColor(Color.TRANSPARENT);
            mCanvas = new Canvas(mBitmap);
        }

        super.onSizeChanged(w, h, oldw, oldh);
        updateBounds();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        mCanvas.drawArc(mCircleOuterBounds, ARC_FIRST_ARC_START_ANGLE, firstAngle, true, mCirclePaint);
        mCanvas.drawArc(mCircleOuterBounds, ARC_SECOND_ARC_START_ANGLE, secondAngle, true, mCirclePaint);
        mCanvas.drawArc(mCircleOuterBounds, ARC_THIRD_ARC_START_ANGLE, thirdAngle, true, mCirclePaint);
        mCanvas.drawOval(mCircleInnerBounds, mEraserPaint);
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    public void startArc1(final int mills) {
        stop();
        mTimerAnimator = ValueAnimator.ofFloat(0f, ARC_FIRST_ARC_ANGLE);
        mTimerAnimator.setDuration(TimeUnit.MILLISECONDS.toMillis(mills));
        mTimerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mTimerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                firstAngle = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mTimerAnimator.start();
    }

    public void startArc2(final int mills) {
        stop();
        mTimerAnimator = ValueAnimator.ofFloat(0f, ARC_SECOND_ARC_ANGLE);
        mTimerAnimator.setDuration(TimeUnit.MILLISECONDS.toMillis(mills));
        mTimerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mTimerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                secondAngle = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mTimerAnimator.start();
    }

    public void startArc3(int mills) {
        stop();
        mTimerAnimator = ValueAnimator.ofFloat(0f, ARC_THIRD_ARC_ANGLE);
        mTimerAnimator.setDuration(TimeUnit.MILLISECONDS.toMillis(mills));
        mTimerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mTimerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                thirdAngle = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mTimerAnimator.start();
    }


    public void stop() {
        if (mTimerAnimator != null && mTimerAnimator.isRunning()) {
            mTimerAnimator.cancel();
            mTimerAnimator = null;
        }
    }


    private void updateBounds() {
        final float thickness = getWidth() * THICKNESS_SCALE;

        mCircleOuterBounds = new RectF(0, 0, getWidth(), getHeight());
        mCircleInnerBounds = new RectF(
                mCircleOuterBounds.left + thickness,
                mCircleOuterBounds.top + thickness,
                mCircleOuterBounds.right - thickness,
                mCircleOuterBounds.bottom - thickness);

        invalidate();
    }
}