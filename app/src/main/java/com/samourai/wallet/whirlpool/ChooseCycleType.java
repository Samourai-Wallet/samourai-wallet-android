package com.samourai.wallet.whirlpool;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.samourai.wallet.R;

public class ChooseCycleType extends AppCompatActivity {

    private ViewGroup superChargeButton, standardButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_cycle_type);
        Toolbar toolbar = findViewById(R.id.toolbar_new_whirlpool);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        standardButton = findViewById(R.id.button_type_standard);
        superChargeButton= findViewById(R.id.button_type_supercharge);


        standardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChooseCycleType.this,NewWhirlpoolCycle.class);
                startActivity(intent);
            }
        });

        superChargeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChooseCycleType.this,NewWhirlpoolCycle.class);
                startActivity(intent);
            }
        });
    }
}
