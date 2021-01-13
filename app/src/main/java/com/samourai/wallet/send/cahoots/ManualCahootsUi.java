package com.samourai.wallet.send.cahoots;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.ManualCahootsMessage;
import com.samourai.soroban.cahoots.ManualCahootsService;
import com.samourai.soroban.cahoots.TxBroadcastInteraction;
import com.samourai.wallet.cahoots.AndroidSorobanCahootsService;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.widgets.HorizontalStepsViewIndicator;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java8.util.function.Function;

public class ManualCahootsUi {
    private Activity activity;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private TextView stepCounts;
    private ViewPager viewPager;

    protected CahootReviewFragment cahootReviewFragment;
    private ArrayList<Fragment> steps = new ArrayList<>();

    // intent
    private int account;
    protected CahootsTypeUser typeUser;
    protected CahootsType cahootsType;

    private ManualCahootsMessage cahootsMessage;

    protected AndroidSorobanCahootsService sorobanCahootsService;

    static Intent createIntent(Context ctx, Class activityClass, int account, CahootsType type, CahootsTypeUser typeUser) {
        Intent intent = new Intent(ctx, activityClass);
        intent.putExtra("_account", account);
        intent.putExtra("typeUser", typeUser.getValue());
        intent.putExtra("cahootsType", type.getValue());
        return intent;
    }

    ManualCahootsUi(HorizontalStepsViewIndicator stepsViewGroup, TextView stepCounts, ViewPager viewPager,
                    Intent intent, FragmentManager fragmentManager, Function<Integer, Fragment> fragmentProvider,
                    Activity activity) throws Exception {
        this.activity = activity;
        this.stepsViewGroup = stepsViewGroup;
        this.stepCounts = stepCounts;
        this.viewPager = viewPager;

        viewPager.enableSwipe(false);
        cahootReviewFragment = CahootReviewFragment.newInstance();

        // sender+receiver
        if (intent.hasExtra("_account")) {
            account = intent.getIntExtra("_account", 0);
        }
        if (intent.hasExtra("cahootsType")) {
            int cahootsType = intent.getIntExtra("cahootsType", -1);
            this.cahootsType = CahootsType.find(cahootsType).get();
        }
        if (intent.hasExtra("typeUser")) {
            int typeUserInt = intent.getIntExtra("typeUser", -1);
            typeUser = CahootsTypeUser.find(typeUserInt).get();
        }

        // validate
        if (typeUser == null) {
            throw new Exception("Invalid typeUser");
        }
        if (cahootsType == null) {
            throw new Exception("Invalid cahootsType");
        }

        createSteps(fragmentManager, fragmentProvider);

        // setup cahoots
        sorobanCahootsService = AndroidSorobanCahootsService.getInstance(activity.getApplicationContext());
    }

    private void createSteps(FragmentManager fragmentManager, Function<Integer, Fragment> fragmentProvider) {
        for (int i = 0; i < (ManualCahootsMessage.NB_STEPS-1); i++) {
            Fragment stepView = fragmentProvider.apply(i);
            steps.add(stepView);
        }
        if (CahootsTypeUser.SENDER.equals(typeUser)) {
            steps.add(cahootReviewFragment);
        } else {
            Fragment stepView = fragmentProvider.apply(ManualCahootsMessage.NB_STEPS-1);
            steps.add(stepView);
        }
        stepsViewGroup.setTotalSteps(steps.size());
        viewPager.setAdapter(new StepAdapter(fragmentManager));

        setStep(0);
    }

    void setCahootsMessage(ManualCahootsMessage msg) throws Exception {
        Log.d("ManualCahootsUi", "# Cahoots => " + msg.toString());

        // check cahootsType
        if (cahootsType != null) {
            if(!msg.getType().equals(cahootsType)) {
                // possible attack?
                throw new Exception("Unexpected Cahoots cahootsType");
            }
        } else {
            cahootsType = msg.getType();
        }

        cahootsMessage = msg;

        // show cahoots progress
        int step = cahootsMessage.getStep();
        setStep(step);

        // show step screen
        Fragment stepFragment = steps.get(step);
        if (stepFragment instanceof AbstractCahootsStepFragment) {
            ((AbstractCahootsStepFragment) steps.get(step)).setCahootsMessage(cahootsMessage);
        }

        if (cahootsMessage.isDone()) {
            notifyWalletAndFinish();
        } else {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Cahoots progress: " + (cahootsMessage.getStep() + 1) + "/" + cahootsMessage.getNbSteps(), Toast.LENGTH_SHORT).show());
        }
    }

    void setInteraction(TxBroadcastInteraction interaction) throws Exception {
        // review last step
        cahootReviewFragment.setCahoots(interaction.getSignedCahoots());
        cahootReviewFragment.setOnBroadcast(() -> {
            // manual cahoots => finish on broadcast success
            notifyWalletAndFinish();
            return null;
        });
        setStep(interaction.getTypeInteraction().getStep());
    }

    private void setStep(final int step) {
        stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 1));
        viewPager.post(() -> viewPager.setCurrentItem(step, true));
        stepCounts.setText("Step " + (step + 1) + "/5");
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

    private void notifyWalletAndFinish() {
        activity.runOnUiThread(() -> Toast.makeText(activity, "Cahoots success", Toast.LENGTH_LONG).show());

        // refresh txs
        Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
        intent.putExtra("notifTx", false);
        intent.putExtra("fetch", true);
        LocalBroadcastManager.getInstance(activity.getApplicationContext()).sendBroadcast(intent);

        // finish
        Intent i = new Intent(activity, BalanceActivity.class);
        activity.finish();
        activity.startActivity(i);
    }

    public String getTitle(CahootsMode cahootsMode) {
        return (CahootsTypeUser.SENDER.equals(typeUser) ? "Sending" : "Receiving") + " " + cahootsMode.getLabel().toLowerCase() + " " + cahootsType.getLabel();
    }

    public CahootsContext computeCahootsContextInitiator(long sendAmount, String sendAddress) throws Exception {
        switch (cahootsType) {
            case STONEWALLX2:
                return CahootsContext.newInitiatorStonewallx2(sendAmount, sendAddress);
            case STOWAWAY:
                return CahootsContext.newInitiatorStowaway(sendAmount);
            default:
                throw new Exception("Unknown #Cahoots");
        }
    }

    public int getAccount() {
        return account;
    }

    public CahootsTypeUser getTypeUser() {
        return typeUser;
    }

    public CahootsType getCahootsType() {
        return cahootsType;
    }

    public ManualCahootsMessage getCahootsMessage() {
        return cahootsMessage;
    }

    public ManualCahootsService getManualCahootsService() {
        return sorobanCahootsService.getManualCahootsService();
    }
}
