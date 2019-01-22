package com.samourai.boltzmann.processor;

import com.google.common.collect.Sets;
import com.samourai.boltzmann.beans.Tx;
import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.linker.IntraFees;
import com.samourai.boltzmann.linker.TxosLinker;
import com.samourai.boltzmann.linker.TxosLinkerOptionEnum;
import com.samourai.boltzmann.linker.TxosLinkerResult;
import com.samourai.boltzmann.utils.ListsUtils;
import java.util.*;
import java.util.Map.Entry;
import java8.util.stream.LongStreams;

public class TxProcessor {

  public TxProcessor() {}

  /**
   * Processes a transaction
   *
   * @param tx Transaction to be processed
   * @param settings settings for processing the transaction
   * @return TxProcessorResult
   */
  public TxProcessorResult processTx(Tx tx, TxProcessorSettings settings) {
    Set<TxosLinkerOptionEnum> options =
        new HashSet<TxosLinkerOptionEnum>(Arrays.asList(settings.getOptions()));

    // Builds lists of filtered input/output txos (with generated ids)
    FilteredTxos filteredIns = filterTxos(tx.getTxos().getInputs(), TxProcessorConst.MARKER_INPUT);
    FilteredTxos filteredOuts =
        filterTxos(tx.getTxos().getOutputs(), TxProcessorConst.MARKER_OUTPUT);

    // Computes total input & output amounts + fees
    long sumInputs =
        LongStreams.of(ListsUtils.toPrimitiveArray(filteredIns.getTxos().values())).sum();
    long sumOutputs =
        LongStreams.of(ListsUtils.toPrimitiveArray(filteredOuts.getTxos().values())).sum();
    long fees = sumInputs - sumOutputs;

    // Sets default intrafees paid by participants (fee_received_by_maker, fees_paid_by_taker)
    IntraFees intraFees = new IntraFees(0, 0);

    TxosLinkerResult result;

    // Processes the transaction
    if (filteredIns.getTxos().size() <= 1 || filteredOuts.getTxos().size() == 1) {
      // Txs having no input (coinbase) or only 1 input/output (null entropy)
      // When entropy = 0, all inputs and outputs are linked and matrix is filled with 1.
      // No need to build this matrix. Every caller should be able to manage that.
      result =
          new TxosLinkerResult(
              1, null, null, new Txos(filteredIns.getTxos(), filteredOuts.getTxos()));
    } else {
      // Initializes the TxosLinker for this tx
      Txos filteredTxos = new Txos(filteredIns.getTxos(), filteredOuts.getTxos());
      TxosLinker linker = new TxosLinker(fees, settings.getMaxDuration(), settings.getMaxTxos());

      // Computes a list of sets of inputs controlled by a same address
      List<Set<String>> linkedIns = new ArrayList<Set<String>>();
      List<Set<String>> linkedOuts = new ArrayList<Set<String>>();

      if (options.contains(TxosLinkerOptionEnum.MERGE_INPUTS)) {
        // Computes a list of sets of inputs controlled by a same address
        linkedIns = getLinkedTxos(filteredIns);
      }

      if (options.contains(TxosLinkerOptionEnum.MERGE_OUTPUTS)) {
        // Computes a list of sets of outputs controlled by a same address (not recommended)
        linkedOuts = getLinkedTxos(filteredOuts);
      }

      // Computes intrafees to be used during processing
      if (settings.getMaxCjIntrafeesRatio() > 0) {
        // Computes a theoretic max number of participants
        List<Set<String>> lsFilteredIns = new LinkedList<Set<String>>();
        for (String txoId : filteredIns.getTxos().keySet()) {
          Set<String> set = new LinkedHashSet<String>();
          set.add(txoId);
          lsFilteredIns.add(set);
        }

        lsFilteredIns.addAll(linkedIns);
        Collection<Set<String>> insToMerge = new ArrayList<Set<String>>();
        insToMerge.addAll(lsFilteredIns);
        insToMerge.addAll(linkedIns);
        int maxNbPtcpts = ListsUtils.mergeSets(insToMerge).size();

        // Checks if tx has a coinjoin pattern + gets estimated number of participants and
        // coinjoined amount
        CoinjoinPattern cjPattern = checkCoinjoinPattern(filteredOuts.getTxos(), maxNbPtcpts);

        // If coinjoin pattern detected, computes theoretic max intrafees
        if (cjPattern != null) {
          intraFees =
              computeCoinjoinIntrafees(
                  cjPattern.getNbPtcpts(),
                  cjPattern.getCjAmount(),
                  settings.getMaxCjIntrafeesRatio());
        }
      }

      // Computes entropy of the tx and txos linkability matrix
      Collection<Set<String>> linkedTxos = new ArrayList<Set<String>>();
      linkedTxos.addAll(linkedIns);
      linkedTxos.addAll(linkedOuts);
      result = linker.process(filteredTxos, linkedTxos, options, intraFees);
    }

    // Computes tx efficiency (expressed as the ratio: nb_cmbn/nb_cmbn_perfect_cj)
    Double efficiency =
        computeWalletEfficiency(
            filteredIns.getTxos().size(), filteredOuts.getTxos().size(), result.getNbCmbn());

    // Post processes results (replaces txo ids by bitcoin addresses)

    Map<String, Long> txoIns =
        postProcessTxos(result.getTxos().getInputs(), filteredIns.getMapIdAddr());
    Map<String, Long> txoOuts =
        postProcessTxos(result.getTxos().getOutputs(), filteredOuts.getMapIdAddr());
    return new TxProcessorResult(
        result.getNbCmbn(),
        result.getMatLnkCombinations(),
        result.computeMatLnkProbabilities(),
        result.computeEntropy(),
        result.getDtrmLnks(),
        new Txos(txoIns, txoOuts),
        fees,
        intraFees,
        efficiency);
  }

  /**
   * Computes a list of sets of txos controlled by a same address Returns a list of sets of txo_ids
   * [ {txo_id1, txo_id2, ...}, {txo_id3, txo_id4, ...} ]
   *
   * @param filteredTxos FilteredTxos
   */
  private List<Set<String>> getLinkedTxos(FilteredTxos filteredTxos) { // TODO test
    List<Set<String>[]> linkedTxos = new ArrayList<Set<String>[]>();

    for (String id : filteredTxos.getTxos().keySet()) {
      Set<String> setIns = new LinkedHashSet<String>();
      setIns.add(id);

      Set<String> setAddr = new LinkedHashSet<String>();
      setAddr.add(filteredTxos.getMapIdAddr().get(id));

      // Checks if this set intersects with some set previously found
      for (Set<String>[] entry : linkedTxos) {
        Set<String> k = entry[0];
        Set<String> v = entry[1];
        if (!Sets.intersection(k, setAddr).isEmpty()) {
          // If an intersection is found, merges the 2 sets and removes previous set
          // from linked_txos
          setIns.addAll(v);
          setAddr.addAll(k);
          linkedTxos.remove(entry);
        }
      }
      linkedTxos.add(new Set[] {setAddr, setIns});
    }

    List<Set<String>> result = new ArrayList<Set<String>>();
    for (Set<String>[] sets : linkedTxos) {
      if (sets[1].size() > 1) {
        result.add(sets[1]);
      }
    }
    return result;
  }

  /**
   * Filters a list of txos by removing txos with null value (OP_RETURN, ...). Defines an id for
   * each txo
   *
   * @param txos list of Txo objects
   * @param prefix a prefix to be used for ids generated
   * @return FilteredTxos
   */
  private FilteredTxos filterTxos(Map<String, Long> txos, String prefix) {
    Map<String, Long> filteredTxos = new LinkedHashMap<String, Long>();
    Map<String, String> mapIdAddr = new LinkedHashMap<String, String>();

    for (Entry<String, Long> entry : txos.entrySet()) {
      if (entry.getValue() > 0) {
        String txoId = prefix + mapIdAddr.size();
        filteredTxos.put(txoId, entry.getValue());
        mapIdAddr.put(txoId, entry.getKey());
      }
    }

    return new FilteredTxos(filteredTxos, mapIdAddr);
  }

  /**
   * Post processes a list of txos Basically replaces txo_id by associated bitcoin address Returns a
   * list of txos (tuples (address, amount))
   *
   * @param txos list of txos (tuples (txo_id, amount))
   * @param mapIdAddr mapping txo_ids to addresses
   */
  public Map<String, Long> postProcessTxos(Map<String, Long> txos, Map<String, String> mapIdAddr) {
    Map<String, Long> results = new LinkedHashMap<String, Long>();
    for (Entry<String, Long> entry : txos.entrySet()) {
      if (entry.getKey().startsWith(TxProcessorConst.MARKER_INPUT)
          || entry.getKey().startsWith(TxProcessorConst.MARKER_OUTPUT)) {
        results.put(mapIdAddr.get(entry.getKey()), entry.getValue());
      } else {
        results.put(entry.getKey(), entry.getValue()); // PACKS, FEES...
      }
    }
    return results;
  }

  /**
   * Checks if a transaction looks like a coinjoin Returns a tuple (is_coinjoin, nb_participants,
   * coinjoined_amount)
   *
   * @param txoOuts list of outputs valves (tuples (tiid, amount))
   * @param maxNbEntities estimated max number of entities participating in the coinjoin (info
   *     coming from a side channel source or from an analysis of tx structure)
   * @return CoinjoinPattern if coinjoin pattern is found, otherwise null
   */
  protected CoinjoinPattern checkCoinjoinPattern(Map<String, Long> txoOuts, int maxNbEntities) {
    // Checks that we have more than 1 input entity
    if (maxNbEntities < 2) {
      return null;
    }

    // Computes a dictionary of #outputs per amount (d[amount] = nb_outputs)
    Map<Long, Integer> nbOutsByAmount = new LinkedHashMap<Long, Integer>();
    for (Entry<String, Long> entry : txoOuts.entrySet()) {
      long amont = entry.getValue();
      int nb = nbOutsByAmount.containsKey(amont) ? nbOutsByAmount.get(amont) : 0;
      nb++;
      nbOutsByAmount.put(amont, nb);
    }

    // Computes #outputs
    int nbTxoOuts = txoOuts.size();

    // Tries to detect a coinjoin pattern in outputs:
    //   n outputs with same value, with n > 1
    //   nb_outputs <= 2*nb_ptcpts (with nb_ptcpts = min(n, max_nb_entities) )
    // If multiple candidate values
    // selects option with max number of participants (and max amount as 2nd criteria)
    boolean isCj = false;
    int resNbPtcpts = 0;
    long resAmount = 0;
    for (Map.Entry<Long, Integer> entry : nbOutsByAmount.entrySet()) {
      long amount = entry.getKey();
      int nbOutsForAmount = entry.getValue();
      if (nbOutsForAmount > 1) {
        int maxNbPtcpts = Math.min(nbOutsForAmount, maxNbEntities);
        boolean condTxoOuts = nbTxoOuts <= 2 * maxNbPtcpts;
        boolean condMaxPtcpts = maxNbPtcpts >= resNbPtcpts;
        boolean condMaxAmount = amount > resAmount;
        if (condTxoOuts && condMaxPtcpts && condMaxAmount) {
          isCj = true;
          resNbPtcpts = maxNbPtcpts;
          resAmount = amount;
        }
      }
    }
    if (!isCj) {
      return null;
    }
    return new CoinjoinPattern(resNbPtcpts, resAmount);
  }

  /**
   * Computes theoretic intrafees involved in a coinjoin transaction (e.g. joinmarket)
   *
   * @param nbPtcpts number of participants
   * @param cjAmount common amount generated for the coinjoin transaction
   * @param prctMax max percentage paid by the taker to all makers
   * @return IntraFees
   */
  protected IntraFees computeCoinjoinIntrafees(int nbPtcpts, long cjAmount, float prctMax) {
    long feeMaker = Math.round(cjAmount * prctMax);
    long feeTaker = feeMaker * (nbPtcpts - 1);
    return new IntraFees(feeMaker, feeTaker);
  }

  /**
   * Computes the efficiency of a transaction defined by: - its number of inputs - its number of
   * outputs - its entropy (expressed as number of combinations)
   *
   * @param nbIns number of inputs
   * @param nbOuts number of outputs
   * @param nbCmbn number of combinations found for the transaction
   * @return an efficiency score computed as the ratio: nb_cmbn / nb_cmbn_closest_perfect_coinjoin
   */
  private Double computeWalletEfficiency(int nbIns, int nbOuts, int nbCmbn) {
    if (nbCmbn == 1) {
      return 0.0;
    }

    NbTxos tgtNbTxos = getClosestPerfectCoinjoin(nbIns, nbOuts);
    Double nbCmbnPrfctCj = computeCmbnsPerfectCj(tgtNbTxos.getNbIns(), tgtNbTxos.getNbOuts());
    if (nbCmbnPrfctCj == null) {
      return null;
    }
    return nbCmbn / nbCmbnPrfctCj;
  }

  /**
   * Computes the structure of the closest perfect coinjoin for a transaction defined by its #inputs
   * and #outputs
   *
   * <p>A perfect coinjoin is defined as a transaction for which: - all inputs have the same amount
   * - all outputs have the same amount - 0 fee are paid (equiv. to same fee paid by each input) -
   * nb_i % nb_o == 0, if nb_i >= nb_o or nb_o % nb_i == 0, if nb_o >= nb_i
   *
   * <p>Returns a tuple (nb_i, nb_o) for the closest perfect coinjoin
   *
   * @param nbIns number of inputs of the transaction
   * @param nbOuts number of outputs of the transaction
   * @return
   */
  private NbTxos getClosestPerfectCoinjoin(int nbIns, int nbOuts) {
    if (nbIns > nbOuts) {
      // Reverses inputs and outputs
      int nbInsInitial = nbIns;
      nbIns = nbOuts;
      nbOuts = nbInsInitial;
    }

    if (nbOuts % nbIns == 0) {
      return new NbTxos(nbIns, nbOuts);
    }
    int tgtRatio = 1 + nbOuts / nbIns;
    return new NbTxos(nbIns, nbIns * tgtRatio);
  }

  /**
   * Computes the number of combinations for a perfect coinjoin with nb_i inputs and nb_o outputs.
   *
   * <p>A perfect coinjoin is defined as a transaction for which: - all inputs have the same amount
   * - all outputs have the same amount - 0 fee are paid (equiv. to same fee paid by each input) -
   * nb_i % nb_o == 0, if nb_i >= nb_o or nb_o % nb_i == 0, if nb_o >= nb_i
   *
   * <p>Notes: Since all inputs have the same amount we can use exponential Bell polynomials to
   * retrieve the number and structure of partitions for the set of inputs.
   *
   * <p>Since all outputs have the same amount we can use a direct computation of combinations of k
   * outputs among n.
   *
   * @param nbIns number of inputs
   * @param nbOuts number of outputs
   * @return the number of combinations
   */
  private Double computeCmbnsPerfectCj(int nbIns, int nbOuts) {
    if (nbIns > nbOuts) {
      // Reverses inputs and outputs
      int nbInsInitial = nbIns;
      nbIns = nbOuts;
      nbOuts = nbInsInitial;
    }

    if (nbOuts % nbIns != 0) {
      return null;
    }

    // Checks if we can use precomputed values
    if (nbIns <= 1 || nbOuts <= 1) {
      return 1.0;
    } else if (nbIns <= 20 && nbOuts <= 60) {
      return TxProcessorConst.getNbCmbnPrfctCj(nbIns, nbOuts);
    }

    return null; // not supported
  }
}
