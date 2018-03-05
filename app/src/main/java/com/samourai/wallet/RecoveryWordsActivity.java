package com.samourai.wallet;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

public class RecoveryWordsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery_words);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
    }
}
