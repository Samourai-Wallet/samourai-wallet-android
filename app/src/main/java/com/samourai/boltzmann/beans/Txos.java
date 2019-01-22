package com.samourai.boltzmann.beans;

import java.util.LinkedHashMap;
import java.util.Map;

public class Txos {

  // List of input txos expressed as tuples (id, amount)
  private Map<String, Long> inputs;

  // List of output txos expressed as tuples (id, amount)
  private Map<String, Long> outputs;

  public Txos() {
    this(new LinkedHashMap<String, Long>(), new LinkedHashMap<String, Long>());
  }

  public Txos(Map<String, Long> inputs, Map<String, Long> outputs) {
    this.inputs = inputs;
    this.outputs = outputs;
  }

  public Map<String, Long> getInputs() {
    return inputs;
  }

  public Map<String, Long> getOutputs() {
    return outputs;
  }
}
