package com.samourai.wallet.utxos;

import com.samourai.wallet.utxos.models.UTXOCoin;

import java.util.HashMap;
import java.util.List;

public class PreSelectUtil {

    private static PreSelectUtil instance = null;

    private static HashMap<String,List<UTXOCoin>> preSelected = null;

    private PreSelectUtil() { ; }

    public static PreSelectUtil getInstance() {

        if(instance == null) {
            preSelected = new HashMap<String,List<UTXOCoin>>();
            instance = new PreSelectUtil();
        }

        return instance;
    }

    public void add(String id, List<UTXOCoin> utxos) {
        preSelected.put(id, utxos);
    }

    public void clear() {
        preSelected.clear();
    }

    public List<UTXOCoin> getPreSelected(String id) {
        return preSelected.get(id);
    }

}
