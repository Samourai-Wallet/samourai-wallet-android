package com.samourai.wallet.whirlpool;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.samourai.wallet.R;

public class EmptyWhirlPool extends Activity {

    private ImageView[] indicators;
    private ViewPager viewPager;
    private LinearLayout pagerIndicatorContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_whirl_pool);

        viewPager = findViewById(R.id.viewpager_intro_whirlpool);
        viewPager.setAdapter(new CustomPagerAdapter(this));
        pagerIndicatorContainer = findViewById(R.id.dots);
//        viewPager.addOnPageChangeListener(new CircularViewPagerHandler(viewPager));
        setPagerIndicators();
    }

    private void setPagerIndicators() {
        final int count = viewPager.getAdapter().getCount();
        indicators = new ImageView[count];
        //Creating circle dot ImageView based on adapter size
        for (int i = 0; i < count; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(getResources().getDrawable(R.drawable.pager_indicator_dot));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    16, 16
            );
            params.setMargins(8, 0, 8, 0);
            pagerIndicatorContainer.addView(indicators[i], params);
        }
        //Setting first ImageView as active indicator
        indicators[0].setImageDrawable(getResources().getDrawable(R.drawable.pager_indicator_dot));
        indicators[0].getDrawable().setColorFilter(Color.parseColor("#fafafa"), PorterDuff.Mode.ADD);
        // Viewpager listener is responsible for changing indicator color
        viewPager.addOnPageChangeListener(new com.samourai.wallet.widgets.ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < count; i++) {
                    indicators[i].setImageDrawable(getResources().getDrawable(R.drawable.pager_indicator_dot));
                }
                // here we using PorterDuff mode to overlay color over ImageView to set Active indicator
                // we don't have to create multiple asset for showing active and inactive states of indicators
                indicators[position].getDrawable().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.ADD);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }


    private class CustomPagerAdapter extends PagerAdapter {

        private Context mContext;

        public CustomPagerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.whirlpool_intro_item, collection, false);
            collection.addView(layout);
            switch (position) {
                case 0: {

                    layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.sea));
                    break;
                }
                case 1: {
                    layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.teal));
                    break;
                }
                case 2: {
                    layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.orange));
                    break;
                }
            }
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }


    }


}
