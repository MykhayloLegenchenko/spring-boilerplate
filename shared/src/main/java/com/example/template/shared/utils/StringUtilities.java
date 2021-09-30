package com.example.template.shared.utils;

import lombok.NonNull;

public final class StringUtilities {
  private StringUtilities() {}

  public static boolean isDigits(@NonNull CharSequence value) {
    return !value.isEmpty() && value.chars().allMatch(code -> code >= '0' && code <= '9');
  }

  public static boolean isHexLowercase(@NonNull CharSequence value) {
    return !value.isEmpty()
        && value
            .chars()
            .allMatch(code -> (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f'));
  }
}
