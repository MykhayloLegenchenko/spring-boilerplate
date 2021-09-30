package com.example.template.shared.model;

import java.util.UUID;
import java.util.regex.Pattern;
import lombok.NonNull;

public final class ModelUtilities {
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}");

  private ModelUtilities() {}

  public static String generateKey() {
    return UUID.randomUUID().toString();
  }

  public static boolean isValidKey(@NonNull CharSequence key) {
    return UUID_PATTERN.matcher(key).matches();
  }
}
