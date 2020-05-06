package com.samourai.wallet.util;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

public class LinearLayoutManagerWrapper extends LinearLayoutManager {

    public LinearLayoutManagerWrapper(Context context) {
        super(context);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        // fix for inconsistency issues
        return false;
    }
}