package com.samourai.boltzmann.processor;

import com.samourai.boltzmann.linker.TxosLinkerOptionEnum;

public class TxProcessorSettings {

    /**
     * max intrafees paid by the taker of a coinjoined transaction. Expressed as a percentage of the coinjoined amount.
     */
    private float maxCjIntrafeesRatio = 0;

    /**
     * max duration allocated to processing of a single tx (in seconds)
     */
    private int maxDuration = 600;

    /**
     * max number of txos. Txs with more than max_txos inputs or outputs are not processed.
     */
    private int maxTxos = 12;

    /**
     * options to be applied during processing
     */
    private TxosLinkerOptionEnum[] options = new TxosLinkerOptionEnum[]{TxosLinkerOptionEnum.PRECHECK, TxosLinkerOptionEnum.LINKABILITY, TxosLinkerOptionEnum.MERGE_INPUTS};

    public TxProcessorSettings() {
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public int getMaxTxos() {
        return maxTxos;
    }

    public void setMaxTxos(int maxTxos) {
        this.maxTxos = maxTxos;
    }

    public float getMaxCjIntrafeesRatio() {
        return maxCjIntrafeesRatio;
    }

    public void setMaxCjIntrafeesRatio(float maxCjIntrafeesRatio) {
        this.maxCjIntrafeesRatio = maxCjIntrafeesRatio;
    }

    public TxosLinkerOptionEnum[] getOptions() {
        return options;
    }

    public void setOptions(TxosLinkerOptionEnum[] options) {
        this.options = options;
    }
}
