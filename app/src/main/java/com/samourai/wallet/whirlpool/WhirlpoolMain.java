package com.samourai.wallet.whirlpool;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.whirlpool.fragments.WhirlpoolCyclesFragment;
import com.samourai.wallet.whirlpool.models.Cycle;
import com.samourai.wallet.whirlpool.newPool.DepositOrChooseUtxoDialog;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;
import java.util.List;

public class WhirlpoolMain extends AppCompatActivity {

    private RecyclerView CycleRecyclerView;
    private WhirlpoolCyclesFragment dashboard, inProgressCycles, completedCycles;
    private ArrayList<Cycle> cycles = new ArrayList();
    private String tabTitle[] = {"Dashboard", "In Progress", "Completed"};
    private ViewPager cyclesViewPager;
    private TabLayout cyclesTabLayout;
    private RecyclerView mixList;
    private TextView totalAmountToDisplay;
    private TextView amountSubText;
    private MixAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whirlpool_main);
        Toolbar toolbar = findViewById(R.id.toolbar_whirlpool);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        totalAmountToDisplay = findViewById(R.id.whirlpool_total_amount_to_display);
        amountSubText = findViewById(R.id.toolbar_subtext);
        mixList = findViewById(R.id.rv_whirlpool_dashboard);
//        cyclesViewPager = findViewById(R.id.whirlpool_viewpager);
//        cyclesTabLayout = findViewById(R.id.whirlpool_home_tabs);
//        cyclesTabLayout.setupWithViewPager(cyclesViewPager);
//        dashboard = new WhirlpoolCyclesFragment();
//        inProgressCycles = new WhirlpoolCyclesFragment();
//        completedCycles = new WhirlpoolCyclesFragment();
//        cyclesViewPager.enableSwipe(true);
//
//        CyclesViewPagerAdapter adapter = new CyclesViewPagerAdapter(getSupportFragmentManager());
//        cyclesViewPager.setAdapter(adapter);
//        cyclesViewPager.setCurrentItem(1);
        findViewById(R.id.whirlpool_fab).setOnClickListener(view -> {
            showBottomSheetDialog();
        });
//
//        cyclesViewPager.addOnPageChangeListener(new android.support.v4.view.ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
//                switch (position) {
//                    //Dashboard
//                    case 0: {
//                        amountSubText.setText(R.string.total_whirlpool_balance);
//                        long value = APIFactory.getInstance(WhirlpoolMain.this).getXpubBalance();
//                        totalAmountToDisplay.setText(Coin.valueOf(value).toPlainString() + " BTC");
//                        break;
//                    }
//
//                    case 1: {
//                        //In-Progress
//                        amountSubText.setText(R.string.premix_balance);
//                        long value = APIFactory.getInstance(WhirlpoolMain.this).getXpubPreMixBalance();
//                        totalAmountToDisplay.setText(Coin.valueOf(value).toPlainString() + " BTC");
//                        break;
//                    }
//
//                    case 2: {
//                        //Completed
//                        amountSubText.setText(R.string.post_mix_balance);
//                        long value = APIFactory.getInstance(WhirlpoolMain.this).getXpubPostMixBalance();
//                        totalAmountToDisplay.setText(Coin.valueOf(value).toPlainString() + " BTC");
//                        break;
//                    }
//
//                }
//
//            }
//
//            @Override
//            public void onPageSelected(int position) { }
//
//            @Override
//            public void onPageScrollStateChanged(int state) { }
//        });
        mixList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MixAdapter(new ArrayList<>());

        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        mixList.addItemDecoration(new ItemDividerDecorator(drawable));
        mixList.setAdapter(adapter);

    }

    private void showBottomSheetDialog() {
        DepositOrChooseUtxoDialog depositOrChooseUtxoDialog = new DepositOrChooseUtxoDialog();
        depositOrChooseUtxoDialog.show(getSupportFragmentManager(), depositOrChooseUtxoDialog.getTag());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_whirl_pool_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(WhirlpoolMain.this).checkTimeOut();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    private class MixAdapter extends RecyclerView.Adapter<MixAdapter.ViewHolder> {


        MixAdapter(List<Cycle> items) {

        }

        @Override
        public MixAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cycle_item, parent, false);
            return new MixAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MixAdapter.ViewHolder holder, int position) {

            //TODO: bind views with WaaS mix states
//            holder.mixingAmount.setText("0.5");

            holder.itemView.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), CycleDetail.class)));
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            TextView mixingProgress, mixingTime, mixingAmount;
            ImageView progressStatus;

            ViewHolder(View view) {
                super(view);
                mView = view;
                mixingAmount = view.findViewById(R.id.whirlpool_cycle_item_mixing_amount);
                mixingProgress = view.findViewById(R.id.whirlpool_cycle_item_mixing_text);
                progressStatus = view.findViewById(R.id.whirlpool_cycle_item_mixing_status_icon);
                mixingTime = view.findViewById(R.id.whirlpool_cycle_item_time);

            }

        }
    }


}
