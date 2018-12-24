package com.samourai.wallet.whirlpool;

import android.support.transition.TransitionManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.View;
import android.view.ViewGroup;

import com.samourai.wallet.R;
import com.samourai.wallet.language.BaseAppCompatActivity;

import java.util.Objects;

public class WhirlPoolActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whirl_pool);
        Toolbar toolbar = findViewById(R.id.toolbar_whirlpool);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }
}
