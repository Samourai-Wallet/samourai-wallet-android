package com.samourai.boltzmann.processor;

public class CoinjoinPattern {

  private int nbPtcpts;
  private long cjAmount;

  public CoinjoinPattern(int nbPtcpts, long cjAmount) {
    this.nbPtcpts = nbPtcpts;
    this.cjAmount = cjAmount;
  }

  public int getNbPtcpts() {
    return nbPtcpts;
  }

  public long getCjAmount() {
    return cjAmount;
  }
}
