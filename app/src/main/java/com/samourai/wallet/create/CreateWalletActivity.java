package com.samourai.wallet.create;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.samourai.wallet.R;
import com.samourai.wallet.util.LogUtil;

public class CreateWalletActivity extends AppCompatActivity {
    private static final String TAG = LogUtil.getTag();

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private Button mBtnNext;
    private Button mBtnBack;

    private CreateWalletPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);

        findViews();
        initializeTabLayout();
        setListeners();
    }

    private void findViews() {
        mTabLayout = findViewById(R.id.create_wallet_tabLayout);
        mViewPager = findViewById(R.id.create_wallet_viewpager);
        mBtnBack = findViewById(R.id.btn_tab_back);
        mBtnNext = findViewById(R.id.btn_tab_next);
    }

    private void initializeTabLayout() {
        mTabLayout.setEnabled(false);

        //Creating our pager adapter
        mPagerAdapter = new CreateWalletPagerAdapter(getSupportFragmentManager());

        //Adding adapter to pager
        mViewPager.setAdapter(mPagerAdapter);

        mTabLayout.setupWithViewPager(mViewPager);
    }

    private void setListeners() {
        //Adding onTabSelectedListener to swipe views
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LogUtil.DEBUG) Log.d(TAG, "Next");
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
            }
        });

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LogUtil.DEBUG) Log.d(TAG, "Back");
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            }
        });
    }

}
