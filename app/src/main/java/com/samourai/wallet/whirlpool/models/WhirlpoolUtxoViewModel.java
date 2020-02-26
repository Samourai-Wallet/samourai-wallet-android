package com.samourai.wallet.whirlpool.models;

import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

public class WhirlpoolUtxoViewModel extends WhirlpoolUtxo {


    private boolean isSection = false;
    private String section = "";
    private int id = 0;

    public WhirlpoolUtxoViewModel(UnspentResponse.UnspentOutput utxo, WhirlpoolAccount account, WhirlpoolUtxoConfig utxoConfig, WhirlpoolUtxoStatus status) {
        super(utxo, account, utxoConfig, status);
    }

    public static WhirlpoolUtxoViewModel copy(WhirlpoolUtxo utxo) {
        return new WhirlpoolUtxoViewModel(utxo.getUtxo(), utxo.getAccount(), utxo.getUtxoConfig(), utxo.getUtxoState().getStatus());
    }

    public static WhirlpoolUtxoViewModel section(String sectionText) {
        WhirlpoolUtxoViewModel section = new WhirlpoolUtxoViewModel(null, null, null, null);
        section.setSection(true);
        section.setSection(sectionText);
        return section;
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
