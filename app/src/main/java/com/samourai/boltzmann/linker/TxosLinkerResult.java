package com.samourai.boltzmann.linker;

import com.samourai.boltzmann.aggregator.TxosAggregatorResult;
import com.samourai.boltzmann.beans.Txos;
import java.util.Set;

public class TxosLinkerResult extends TxosAggregatorResult {

  private Set<int[]> dtrmLnks;
  private Txos txos;

  public TxosLinkerResult(int nbCmbn, int[][] matLnk, Set<int[]> dtrmLnks, Txos txos) {
    super(nbCmbn, matLnk);
    this.dtrmLnks = dtrmLnks;
    this.txos = txos;
  }

  public Set<int[]> getDtrmLnks() {
    return dtrmLnks;
  }

  public Txos getTxos() {
    return txos;
  }
}
