package com.samourai.wallet;

import android.app.Activity;
import android.os.Bundle;

public class AboutActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle("Samourai, v" + getResources().getString(R.string.version_name));
    }

}
