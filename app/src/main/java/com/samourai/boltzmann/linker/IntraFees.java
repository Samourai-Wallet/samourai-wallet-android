package com.samourai.boltzmann.linker;

public class IntraFees {

  private long feesMaker;
  private long feesTaker;
  private boolean hasFees;

  public IntraFees(long feesMaker, long feesTaker) {
    this.feesMaker = feesMaker;
    this.feesTaker = feesTaker;
    hasFees = (feesMaker > 0 || feesTaker > 0);
  }

  public long getFeesMaker() {
    return feesMaker;
  }

  public long getFeesTaker() {
    return feesTaker;
  }

  public boolean hasFees() {
    return hasFees;
  }
}
