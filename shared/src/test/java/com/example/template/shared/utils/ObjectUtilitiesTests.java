package com.example.template.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ObjectUtilitiesTests {
  @Test
  void fromBytesShouldReturnStringEqualsToOnePassedToGetBytes() throws Exception {
    var value = " Test string. ";
    var bytes = ObjectUtilities.toBytes(value);
    assertEquals(value, ObjectUtilities.fromBytes(bytes));
  }
}
