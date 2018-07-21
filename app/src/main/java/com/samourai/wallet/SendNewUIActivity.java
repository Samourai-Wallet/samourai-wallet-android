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
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.samourai.wallet.widgets.SendTransactionDetailsView;

import java.util.Objects;


public class SendNewUIActivity extends AppCompatActivity {

    private SendTransactionDetailsView sendTransactionDetailsView;
    private ViewSwitcher amountViewSwitcher;
    private EditText toEditText, btcEditText, fiatEditText;
    private Button btnReview, btnSend;
    private Switch ricochetHopsSwitch;
    private Boolean isOnReviewPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_new_ui);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_send));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle("");

        //CustomView for showing and hiding body of th UI
        sendTransactionDetailsView = findViewById(R.id.sendTransactionDetailsView);

        //ViewSwitcher Element for toolbar section of the UI.
        //we can switch between Form and review screen with this element
        amountViewSwitcher = findViewById(R.id.toolbar_view_switcher);

        //Input elements from toolbar section of the UI
        toEditText = findViewById(R.id.edt_send_to);
        btcEditText = findViewById(R.id.amountBTC);
        fiatEditText = findViewById(R.id.amountFiat);

        //view elements from review segment and transaction segment can be access through respective
        //methods which returns root viewGroup
        btnReview = sendTransactionDetailsView.getTransactionView().findViewById(R.id.review_button);
        ricochetHopsSwitch = sendTransactionDetailsView.getTransactionView().findViewById(R.id.ricochet_hops_switch);

        btnSend = sendTransactionDetailsView.getTransactionReview().findViewById(R.id.send_btn);


        btnReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                review();
            }
        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SendNewUIActivity.this, "Send Clicked", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void review() {
        amountViewSwitcher.showNext();
        sendTransactionDetailsView.showReview(ricochetHopsSwitch.isChecked());
        isOnReviewPage = true;
    }

    private void backToTransactionView() {
        amountViewSwitcher.showPrevious();
        sendTransactionDetailsView.showTransaction();
        isOnReviewPage = false;
    }

    @Override
    public void onBackPressed() {
        if (isOnReviewPage) {
            backToTransactionView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


}
