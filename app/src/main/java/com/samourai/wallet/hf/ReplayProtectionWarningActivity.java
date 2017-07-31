package com.samourai.wallet.hf;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.PrefsUtil;

public class ReplayProtectionWarningActivity extends Activity {

    private Button btEnable = null;
    private Button btDismiss = null;
    private LinearLayout layoutAlert = null;

    private static final int COLOR_RED = 0xffb71c1c;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.replay_protection_warning_layout);

        layoutAlert = (LinearLayout)findViewById(R.id.alert);
        layoutAlert.setBackgroundColor(COLOR_RED);

        ((TextView)layoutAlert.findViewById(R.id.left)).setText(getText(R.string.replay_protection));
        ((TextView)layoutAlert.findViewById(R.id.right)).setText(getText(R.string.replay_unprotected));

        btEnable = (Button)findViewById(R.id.proceed);
        btEnable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ReplayProtectionWarningActivity.this, ReplayProtectionActivity.class);
                startActivity(intent);
            }
        });

        btDismiss = (Button)findViewById(R.id.dismiss);
        btDismiss.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PrefsUtil.getInstance(ReplayProtectionWarningActivity.this).setValue(PrefsUtil.BCC_DISMISSED, true);
                finish();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(ReplayProtectionWarningActivity.this).checkTimeOut();

    }
}
