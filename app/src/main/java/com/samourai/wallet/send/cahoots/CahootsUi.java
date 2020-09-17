package com.samourai.wallet.send.cahoots;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import com.samourai.wallet.cahoots.AndroidSorobanClientService;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.ManualCahootsMessage;
import com.samourai.wallet.cahoots.ManualCahootsService;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.widgets.HorizontalStepsViewIndicator;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java8.util.function.Function;

public class CahootsUi {
    private HorizontalStepsViewIndicator stepsViewGroup;
    private TextView stepCounts;
    private ViewPager viewPager;

    private CahootReviewFragment cahootReviewFragment;
    private ArrayList<Fragment> steps = new ArrayList<>();

    // intent
    private int account;
    private CahootsTypeUser typeUser;
    private CahootsType cahootsType;

    private AndroidSorobanClientService sorobanClientService;

    private ManualCahootsMessage cahootsMessage;

    static Intent createIntent(Context ctx, Class activityClass, int account, CahootsType type, CahootsTypeUser typeUser) {
        Intent intent = new Intent(ctx, activityClass);
        intent.putExtra("_account", account);
        intent.putExtra("typeUser", typeUser.getValue());
        intent.putExtra("cahootsType", type.getValue());
        return intent;
    }

    CahootsUi(HorizontalStepsViewIndicator stepsViewGroup, TextView stepCounts, ViewPager viewPager,
              Intent intent, FragmentManager fragmentManager, Function<Integer, Fragment> fragmentProvider,
               Context ctx) throws Exception {
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
        sorobanClientService = AndroidSorobanClientService.getInstance(ctx);
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
        Log.d("CahootsUi", "# Cahoots => " + msg.toString());

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

        // show current step
        int step = cahootsMessage.getStep();
        setStep(step);

        if (CahootsTypeUser.SENDER.equals(typeUser) && cahootsMessage.isDone()) {
            // review last step
            cahootReviewFragment.setCahoots(cahootsMessage.getCahoots());

            // listen for broadcast
            Subject<SorobanMessage> onBroadcast = BehaviorSubject.create();
            onBroadcast.doOnEach(x -> sorobanClientService.initiator().confirmTxBroadcast(x.getValue()));
            cahootReviewFragment.setOnBroadcastListener(onBroadcast);
        } else {
            // show cahoots progress
            ((AbstractCahootsStepFragment) steps.get(step)).setCahootsMessage(cahootsMessage);
        }
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

    public String getTitle(CahootsMode cahootsMode) {
        return (CahootsTypeUser.SENDER.equals(typeUser) ? "Sending" : "Receiving") + " " + cahootsMode.getLabel().toLowerCase() + " " + cahootsType.getLabel();
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

    public AndroidSorobanClientService getSorobanClientService() {
        return sorobanClientService;
    }

    public ManualCahootsService getManualCahootsService() {
        return sorobanClientService.getManualCahootsService();
    }
}
