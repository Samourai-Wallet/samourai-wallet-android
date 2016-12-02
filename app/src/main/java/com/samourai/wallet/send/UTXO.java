package com.samourai.wallet.send;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//import org.apache.commons.lang3.tuple.Pair;

public class UTXO {

    private List<MyTransactionOutPoint> outpoints = null;

    public UTXO() {
        this.outpoints = new ArrayList<MyTransactionOutPoint>();
    }

    public UTXO(List<MyTransactionOutPoint> outpoints) {
        this.outpoints = outpoints;
    }

    public List<MyTransactionOutPoint> getOutpoints() {
        return outpoints;
    }

    public void setOutpoints(List<MyTransactionOutPoint> outpoints) {
        this.outpoints = outpoints;
    }

    public long getValue() {

        long value = 0L;

        for (MyTransactionOutPoint out : outpoints) {
            value += out.getValue().longValue();
        }

        return value;
    }

    public static class UTXOComparator implements Comparator<UTXO> {

        public int compare(UTXO o1, UTXO o2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (o1.getValue() > o2.getValue()) {
                return BEFORE;
            } else if (o1.getValue() < o2.getValue()) {
                return AFTER;
            } else {
                return EQUAL;
            }

        }

    }

}
