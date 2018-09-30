package com.masanari.wallet;

import android.app.Activity;
import android.os.Bundle;

import com.masanari.wallet.util.TimeOutUtil;

public class ExodusActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimeOutUtil.getInstance().reset();
        
        finish();
    }

}
