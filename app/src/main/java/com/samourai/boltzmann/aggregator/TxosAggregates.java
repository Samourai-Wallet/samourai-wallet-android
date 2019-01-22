package com.samourai.boltzmann.aggregator;

public class TxosAggregates {

  private TxosAggregatesData inAgg;
  private TxosAggregatesData outAgg;

  public TxosAggregates(TxosAggregatesData inAgg, TxosAggregatesData outAgg) {
    this.inAgg = inAgg;
    this.outAgg = outAgg;
  }

  public TxosAggregatesData getInAgg() {
    return inAgg;
  }

  public TxosAggregatesData getOutAgg() {
    return outAgg;
  }
}
