package com.samourai.wallet;

import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.samourai.wallet.widgets.SendTransactionDetailsView;

import java.util.Objects;


public class SendNewUIActivity extends AppCompatActivity {

    private Button reviewButton;
    private ViewSwitcher viewSwitcher;
    private FrameLayout mainPager;
    private View main, review;
    private SendTransactionDetailsView detailsView;
    private ViewSwitcher amountViewSwitcher;
    private EditText toEditText,btcEditText,fiatEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_new_ui);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_send));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle("");
        detailsView = findViewById(R.id.mainPager);
        amountViewSwitcher = findViewById(R.id.toolbar_view_switcher);
        toEditText = findViewById(R.id.edt_send_to);
        btcEditText = findViewById(R.id.amountBTC);
        fiatEditText = findViewById(R.id.amountFiat);



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


}
