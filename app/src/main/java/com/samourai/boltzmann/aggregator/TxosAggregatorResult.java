package com.samourai.boltzmann.aggregator;

import com.google.common.math.DoubleMath;

import java.util.Arrays;

public class TxosAggregatorResult {

    private int nbCmbn;
    private int[][] matLnkCombinations;

    /**
     *
     * @param nbCmbn
     * @param matLnk Matrix of txos linkability: Columns = input txos, Rows = output txos, Cells = number of combinations for which an input and an output are linked
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
        double[][] matLnkProbabilities = null;
        if (nbCmbn > 0) {
            matLnkProbabilities = Arrays.stream(matLnkCombinations).map(line -> Arrays.stream(line).mapToDouble(val -> (new Double(val)/nbCmbn)).toArray()).toArray(double[][]::new);
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
