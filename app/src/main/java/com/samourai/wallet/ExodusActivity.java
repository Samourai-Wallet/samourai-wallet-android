package com.samourai.wallet;

import android.os.Bundle;

import com.samourai.wallet.language.BaseActivity;
import com.samourai.wallet.util.TimeOutUtil;

public class ExodusActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimeOutUtil.getInstance().reset();
        
        finish();
    }

}
