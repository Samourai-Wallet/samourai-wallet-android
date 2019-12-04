package com.samourai.wallet.whirlpool;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.whirlpool.models.Cycle;
import com.samourai.wallet.whirlpool.newPool.DepositOrChooseUtxoDialog;
import com.samourai.wallet.widgets.ItemDividerDecorator;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.List;

public class WhirlpoolMain extends AppCompatActivity {

    private ArrayList<Cycle> cycles = new ArrayList();
    private String tabTitle[] = {"Dashboard", "In Progress", "Completed"};
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
        findViewById(R.id.whirlpool_fab).setOnClickListener(view -> {
            showBottomSheetDialog();
        });
        mixList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MixAdapter(new ArrayList<>());

        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        mixList.addItemDecoration(new ItemDividerDecorator(drawable));
        mixList.setAdapter(adapter);

        long postMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPostMixBalance();
        long preMixBalance = APIFactory.getInstance(WhirlpoolMain.this).getXpubPreMixBalance();

        totalAmountToDisplay.setText(Coin.valueOf(postMixBalance + preMixBalance).toPlainString().concat(" BTC"));
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
