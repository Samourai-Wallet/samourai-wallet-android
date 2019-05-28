package com.samourai.wallet.paynym.paynymDetails;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.tx.TxDetailsActivity;

import org.bitcoinj.core.Coin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class PaynymTxListAdapter extends RecyclerView.Adapter<PaynymTxListAdapter.ViewHolder> {

    private List<Tx> txList = new ArrayList<>();
    private Context context;

    public PaynymTxListAdapter(List<Tx> txList, Context context) {
        this.txList = txList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tx_item_layout_paynym, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Tx tx = txList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());

        holder.date.setText(sdf.format(tx.getTS() * 1000L));
        holder.amount.setText(sdf.format(tx.getTS() * 1000L));

        long _amount = 0L;
        if (tx.getAmount() < 0.0) {
            _amount = Math.abs((long) tx.getAmount());

        } else {
            _amount = (long) tx.getAmount();

        }
        if (tx.getAmount() < 0.0) {
            holder.icon.setImageResource(R.drawable.out_going_tx_whtie_arrow);
            holder.amount.setText(context.getString(R.string.you_sent).concat(" ").concat(this.getBTCDisplayAmount(_amount)).concat(" BTC"));
        } else {
            holder.icon.setImageResource(R.drawable.incoming_tx_green);
            holder.amount.setText(context.getString(R.string.you_received).concat(" ").concat(this.getBTCDisplayAmount(_amount)).concat(" BTC"));
        }
        holder.amount.getRootView().setOnClickListener(view -> {
            Intent txIntent = new Intent(context, TxDetailsActivity.class);
            txIntent.putExtra("TX", tx.toJSON().toString());
            context.startActivity(txIntent);
        });
    }

    @Override
    public int getItemCount() {
        return txList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView date, amount;
        private ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.tvDate);
            amount = itemView.findViewById(R.id.amount_text_view);
            icon = itemView.findViewById(R.id.TransactionDirection);
        }
    }

    private String getBTCDisplayAmount(long value) {
        return Coin.valueOf(value).toPlainString();
    }
}
