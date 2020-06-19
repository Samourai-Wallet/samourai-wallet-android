package com.samourai.wallet.whirlpool.models;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

public class WhirlpoolUtxoViewModel {

    private WhirlpoolUtxo whirlpoolUtxo;
    private boolean isSection = false;
    private String section = "";
    private int id = 0;

    public WhirlpoolUtxoViewModel(WhirlpoolUtxo whirlpoolUtxo) {
        this.whirlpoolUtxo = whirlpoolUtxo;
    }

    public static WhirlpoolUtxoViewModel copy(WhirlpoolUtxo whirlpoolUtxo) {
        return new WhirlpoolUtxoViewModel(whirlpoolUtxo);
    }

    public static WhirlpoolUtxoViewModel section(String sectionText) {
        WhirlpoolUtxoViewModel section = new WhirlpoolUtxoViewModel(null);
        section.setSection(true);
        section.setSection(sectionText);
        return section;
    }

    public WhirlpoolUtxo getWhirlpoolUtxo() {
        return whirlpoolUtxo;
    }

    public boolean isSection() {
        return isSection;
    }

    public void setSection(boolean section) {
        isSection = section;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
