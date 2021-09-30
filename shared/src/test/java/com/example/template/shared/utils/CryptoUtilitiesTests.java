package com.example.template.shared.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CryptoUtilitiesTests {
  @Test
  void getRandomShouldReturnNonNull() {
    assertNotNull(CryptoUtilities.getRandom());
  }
}
