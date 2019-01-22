package com.samourai.boltzmann.linker;

import com.samourai.boltzmann.beans.Txos;

public class UnpackLinkMatrixResult {

  private Txos txos;
  private int[][] matLnk;

  public UnpackLinkMatrixResult(Txos txos, int[][] matLnk) {
    this.txos = txos;
    this.matLnk = matLnk;
  }

  public Txos getTxos() {
    return txos;
  }

  public int[][] getMatLnk() {
    return matLnk;
  }
}
