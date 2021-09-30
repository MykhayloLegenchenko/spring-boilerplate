package com.example.template.shared.model;

import com.example.template.shared.model.LinkedStringMap.Deserializer;
import com.example.template.shared.model.deserializer.AbstractMapDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonDeserialize(using = Deserializer.class)
public class LinkedStringMap extends LinkedHashMap<String, String> {
  @Serial private static final long serialVersionUID = -933025568543673935L;

  public LinkedStringMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public LinkedStringMap(int initialCapacity) {
    super(initialCapacity);
  }

  public LinkedStringMap() {}

  public LinkedStringMap(Map<String, String> map) {
    super(map);
  }

  public LinkedStringMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor, accessOrder);
  }

  public static class Deserializer extends AbstractMapDeserializer<String, String> {
    @Override
    protected Map<String, String> createMap() {
      return new LinkedStringMap();
    }

    @Override
    protected String parseKey(String key) {
      return key;
    }

    @Override
    protected String parseValue(JsonNode node) {
      return node.asText();
    }
  }
}
