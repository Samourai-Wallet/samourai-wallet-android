package com.samourai.wallet.tx;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.samourai.wallet.R;

import java.util.Objects;


public class TxPrivacyDetailsActivity extends AppCompatActivity {

    private Button hideLogButton, copyBtn;
    private ViewGroup logContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx_privacy_details);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        hideLogButton = findViewById(R.id.hide_log_button);
        logContainer = findViewById(R.id.log_container);
        copyBtn = findViewById(R.id.copy_btn);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        hideLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLogView();
            }
        });

    }


    /**
     * Toggles visibility of log view
     */
    private void toggleLogView() {


        TransitionManager.beginDelayedTransition((ViewGroup) logContainer.getRootView());

        if (logContainer.getVisibility() == View.VISIBLE) {
            logContainer.setVisibility(View.GONE);
            copyBtn.setVisibility(View.GONE);
            hideLogButton.setText(R.string.show_log);
        } else {
            logContainer.setVisibility(View.VISIBLE);
            copyBtn.setVisibility(View.VISIBLE);
            hideLogButton.setText(this.getResources().getText(R.string.hide_log));
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tx_details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
