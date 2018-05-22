package com.samourai.wallet.send;

import java.math.BigInteger;

public class SuggestedFee {

    private static final BigInteger defaultAmount = BigInteger.valueOf(1200L);

    private BigInteger defaultPerKB = defaultAmount;
    private boolean isStressed = false;
    private boolean isOK = true;

    public SuggestedFee()   { ; }

    public BigInteger getDefaultPerKB() {
        return defaultPerKB;
    }

    public void setDefaultPerKB(BigInteger defaultPerKB) {
        this.defaultPerKB = defaultPerKB;
    }

    public boolean isStressed() {
        return isStressed;
    }

    public void setStressed(boolean stressed) {
        isStressed = stressed;
    }

    public boolean isOK() {
        return isOK;
    }

    public void setOK(boolean OK) {
        isOK = OK;
    }

}
