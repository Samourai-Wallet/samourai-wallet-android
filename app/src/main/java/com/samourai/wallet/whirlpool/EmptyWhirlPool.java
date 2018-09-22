package com.samourai.wallet.whirlpool;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.wallet.R;

public class EmptyWhirlPool extends AppCompatActivity {

    private ImageView[] indicators;
    private ViewPager viewPager;
    private LinearLayout pagerIndicatorContainer;
    private FloatingActionButton newWhirlPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_whirl_pool);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Whirlpool");
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = findViewById(R.id.viewpager_intro_whirlpool);
        viewPager.setAdapter(new SliderAdapter(this));
        pagerIndicatorContainer = findViewById(R.id.dots);
        newWhirlPool = findViewById(R.id.fab_new_whirlpool);
        setPagerIndicators();
        newWhirlPool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EmptyWhirlPool.this, ChooseCycleType.class);
                startActivity(intent);
            }
        });
        newWhirlPool.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(EmptyWhirlPool.this, WhirlpoolMain.class);
                startActivity(intent);
                return true;
            }
        });

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
                int color[] = new int[]{R.color.sea, R.color.teal, R.color.orange};

                for (int i = 0; i < count; i++) {
                    indicators[i].setImageDrawable(getResources().getDrawable(R.drawable.pager_indicator_dot));
                }
                // here we using PorterDuff mode to overlay color over ImageView to set Active indicator
                // we don't have to create multiple asset for showing active and inactive states of indicators
                indicators[position].getDrawable().setColorFilter(getResources().getColor(color[position]), PorterDuff.Mode.ADD);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }


    private class SliderAdapter extends PagerAdapter {

        private Context mContext;

        public SliderAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.whirlpool_intro_item, collection, false);
            TextView title = layout.findViewById(R.id.whirlpool_intro_title);
            TextView caption = layout.findViewById(R.id.whirlpool_intro_caption);
            ImageView image = layout.findViewById(R.id.whirlpool_intro_img);
            collection.addView(layout);
            switch (position) {
                case 0: {
                    image.setImageResource(R.drawable.ic_bubbles);
                    image.setMinimumWidth(160);
                    image.setMinimumHeight(160);
                    layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.sea));
                    title.setText(R.string.metadata_cleaner);
                    caption.setText(R.string.cycling_bitcoin);
                    break;
                }
                case 1: {
                    title.setText(R.string.end_to_end_trustless);
                    caption.setText(R.string.your_private_keys);
                    layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.teal));
                    break;
                }
                case 2: {
                    title.setText(R.string.different_cycles_for);
                    caption.setText(R.string.complete_a_slow_cycle);
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
