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

import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.adapters.CoinsAdapter;
import com.samourai.wallet.whirlpool.models.Coin;

import java.util.ArrayList;


public class ChooseUTXOsFragment extends Fragment {

    private static final String TAG = "ChooseUTXOsFragment";


    private OnUTXOSelectionListener onUTXOSelectionListener;
    private ArrayList<Coin> coins = new ArrayList<Coin>();

    public ChooseUTXOsFragment() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.coins_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        CoinsAdapter coinsAdapter = new CoinsAdapter(getContext(), coins);
        loadDummyCoins();
        recyclerView.setAdapter(coinsAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new SeparatorDecoration(getContext(), ContextCompat.getColor(getContext(), R.color.item_separator_grey), 1));
        coinsAdapter.setOnItemsSelectListener(coinArrayList -> {
            if (this.onUTXOSelectionListener != null) {
                this.onUTXOSelectionListener.onSelect(coinArrayList);
            }
        });
    }

    private void loadDummyCoins() {
        for (int i = 0; i <= 100; i++) {
            Coin coin = new Coin();
            coin.setAddress("16Fg2yjwrbtC6fZp61EV9mN9mNVKmwCzGasw5zGasw5");
            coin.setValue(3.1F);
            coins.add(coin);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choose_utxos, container, false);
    }


    public void setOnUTXOSelectionListener(OnUTXOSelectionListener onUTXOSelectionListener) {
        this.onUTXOSelectionListener = onUTXOSelectionListener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.onUTXOSelectionListener = null;
    }

    public interface OnUTXOSelectionListener {
        void onSelect(ArrayList<Coin> coins);
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
