package com.example.template.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

public final class JsonUtilities {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonUtilities() {}

  public static String toJson(@NonNull Object obj) throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  public static <T> T parse(@NonNull String json, Class<T> valueType)
      throws JsonProcessingException {
    return OBJECT_MAPPER.readValue(json, valueType);
  }

  public static Object toJsonForLog(Object obj) {
    try {
      return toJson(obj);
    } catch (JsonProcessingException e) {
      return obj.toString();
    }
  }
}
