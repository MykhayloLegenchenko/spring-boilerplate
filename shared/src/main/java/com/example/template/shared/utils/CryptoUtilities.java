package com.example.template.shared.utils;

import java.security.SecureRandom;

public final class CryptoUtilities {
  private static final ThreadLocal<SecureRandom> SECURE_RANDOM =
      ThreadLocal.withInitial(SecureRandom::new);

  private CryptoUtilities() {}

  public static SecureRandom getRandom() {
    return SECURE_RANDOM.get();
  }
}
