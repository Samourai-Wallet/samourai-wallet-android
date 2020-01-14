package com.samourai.wallet.whirlpool.models;

import com.samourai.wallet.api.Tx;
import com.samourai.wallet.util.LogUtil;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus.MIX_SUCCESS;

public class Cycle extends Tx {


    List<WhirlpoolUtxo> whirlpoolUtxos = new ArrayList<>();

    //temporary fix for whirlpool client delay
    //this will used to check if the tx has any valid utxo's in premix
    private boolean foundUTXOsInPremix = false;

    public Cycle(JSONObject jsonObj) {
        super(jsonObj);
    }

    public void addWhirlpoolUTXO(WhirlpoolUtxo whirlpoolUtxo) {
        if (!this.whirlpoolUtxos.contains(whirlpoolUtxo))
            this.whirlpoolUtxos.add(whirlpoolUtxo);
    }

    public WhirlpoolUtxo getCurrentRunningMix() {
        for (WhirlpoolUtxo utxo :
                whirlpoolUtxos) {
            if (utxo.getUtxoState() != null && utxo.getUtxoState().getMixProgress() != null) {
                return utxo;
            }
        }
        return null;
    }

    public List<WhirlpoolUtxo> getWhirlpoolUtxos() {
        return whirlpoolUtxos;
    }

    public boolean isFoundUTXOsInPremix() {
        return foundUTXOsInPremix;
    }

    public void setFoundUTXOsInPremix(boolean foundUTXOsInPremix) {
        this.foundUTXOsInPremix = foundUTXOsInPremix;
    }
}
