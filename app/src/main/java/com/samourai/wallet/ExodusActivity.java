package com.samourai.wallet;

import android.app.Activity;
import android.os.Bundle;

import com.samourai.wallet.util.TimeOutUtil;

public class ExodusActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimeOutUtil.getInstance().reset();
        
        finish();
    }

}
