package com.samourai.wallet.send.cahoots;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.Subject;

public class SorobanCahootsActivity extends SamouraiActivity {

    private ViewPager viewPager;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private ArrayList<Fragment> steps = new ArrayList<>();
    private CahootReviewFragment cahootReviewFragment;
    private TextView stepCounts, textViewCahootsUser;
    private long amount = 0L;
    private String address = "";
    private static final String TAG = "SorobanCahootsActivity";
    private CahootsMessage cahootsMessage;
    private CahootsType type;
    private CahootsTypeUser typeUser;
    private AndroidSorobanClientService sorobanClientService;
    private String sendToPCode;
    private String receiveFromPCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soroban_cahoots);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        cahootReviewFragment = CahootReviewFragment.newInstance();
        createSteps();
        stepsViewGroup = findViewById(R.id.step_view);
        stepCounts = findViewById(R.id.step_numbers);
        textViewCahootsUser = findViewById(R.id.cahoots_user);
        viewPager = findViewById(R.id.view_flipper);
        viewPager.enableSwipe(false);
        stepsViewGroup.setTotalSteps(CahootsMessage.NB_STEPS);
        steps.add(cahootReviewFragment);
        viewPager.setAdapter(new StepAdapter(getSupportFragmentManager()));

        if (getIntent().hasExtra("amount")) {
            amount = getIntent().getLongExtra("amount", 0);
        }
        if (getIntent().hasExtra("type")) {
            int cahootsType = getIntent().getIntExtra("type", -1);
            type = CahootsType.find(cahootsType).get();
        }
        if (getIntent().hasExtra("address")) {
            address = getIntent().getStringExtra("address");
        }
        if (getIntent().hasExtra("sendToPCode")) {
            // send to soroban
            sendToPCode = getIntent().getStringExtra("sendToPCode");
            typeUser = CahootsTypeUser.SENDER;
        } else if (getIntent().hasExtra("receiveFromPCode")) {
            // receive from soroban
            receiveFromPCode = getIntent().getStringExtra("receiveFromPCode");
            typeUser = CahootsTypeUser.RECEIVER;
        }

        // start sending or receiving Cahoots
        sorobanClientService = AndroidSorobanClientService.getInstance(getApplicationContext());
        try {
            if (typeUser == null) {
                throw new Exception("Unknown Cahoots typeUser");
            }

            switch (typeUser) {
                case SENDER:
                    startSender();
                    break;

                case RECEIVER:
                    startReceiver();
                    break;

                default:
                    throw new Exception("Unknown Cahoots typeUser");
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        textViewCahootsUser.setText((type != null ? type.name() : "") + "as " + typeUser.name());
    }

    private void startSender() {
        // start cahoots
        new Thread(() -> {
            try {
                // send cahoots
                PaymentCode paymentCodeCounterparty = new PaymentCode(sendToPCode);
                Subject<SorobanMessage> sorobanListener;
                switch (type) {
                    case STONEWALLX2:
                        if (amount <= 0L || address == null) {
                            throw new Exception("Invalid arguments");
                        }
                        sorobanListener = sorobanClientService.newStonewallx2(amount, address, paymentCodeCounterparty);
                        break;
                    case STOWAWAY:
                        sorobanListener = sorobanClientService.newStowaway(amount, paymentCodeCounterparty);
                        break;
                    default:
                        throw new Exception("Unknown #Cahoots");
                }

                // listen for cahoots progress
                setStep(0, "Sending online " + type + "...");
                subscribeOnMessage(sorobanListener);
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Cahoots completed", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }).start();
    }

    private void startReceiver() throws Exception {
        // receive from soroban
        PaymentCode paymentCode = new PaymentCode(receiveFromPCode);
        AndroidSorobanClientService sorobanClientService = AndroidSorobanClientService.getInstance(getApplicationContext());
        sorobanClientService.startListening(paymentCode);
        setStep(0, "Listening for online Cahoots...");

        Toast.makeText(this, "Listening for online Cahoots", Toast.LENGTH_SHORT).show();
        subscribeOnMessage(sorobanClientService.getOnMessage());
        sorobanClientService.getOnResult().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sorobanMessage -> {
                    Toast.makeText(this, "Online Cahoots SUCCESS", Toast.LENGTH_SHORT).show();
                }, sorobanError -> {
                    Toast.makeText(this, "Online Cahoots ERROR: " + sorobanError.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void subscribeOnMessage(Subject<SorobanMessage> onMessage) {
        onMessage.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sorobanMessage -> {
                    CahootsMessage cahootsMessage = (CahootsMessage)sorobanMessage;
                    setCahootsMessage(cahootsMessage);
                    Toast.makeText(this, "Online Cahoots progress: " + (cahootsMessage.getStep()+1) + "/" + cahootsMessage.getNbSteps() + " " + cahootsMessage.getType() + " " + cahootsMessage.getTypeUser().name(), Toast.LENGTH_SHORT).show();
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
        for (int i = 0; i < CahootsMessage.LAST_STEP; i++) {
            boolean connecting = (i == 0);
            SorobanCahootsStepFragment stepView = SorobanCahootsStepFragment.newInstance(i, typeUser, connecting);
            steps.add(stepView);
        }
    }

    private void setCahootsMessage(CahootsMessage msg) throws Exception {
        if (type != null) {
            if (!cahootsMessage.getType().equals(type)) {
                // possible attack?
                throw new Exception("Unexpected Cahoots type");
            }
        } else {
            type = msg.getType();
        }

        cahootsMessage = msg;
        int step;
        String stepTitle = "";
        if (cahootsMessage != null) {
            // message received from QR or Soroban
            Log.d("ManualCahootsActivity", "# Cahoots => " + cahootsMessage.toString());
            step = cahootsMessage.getStep();
            stepTitle = (cahootsMessage.getTypeUser().equals(typeUser) ? " => Sending" : " <= Receiving");

            if (cahootsMessage.isLastMessage()) {
                // review last step
                cahootReviewFragment.setCahoots(cahootsMessage.getCahoots());
            } else {
                // show cahoots progress
                ((SorobanCahootsStepFragment) steps.get(step)).setCahootsMessage(cahootsMessage);
            }
        } else {
            // initializing Soroban
            step = 0;
            if (receiveFromPCode != null) {
                stepTitle = "Receiving online Cahoots...";
            } else {
                stepTitle = "Sending online Cahoots...";
            }
        }

        // show current step
        setStep(step, stepTitle);
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

    @Override
    public void finish() {
        try {
            if (sorobanClientService.isStartedListening()) {
                sorobanClientService.stopListening();
            }
        } catch(Exception e){}
        super.finish();
    }
}
