package com.samourai.wallet.bip47.rpc;

public class NotSecp256k1Exception extends Exception {
  public NotSecp256k1Exception() { super(); }
  public NotSecp256k1Exception(String message) { super(message); }
  public NotSecp256k1Exception(String message, Throwable cause) { super(message, cause); }
  public NotSecp256k1Exception(Throwable cause) { super(cause); }
}
