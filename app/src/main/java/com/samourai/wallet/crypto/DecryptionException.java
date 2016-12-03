package com.samourai.wallet.crypto;

public class DecryptionException extends Exception {
    //Parameterless Constructor
    public DecryptionException() {
    }

    //Constructor that accepts a message
    public DecryptionException(String message) {
        super(message);
    }
}