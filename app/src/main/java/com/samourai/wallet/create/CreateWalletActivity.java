package com.samourai.wallet.create;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.samourai.wallet.R;
import com.samourai.wallet.util.LogUtil;

public class CreateWalletActivity extends AppCompatActivity {
    private static final String TAG = LogUtil.getTag();

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);

        findViews();
        initializeTabLayout();
    }

    private void findViews() {
        mTabLayout = findViewById(R.id.create_wallet_tabLayout);
        mViewPager = findViewById(R.id.create_wallet_viewpager);
    }
    
    private void initializeTabLayout() {
        mTabLayout.addTab(mTabLayout.newTab().setText("Tab1"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Tab2"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Tab3"));
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //Creating our pager adapter
        CreateWalletPagerAdapter adapter = new CreateWalletPagerAdapter(getSupportFragmentManager());

        //Adding adapter to pager
        mViewPager.setAdapter(adapter);

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
    }
}
