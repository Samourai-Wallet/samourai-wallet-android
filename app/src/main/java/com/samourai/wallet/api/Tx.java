package com.samourai.wallet.api;

import java.util.Map;

public class Tx {

    private String strHash = null;
    private String strNote = null;
    private String strDirection = null;
    private String strAddress = null;
    private String strPaymentCode = null;
    private double amount = 0.0;
    private long confirmations = 0L;
    private long ts = 0L;
    private Map<Integer,String> tags = null;

    public Tx(String hash, String address, double amount, long date, long confirmations) {
        strHash = hash;
        strAddress = address;
        this.amount = amount;
        ts = date;
        this.confirmations = confirmations;
        this.strPaymentCode = null;
    }

    public Tx(String hash, String address, double amount, long date, long confirmations, String pcode) {
        strHash = hash;
        strAddress = address;
        this.amount = amount;
        ts = date;
        this.confirmations = confirmations;
        this.strPaymentCode = pcode;
    }

    public String getAddress() {
        return strAddress;
    }

    public void setAddress(String address) {
        strAddress = address;
    }

    public String getHash() {
        return strHash;
    }

    public void setHash(String hash) {
        strHash = hash;
    }

    public String getNote() {
        return strNote;
    }

    public void setNote(String note) {
        strNote = note;
    }

    public String getDirection() {
        return strDirection;
    }

    public void setDirection(String direction) {
        strDirection = direction;
    }

    public long getTS() {
        return ts;
    }

    public void setTS(long ts) {
        this.ts = ts;
    }

    public long getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(long confirmations) {
        this.confirmations = confirmations;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Map<Integer,String> getTags() {
        return this.tags;
    }

    public void setTags(Map<Integer,String> tags) {
        this.tags = tags;
    }

    public String getPaymentCode() {
        return strPaymentCode;
    }

    public void setPaymentCode(String pcode) {
        this.strPaymentCode = pcode;
    }

}
