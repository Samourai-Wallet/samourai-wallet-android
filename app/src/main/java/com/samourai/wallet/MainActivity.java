package com.samourai.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.util.TimeOutUtil;

public class MainActivity extends Activity {

    private static boolean genesis = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);

        String action = getIntent().getAction();
        String scheme = getIntent().getScheme();
        String strUri = null;
        if(action != null && Intent.ACTION_VIEW.equals(action) && scheme.equals("bitcoin")) {
            strUri = getIntent().getData().toString();
        }

        doMain(strUri);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(genesis)    {
            genesis = false;
            finish();
        }

    }

    private void doMain(String strUri) {
        Intent intent;
        intent = new Intent(MainActivity.this, MainActivity2.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if(strUri != null)    {
            intent.putExtra("uri", strUri);
        }
        genesis = true;
        startActivity(intent);
    }

}
