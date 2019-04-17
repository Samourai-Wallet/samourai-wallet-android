package com.samourai.wallet.paynym;

import android.os.Bundle;
import android.app.Activity;
import android.support.constraint.Group;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.squareup.picasso.Picasso;


public class PayNymDetailsActivity extends AppCompatActivity {

    private String pcode = null;
    private ImageView userAvatar;
    private TextView paynymCode, followMessage;
    private RecyclerView historyRecyclerView;
    private Button followBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paynym_details);
        setSupportActionBar(findViewById(R.id.toolbar_paynym));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userAvatar = findViewById(R.id.paybyn_user_avatar);
        paynymCode = findViewById(R.id.paynym_payment_code);
        followMessage = findViewById(R.id.follow_message);
        historyRecyclerView = findViewById(R.id.recycler_view_paynym_history);
        followBtn = findViewById(R.id.paynym_follow_btn);

        if (getIntent().hasExtra("pcode")) {
            pcode = getIntent().getStringExtra("pcode");
        } else {
            finish();
        }

        paynymCode.setText(BIP47Meta.getInstance().getAbbreviatedPcode(pcode));
        getSupportActionBar().setTitle(BIP47Meta.getInstance().getLabel(pcode));
        Picasso.with(getApplicationContext()).load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar);
        followMessage.setText(getResources().getString(R.string.follow).concat(" ").concat(BIP47Meta.getInstance().getLabel(pcode)).concat(" ").concat(getResources().getText(R.string.paynym_follow_message_2).toString()));
        if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT) {
            showFollow();
        }else {
            hideFollow();
        }


    }

    private void hideFollow() {
        followMessage.setVisibility(View.GONE);
        followBtn.setVisibility(View.GONE);
        historyRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showFollow() {
        followMessage.setVisibility(View.VISIBLE);
        followBtn.setVisibility(View.VISIBLE);
        historyRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.paynym_details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
