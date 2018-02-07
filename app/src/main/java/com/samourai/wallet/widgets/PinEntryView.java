package com.samourai.wallet.widgets;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.access.ScrambledPin;

import static android.content.Context.VIBRATOR_SERVICE;


/**
 * Re-Usable custom KeypadView for pin entry
 */
public class PinEntryView extends FrameLayout implements View.OnClickListener {


    public enum KeyClearTypes {
        CLEAR_ALL,
        CLEAR
    }

    private Button ta = null;
    private Button tb = null;
    private Button tc = null;
    private Button td = null;
    private Button te = null;
    private Button tf = null;
    private Button tg = null;
    private Button th = null;
    private Button ti = null;
    private Button tj = null;
    private ImageButton tsend = null;
    private ImageButton tback = null;
    private ScrambledPin keypad = null;
    private int pinLen = 0;
    private boolean scramble = false;
    private pinEntryListener entryListener = null;
    private pinClearListener clearListener = null;
    private Vibrator vibrator;
    private boolean enableHaptic = true;

    public PinEntryView(Context context) {
        super(context);
        initView();
    }

    public PinEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PinEntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void setEntryListener(pinEntryListener entryListener) {
        this.entryListener = entryListener;
    }

    public void setClearListener(pinClearListener clearListener) {
        this.clearListener = clearListener;
    }

    public void setScramble(boolean scramble) {
        this.scramble = scramble;
        setButtonLabels();
    }

    private void initView() {
        vibrator = (Vibrator) getContext().getSystemService(VIBRATOR_SERVICE);
        View view = inflate(getContext(), R.layout.keypad_view, null);
        ta = (Button) view.findViewById(R.id.ta);
        tb = (Button) view.findViewById(R.id.tb);
        tc = (Button) view.findViewById(R.id.tc);
        td = (Button) view.findViewById(R.id.td);
        te = (Button) view.findViewById(R.id.te);
        tf = (Button) view.findViewById(R.id.tf);
        tg = (Button) view.findViewById(R.id.tg);
        th = (Button) view.findViewById(R.id.th);
        ti = (Button) view.findViewById(R.id.ti);
        tj = (Button) view.findViewById(R.id.tj);
        tsend = (ImageButton) view.findViewById(R.id.tsend);
        tback = (ImageButton) view.findViewById(R.id.tback);
        tback.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hapticFeedBack();
                pinLen = pinLen--;

                if (clearListener != null) {
                    clearListener.onPinClear(KeyClearTypes.CLEAR);
                }
            }
        });
        tback.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                pinLen = 0;
                if (clearListener != null) {
                    clearListener.onPinClear(KeyClearTypes.CLEAR_ALL);
                }
                hapticFeedBack();
                return false;
            }
        });
        setButtonLabels();
        addView(view);
    }

    @Override
    public void onClick(View view) {
        hapticFeedBack();
        if (pinLen <= AccessFactory.MAX_PIN_LENGTH) {
            if (this.entryListener != null) {
                if (((Button) view).getText().toString().length() < AccessFactory.MAX_PIN_LENGTH)
                    entryListener.onPinEntered(((Button) view).getText().toString(), view);
            }
            pinLen = pinLen++;
        }
    }

    private void hapticFeedBack() {
        if (this.enableHaptic) {
            vibrator.vibrate(44);
        }
    }

    private void setButtonLabels() {
        keypad = new ScrambledPin();
        ta.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(0).getValue()) : "1");
        ta.setOnClickListener(this);
        tb.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(1).getValue()) : "2");
        tb.setOnClickListener(this);
        tc.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(2).getValue()) : "3");
        tc.setOnClickListener(this);
        td.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(3).getValue()) : "4");
        td.setOnClickListener(this);
        te.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(4).getValue()) : "5");
        te.setOnClickListener(this);
        tf.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(5).getValue()) : "6");
        tf.setOnClickListener(this);
        tg.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(6).getValue()) : "7");
        tg.setOnClickListener(this);
        th.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(7).getValue()) : "8");
        th.setOnClickListener(this);
        ti.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(8).getValue()) : "9");
        ti.setOnClickListener(this);
        tj.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(9).getValue()) : "0");
        tj.setOnClickListener(this);
    }

    public interface pinEntryListener {
        void onPinEntered(String key, View view);
    }

    public interface pinClearListener {
        void onPinClear(KeyClearTypes clearType);
    }
}
