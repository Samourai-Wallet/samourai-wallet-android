package com.samourai.wallet;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IntroActivity extends Activity {

    private TextView mVersionText;
    private Button mCreateWalletButton;

    private String mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey(MainActivity.URI_KEY))	{
            mUri = extras.getString(MainActivity.URI_KEY);
        }

        findViews();
        setupViewContent();
        addListeners();
    }

    private void findViews() {
        mVersionText = (TextView) findViewById(R.id.versionNumber);
        mCreateWalletButton = (Button) findViewById(R.id.btn_create_wallet);
    }

    private void setupViewContent() {
        String versionPrefix = "v ";
        String version = "";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = versionPrefix + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mVersionText.setText(version);
    }

    private void addListeners() {
        mCreateWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMainActivity2();
            }
        });
    }

    private void startMainActivity2() {
        Intent intent = new Intent(this, MainActivity2.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if(mUri!= null)    {
            intent.putExtra(MainActivity.URI_KEY, mUri);
        }
        startActivity(intent);
    }

}
