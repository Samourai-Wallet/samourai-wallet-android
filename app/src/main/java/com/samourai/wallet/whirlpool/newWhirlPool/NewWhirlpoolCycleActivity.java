package com.samourai.wallet.whirlpool.newWhirlPool;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Group;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.WhirlPoolActivity;
import com.samourai.wallet.whirlpool.adapters.CoinsAdapter;
import com.samourai.wallet.whirlpool.models.Coin;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;

public class NewWhirlpoolCycleActivity extends AppCompatActivity {

    private static final String TAG = "NewWhirlpoolCycleActivi";
    private RecyclerView recyclerView;
    private CoinsAdapter coinsAdapter;
    private ViewGroup reviewButton;

    private TextView stepperMessage1, stepperMessage2, stepperMessage3, cycleTotalAmount;
    private View stepperLine1, stepperLine2;
    private ImageView stepperPoint1, stepperPoint2, stepperPoint3;
    private ChooseUTXOsFragment chooseUTXOsFragment;
    private SelectPoolFragment selectPoolFragment;
    private ChooseUTXOsFragment chooseUTXOsFragment2;
    private ViewPager newPoolViewPager;
    private Button confirmUTXObtn;
    private ConstraintLayout confirmPoolSelectionBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_whirlpool_cycle);
        Toolbar toolbar = findViewById(R.id.toolbar_new_whirlpool);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cycleTotalAmount = findViewById(R.id.cycle_total_amount);

        chooseUTXOsFragment = new ChooseUTXOsFragment();
        selectPoolFragment = new SelectPoolFragment();
        chooseUTXOsFragment2 = new ChooseUTXOsFragment();

        newPoolViewPager = findViewById(R.id.new_pool_viewpager);
//
        setUpStepper();

        newPoolViewPager.setAdapter(new NewPoolStepsPager(getSupportFragmentManager()));
        newPoolViewPager.enableSwipe(false);


        confirmUTXObtn = findViewById(R.id.utxo_selection_confirm_btn);
        confirmPoolSelectionBtn = findViewById(R.id.pool_selection_review_btn);
        confirmUTXObtn.setVisibility(View.VISIBLE);
        confirmPoolSelectionBtn.setVisibility(View.GONE);

        chooseUTXOsFragment.setOnUTXOSelectionListener(coins -> {
            if (coins.size() == 0) {
                enableUTXOConfirmButton(false);
            } else {
                enableUTXOConfirmButton(true);
            }
        });
        confirmUTXObtn.setOnClickListener(view -> {
            newPoolViewPager.setCurrentItem(1);
        });

        setUpViewPager();

    }

    private void setUpViewPager() {
        newPoolViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: {
                        enableStep1(true);
                        break;
                    }
                    case 1: {
                        enableStep2(true);
                        break;
                    }
                    case 2: {
                        enableStep3(true);
                        break;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setUpStepper() {
        stepperMessage1 = findViewById(R.id.stepper_1_message);
        stepperMessage2 = findViewById(R.id.stepper_2_message);
        stepperMessage3 = findViewById(R.id.stepper_3_message);

        stepperLine1 = findViewById(R.id.step_line_1);
        stepperLine2 = findViewById(R.id.step_line_2);

        stepperPoint1 = findViewById(R.id.stepper_point_1);
        stepperPoint2 = findViewById(R.id.stepper_point_2);
        stepperPoint3 = findViewById(R.id.stepper_point_3);

    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed: ".concat(String.valueOf(newPoolViewPager.getCurrentItem())));
        switch (newPoolViewPager.getCurrentItem()) {
            case 0: {
                super.onBackPressed();
                break;
            }
            case 1: {
                newPoolViewPager.setCurrentItem(0);
                enableStep1(true);
                break;
            }
            case 2: {
                newPoolViewPager.setCurrentItem(1);
                enableStep2(true);
                break;
            }
        }
    }


    private void enableUTXOConfirmButton(boolean enable) {
        if (enable) {
            confirmUTXObtn.setEnabled(true);
            confirmUTXObtn.setBackgroundResource(R.drawable.button_blue);
        } else {
            confirmUTXObtn.setEnabled(false);
            confirmUTXObtn.setBackgroundResource(R.drawable.disabled_grey_button);
        }
    }


    class NewPoolStepsPager extends FragmentPagerAdapter {


        NewPoolStepsPager(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: {
                    return chooseUTXOsFragment;
                }
                case 1: {
                    return selectPoolFragment;

                }
                case 2: {
                    return chooseUTXOsFragment2;
                }
            }
            return chooseUTXOsFragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "";
        }
    }

    private void enableStep1(boolean enable) {
        if (enable) {
            stepperMessage1.setTextColor(ContextCompat.getColor(this, R.color.white));
            stepperPoint1.setColorFilter(ContextCompat.getColor(this, R.color.whirlpoolGreen), PorterDuff.Mode.SRC_ATOP);
            enableStep2(false);
            enableStep3(false);
            confirmUTXObtn.setVisibility(View.VISIBLE);
            confirmPoolSelectionBtn.setVisibility(View.GONE);

        } else {
            stepperMessage1.setTextColor(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint1.setColorFilter(ContextCompat.getColor(this, R.color.disabled_white), PorterDuff.Mode.SRC_ATOP);
            stepperPoint1.setAlpha(0.6f);

        }
    }

    private void enableStep2(boolean enable) {
        if (enable) {
            stepperMessage2.setTextColor(ContextCompat.getColor(this, R.color.white));
            stepperPoint2.setColorFilter(ContextCompat.getColor(this, R.color.whirlpoolGreen), PorterDuff.Mode.SRC_ATOP);
            stepperLine1.setBackgroundResource(R.color.whirlpoolGreen);
            enableStep3(false);
            confirmUTXObtn.setVisibility(View.GONE);
            confirmPoolSelectionBtn.setVisibility(View.VISIBLE);
        } else {
            stepperMessage2.setTextColor(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint2.setColorFilter(ContextCompat.getColor(this, R.color.disabled_white), PorterDuff.Mode.SRC_ATOP);
            stepperPoint2.setAlpha(0.6f);
            stepperLine1.setBackgroundResource(R.color.disabled_white);
        }
    }

    private void enableStep3(boolean enable) {
        if (enable) {
            stepperMessage3.setTextColor(ContextCompat.getColor(this, R.color.white));
            stepperPoint3.setColorFilter(ContextCompat.getColor(this, R.color.whirlpoolGreen), PorterDuff.Mode.SRC_ATOP);
            stepperLine2.setBackgroundResource(R.color.whirlpoolGreen);
        } else {
            stepperMessage3.setTextColor(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint3.setColorFilter(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint3.setAlpha(0.6f);
            stepperLine2.setBackgroundResource(R.color.disabled_white);
        }
    }

}
