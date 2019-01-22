package com.samourai.boltzmann.aggregator;

import com.google.common.math.DoubleMath;
import java8.util.function.IntToDoubleFunction;
import java8.util.stream.IntStreams;

public class TxosAggregatorResult {

  private int nbCmbn;
  private int[][] matLnkCombinations;

  /**
   * @param nbCmbn
   * @param matLnk Matrix of txos linkability: Columns = input txos, Rows = output txos, Cells =
   *     number of combinations for which an input and an output are linked
   */
  public TxosAggregatorResult(int nbCmbn, int[][] matLnk) {
    this.nbCmbn = nbCmbn;
    this.matLnkCombinations = matLnk;
  }

  public int getNbCmbn() {
    return nbCmbn;
  }

  public int[][] getMatLnkCombinations() {
    return matLnkCombinations;
  }

  public double[][] computeMatLnkProbabilities() {
    double[][] matLnkProbabilities = new double[matLnkCombinations.length][];
    if (nbCmbn > 0) {
      for (int i = 0; i < matLnkCombinations.length; i++) {
        int[] line = matLnkCombinations[i];
        matLnkProbabilities[i] =
            IntStreams.of(line)
                .mapToDouble(
                    new IntToDoubleFunction() {
                      @Override
                      public double applyAsDouble(int val) {
                        return new Double(val) / nbCmbn;
                      }
                    })
                .toArray();
      }
    }
    return matLnkProbabilities;
  }

  public double computeEntropy() {
    double entropy = 0;
    if (nbCmbn > 0) {
      entropy = DoubleMath.log2(nbCmbn);
    }
    return entropy;
  }
}
