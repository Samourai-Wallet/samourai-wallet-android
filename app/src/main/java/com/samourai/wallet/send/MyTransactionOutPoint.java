package com.samourai.wallet.send;

import com.samourai.wallet.SamouraiWallet;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;

import java.math.BigInteger;

public class MyTransactionOutPoint extends TransactionOutPoint {

    private static final long serialVersionUID = 1L;
    private byte[] scriptBytes;
    private int txOutputN;
    private Sha256Hash txHash;
    private BigInteger value;
    private int confirmations;
    private String address;
    private boolean isChange = false;

    public MyTransactionOutPoint(Sha256Hash txHash, int txOutputN, BigInteger value, byte[] scriptBytes, String address) throws ProtocolException {
        super(SamouraiWallet.getInstance().getCurrentNetworkParams(), txOutputN, new Sha256Hash(txHash.getBytes()));
        this.scriptBytes = scriptBytes;
        this.value = value;
        this.txOutputN = txOutputN;
        this.txHash = txHash;
        this.address = address;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    public int getTxOutputN() {
        return txOutputN;
    }

    public Sha256Hash getTxHash() {
        return txHash;
    }

    public Coin getValue() {
        return Coin.valueOf(value.longValue());
    }

    public String getAddress() {
        return address;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    public boolean isChange() {
        return isChange;
    }

    public void setIsChange(boolean isChange) {
        this.isChange = isChange;
    }

    @Override
    public TransactionOutput getConnectedOutput() {
        return new TransactionOutput(params, null, Coin.valueOf(value.longValue()), scriptBytes);
    }

    //@Override
    public byte[] getConnectedPubKeyScript() {
        return scriptBytes;
    }
}