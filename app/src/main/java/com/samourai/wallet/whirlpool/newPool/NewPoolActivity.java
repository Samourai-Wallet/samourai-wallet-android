package com.samourai.wallet.whirlpool.newPool;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.models.Coin;
import com.samourai.wallet.whirlpool.models.Pool;
import com.samourai.wallet.whirlpool.models.PoolCyclePriority;
import com.samourai.wallet.whirlpool.newPool.fragments.ChooseUTXOsFragment;
import com.samourai.wallet.whirlpool.newPool.fragments.ReviewPoolFragment;
import com.samourai.wallet.whirlpool.newPool.fragments.SelectPoolFragment;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;

import static android.graphics.Typeface.BOLD;

public class NewPoolActivity extends AppCompatActivity {

    private static final String TAG = "NewPoolActivity";


    private TextView stepperMessage1, stepperMessage2, stepperMessage3, cycleTotalAmount;
    private View stepperLine1, stepperLine2;
    private ImageView stepperPoint1, stepperPoint2, stepperPoint3;
    private ChooseUTXOsFragment chooseUTXOsFragment;
    private SelectPoolFragment selectPoolFragment;
    private ReviewPoolFragment reviewPoolFragment;
    private ViewPager newPoolViewPager;
    private Button confirmButton;

    private ArrayList<Coin> selectedCoins = new ArrayList<>();
    private ArrayList<Long> fees = new ArrayList<>();
    private Pool selectedPool = null;
    private PoolCyclePriority selectedPoolPriority = PoolCyclePriority.NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_whirlpool_cycle);
        Toolbar toolbar = findViewById(R.id.toolbar_new_whirlpool);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cycleTotalAmount = findViewById(R.id.cycle_total_amount);

        fees.add(20L);
        fees.add(30L);
        fees.add(60L);

        chooseUTXOsFragment = new ChooseUTXOsFragment();
        selectPoolFragment = new SelectPoolFragment();
        reviewPoolFragment = new ReviewPoolFragment();
        selectPoolFragment.setFees(this.fees);

        newPoolViewPager = findViewById(R.id.new_pool_viewpager);

        setUpStepper();

        newPoolViewPager.setAdapter(new NewPoolStepsPager(getSupportFragmentManager()));
        newPoolViewPager.enableSwipe(false);

        confirmButton = findViewById(R.id.utxo_selection_confirm_btn);

        confirmButton.setVisibility(View.VISIBLE);

        enableConfirmButton(false);

        chooseUTXOsFragment.setOnUTXOSelectionListener(coins -> {
            if (coins.size() == 0) {
                enableConfirmButton(false);
            } else {
                enableConfirmButton(true);
            }
        });
        selectPoolFragment.setOnPoolSelectionComplete((pool, priority) -> {
            selectedPool = pool;
            selectedPoolPriority = priority;
            enableConfirmButton(selectedPool != null);
        });

        confirmButton.setOnClickListener(view -> {
            switch (newPoolViewPager.getCurrentItem()) {
                case 0: {
                    newPoolViewPager.setCurrentItem(1);
                    initUTXOReviewButton();
                    enableConfirmButton(selectedPool != null);
                    break;
                }
                case 1: {
                    newPoolViewPager.setCurrentItem(2);
                    confirmButton.setText(getString(R.string.begin_cycle));
                    confirmButton.setBackgroundResource(R.drawable.button_green);
                    break;
                }
                case 2: {
                    processWhirlPool();
                    break;
                }
            }
        });

        setUpViewPager();

    }

    private void processWhirlPool() {
        Toast.makeText(this,"Begin Pool",Toast.LENGTH_SHORT).show();
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
                        confirmButton.setBackgroundResource(R.drawable.whirlpool_btn_blue);
                        confirmButton.setText(R.string.next);
                        break;
                    }
                    case 1: {
                        initUTXOReviewButton();
                        enableStep2(true);
                        enableConfirmButton(selectedPool != null);
                        break;
                    }
                    case 2: {
                        //pass Pool information to @ReviewPoolFragment
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

    private void initUTXOReviewButton() {

        String reviewMessage = getString(R.string.review_cycle_details).concat("\n");
        String reviewAmountMessage = getString(R.string.total_being_cycled).concat(" ");
        String amount = "1.1".concat(" BTC");

        SpannableString spannable = new SpannableString(reviewMessage.concat(reviewAmountMessage).concat(amount));
        spannable.setSpan(
                new StyleSpan(BOLD),
                0, reviewMessage.length() - 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new RelativeSizeSpan(.8f),
                reviewMessage.length() - 1, spannable.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        confirmButton.setBackgroundResource(R.drawable.whirlpool_btn_blue);
        confirmButton.setText(spannable);
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

        enableStep3(false);
        enableStep2(false);

    }

    @Override
    public void onBackPressed() {
        switch (newPoolViewPager.getCurrentItem()) {
            case 0: {
                super.onBackPressed();
                break;
            }
            case 1: {
                newPoolViewPager.setCurrentItem(0);
                break;
            }
            case 2: {
                newPoolViewPager.setCurrentItem(1);
                break;
            }
        }
    }

    private void enableConfirmButton(boolean enable) {
        if (enable) {
            confirmButton.setEnabled(true);
            confirmButton.setBackgroundResource(R.drawable.button_blue);
        } else {
            confirmButton.setEnabled(false);
            confirmButton.setBackgroundResource(R.drawable.disabled_grey_button);
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
                    return reviewPoolFragment;
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
            stepperPoint1.setColorFilter(ContextCompat.getColor(this, R.color.whirlpoolGreen));
            stepperMessage1.setAlpha(1f);
            enableStep2(false);
            enableStep3(false);
        } else {
            stepperMessage1.setTextColor(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint1.setColorFilter(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint1.setAlpha(0.6f);

        }
    }

    private void enableStep2(boolean enable) {
        if (enable) {
            stepperMessage2.setTextColor(ContextCompat.getColor(this, R.color.white));
            stepperMessage2.setAlpha(1f);
            stepperPoint2.setColorFilter(ContextCompat.getColor(this, R.color.whirlpoolGreen));
            stepperPoint2.setAlpha(1f);
            stepperMessage1.setAlpha(0.6f);
            stepperLine1.setBackgroundResource(R.color.whirlpoolGreen);
            enableStep3(false);
        } else {
            stepperMessage2.setTextColor(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint2.setColorFilter(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint2.setAlpha(0.6f);
            stepperLine1.setBackgroundResource(R.color.disabled_white);
        }
    }

    private void enableStep3(boolean enable) {
        if (enable) {
            stepperMessage3.setTextColor(ContextCompat.getColor(this, R.color.white));
            stepperPoint3.setColorFilter(ContextCompat.getColor(this, R.color.whirlpoolGreen));
            stepperPoint3.setAlpha(1f);
            stepperMessage2.setAlpha(0.6f);
            stepperLine2.setBackgroundResource(R.color.whirlpoolGreen);
        } else {
            stepperMessage3.setTextColor(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint3.setColorFilter(ContextCompat.getColor(this, R.color.disabled_white));
            stepperPoint3.setAlpha(0.6f);
            stepperLine2.setBackgroundResource(R.color.disabled_white);
        }
    }

}
