package com.samourai.wallet.whirlpool.models;

import com.samourai.wallet.api.Tx;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Cycle extends Tx {


    List<WhirlpoolUtxo> whirlpoolUtxos = new ArrayList<>();

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
            if (utxo.getUtxoState() != null && utxo.getUtxoState().getMixProgress() != null ) {
                return utxo;
            }
        }
        return null;
    }
}
