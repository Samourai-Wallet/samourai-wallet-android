package com.samourai.wallet.cahoots;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;

public enum CahootsMode {
    MANUAL(0),
    SOROBAN(1),
    SAMOURAI(2);

    private int value;

    CahootsMode(int value) {
        this.value = value;
    }

    public static Optional<CahootsMode> find(int value) {
      for (CahootsMode item : CahootsMode.values()) {
          if (item.value == value) {
              return Optional.of(item);
          }
      }
      return Optional.absent();
    }

    @JsonValue
    public int getValue() {
        return value;
    }
}