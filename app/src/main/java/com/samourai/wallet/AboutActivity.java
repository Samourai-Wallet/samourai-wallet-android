package com.samourai.wallet;

import android.os.Bundle;

import com.samourai.wallet.language.BaseActivity;
import com.samourai.wallet.util.AppUtil;

public class AboutActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle("Samourai, v" + getResources().getString(R.string.version_name));
    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(AboutActivity.this).checkTimeOut();

    }
}
