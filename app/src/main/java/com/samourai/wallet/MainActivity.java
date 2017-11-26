package com.samourai.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.bitcoinj.params.TestNet3Params;

public class MainActivity extends Activity {
    private static final String TAG = LogUtil.getTag();

    public static final String URI_KEY = "uri";

    private static boolean genesis = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LogUtil.DEBUG) Log.d(TAG, "onCreate");

        if(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.TESTNET, false)) {
            if (LogUtil.DEBUG) Log.d(TAG, "setting testnet");
            SamouraiWallet.getInstance().setCurrentNetworkParams(TestNet3Params.get());
        }

        String action = getIntent().getAction();
        String scheme = getIntent().getScheme();
        String strUri = null;
        if(action != null && Intent.ACTION_VIEW.equals(action) && "bitcoin".equals(scheme)) {
            if (getIntent().getData() != null) {
                strUri = getIntent().getData().toString();
            }
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
        intent = new Intent(MainActivity.this, IntroActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if(strUri != null)    {
            intent.putExtra(URI_KEY, strUri);
        }
        genesis = true;
        startActivity(intent);
    }

}
