package com.samourai.boltzmann.aggregator;

import java.util.Map;

public class TxosAggregatesData {

  private Map<String, Long> txos;
  private int[][]
      allAggIndexes; // each entry value contains array of txos indexes for corresponding
  // allAggVal[entry.key]
  private long[] allAggVal;

  public TxosAggregatesData(Map<String, Long> txos, int[][] allAggIndexes, long[] allAggVal) {
    this.txos = txos;
    this.allAggIndexes = allAggIndexes;
    this.allAggVal = allAggVal;
  }

  public Map<String, Long> getTxos() {
    return txos;
  }

  public int[][] getAllAggIndexes() {
    return allAggIndexes;
  }

  public long[] getAllAggVal() {
    return allAggVal;
  }
}
