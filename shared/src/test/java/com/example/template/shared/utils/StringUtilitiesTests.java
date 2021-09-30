package com.example.template.shared.utils;

import static com.example.template.shared.test.TestUtilities.assertAllFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class StringUtilitiesTests {
  @Test
  void isDigitsShouldReturnTrueForCorrectValues() {
    assertTrue(StringUtilities.isDigits("0123456789"));
  }

  @Test
  void isDigitsShouldReturnFalseForIncorrectValues() {
    assertAllFalse(
        Arrays.asList("", "a", "A", "i", " 1", "2 ", "+1", "-1"), StringUtilities::isDigits);
  }

  @Test
  void isHexLowercaseShouldReturnTrueForCorrectValues() {
    assertTrue(StringUtilities.isHexLowercase("0123456789abcdef"));
  }

  @Test
  void isHexLowercaseShouldReturnFalseForIncorrectValues() {
    assertAllFalse(
        Arrays.asList("", "A", "i", " 1", "2 ", "+1", "-1"), StringUtilities::isHexLowercase);
  }
}
