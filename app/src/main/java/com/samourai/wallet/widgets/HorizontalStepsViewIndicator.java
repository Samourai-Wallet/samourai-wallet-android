package com.samourai.wallet.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.samourai.wallet.R;


public class HorizontalStepsViewIndicator extends View {

    private Paint paintCircle = new Paint();
    private Paint paintCircleTrack = new Paint();
    private Paint paintLineTrack = new Paint();
    private Paint paintLine = new Paint();
    private int screenW;
    private int distance = 0;
    private Path pathTrack = new Path();
    private Path path = new Path();
    private int points = 3;
    private int animationDuration = 1000;
    private float circleRadius = 32f;
    private float center = 0f;
    private float pathStrokeSize = 12f;
    private float animatedPoint = circleRadius - 5;
    private static final String TAG = "HorizontalStepsViewIndicator";
    private ValueAnimator animator;

    public HorizontalStepsViewIndicator(Context context) {
        super(context);
        init();
    }

    public HorizontalStepsViewIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public HorizontalStepsViewIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public HorizontalStepsViewIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {

        paintCircle = new Paint();
        paintCircle.setColor(ContextCompat.getColor(getContext(), R.color.green_ui_2));
        paintCircle.setStrokeWidth(circleRadius);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setAntiAlias(true);


        paintCircleTrack = new Paint();
        paintCircleTrack.setColor(Color.parseColor("#7A8694"));
        paintCircleTrack.setStrokeWidth(circleRadius);
        paintCircleTrack.setStyle(Paint.Style.FILL);
        paintCircleTrack.setAntiAlias(true);

        paintLine.setColor(ContextCompat.getColor(getContext(), R.color.green_ui_2));
        paintLine.setStrokeWidth(pathStrokeSize);
        paintLine.setAntiAlias(true);
        paintLine.setStyle(Paint.Style.FILL_AND_STROKE);


        paintLineTrack.setColor(Color.parseColor("#7A8694"));
        paintLineTrack.setStrokeWidth(pathStrokeSize);
        paintLineTrack.setAntiAlias(true);
        paintLineTrack.setStyle(Paint.Style.FILL_AND_STROKE);

        this.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int newscreenW = getWidth();
            int newdistance = (screenW / points);
            float NewanimatedPoint = circleRadius * 2;
            if (newscreenW != screenW || newdistance != distance) {
                screenW = newscreenW;
                distance = newdistance;
                animatedPoint = NewanimatedPoint;
                invalidate();
            }
        });
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putFloat("animatedvalue", this.animatedPoint);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            this.animatedPoint = bundle.getInt("animatedvalue");
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }


    public void setPointRadius(Float pointRadius) {
        circleRadius = pointRadius;
    }

    public void setTotalSteps(int point) {
        points = point - 1;
        distance = (screenW / points);
        animatedPoint = circleRadius / 2;
        invalidate();
    }

    public void animateItem(int distance) {
        animator = ValueAnimator.ofFloat(animatedPoint, distance);
        animator.setDuration(animationDuration);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedPoint = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }


    public void setPathStrokeSize(float pathStrokeSize) {
        this.pathStrokeSize = pathStrokeSize;
        invalidate();
    }

    public void setCircleRadius(float radius) {
        this.circleRadius = radius;
        invalidate();
    }

    public void setStep(int i) {
        if (i == 1) {
            animateItem((int) circleRadius);
            return;
        }
        animateItem(distance * (i - 1));
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        path.reset();
        pathTrack.reset();

        path.moveTo(0, center);
        path.lineTo(animatedPoint, center);

        pathTrack.moveTo(0, center);
        pathTrack.lineTo(screenW, center);

        canvas.drawPath(pathTrack, paintLineTrack);
        canvas.drawPath(path, paintLine);

        canvas.drawCircle(circleRadius, center, circleRadius, animatedPoint >= circleRadius ? paintCircle : paintCircleTrack);

        canvas.drawCircle(screenW - circleRadius, center, circleRadius, animatedPoint >= (screenW - circleRadius) ? paintCircle : paintCircleTrack);

        for (int i = 1; i < points; i++) {
            if (animatedPoint >= ((distance * i) - (circleRadius))) {
                canvas.drawCircle(distance * i, center, circleRadius, paintCircle);

            } else {
                canvas.drawCircle(distance * i, center, circleRadius, paintCircleTrack);
            }
        }

        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenW = w;
        center = (float) (h / 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        screenW = getWidth();
        distance = (screenW / points);
        invalidate();
    }


}