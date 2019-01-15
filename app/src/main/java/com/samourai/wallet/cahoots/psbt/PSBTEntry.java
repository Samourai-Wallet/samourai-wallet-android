package com.samourai.wallet.cahoots.psbt;

public class PSBTEntry    {

    public PSBTEntry() { ; }

    private byte[] key = null;
    private byte[] keyType = null;
    private byte[] keyData = null;
    private byte[] data = null;

    private int state = -1;

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getKeyType() {
        return keyType;
    }

    public void setKeyType(byte[] keyType) {
        this.keyType = keyType;
    }

    public byte[] getKeyData() {
        return keyData;
    }

    public void setKeyData(byte[] keyData) {
        this.keyData = keyData;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
