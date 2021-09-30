package com.example.template.shared.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModelUtilitiesTests {

  @Test
  void generateKeyShouldReturnValidKey() {
    var key = ModelUtilities.generateKey();
    assertTrue(ModelUtilities.isValidKey(key));
  }
}
