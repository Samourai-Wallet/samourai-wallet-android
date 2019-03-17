package com.samourai.wallet.home.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.samourai.wallet.R;

public class ItemDividerDecorator extends RecyclerView.ItemDecoration {

    private Drawable mDivider;

    public ItemDividerDecorator(Context context) {
        mDivider = context.getResources().getDrawable(R.drawable.divider);
    }
    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            mDivider.setBounds(left-120, top, right, bottom);
            mDivider.draw(c);
        }
    }
}
