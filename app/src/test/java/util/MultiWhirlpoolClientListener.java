package util;

import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.whirlpool.listener.LoggingWhirlpoolClientListener;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;

public class MultiWhirlpoolClientListener extends LoggingWhirlpoolClientListener {
    private MixStatus mixStatus;
    private MixStep mixStep;



    @Override
    public void success(int nbMixs, MixSuccess mixSuccess) {
        super.success(nbMixs, mixSuccess);
        this.mixStatus = MixStatus.SUCCESS;
    }

    @Override
    public void progress(int currentMix, int nbMixs, MixStep step, String stepInfo, int stepNumber, int nbSteps) {
        super.progress(currentMix, nbMixs, step, stepInfo, stepNumber, nbSteps);
        this.mixStep = step;
    }

    @Override
    public void fail(int currentMix, int nbMixs) {
        super.fail(currentMix, nbMixs);
        this.mixStatus = MixStatus.FAIL;
    }

    @Override
    public void mixSuccess(int currentMix, int nbMixs, MixSuccess mixSuccess) {
        super.mixSuccess(currentMix, nbMixs, mixSuccess);
        this.mixStatus = MixStatus.SUCCESS;
    }

    public MixStatus getMixStatus() {
        return mixStatus;
    }

    public MixStep getMixStep() {
        return mixStep;
    }
}