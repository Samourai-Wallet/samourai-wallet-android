package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.PrefsUtil;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

public class FeeActivity extends Activity {

    private Button btLowFee = null;
    private Button btAutoFee = null;
    private Button btPriorityFee = null;
    private Button btCustomFee = null;
    private TextView tvFeePrompt = null;

    private final static int FEE_LOW = 0;
    private final static int FEE_NORMAL = 1;
    private final static int FEE_PRIORITY = 2;
    private final static int FEE_CUSTOM = 3;
    private int FEE_TYPE = FEE_LOW;

    private Button btOK = null;
    private Button btCancel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fee);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        FeeActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        btLowFee = (Button)findViewById(R.id.low_fee);
        btAutoFee = (Button)findViewById(R.id.auto_fee);
        btPriorityFee = (Button)findViewById(R.id.priority_fee);
        btCustomFee = (Button)findViewById(R.id.custom_fee);
        tvFeePrompt = (TextView)findViewById(R.id.current_fee_prompt);

        FEE_TYPE = PrefsUtil.getInstance(FeeActivity.this).getValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL);

        long lo = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
        long mi = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
        long hi = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

        if(lo == mi && mi == hi) {
            lo = (long) ((double) mi * 0.85);
            hi = (long) ((double) mi * 1.15);
            SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(lo * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(hi * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }
        else if(lo == mi || mi == hi)    {
            mi = (lo + hi) / 2L;
            SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(mi * 1000L));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        else    {
            ;
        }

        FeeUtil.getInstance().sanitizeFee();

        switch(FEE_TYPE)    {
            case FEE_LOW:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getLowFee());
                FeeUtil.getInstance().sanitizeFee();
                btLowFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.blue));
                btAutoFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.BOLD);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                tvFeePrompt.setText(getText(R.string.fee_low_priority) + " " + getText(R.string.blocks_to_cf));
                break;
            case FEE_PRIORITY:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                FeeUtil.getInstance().sanitizeFee();
                btLowFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.blue));
                btCustomFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.BOLD);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                tvFeePrompt.setText(getText(R.string.fee_high_priority) + " " + getText(R.string.blocks_to_cf));
                break;
            default:
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                FeeUtil.getInstance().sanitizeFee();
                btLowFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.blue));
                btPriorityFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.BOLD);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                tvFeePrompt.setText(getText(R.string.fee_mid_priority) + " " + getText(R.string.blocks_to_cf));
                break;
        }

        btLowFee.setText((FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L) + "\n" + getString(R.string.sat_b));
        btPriorityFee.setText((FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L) + "\n" + getString(R.string.sat_b));
        btAutoFee.setText((FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L) + "\n" + getString(R.string.sat_b));

        btLowFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getLowFee());
                FeeUtil.getInstance().sanitizeFee();
                PrefsUtil.getInstance(FeeActivity.this).setValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_LOW);
                btLowFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.blue));
                btAutoFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.BOLD);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setText(R.string.custom_fee);
                tvFeePrompt.setText(getText(R.string.fee_low_priority) + " " + getText(R.string.blocks_to_cf));
            }
        });

        btAutoFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
                FeeUtil.getInstance().sanitizeFee();
                PrefsUtil.getInstance(FeeActivity.this).setValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_NORMAL);
                btLowFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.blue));
                btPriorityFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btCustomFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.BOLD);
                btPriorityFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setText(R.string.custom_fee);
                tvFeePrompt.setText(getText(R.string.fee_mid_priority) + " " + getText(R.string.blocks_to_cf));
            }
        });

        btPriorityFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                FeeUtil.getInstance().sanitizeFee();
                PrefsUtil.getInstance(FeeActivity.this).setValue(PrefsUtil.CURRENT_FEE_TYPE, FEE_PRIORITY);
                btLowFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btAutoFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btPriorityFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.blue));
                btCustomFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                btLowFee.setTypeface(null, Typeface.NORMAL);
                btAutoFee.setTypeface(null, Typeface.NORMAL);
                btPriorityFee.setTypeface(null, Typeface.BOLD);
                btCustomFee.setTypeface(null, Typeface.NORMAL);
                btCustomFee.setText(R.string.custom_fee);
                tvFeePrompt.setText(getText(R.string.fee_high_priority) + " " + getText(R.string.blocks_to_cf));
            }
        });

        btCustomFee.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doCustomFee();
            }
        });

        btOK = (Button)findViewById(R.id.ok);
        btOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent data = new Intent();
                setResult(RESULT_OK, data);
                finish();
            }

        });

        btCancel = (Button)findViewById(R.id.cancel);
        btCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }

        });

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
        FeeActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        super.onDestroy();
    }

    private String getCurrentFeeSetting()   {
        return getText(R.string.current_fee_selection) + "\n" + (FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() / 1000L) + " " + getText(R.string.slash_sat);
    }

    private void doCustomFee()   {

        double sanitySat = FeeUtil.getInstance().getHighFee().getDefaultPerKB().doubleValue() / 1000.0;
        final long sanityValue;
        if(sanitySat < 10.0)    {
            sanityValue = 15L;
        }
        else    {
            sanityValue = (long)(sanitySat * 1.5);
        }

        final EditText etCustomFee = new EditText(FeeActivity.this);
        double d = FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().doubleValue() / 1000.0;
        NumberFormat decFormat = NumberFormat.getInstance(Locale.US);
        decFormat.setMaximumFractionDigits(3);
        decFormat.setMinimumFractionDigits(0);
        etCustomFee.setText(decFormat.format(d));

        InputFilter filter = new InputFilter() {

            String strCharset = "0123456789nollNOLL.";

            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                for(int i = start; i < end; i++) {
                    if(strCharset.indexOf(source.charAt(i)) == -1) {
                        return "";
                    }
                }

                return null;
            }
        };

        etCustomFee.setFilters(new InputFilter[] { filter } );

        AlertDialog.Builder dlg = new AlertDialog.Builder(FeeActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.set_sat)
                .setView(etCustomFee)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String strCustomFee = etCustomFee.getText().toString();
                        double customValue = 0.0;

                        if(strCustomFee.equalsIgnoreCase("noll") && PrefsUtil.getInstance(FeeActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true)    {
                            customValue = 0.0;
                        }
                        else {

                            try {
                                customValue = Double.valueOf(strCustomFee);
                            } catch (Exception e) {
                                Toast.makeText(FeeActivity.this, R.string.custom_fee_too_low, Toast.LENGTH_SHORT).show();
                                return;
                            }

                        }

                        if(customValue < 1.0 && !strCustomFee.equalsIgnoreCase("noll"))    {
                            Toast.makeText(FeeActivity.this, R.string.custom_fee_too_low, Toast.LENGTH_SHORT).show();
                        }
                        else if(customValue > sanityValue)   {
                            Toast.makeText(FeeActivity.this, R.string.custom_fee_too_high, Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            SuggestedFee suggestedFee = new SuggestedFee();
                            suggestedFee.setStressed(false);
                            suggestedFee.setOK(true);
                            suggestedFee.setDefaultPerKB(BigInteger.valueOf((long)(customValue * 1000.0)));
                            Log.d("FeeActivity", "custom fee:" + BigInteger.valueOf((long)(customValue * 1000.0)));
                            FeeUtil.getInstance().setSuggestedFee(suggestedFee);

                            btLowFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                            btAutoFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                            btPriorityFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.darkgrey));
                            btCustomFee.setBackgroundColor(FeeActivity.this.getResources().getColor(R.color.blue));

                            btCustomFee.setText((FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().doubleValue() / 1000L) + "\n" + getString(R.string.sat_b));
                            btCustomFee.setTypeface(null, Typeface.BOLD);
                            btLowFee.setTypeface(null, Typeface.NORMAL);
                            btAutoFee.setTypeface(null, Typeface.NORMAL);
                            btPriorityFee.setTypeface(null, Typeface.NORMAL);

                            long lowFee = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
                            long normalFee = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
                            long highFee = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

                            double pct = 0.0;
                            int nbBlocks = 6;
                            if(customValue == 0.0)    {
                                customValue = 1.0;
                            }
                            if(customValue <= (double)lowFee)    {
                                pct = ((double)lowFee / customValue);
                                nbBlocks = ((Double)Math.ceil(pct * 24.0)).intValue();
                            }
                            else if(customValue >= (double)highFee)   {
                                pct = ((double)highFee / customValue);
                                nbBlocks = ((Double)Math.ceil(pct * 2.0)).intValue();
                                if(nbBlocks < 1)    {
                                    nbBlocks = 1;
                                }
                            }
                            else    {
                                pct = ((double)normalFee / customValue);
                                nbBlocks = ((Double)Math.ceil(pct * 6.0)).intValue();
                            }
                            tvFeePrompt.setText(getText(R.string.fee_custom_priority) + " " + nbBlocks  + " " + getText(R.string.blocks_to_cf));
                        }

                        dialog.dismiss();

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        if(!isFinishing())    {
            dlg.show();
        }

    }

}
