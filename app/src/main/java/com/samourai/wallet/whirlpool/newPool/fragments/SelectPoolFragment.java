package com.samourai.wallet.whirlpool.newPool.fragments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.samourai.wallet.R;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.whirlpool.adapters.PoolsAdapter;
import com.samourai.wallet.whirlpool.models.PoolCyclePriority;
import com.samourai.wallet.whirlpool.models.PoolViewModel;
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;


public class SelectPoolFragment extends Fragment {

    private static final String TAG = "SelectPoolFragment";

    private RecyclerView recyclerView;
    private PoolsAdapter poolsAdapter;
    private ArrayList<PoolViewModel> poolViewModels = new ArrayList<PoolViewModel>();
    private MaterialButton feeNormalBtn, feeLowBtn, feeHighBtn;
    private TextView poolFee;
    private PoolCyclePriority poolCyclePriority = PoolCyclePriority.NORMAL;
    private OnPoolSelectionComplete onPoolSelectionComplete;
    private ArrayList<Long> fees = new ArrayList<>();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Long tx0Amount = 0L;
    private List<UTXOCoin> coins = new ArrayList<>();

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
        poolsAdapter = new PoolsAdapter(getContext(), poolViewModels);
        recyclerView.setAdapter(poolsAdapter);
        poolFee = view.findViewById(R.id.pool_fee_txt);
        feeLowBtn.setOnClickListener(view1 -> setPoolCyclePriority(PoolCyclePriority.LOW));
        feeHighBtn.setOnClickListener(view1 -> setPoolCyclePriority(PoolCyclePriority.HIGH));
        feeNormalBtn.setOnClickListener(view1 -> setPoolCyclePriority(PoolCyclePriority.NORMAL));

        if (fees.size() >= 2)
            poolFee.setText(String.valueOf(fees.get(1)).concat(" ").concat(getString(R.string.sat_b)));

        poolsAdapter.setOnItemsSelectListener(position -> {
            for (int i = 0; i < poolViewModels.size(); i++) {
                if (i == position) {
                    boolean selected = !poolViewModels.get(position).isSelected();
                    poolViewModels.get(i).setSelected(selected);
                    if (selected && this.onPoolSelectionComplete != null) {
                        onPoolSelectionComplete.onSelect(poolViewModels.get(i), poolCyclePriority);
                    } else {
                        if (onPoolSelectionComplete != null)
                            onPoolSelectionComplete.onSelect(null, poolCyclePriority);
                    }
                } else {
                    poolViewModels.get(i).setSelected(false);
                }
            }
            poolsAdapter.update(poolViewModels);
        });
    }

    public void setFees(ArrayList<Long> fees) {
        this.fees = fees;
    }

    private void loadPools() {

        poolViewModels.clear();
        this.tx0Amount = NewPoolActivity.getCycleTotalAmount(coins);
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
        if (whirlpoolWallet == null) {
            return;
        }
        if (poolsAdapter == null) {
            poolsAdapter = new PoolsAdapter(getContext(), poolViewModels);
        }
        Disposable disposable = Single.fromCallable(whirlpoolWallet.getPoolSupplier()::getPools)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((whirlpoolPools) -> {

                    for (com.samourai.whirlpool.client.whirlpool.beans.Pool whirlpoolPool : whirlpoolPools) {
                        PoolViewModel poolViewModel = new PoolViewModel(whirlpoolPool);
                        Long fee = fees.get(1); ;
                        switch (this.poolCyclePriority) {
                            case HIGH:
                                fee = fees.get(2);
                                break;
                            case NORMAL:
                                fee = fees.get(1);
                                break;
                            case LOW: {
                                fee = fees.get(0);
                                break;
                            }
                        }

                        poolViewModel.setMinerFee(fee,this.coins);
                        poolViewModels.add(poolViewModel);
                        if (poolViewModel.getDenomination() + poolViewModel.getFeeValue() + poolViewModel.getMinerFee() > this.tx0Amount) {
                            poolViewModel.setDisabled(true);
                        } else {
                            poolViewModel.setDisabled(false);
                        }
                    }
                    poolsAdapter.notifyDataSetChanged();
                }, Throwable::printStackTrace);
        compositeDisposable.add(disposable);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choose_pools, container, false);
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void setPoolCyclePriority(PoolCyclePriority poolCyclePriority) {
        this.poolCyclePriority = poolCyclePriority;

        switch (poolCyclePriority) {
            case LOW: {
                setButtonColor(feeNormalBtn,R.color.window);
                setButtonColor(feeHighBtn,R.color.window);
                setButtonColor(feeLowBtn,R.color.blue_ui_2);
                if (fees.size() >= 1)
                    poolFee.setText(String.valueOf(fees.get(0)).concat(" ").concat(getString(R.string.sat_b)));
                if (fees.size() >= 1)
                    loadPools();
                break;
            }
            case NORMAL: {
                setButtonColor(feeLowBtn,R.color.window);
                setButtonColor(feeHighBtn,R.color.window);
                setButtonColor(feeNormalBtn,R.color.blue_ui_2);
                if (fees.size() >= 2)
                    poolFee.setText(String.valueOf(fees.get(1)).concat(" ").concat(getString(R.string.sat_b)));
                if (fees.size() >= 2)
                    loadPools();
                break;
            }

            case HIGH: {
                setButtonColor(feeLowBtn,R.color.window);
                setButtonColor(feeNormalBtn,R.color.window);
                setButtonColor(feeHighBtn,R.color.blue_ui_2);
                if (fees.size() >= 2)
                    poolFee.setText(String.valueOf(fees.get(2)).concat(" ").concat(getString(R.string.sat_b)));
                if (fees.size() >= 2)
                    loadPools();
                break;
            }


        }
    }


    void setButtonColor(MaterialButton button, int color){
        button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(),color));
    }
    @Override
    public void onDetach() {
        this.onPoolSelectionComplete = null;
        super.onDetach();
    }

    public void setTX0(List<UTXOCoin> coins) {
        this.coins = coins;
        loadPools();
    }

    public interface OnPoolSelectionComplete {
        void onSelect(PoolViewModel poolViewModel, PoolCyclePriority priority);
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
