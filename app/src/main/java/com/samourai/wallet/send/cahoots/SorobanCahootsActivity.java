package com.samourai.wallet.send.cahoots;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.AndroidSorobanClientService;
import com.samourai.wallet.cahoots.CahootsMessage;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.widgets.HorizontalStepsViewIndicator;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SorobanCahootsActivity extends SamouraiActivity {
    private static final int TIMEOUT_MS = 60000;

    private ViewPager viewPager;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private ArrayList<Fragment> steps = new ArrayList<>();
    private CahootReviewFragment cahootReviewFragment;
    private TextView stepCounts, textViewCahootsUser;

    private int account;
    private long sendAmount;
    private String sendAddress;
    private CahootsType type;
    private CahootsTypeUser typeUser;
    private PaymentCode paymentCode;

    private static final String TAG = "SorobanCahootsActivity";
    private CahootsMessage cahootsMessage;
    private AndroidSorobanClientService sorobanClientService;
    private Disposable sorobanDisposable;

    public static Intent createIntentSender(Context ctx, int account, String pcode, CahootsType type, long amount, String address) {
        Intent intent = new Intent(ctx, SorobanCahootsActivity.class);
        intent.putExtra("_account", account);
        intent.putExtra("pcode", pcode);
        intent.putExtra("type", type.getValue());
        intent.putExtra("typeUser", CahootsTypeUser.SENDER.getValue());
        intent.putExtra("sendAmount", amount);
        intent.putExtra("sendAddress", address);
        return intent;
    }

    public static Intent createIntentReceiver(Context ctx, int account, String pcode, CahootsType type) {
        Intent intent = new Intent(ctx, SorobanCahootsActivity.class);
        intent.putExtra("_account", account);
        intent.putExtra("pcode", pcode);
        intent.putExtra("type", type.getValue());
        intent.putExtra("typeUser", type.getTypeUserCounterparty().getValue());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soroban_cahoots);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        stepsViewGroup = findViewById(R.id.step_view);
        stepCounts = findViewById(R.id.step_numbers);
        textViewCahootsUser = findViewById(R.id.cahoots_user);
        viewPager = findViewById(R.id.view_flipper);

        viewPager.enableSwipe(false);
        cahootReviewFragment = CahootReviewFragment.newInstance();

        try {
            if (getIntent().hasExtra("sendAmount")) {
                sendAmount = getIntent().getLongExtra("sendAmount", 0);
            }
            if (getIntent().hasExtra("sendAddress")) {
                sendAddress = getIntent().getStringExtra("sendAddress");
            }
            if (getIntent().hasExtra("pcode")) {
                String pcode = getIntent().getStringExtra("pcode");
                paymentCode = new PaymentCode(pcode);
            }
            if (getIntent().hasExtra("typeUser")) {
                int typeUserInt = getIntent().getIntExtra("typeUser", -1);
                typeUser = CahootsTypeUser.find(typeUserInt).get();
            }
            if (getIntent().hasExtra("type")) {
                int cahootsType = getIntent().getIntExtra("type", -1);
                type = CahootsType.find(cahootsType).get();
            }
            if (typeUser == null || type == null) {
                throw new Exception("Invalid arguments");
            }

            createSteps();
            viewPager.setAdapter(new StepAdapter(getSupportFragmentManager()));

            sorobanClientService = AndroidSorobanClientService.getInstance(getApplicationContext());
            switch(typeUser) {
                case SENDER:
                    startSender();
                    break;
                case RECEIVER: case COUNTERPARTY:
                    startReceiver();
                    break;
                default:
                    throw new Exception("Unknown typeUser");
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        textViewCahootsUser.setText(type.name() + " as " + typeUser.name());
    }

    private void startSender() {
        // start cahoots
        try {
            // send cahoots
            Observable<SorobanMessage> sorobanListener;
            switch (type) {
                case STONEWALLX2:
                    sorobanListener = sorobanClientService.initiator(account).newStonewallx2(account, sendAmount, sendAddress, paymentCode, TIMEOUT_MS);
                    break;
                case STOWAWAY:
                    sorobanListener = sorobanClientService.initiator(account).newStowaway(account, sendAmount, paymentCode, TIMEOUT_MS);
                    break;
                default:
                    throw new Exception("Unknown #Cahoots");
            }

            // listen for cahoots progress
            setStep(0, "Sending online " + type + "...");
            subscribeOnMessage(sorobanListener);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startReceiver() throws Exception {
        // receive from soroban
        Observable<SorobanMessage> sorobanListener = sorobanClientService.contributor(account).contributor(account, paymentCode, TIMEOUT_MS);
        subscribeOnMessage(sorobanListener);
        Toast.makeText(this, "Listening for online Cahoots", Toast.LENGTH_SHORT).show();
    }

    private void subscribeOnMessage(Observable<SorobanMessage> onMessage) {
        sorobanDisposable = onMessage.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sorobanMessage -> {
                    CahootsMessage cahootsMessage = (CahootsMessage)sorobanMessage;
                    if (cahootsMessage != null) {
                        setCahootsMessage(cahootsMessage);
                        if (cahootsMessage.isLastMessage()) {
                            Toast.makeText(getApplicationContext(), "Cahoots completed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Cahoots progress: " + (cahootsMessage.getStep() + 1) + "/" + cahootsMessage.getNbSteps(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                sorobanError -> {
                    Toast.makeText(getApplicationContext(), "Cahoots error: " + sorobanError.getMessage(), Toast.LENGTH_SHORT).show();
                    sorobanError.printStackTrace();
                    finish();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).setIsInForeground(true);

        AppUtil.getInstance(this).checkTimeOut();

    }

    private void createSteps() {
        for (int i = 0; i < (CahootsMessage.NB_STEPS-1); i++) {
            SorobanCahootsStepFragment stepView = SorobanCahootsStepFragment.newInstance(i);
            steps.add(stepView);
        }
        steps.add(cahootReviewFragment);
        stepsViewGroup.setTotalSteps(steps.size());
    }

    private void setCahootsMessage(CahootsMessage msg) throws Exception {
        Log.d("ManualCahootsActivity", "# Cahoots => " + msg.toString());
        if (!msg.getType().equals(type)) {
            // possible attack?
            throw new Exception("Unexpected Cahoots type");
        }
        cahootsMessage = msg;

        // show current step
        int step = cahootsMessage.getStep();
        String stepTitle = (cahootsMessage.getTypeUser().equals(typeUser) ? "Sending payload..." : "Receiving payload...");
        setStep(step, stepTitle);

        if (cahootsMessage.isLastMessage()) {
            // review last step
            cahootReviewFragment.setCahoots(cahootsMessage.getCahoots());
        } else {
            // show cahoots progress
            ((SorobanCahootsStepFragment) steps.get(step)).setCahootsMessage(cahootsMessage);
        }
    }

    private void setStep(final int step, String stepTitle) {
        stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 1));
        viewPager.post(() -> viewPager.setCurrentItem(step, true));
        stepCounts.setText((step + 1) + "/5 - " + stepTitle);
    }

    private class StepAdapter extends FragmentPagerAdapter {
        StepAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return steps.get(position);
        }

        @Override
        public int getCount() {
            return steps.size();
        }
    }

    private void clearDisposable() {
        if (sorobanDisposable != null && !sorobanDisposable.isDisposed()) {
            sorobanDisposable.dispose();
            sorobanDisposable = null;
        }
    }

    @Override
    public void finish() {
        clearDisposable();
        super.finish();
    }

    @Override
    public void onBackPressed() {// cancel cahoots request
        clearDisposable();
        super.onBackPressed();
    }
}
