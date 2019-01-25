package com.samourai.boltzmann.client;

import com.google.common.math.DoubleMath;
import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.processor.TxProcessorResult;

import java.util.Arrays;
import java.util.Set;

public class Client {

    /**
     * Displays the results for a given transaction
     * @param result {@link TxProcessorResult}
     */
    public void displayResults(TxProcessorResult result) {
        System.out.println("Inputs = " + result.getTxos().getInputs());
        System.out.println("Outputs = " + result.getTxos().getOutputs());
        System.out.println("Fees = " + result.getFees() + " satoshis");

        if (result.getIntraFees() != null && result.getIntraFees().getFeesMaker() > 0 && result.getIntraFees().getFeesTaker() > 0) {
            System.out.println("Hypothesis: Max intrafees received by a participant = " + result.getIntraFees().getFeesMaker() + " satoshis");
            System.out.println("Hypothesis: Max intrafees paid by a participant = " + result.getIntraFees().getFeesTaker() + " satoshis");
        }

        System.out.println("Nb combinations = "+result.getNbCmbn());
        if (result.getEntropy() != null) {
            System.out.println("Tx entropy = "+result.getEntropy()+" bits");
        }

        if (result.getEfficiency() != null) {
            System.out.println("Wallet efficiency = "+(result.getEfficiency()*100)+"% ("+ DoubleMath.log2(result.getEfficiency())+" bits)");
        }

        if (result.getMatLnkCombinations() == null) {
            if (result.getNbCmbn() == 0) {
                System.out.println("Skipped processing of this transaction (too many inputs and/or outputs)");
            }
        }
        else {
            if (result.getMatLnkProbabilities() != null) {
                System.out.println("Linkability Matrix (probabilities) :");
                System.out.println(Arrays.deepToString(result.getMatLnkProbabilities()));
            }
            if (result.getMatLnkCombinations() != null){
                System.out.println("Linkability Matrix (#combinations with link) :");
                System.out.println(Arrays.deepToString(result.getMatLnkCombinations()));
            }
        }

        System.out.println("Deterministic links :");
        String[][] readableDtrmLnks = replaceDtrmLinks(result.getDtrmLnks(), result.getTxos());
        for (String[] dtrmLink : readableDtrmLnks) {
            String out = dtrmLink[0];
            String in = dtrmLink[1];
            System.out.println(in+" & "+out+" are deterministically linked");
        }
    }

    public String[][] replaceDtrmLinks(Set<int[]> dtrmLinks, Txos txos) {
        String[][] result = new String[dtrmLinks.size()][2];

        String[] outs = txos.getOutputs().keySet().toArray(new String[]{});
        String[] ins = txos.getInputs().keySet().toArray(new String[]{});

        int i=0;
        for (int[] dtrmLink : dtrmLinks) {
            String out = outs[dtrmLink[0]];
            String in = ins[dtrmLink[1]];
            result[i] = new String[]{out,in};
            i++;
        }

        return result;
    }

}
