package com.samourai.wallet.send;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;

import java.math.BigInteger;

public class MyTransactionInput extends TransactionInput {

    private static final long serialVersionUID = 1L;

    public String address;
    public BigInteger value;
    public NetworkParameters params;

    public String txHash;
    public int txPos;

    public MyTransactionInput(NetworkParameters params, Transaction parentTransaction, byte[] scriptBytes, TransactionOutPoint outpoint, String txHash, int txPos) {
        super(params, parentTransaction, scriptBytes, outpoint);
        this.params = params;
        this.txHash = txHash;
        this.txPos = txPos;
    }

    public MyTransactionInput(NetworkParameters params, Transaction parentTransaction, byte[] scriptBytes, TransactionOutPoint outpoint) {
        super(params, parentTransaction, scriptBytes, outpoint);
        this.params = params;
        this.txHash = "";
        this.txPos = -1;
    }

    @Override
    public Address getFromAddress() {

        try {
            return new Address(params, address);
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Coin getValue() {
        if(value == null)    {
            return null;
        }
        else    {
            return Coin.valueOf(value.longValue());
        }
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String getTxHash() {
        return txHash;
    }

    public int getTxPos() {
        return txPos;
    }

}
