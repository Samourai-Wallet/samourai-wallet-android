package com.samourai.boltzmann.linker;

/** Actions to be applied by {@link TxosLinker} */
public enum TxosLinkerOptionEnum {
  /** compute the linkability matrix */
  LINKABILITY,

  /** precheck existence of deterministic links between inputs and outputs */
  PRECHECK,

  /** Merges inputs "controlled" by a same address. Speeds up computations. */
  MERGE_INPUTS,

  /**
   * Merges outputs "controlled" by a same address. Speeds up computations but this option is not
   * recommended.
   */
  MERGE_OUTPUTS,

  /**
   * consider that all fees have been paid by a unique sender and manage fees as an additionnal
   * output
   */
  MERGE_FEES
}
