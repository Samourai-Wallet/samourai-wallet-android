package com.samourai.wallet.whirlpool;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samourai.wallet.R;

import java.util.ArrayList;


public class CycleDetail extends AppCompatActivity {

    private Boolean showMenuItems = false;
    private TextView CycleStatus, TransactionStatus, TransactionId;
    private RecyclerView cycledTxsRecyclerView;
    private TxCyclesAdapter txCyclesAdapter;
    private ArrayList<String> txIds = new ArrayList<>();
    private ProgressBar cycleProgress;
    private TextView registeringInputs, cyclingTx, cycledTxesListHeader;
    private ImageView registeringCheck, cyclingCheck, confirmCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle);
        setSupportActionBar(findViewById(R.id.toolbar));

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TransactionId = findViewById(R.id.transaction_id);
        cycleProgress = findViewById(R.id.pool_cycle_progress);
        cycledTxsRecyclerView = findViewById(R.id.whirpool_cycled_tx_rv);
        cycledTxsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        txCyclesAdapter = new TxCyclesAdapter();
        cycledTxsRecyclerView.setAdapter(txCyclesAdapter);
        cyclingTx = findViewById(R.id.cycling_tx_txt);
        registeringInputs = findViewById(R.id.registering_inputs_txt);
        cycledTxesListHeader = findViewById(R.id.cycled_transaction_list_header);
        registeringCheck = findViewById(R.id.register_input_check);
        cyclingCheck = findViewById(R.id.cycling_check);
        confirmCheck = findViewById(R.id.blockchain_confirm_check);

        disableProgressSection(registeringInputs, registeringCheck);
        disableProgressSection(cyclingTx, cyclingCheck);

        cycleProgress.setMax(100);
        cycleProgress.setProgress(0);

        txIds.add("36ede7de4834dcbf83d0afd5f5209cd7afcb64b6eed4a0bfaea3b3dcc9b84313");
        txCyclesAdapter.notifyDataSetChanged();

        new Handler().postDelayed(() -> {
            enableCheck(confirmCheck);
            txIds.add("36ede7de4834dcbf83d0afd5f5209cd7afcb64b6eed4a0bfaea3b3dcc9b84313");
            txCyclesAdapter.notifyDataSetChanged();
            cycleProgress.setProgress(20);
        }, 1000);

        new Handler().postDelayed(() -> {
            enableSection(registeringInputs, registeringCheck);
            enableCheck(registeringCheck);
            txIds.add("ede7de4834dcbf83d0afd5f5209cd367afcb64b6eed4a0bfaea3b3dcc9b84313");
            txIds.add("bfaea3b3dcc9b8431336ede7de4834dcbf83d0afd5f5209cd7afcb64b6eed4a0");
            txCyclesAdapter.notifyDataSetChanged();
            cycleProgress.setProgress(46);

        }, 3000);

        new Handler().postDelayed(() -> {
            enableSection(cyclingTx, cyclingCheck);
            enableCheck(cyclingCheck);
            txIds.add("34b6eed4a0bfae6ede7de4834dcbf83d0afd5f5209cd7afcb6a3b3dcc9b84313");
            txIds.add("b64b6eed4a0bfaea36ede7de4834dcbf83d0afd5f5209cd7afc3b3dcc9b84313");
            txIds.add("ea3b36ede7de4834dcbf83d0afd5f5209cd7afcb64b6eed4a0bfa3dcc9b84313");
            txCyclesAdapter.notifyDataSetChanged();
            cycleProgress.setProgress(72);
        }, 4000);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whirlpool_cycle_detail_menu, menu);
        menu.findItem(R.id.whirpool_explore_menu).setVisible(showMenuItems);
        menu.findItem(R.id.whirpool_chart_menu).setVisible(showMenuItems);
        return super.onCreateOptionsMenu(menu);
    }

    private void enableCheck(ImageView imageView) {
        imageView.setAlpha(1f);
        imageView.setImageResource(R.drawable.ic_check_circle_24dp);
        imageView.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green_ui_2));
    }

    private void disableProgressSection(TextView textView, ImageView imageView) {
        textView.setAlpha(0.6f);
        imageView.setAlpha(0.6f);
        imageView.setImageResource(R.drawable.circle_dot_white);
        imageView.clearColorFilter();
    }

    private void enableSection(TextView textView, ImageView imageView) {
        textView.setAlpha(1f);
        imageView.setAlpha(1f);
        imageView.setImageResource(R.drawable.circle_dot_white);
        imageView.clearColorFilter();
    }

    public class TxCyclesAdapter extends RecyclerView.Adapter<TxCyclesAdapter.ViewHolder> {

        private Context mContext;
        private static final String TAG = "TxCyclesAdapter";

        public TxCyclesAdapter() {

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cycle_tx, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.item_separator_grey));
            }
        }

        @Override
        public int getItemCount() {

            return txIds.size();
        }


        class ViewHolder extends RecyclerView.ViewHolder {

            private TextView txId, number, minorFees, totalFees;


            ViewHolder(View itemView) {
                super(itemView);


            }
        }


    }

}
