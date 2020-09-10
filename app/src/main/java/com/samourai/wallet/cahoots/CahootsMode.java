package com.samourai.wallet.cahoots;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;

public enum CahootsMode {
    MANUAL(0, "Manual"),
    SOROBAN(1, "Online"),
    SAMOURAI(2, "Samourai");

    private int value;
    private String label;

    CahootsMode(int value, String label) {
        this.value = value;
        this.label = label;
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

    public String getLabel() {
        return label;
    }
}