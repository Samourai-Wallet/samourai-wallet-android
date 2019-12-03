package com.samourai.wallet.utxos.models;


import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;


/**
 * Sections for UTXO lists
 */
public class UTXOCoinSegment extends UTXOCoin {

    //for UTXOActivity
    public boolean isActive = false;

    //for whirlpool utxo list
    public boolean unCycled = false;

    public UTXOCoinSegment(MyTransactionOutPoint outPoint, UTXO utxo) {
        super(outPoint, utxo);
    }
}