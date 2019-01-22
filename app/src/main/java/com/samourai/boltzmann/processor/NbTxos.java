package com.samourai.boltzmann.processor;

public class NbTxos {

  private int nbIns;
  private int nbOuts;

  public NbTxos(int nbIns, int nbOuts) {
    this.nbIns = nbIns;
    this.nbOuts = nbOuts;
  }

  public int getNbIns() {
    return nbIns;
  }

  public int getNbOuts() {
    return nbOuts;
  }
}
