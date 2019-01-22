package com.samourai.boltzmann.linker;

import java.util.List;
import java.util.Map.Entry;

class Pack {
  private String lbl;
  private PackType packType;
  private List<Entry<String, Long>> ins;
  private List<String> outs;

  Pack(String lbl, PackType packType, List<Entry<String, Long>> ins, List<String> outs) {
    this.lbl = lbl;
    this.packType = packType;
    this.ins = ins;
    this.outs = outs;
  }

  public String getLbl() {
    return lbl;
  }

  public PackType getPackType() {
    return packType;
  }

  public List<Entry<String, Long>> getIns() {
    return ins;
  }

  public List<String> getOuts() {
    return outs;
  }
}
