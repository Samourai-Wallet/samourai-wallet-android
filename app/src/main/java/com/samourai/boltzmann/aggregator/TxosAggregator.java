package com.samourai.boltzmann.aggregator;

import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.linker.IntraFees;
import com.samourai.boltzmann.utils.ListsUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java8.util.function.IntToLongFunction;
import java8.util.stream.IntStreams;
import java8.util.stream.LongStreams;

public class TxosAggregator {

  public TxosAggregator() {}

  /** Computes several data structures which will be used later */
  public TxosAggregates prepareData(Txos txos) {
    TxosAggregatesData allInAgg = prepareTxos(txos.getInputs());
    TxosAggregatesData allOutAgg = prepareTxos(txos.getOutputs());
    return new TxosAggregates(allInAgg, allOutAgg);
  }

  /**
   * Computes several data structures related to a list of txos
   *
   * @param initialTxos list of txos (list of tuples (id, value))
   * @return list of txos sorted by decreasing values array of aggregates (combinations of txos) in
   *     binary format array of values associated to the aggregates
   */
  public TxosAggregatesData prepareTxos(Map<String, Long> initialTxos) {
    Map<String, Long> txos = new LinkedHashMap<String, Long>();

    // Orders txos by decreasing value
    Comparator<Entry<String, Long>> comparingByValueReverse =
        Collections.reverseOrder(ListsUtils.<String, Long>comparingByValue());
    for (Entry<String, Long> entry :
        ListsUtils.sortMap(initialTxos, comparingByValueReverse).entrySet()) {
      // Removes txos with null value
      if (entry.getValue() > 0) {
        txos.put(entry.getKey(), entry.getValue());
      }
    }

    // Creates a 1D array of values
    final Long[] allVal = txos.values().toArray(new Long[] {});
    List<Integer> allIndexes = new ArrayList<Integer>();
    for (int i=0; i<txos.size(); i++) {
      allIndexes.add(i);
    }

    //// int[][] allAgg = ListsUtils.powerSet(allVal);
    int[][] allAggIndexes = ListsUtils.powerSet(allIndexes.toArray(new Integer[]{}));
    // int[] allAggVal = Arrays.stream(allAgg).mapToInt(array ->
    // Arrays.stream(array).sum()).toArray();
    List<Long> allAggVal = new LinkedList<Long>();
    for (int[] array : allAggIndexes) {
      allAggVal.add(
          IntStreams.of(array)
              .mapToLong(
                  new IntToLongFunction() {
                    @Override
                    public long applyAsLong(int indice) {
                      return allVal[indice];
                    }
                  })
              .sum());
    }
    return new TxosAggregatesData(txos, allAggIndexes, ListsUtils.toPrimitiveArray(allAggVal));
  }

  /**
   * Matches input/output aggregates by values and returns a bunch of data structs
   *
   * @param allAgg
   * @param fees
   * @param intraFees
   * @return
   */
  public TxosAggregatesMatches matchAggByVal(
      TxosAggregates allAgg, long fees, IntraFees intraFees) {
    long[] allInAggVal = allAgg.getInAgg().getAllAggVal();
    long[] allOutAggVal = allAgg.getOutAgg().getAllAggVal();

    // Gets unique values of input / output aggregates sorted ASC
    long[] allUniqueInAggVal = LongStreams.of(allInAggVal).distinct().sorted().toArray();
    long[] allUniqueOutAggVal = LongStreams.of(allOutAggVal).distinct().sorted().toArray();

    Set<Integer> allMatchInAgg = new HashSet<Integer>();
    Map<Integer, Long> matchInAggToVal = new HashMap<Integer, Long>();
    Map<Long, List<Integer>> valToMatchOutAgg = new LinkedHashMap<Long, List<Integer>>();

    // Computes total fees paid/receiver by taker/maker
    long feesTaker = 0, feesMaker = 0;
    boolean hasIntraFees = intraFees != null && intraFees.hasFees();
    if (hasIntraFees) {
      feesTaker = fees + intraFees.getFeesTaker();
      feesMaker = -intraFees.getFeesMaker(); // doesn 't take into account tx fees paid by makers
    }

    // Finds input and output aggregates with matching values
    for (int i = 0; i < allUniqueInAggVal.length; i++) {
      long inAggVal = allUniqueInAggVal[i];
      for (int j = 0; j < allUniqueOutAggVal.length; j++) {
        long outAggVal = allUniqueOutAggVal[j];

        long diff = inAggVal - outAggVal;

        if (!hasIntraFees && diff < 0) {
          break;
        } else {
          // Computes conditions required for a matching
          boolean condNoIntrafees = (!hasIntraFees && diff <= fees);
          boolean condIntraFees =
              (hasIntraFees
                  && ((diff <= 0 && diff >= feesMaker) || (diff >= 0 && diff <= feesTaker)));

          if (condNoIntrafees || condIntraFees) {
            // Registers the matching input aggregate
            for (int inIdx = 0; inIdx < allInAggVal.length; inIdx++) {
              if (allInAggVal[inIdx] == inAggVal) {
                if (!allMatchInAgg.contains(inIdx)) {
                  allMatchInAgg.add(inIdx);
                  matchInAggToVal.put(inIdx, inAggVal);
                }
              }
            }

            // Registers the matching output aggregate
            List<Integer> keysMatchOutAgg = new ArrayList<Integer>();
            for (int indice = 0; indice < allOutAggVal.length; indice++) {
              if (allOutAggVal[indice] == outAggVal) {
                keysMatchOutAgg.add(indice);
              }
            }
            if (!valToMatchOutAgg.containsKey(inAggVal)) {
              valToMatchOutAgg.put(inAggVal, new ArrayList<Integer>());
            }
            valToMatchOutAgg.get(inAggVal).addAll(keysMatchOutAgg);
          }
        }
      }
    }
    return new TxosAggregatesMatches(allMatchInAgg, matchInAggToVal, valToMatchOutAgg);
  }

  /**
   * Computes a matrix of valid combinations (pairs) of input aggregates Returns a dictionary
   * (parent_agg => (child_agg1, child_agg2)) We have a valid combination (agg1, agg2) if: R1/
   * child_agg1 & child_agg2 = 0 (no bitwise overlap) R2/ child_agg1 > child_agg2 (matrix is
   * symmetric)
   */
  public Map<Integer, List<int[]>> computeInAggCmbn(TxosAggregatesMatches aggMatches) {

    // aggs = allMatchInAgg[1:-1]
    LinkedList<Integer> aggs = new LinkedList<Integer>(aggMatches.getAllMatchInAgg());
    aggs.pollFirst(); // remove 0

    Map<Integer, List<int[]>> mat = new LinkedHashMap<Integer, List<int[]>>();
    if (!aggs.isEmpty()) {
      int tgt = aggs.pollLast();

      for (int i = 0; i < tgt + 1; i++) {
        if (aggs.contains(i)) {
          int jMax = Math.min(i, tgt - i + 1);
          for (int j = 0; j < jMax; j++) {
            if ((i & j) == 0 && aggs.contains(j)) {
              List<int[]> aggChilds = mat.get(i + j);
              if (aggChilds == null) {
                aggChilds = new ArrayList<int[]>();
                mat.put(i + j, aggChilds);
              }
              aggChilds.add(new int[] {i, j});
            }
          }
        }
      }
    }
    return mat;
  }

  /**
   * Checks the existence of deterministic links between inputs and outputs
   *
   * @return list of deterministic links as tuples (idx_output, idx_input)
   */
  public Set<int[]> checkDtrmLinks(
      Txos txos, TxosAggregates allAgg, TxosAggregatesMatches aggMatches) {
    int nbIns = txos.getInputs().size();
    int nbOuts = txos.getOutputs().size();

    int[][] matCmbn = new int[nbOuts][nbIns];
    int[] inCmbn = new int[nbIns];
    for (Map.Entry<Integer, Long> inEntry : aggMatches.getMatchInAggToVal().entrySet()) {
      int inIdx = inEntry.getKey();
      long val = inEntry.getValue();
      for (int outIdx : aggMatches.getValToMatchOutAgg().get(val)) {
        // Computes a matrix storing numbers of raw combinations matching input/output pairs
        updateLinkCmbn(matCmbn, inIdx, outIdx, allAgg);

        // Computes sum of combinations along inputs axis to get the number of combinations
        int[] inIndexes = allAgg.getInAgg().getAllAggIndexes()[inIdx];
        for (int inIndex : inIndexes) {
          inCmbn[inIndex]++;
        }
      }
    }

    // Builds a list of sets storing inputs having a deterministic link with an output
    int nbCmbn = inCmbn[0];
    Set<int[]> dtrmCoords = findDtrmLinks(matCmbn, nbCmbn);
    return dtrmCoords;
  }

  private class ComputeLinkMatrixTask {
    private int idxIl;
    private int il;
    private int ir;
    private Map<Integer, Map<Integer, int[]>> dOut;

    public ComputeLinkMatrixTask(
        int idxIl, int il, int ir, Map<Integer, Map<Integer, int[]>> dOut) {
      this.idxIl = idxIl;
      this.il = il;
      this.ir = ir;
      this.dOut = dOut;
    }

    public int getIdxIl() {
      return idxIl;
    }

    public void setIdxIl(int idxIl) {
      this.idxIl = idxIl;
    }

    public int getIl() {
      return il;
    }

    public int getIr() {
      return ir;
    }

    public Map<Integer, Map<Integer, int[]>> getdOut() {
      return dOut;
    }
  }

  /**
   * Computes the linkability matrix Returns the number of possible combinations and the links
   * matrix Implements a depth-first traversal of the inputs combinations tree (right to left) For
   * each input combination we compute the matching output combinations. This is a basic brute-force
   * solution. Will have to find a better method later.
   *
   * @param maxDuration in seconds
   */
  public TxosAggregatorResult computeLinkMatrix(
      Txos txos,
      TxosAggregates allAgg,
      TxosAggregatesMatches aggMatches,
      Map<Integer, List<int[]>> matInAggCmbn,
      int maxDuration) {
    int nbTxCmbn = 0;
    int itGt = (int) Math.pow(2, txos.getInputs().size()) - 1; // TODO int
    int otGt = (int) Math.pow(2, txos.getOutputs().size()) - 1;
    Map<int[], Integer> dLinks = new LinkedHashMap<int[], Integer>();

    // Initializes a stack of tasks & sets the initial task
    //  0: index used to resume the processing of the task (required for depth-first algorithm)
    //  1: il = left input aggregate
    //  2: ir = right input aggregate
    //  3: d_out = outputs combination matching with current input combination
    //             dictionary of dictionary :  { or =>  { ol => (nb_parents_cmbn, nb_children_cmbn)
    // } }

    Deque<ComputeLinkMatrixTask> stack = new LinkedList<ComputeLinkMatrixTask>();

    // ini_d_out[otgt] = { 0: (1, 0) }
    Map<Integer, Map<Integer, int[]>> dOutInitial =
        new LinkedHashMap<Integer, Map<Integer, int[]>>();
    Map<Integer, int[]> dOutEntry = new LinkedHashMap<Integer, int[]>();
    dOutEntry.put(0, new int[] {1, 0});
    dOutInitial.put(otGt, dOutEntry);

    stack.add(new ComputeLinkMatrixTask(0, 0, itGt, dOutInitial));

    // Sets start date/hour
    long startTime = System.currentTimeMillis();

    // Iterates over all valid inputs combinations (top->down)
    while (!stack.isEmpty()) {
      // Checks duration
      long currTime = System.currentTimeMillis();
      long deltaTimeSeconds = (currTime - startTime) / 1000;
      if (deltaTimeSeconds >= maxDuration) {
        return new TxosAggregatorResult(0, null);
      }

      // Gets data from task
      ComputeLinkMatrixTask t = stack.getLast();
      int nIdxIl = t.getIdxIl();

      // Gets all valid decompositions of right input aggregate
      List<int[]> ircs = matInAggCmbn.get(t.getIr());
      int lenIrcs = (ircs != null ? ircs.size() : 0);

      for (int i = t.getIdxIl(); i < lenIrcs; i++) {
        nIdxIl = i;
        Map<Integer, Map<Integer, int[]>> ndOut = new LinkedHashMap<Integer, Map<Integer, int[]>>();

        // Gets left input sub-aggregate (column from ircs)
        int nIl = ircs.get(i)[1];

        // Checks if we must process this pair (columns from ircs are sorted in decreasing order)
        if (nIl > t.getIl()) {
          // Gets the right input sub-aggregate (row from ircs)
          int nIr = ircs.get(i)[0];

          // Iterates over outputs combinations previously found
          for (Map.Entry<Integer, Map<Integer, int[]>> oREntry : t.getdOut().entrySet()) {
            int oR = oREntry.getKey();
            int sol = otGt - oR;

            // Computes the number of parent combinations
            int nbPrt = 0;
            for (int[] s : oREntry.getValue().values()) {
              nbPrt += s[0];
            }

            // Iterates over output sub-aggregates matching with left input sub-aggregate
            long valIl = aggMatches.getMatchInAggToVal().get(nIl);
            for (int nOl : aggMatches.getValToMatchOutAgg().get(valIl)) {
              // Checks compatibility of output sub-aggregate with left part of output combination
              if ((sol & nOl) == 0) {
                // Computes:
                //   the sum corresponding to the left part of the output combination
                //   the complementary right output sub-aggregate
                int nSol = sol + nOl;
                int nOr = otGt - nSol;

                // Checks if the right output sub-aggregate is valid
                long valIr = aggMatches.getMatchInAggToVal().get(nIr);
                List<Integer> matchOutAgg = aggMatches.getValToMatchOutAgg().get(valIr);

                // Adds this output combination into n_d_out if all conditions met
                if ((nSol & nOr) == 0 && matchOutAgg.contains(nOr)) {
                  Map<Integer, int[]> ndOutVal = ndOut.get(nOr);
                  if (ndOutVal == null) {
                    ndOutVal = new LinkedHashMap<Integer, int[]>();
                    ndOut.put(nOr, ndOutVal);
                  }
                  ndOutVal.put(nOl, new int[] {nbPrt, 0});
                }
              }
            }
          }

          // Updates idx_il for the current task
          t.setIdxIl(i + 1);

          // Pushes a new task which will decompose the right input aggregate
          stack.add(new ComputeLinkMatrixTask(0, nIl, nIr, ndOut));

          // Executes the new task (depth-first)
          break;
        } else {
          // No more results for il, triggers a break and a pop
          nIdxIl = ircs.size();
          break;
        }
      }

      // Checks if task has completed
      if (nIdxIl > (lenIrcs - 1)) {
        // Pops the current task
        t = stack.removeLast();

        // Checks if it's the root task
        if (stack.isEmpty()) {
          // Retrieves the number of combinations from root task
          nbTxCmbn = t.getdOut().get(otGt).get(0)[1];
        } else {
          // Gets parent task
          ComputeLinkMatrixTask pt = stack.getLast();

          // Iterates over all entries from d_out
          for (Map.Entry<Integer, Map<Integer, int[]>> doutEntry : t.getdOut().entrySet()) {
            int or = doutEntry.getKey();
            Map<Integer, int[]> lOl = doutEntry.getValue();
            int[] rKey = new int[] {t.getIr(), or};

            // Iterates over all left aggregates
            for (Map.Entry<Integer, int[]> olEntry : lOl.entrySet()) {
              int ol = olEntry.getKey();
              int nbPrnt = olEntry.getValue()[0];
              int nbChld = olEntry.getValue()[1];

              int[] lKey = new int[] {t.getIl(), ol};

              // Updates the dictionary of links for the pair of aggregates
              int nbOccur = nbChld + 1;
              dLinks.put(rKey, (dLinks.containsKey(rKey) ? dLinks.get(rKey) : 0) + nbPrnt);
              dLinks.put(
                  lKey, (dLinks.containsKey(lKey) ? dLinks.get(lKey) : 0) + nbPrnt * nbOccur);

              // Updates parent d_out by back-propagating number of child combinations
              int pOr = ol + or;
              Map<Integer, int[]> plOl = pt.getdOut().get(pOr);
              for (Map.Entry<Integer, int[]> plOlEntry : plOl.entrySet()) {
                int pOl = plOlEntry.getKey();
                int pNbPrt = plOlEntry.getValue()[0];
                int pNbChld = plOlEntry.getValue()[1];
                pt.getdOut().get(pOr).put(pOl, new int[] {pNbPrt, pNbChld + nbOccur});
              }
            }
          }
        }
      }
    }

    // Fills the matrix
    int[][] links = newLinkCmbn(allAgg);
    updateLinkCmbn(links, itGt, otGt, allAgg);
    nbTxCmbn++;
    for (Map.Entry<int[], Integer> linkEntry : dLinks.entrySet()) {
      int[] link = linkEntry.getKey();
      int mult = linkEntry.getValue();
      int[][] linkCmbn = newLinkCmbn(allAgg);
      updateLinkCmbn(linkCmbn, link[0], link[1], allAgg);
      for (int i = 0; i < links.length; i++) {
        for (int j = 0; j < links[i].length; j++) {
          links[i][j] += linkCmbn[i][j] * mult;
        }
      }
    }
    return new TxosAggregatorResult(nbTxCmbn, links);
  }

  /**
   * Creates a new linkability matrix.
   *
   * @param allAgg
   */
  private int[][] newLinkCmbn(TxosAggregates allAgg) {
    int maxOutIndex = 0;
    for (int[] indexes : allAgg.getOutAgg().getAllAggIndexes()) {
      int max = IntStreams.of(indexes).max().orElse(0);
      if (max > maxOutIndex) {
        maxOutIndex = max;
      }
    }

    int maxInIndex = 0;
    for (int[] indexes : allAgg.getInAgg().getAllAggIndexes()) {
      int max = IntStreams.of(indexes).max().orElse(0);
      if (max > maxInIndex) {
        maxInIndex = max;
      }
    }
    int[][] matCmbn = new int[maxOutIndex + 1][maxInIndex + 1];
    return matCmbn;
  }

  /**
   * Updates the linkability matrix for aggregate designated by inAgg/outAgg.
   *
   * @param matCmbn linkability matrix
   * @param inAgg input aggregate
   * @param outAgg output aggregate
   * @param allAgg
   */
  private int[][] updateLinkCmbn(int[][] matCmbn, int inAgg, int outAgg, TxosAggregates allAgg) {
    int[] outIndexes = allAgg.getOutAgg().getAllAggIndexes()[outAgg];
    int[] inIndexes = allAgg.getInAgg().getAllAggIndexes()[inAgg];

    for (int inIndex : inIndexes) {
      for (int outIndex : outIndexes) {
        matCmbn[outIndex][inIndex]++;
      }
    }
    return matCmbn;
  }

  /**
   * Builds a list of sets storing inputs having a deterministic link with an output
   *
   * @param matCmbn linkability matrix
   * @param nbCmbn number of combination
   * @return
   */
  public Set<int[]> findDtrmLinks(int[][] matCmbn, int nbCmbn) {
    Set<int[]> dtrmCoords = new LinkedHashSet<int[]>();
    for (int i = 0; i < matCmbn.length; i++) {
      for (int j = 0; j < matCmbn[i].length; j++) {
        if (matCmbn[i][j] == nbCmbn) {
          dtrmCoords.add(new int[] {i, j});
        }
      }
    }
    return dtrmCoords;
  }
}
