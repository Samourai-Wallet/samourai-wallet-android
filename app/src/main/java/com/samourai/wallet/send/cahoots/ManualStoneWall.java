package com.samourai.wallet.send.cahoots;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.widgets.HorizontalStepsViewIndicator;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;

public class ManualStoneWall extends AppCompatActivity {

    private ViewPager viewPager;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private ViewGroup broadCastReviewView;
    private ArrayList<Fragment> steps = new ArrayList<>();
    private CahootReviewFragment cahootReviewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_stone_wall);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        cahootReviewFragment = CahootReviewFragment.newInstance();
        stepsViewGroup = findViewById(R.id.step_view);
        viewPager = findViewById(R.id.view_flipper);

        createSteps();
        viewPager.enableSwipe(false);
        stepsViewGroup.setTotalSteps(5);
//        stepsViewGroup.moveToStep(1);
        broadCastReviewView = (ViewGroup) getLayoutInflater().inflate(R.layout.stonewall_broadcast_details, (ViewGroup) stepsViewGroup.getRootView(), false);
        viewPager.setAdapter(new StepAdapter(getSupportFragmentManager()));
        viewPager.addOnPageChangeListener(new android.support.v4.view.ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                stepsViewGroup.setStep(position + 1);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private void createSteps() {
        for (int i = 0; i < 4; i++) {
            CahootsStepFragment stepView = CahootsStepFragment.newInstance(i);
            stepView.setCahootsScanListener(listener);
            steps.add(stepView);
        }
        steps.add(cahootReviewFragment);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manual_stonewall_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return true;
    }

    private CahootsStepFragment.CahootsScanListener listener = new CahootsStepFragment.CahootsScanListener() {
        @Override
        public void onScan(int step, String qrData) {
            Toast.makeText(getApplicationContext(), String.valueOf(step), Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(), qrData, Toast.LENGTH_LONG).show();
            viewPager.setCurrentItem(step + 1, true);

        }
    };


    private void processScan(String code) {
        Toast.makeText(getApplicationContext(), code, Toast.LENGTH_LONG).show();
    }

    private class StepAdapter extends FragmentPagerAdapter {


        StepAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return steps.get(position);
        }

        @Override
        public int getCount() {
            return steps.size();
        }
    }


}
