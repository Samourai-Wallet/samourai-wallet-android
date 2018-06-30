package com.samourai.wallet;

import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.samourai.wallet.widgets.SendTransactionDetailsView;

import java.util.Objects;


public class SendNewUIActivity extends AppCompatActivity {

    private Button reviewButton;
    private ViewSwitcher viewSwitcher;
    private FrameLayout mainPager;
    private View main, review;
    private SendTransactionDetailsView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_new_ui);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_send));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle("");
        view = findViewById(R.id.mainPager);
//        viewSwitcher = findViewById(R.id.send_main_view_switcher);
//        reviewButton = findViewById(R.id.review_button);
//        reviewButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                TransitionManager.beginDelayedTransition(viewSwitcher, new ChangeBounds());
//                viewSwitcher.showNext();
//            }
//        });

//        com.samourai.wallet.widgets.ViewPager mViewPager =  findViewById(R.id.mainPager);
//        mViewPager.setAdapter(new PagerAdapter() {
//
//            int[] layouts = {R.layout.send_transaction_main_segment, R.layout.send_transaction_review};
//
//            @Override
//            public Object instantiateItem(ViewGroup container, int position) {
//                LayoutInflater inflater = LayoutInflater.from(SendNewUIActivity.this);
//                ViewGroup layout = (ViewGroup) inflater.inflate(layouts[position], container, false);
//                container.addView(layout);
//                return layout;
//            }
//
//            @Override
//            public void destroyItem(ViewGroup container, int position, Object object) {
//                container.removeView((View)object);
//            }
//
//            @Override
//            public CharSequence getPageTitle(int position) {
//                return "";
//            }
//
//            @Override
//            public int getCount() {
//                return layouts.length;
//            }
//
//            @Override
//            public boolean isViewFromObject(View view, Object object) {
//                return view == object;
//            }
//        });
//        mainPager = findViewById(R.id.mainPager);
//        main = LayoutInflater.from(this).inflate(R.layout.send_transaction_main_segment, null);
//        review = LayoutInflater.from(this).inflate(R.layout.send_transaction_review, null);
//
//        mainPager.addView(main);
//
//        main.findViewById(R.id.review_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                TransitionSet set = new TransitionSet();
//                set.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
//                set.addTransition(new Fade())
//                        .addTarget(main)
//                        .addTransition(new Slide(Gravity.END))
//                        .addTarget(review);
//                TransitionManager.beginDelayedTransition(mainPager, set);
//                mainPager.addView(review);
//                mainPager.removeView(main);
//            }
//        });


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                view.showReview(false);
            }
        }, 2500);
    }

}
