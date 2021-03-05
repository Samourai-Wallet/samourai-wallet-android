package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.PrefsUtil;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class FeeActivity extends SamouraiActivity {

    private Button btLowFee = null;
    private Button btAutoFee = null;
    private Button btPriorityFee = null;
    private Button btCustomFee = null;
    private TextView tvFeePrompt = null;

    private final static int FEE_LOW = 0;
    private final static int FEE_NORMAL = 1;
    private final static int FEE_PRIORITY = 2;
    private int FEE_TYPE = FEE_LOW;
    private long feeLow, feeMed, feeHigh;
    private TextView totalMinerFee, estBlockWait, totalFeeText, selectedFeeLayman, selectedFee, satbText;
    int multiplier = 10000;

    private SeekBar feeSeekBar;

    private Button btOK = null;
    private Button btCancel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fee);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        feeSeekBar = findViewById(R.id.fee_seekbar);
        totalFeeText = findViewById(R.id.total_fee_text);
        totalMinerFee = findViewById(R.id.total_fee);
        estBlockWait = findViewById(R.id.est_block_time);
        selectedFee = findViewById(R.id.selected_fee_rate);
        selectedFeeLayman = findViewById(R.id.selected_fee_rate_in_layman);
        totalMinerFee.setVisibility(View.INVISIBLE);
        totalFeeText.setVisibility(View.INVISIBLE);
        satbText = findViewById(R.id.sat_b);

        btOK = findViewById(R.id.ok);
        btOK.setOnClickListener(v -> {
            Intent data = new Intent();
            setResult(RESULT_OK, data);
            finish();
        });

        btCancel = findViewById(R.id.cancel);
        btCancel.setOnClickListener(view -> finish());


        FEE_TYPE = PrefsUtil.getInstance(this).getValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL);


        feeLow = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
        feeMed = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
        feeHigh = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

        float high = ((float) feeHigh / 2) + (float) feeHigh;
        int feeHighSliderValue = (int) (high * multiplier);
        int feeMedSliderValue = (int) (feeMed * multiplier);


        feeSeekBar.setMax(feeHighSliderValue - multiplier);

        if (feeLow == feeMed && feeMed == feeHigh) {
            feeLow = (long) ((double) feeMed * 0.85);
            feeHigh = (long) ((double) feeMed * 1.15);
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        } else if (feeLow == feeMed || feeMed == feeMed) {
            feeMed = (feeLow + feeHigh) / 2L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        } else {
            ;
        }

        if (feeLow < 1L) {
            feeLow = 1L;
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
        }
        if (feeMed < 1L) {
            feeMed = 1L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        if (feeHigh < 1L) {
            feeHigh = 1L;
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }
//        tvEstimatedBlockWait.setText("6 blocks");
        selectedFeeLayman.setText(getString(R.string.normal));


        FeeUtil.getInstance().sanitizeFee();

        selectedFee.setText((String.valueOf((int) feeMed)));

        // android slider starts at 0
        feeSeekBar.setProgress((int) feeMed - 1);
        setFeeLabels();

        View.OnClickListener inputFeeListener = v -> {
            selectedFee.requestFocus();
            selectedFee.setFocusableInTouchMode(true);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.showSoftInput(selectedFee, InputMethodManager.SHOW_FORCED);
        };

        selectedFeeLayman.setOnClickListener(inputFeeListener);
        satbText.setOnClickListener(inputFeeListener);

        selectedFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int i = (int) ((Double.parseDouble(selectedFee.getText().toString())*multiplier) - multiplier);
                    //feeSeekBar.setMax(feeHighSliderValue - multiplier);
                    feeSeekBar.setProgress(i);
                } catch(NumberFormatException nfe) {
                    System.out.println("Could not parse " + nfe);
                }
                int position = selectedFee.length();
                Editable etext = (Editable) selectedFee.getText();
                Selection.setSelection(etext, position);
            }
        });

        feeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                onSliderChange(i);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        switch (FEE_TYPE) {
            case FEE_LOW:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getLowFee());
                FeeUtil.getInstance().sanitizeFee();
                break;
            case FEE_PRIORITY:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                FeeUtil.getInstance().sanitizeFee();
                break;
            default:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                FeeUtil.getInstance().sanitizeFee();
                break;
        }
        onSliderChange((int) feeMed - 1);


    }

    private void onSliderChange(int progress) {
        DecimalFormat decimalFormat = new DecimalFormat("##.##");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);

        // here we get progress value at 0 , so we need to add 1

        double value = ((double) progress + multiplier) / (double) multiplier;

        selectedFee.setText(String.valueOf(decimalFormat.format(value)));
        if (value == 0.0) {
            value = 1.0;
        }
        double pct = 0.0;
        int nbBlocks = 6;
        if (value <= (double) feeLow) {
            pct = ((double) feeLow / value);
            nbBlocks = ((Double) Math.ceil(pct * 24.0)).intValue();
        } else if (value >= (double) feeHigh) {
            pct = ((double) feeHigh / value);
            nbBlocks = ((Double) Math.ceil(pct * 2.0)).intValue();
            if (nbBlocks < 1) {
                nbBlocks = 1;
            }
        } else {
            pct = ((double) feeMed / value);
            nbBlocks = ((Double) Math.ceil(pct * 6.0)).intValue();
        }
        estBlockWait.setText(nbBlocks + " blocks");
        setFee(value);
        setFeeLabels();

    }

    private void setFee(double fee) {

        double sanitySat = FeeUtil.getInstance().getHighFee().getDefaultPerKB().doubleValue() / 1000.0;
        final long sanityValue;
        if (sanitySat < 10.0) {
            sanityValue = 15L;
        } else {
            sanityValue = (long) (sanitySat * 1.5);
        }

        //        String val  = null;
        double d = FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().doubleValue() / 1000.0;
        NumberFormat decFormat = NumberFormat.getInstance(Locale.US);
        decFormat.setMaximumFractionDigits(3);
        decFormat.setMinimumFractionDigits(0);
        double customValue = 0.0;
        try {
            customValue = (double) fee;
        } catch (Exception e) {
            Toast.makeText(this, R.string.custom_fee_too_low, Toast.LENGTH_SHORT).show();
            return;
        }
        SuggestedFee suggestedFee = new SuggestedFee();
        suggestedFee.setStressed(false);
        suggestedFee.setOK(true);
        suggestedFee.setDefaultPerKB(BigInteger.valueOf((long) (customValue * 1000.0)));
        FeeUtil.getInstance().setSuggestedFee(suggestedFee);

    }


    private void setFeeLabels() {
        float sliderValue = (((float) feeSeekBar.getProgress()) / feeSeekBar.getMax());

        float sliderInPercentage = sliderValue * 100;

        if (sliderInPercentage < 33) {
            selectedFeeLayman.setText(R.string.low);
        } else if (sliderInPercentage > 33 && sliderInPercentage < 66) {
            selectedFeeLayman.setText(R.string.normal);
        } else if (sliderInPercentage > 66) {
            selectedFeeLayman.setText(R.string.urgent);

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(FeeActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}
