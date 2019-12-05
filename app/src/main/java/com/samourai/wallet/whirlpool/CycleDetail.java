package com.samourai.wallet.whirlpool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.widgets.ItemDividerDecorator;

import java.util.ArrayList;


public class CycleDetail extends AppCompatActivity {

    private Boolean showMenuItems = true;
    private TextView cycleStatus, transactionStatus, transactionId;
    private RecyclerView cycledTxsRecyclerView;
    private TxCyclesAdapter txCyclesAdapter;
    private ArrayList<UTXOCoin> mixedUTXOs = new ArrayList<>();
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

        transactionId = findViewById(R.id.whirlpool_header_tx_hash);
        cycleProgress = findViewById(R.id.pool_cycle_progress);
        cycledTxsRecyclerView = findViewById(R.id.whirlpool_cycled_tx_rv);
        cycledTxsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        txCyclesAdapter = new TxCyclesAdapter();
        cycledTxsRecyclerView.setAdapter(txCyclesAdapter);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        cycledTxsRecyclerView.addItemDecoration(new ItemDividerDecorator(drawable));
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

        getSupportActionBar().setTitle("0.5 BTC");

        mixedUTXOs.add(new UTXOCoin(null, null));
        txCyclesAdapter.notifyDataSetChanged();
        transactionId.setText("36ede7de4834dcbf83d0afd5f5209cd7afcb64b6eed4a0bfaea3b3dcc9b84313");
        new Handler().postDelayed(() -> {
            enableCheck(confirmCheck);
            mixedUTXOs.add(new UTXOCoin(null, null));
            txCyclesAdapter.notifyDataSetChanged();
            cycleProgress.setProgress(20);
        }, 1000);

        new Handler().postDelayed(() -> {
            enableSection(registeringInputs, registeringCheck);
            enableCheck(registeringCheck);
            mixedUTXOs.add(new UTXOCoin(null, null));
            txCyclesAdapter.notifyDataSetChanged();
            cycleProgress.setProgress(46);

        }, 3000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whirlpool_cycle_detail_menu, menu);
        menu.findItem(R.id.whirlpool_explore_menu).setVisible(showMenuItems);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.whirlpool_explore_menu: {
                Toast.makeText(getApplicationContext(), "Open in browser", Toast.LENGTH_SHORT).show();
                break;
            }
            case android.R.id.home: {
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
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


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cycle_tx, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            //TODO - Bind views
        }

        @Override
        public int getItemCount() {

            return mixedUTXOs.size();
        }


        class ViewHolder extends RecyclerView.ViewHolder {

            TextView utxoHash, amount;


            ViewHolder(View itemView) {
                super(itemView);
                amount = itemView.findViewById(R.id.whirlpool_cycle_list_item_utxo_amount);
                utxoHash = itemView.findViewById(R.id.whirlpool_cycle_list_item_utxo_hash);
            }
        }


    }

}
