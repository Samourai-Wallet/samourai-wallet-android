package com.samourai.wallet.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.wallet.R;


public class StepCircleView extends FrameLayout {


    String step = "";
    TextView stepTxview;
    ImageView stepCircle;
    boolean active = false;


    public StepCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public StepCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        init(attrs, defStyleAttr);
    }


    private void init(AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            TypedArray a = null;
            try {
                a = getContext().getTheme()
                        .obtainStyledAttributes(attrs, R.styleable.StepCircleView, defStyleAttr, 0);
                step = a.getString(R.styleable.StepCircleView_step_text);
                active = a.getBoolean(R.styleable.StepCircleView_active, false);

            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }
        View view = inflate(getContext(), R.layout.circle_step_item, null);
        stepCircle = view.findViewById(R.id.step_circle);
        stepTxview = view.findViewById(R.id.step_text);
        stepTxview.setText(step);
        if (active)
            setActive();
        else
            setInactive();
        addView(view);
    }

    public void setActive() {
        active = true;
        stepCircle.setImageResource(R.drawable.circle_shape);
        stepTxview.setVisibility(VISIBLE);
    }
    public void setCompleted() {
        active = true;
        stepCircle.setImageResource(R.drawable.ic_check_circle_24dp);
        stepCircle.setColorFilter(ContextCompat.getColor(getContext(), R.color.green_ui_2));
        stepTxview.setVisibility(GONE);
    }

    public void setInactive() {
        active = true;
        stepCircle.setImageResource(R.drawable.circle_dot_white);
        stepCircle.clearColorFilter();
        stepTxview.setVisibility(GONE);

    }

    public static class StepCircleViewGroup extends LinearLayout {

        public StepCircleViewGroup(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init(attrs, 0);
        }

        public StepCircleViewGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init(attrs, defStyleAttr);
        }

        public StepCircleViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init(attrs, defStyleAttr);
        }

        private void init(AttributeSet attrs, int defStyleAttr) {
            this.setGravity(Gravity.CENTER);
            setStep(0);
        }

        public void setStep(int step) {
            step = step-1;
            if(step > getChildCount() ){
                return;
            }
            for (int i = 0; i < getChildCount(); i++) {
                if (step > i) {
                    ((StepCircleView) getChildAt(i)).setCompleted();
                } else if (step == i) {
                    ((StepCircleView) getChildAt(i)).setActive();
                } else {
                    ((StepCircleView) getChildAt(i)).setInactive();
                }
            }
        }
    }

}
