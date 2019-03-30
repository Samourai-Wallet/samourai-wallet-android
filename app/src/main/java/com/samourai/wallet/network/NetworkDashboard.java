package com.samourai.wallet.network;

import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.wallet.R;

import java.util.Objects;


public class NetworkDashboard extends AppCompatActivity {

    enum CONNECTION_STATUS {ENABLED, DISABLED}

    Button torButton, dataButton, dojoBtn;
    TextView dataConnectionStatus, torConnectionStatus, dojoConnectionStatus;
    ImageView dataConnectionIcon, torConnectionIcon, dojoConnectionIcon;
    LinearLayout offlineMessage;
    int activeColor, disabledColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_dashboard);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        activeColor = ContextCompat.getColor(this, R.color.green_ui_2);
        disabledColor = ContextCompat.getColor(this, R.color.disabledRed);

        offlineMessage = findViewById(R.id.offline_message);

        dataButton = findViewById(R.id.networking_data_btn);
        torButton = findViewById(R.id.networking_tor_btn);
        dojoBtn = findViewById(R.id.networking_dojo_btn);

        dataConnectionStatus = findViewById(R.id.network_data_status);
        torConnectionStatus = findViewById(R.id.network_tor_status);
        dojoConnectionStatus = findViewById(R.id.network_dojo_status);

        dataConnectionIcon = findViewById(R.id.network_data_status_icon);
        torConnectionIcon = findViewById(R.id.network_tor_status_icon);
        dojoConnectionIcon = findViewById(R.id.network_dojo_status_icon);

        dataButton.setOnClickListener(view -> {
            if (dataConnectionStatus.getText().equals("Disabled")) {
                setDataConnectionState(CONNECTION_STATUS.ENABLED);
            } else {
                setDataConnectionState(CONNECTION_STATUS.DISABLED);
            }
        });

    }

    private void setDataConnectionState(CONNECTION_STATUS enabled) {
        if (enabled == CONNECTION_STATUS.ENABLED) {
            showOfflineMessage(false);
            dataButton.setText("Disable");
            dataConnectionIcon.setColorFilter(activeColor);
            dataConnectionStatus.setText("Enabled");
        } else {
            dataButton.setText("Enable");
            showOfflineMessage(true);
            dataConnectionIcon.setColorFilter(disabledColor);
            dataConnectionStatus.setText("Disabled");
        }
    }

    private void showOfflineMessage(boolean show){
        TransitionManager.beginDelayedTransition((ViewGroup) offlineMessage.getRootView());
        offlineMessage.setVisibility(show ? View.VISIBLE : View.GONE);
    }


}
