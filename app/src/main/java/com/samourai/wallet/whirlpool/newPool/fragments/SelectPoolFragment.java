package com.samourai.wallet.whirlpool.newPool.fragments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.adapters.PoolsAdapter;
import com.samourai.wallet.whirlpool.models.Pool;
import com.samourai.wallet.whirlpool.models.PoolCyclePriority;

import java.util.ArrayList;


public class SelectPoolFragment extends Fragment {

    private static final String TAG = "SelectPoolFragment";

    private RecyclerView recyclerView;
    private PoolsAdapter poolsAdapter;
    private ArrayList<Pool> pools = new ArrayList<Pool>();
    private Button feeNormalBtn, feeLowBtn, feeHighBtn;
    private TextView poolFee;
    private PoolCyclePriority poolCyclePriority = PoolCyclePriority.NORMAL;
    private OnPoolSelectionComplete onPoolSelectionComplete;
    private ArrayList<Long> fees = new ArrayList<>();

    public SelectPoolFragment() {
    }

    public void setOnPoolSelectionComplete(OnPoolSelectionComplete onPoolSelectionComplete) {
        this.onPoolSelectionComplete = onPoolSelectionComplete;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        feeNormalBtn = view.findViewById(R.id.pool_fee_normal_btn);
        feeHighBtn = view.findViewById(R.id.pool_fee_high_btn);
        feeLowBtn = view.findViewById(R.id.pool_fee_low_btn);

        recyclerView = view.findViewById(R.id.pool_recycler_view);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new SeparatorDecoration(getContext(), ContextCompat.getColor(getContext(), R.color.item_separator_grey), 1));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        poolsAdapter = new PoolsAdapter(getContext(), pools);
        recyclerView.setAdapter(poolsAdapter);

        loadPools();

        poolFee = view.findViewById(R.id.pool_fee_txt);
        feeLowBtn.setOnClickListener(view1 -> setPoolCyclePriority(PoolCyclePriority.LOW));
        feeHighBtn.setOnClickListener(view1 -> setPoolCyclePriority(PoolCyclePriority.HIGH));
        feeNormalBtn.setOnClickListener(view1 -> setPoolCyclePriority(PoolCyclePriority.NORMAL));

        if (fees.size() >= 2)
            poolFee.setText(String.valueOf(fees.get(1)).concat(" ").concat(getString(R.string.sat_b)));

        poolsAdapter.setOnItemsSelectListener(position -> {
            for (int i = 0; i < pools.size(); i++) {
                if (i == position) {
                    boolean selected = !pools.get(position).isSelected();
                    pools.get(i).setSelected(selected);
                    if (selected && this.onPoolSelectionComplete != null) {
                        onPoolSelectionComplete.onSelect(pools.get(i), poolCyclePriority);
                    } else {
                        if (onPoolSelectionComplete != null)
                            onPoolSelectionComplete.onSelect(null, poolCyclePriority);
                    }
                } else {
                    pools.get(i).setSelected(false);
                }
            }
            poolsAdapter.update(pools);
        });
    }

    public void setFees(ArrayList<Long> fees) {
        this.fees = fees;
    }

    private void loadPools() {
        Pool pool1 = new Pool();
        pool1.setPoolAmount(1000000);
        pool1.setMinerFee(250000);
        pool1.setTotalFee(348450);
        pools.add(pool1);

        Pool pool2 = new Pool();
        pool2.setPoolAmount(5000000);
        pool2.setMinerFee(250000);
        pool2.setTotalFee(348450);
        pools.add(pool2);

        Pool pool3 = new Pool();
        pool3.setPoolAmount(10000000);
        pool3.setMinerFee(250000);
        pool3.setTotalFee(348450);
        pools.add(pool3);
        poolsAdapter.notifyDataSetChanged();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choose_pools, container, false);
    }


    private void setPoolCyclePriority(PoolCyclePriority poolCyclePriority) {

        switch (poolCyclePriority) {
            case LOW: {
                feeNormalBtn.setBackgroundResource(R.drawable.whirlpool_btn_inactive);
                feeHighBtn.setBackgroundResource(R.drawable.whirlpool_btn_inactive);
                feeLowBtn.setBackgroundResource(R.drawable.whirlpool_btn_blue);
                if (fees.size() >= 1)
                    poolFee.setText(String.valueOf(fees.get(0)).concat(" ").concat(getString(R.string.sat_b)));
                break;
            }
            case NORMAL: {
                feeNormalBtn.setBackgroundResource(R.drawable.whirlpool_btn_blue);
                feeHighBtn.setBackgroundResource(R.drawable.whirlpool_btn_inactive);
                feeLowBtn.setBackgroundResource(R.drawable.whirlpool_btn_inactive);
                if (fees.size() >= 2)
                    poolFee.setText(String.valueOf(fees.get(1)).concat(" ").concat(getString(R.string.sat_b)));
                break;
            }

            case HIGH: {
                feeNormalBtn.setBackgroundResource(R.drawable.whirlpool_btn_inactive);
                feeHighBtn.setBackgroundResource(R.drawable.whirlpool_btn_blue);
                feeLowBtn.setBackgroundResource(R.drawable.whirlpool_btn_inactive);
                if (fees.size() >= 2)
                    poolFee.setText(String.valueOf(fees.get(2)).concat(" ").concat(getString(R.string.sat_b)));
                break;
            }


        }
        this.poolCyclePriority = poolCyclePriority;
    }


    @Override
    public void onDetach() {
        this.onPoolSelectionComplete = null;
        super.onDetach();
    }

    public interface OnPoolSelectionComplete {
        void onSelect(Pool pool, PoolCyclePriority priority);
    }


    // RV decorator that sets custom divider for the list
    private class SeparatorDecoration extends RecyclerView.ItemDecoration {

        private final Paint mPaint;

        SeparatorDecoration(@NonNull Context context, @ColorInt int color,
                            @FloatRange(from = 0, fromInclusive = false) float heightDp) {
            mPaint = new Paint();
            mPaint.setColor(color);
            final float thickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    heightDp, context.getResources().getDisplayMetrics());
            mPaint.setStrokeWidth(thickness);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

            final int position = params.getViewAdapterPosition();

            if (position < state.getItemCount()) {
                outRect.set(0, 0, 0, (int) mPaint.getStrokeWidth()); // left, top, right, bottom
            } else {
                outRect.setEmpty(); // 0, 0, 0, 0
            }
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            final int offset = (int) (mPaint.getStrokeWidth() / 2);
            for (int i = 0; i < parent.getChildCount(); i++) {
                // get the view
                final View view = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

                // get the position
                final int position = params.getViewAdapterPosition();
                // draw top separator
                c.drawLine(view.getLeft(), view.getTop() + offset, view.getRight(), view.getTop() + offset, mPaint);

                if (position == state.getItemCount() - 1) {
                    // draw bottom line for the last one
                    c.drawLine(view.getLeft(), view.getBottom() + offset, view.getRight(), view.getBottom() + offset, mPaint);
                }
            }
        }
    }
}
