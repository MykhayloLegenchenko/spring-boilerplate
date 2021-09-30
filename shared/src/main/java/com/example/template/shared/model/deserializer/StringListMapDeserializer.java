package com.example.template.shared.model.deserializer;

import com.example.template.shared.model.StringList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StringListMapDeserializer extends AbstractListMapDeserializer<String, String> {
  private static final AbstractListDeserializer<String> LIST_DESERIALIZER =
      new StringList.Deserializer();

  @Override
  protected Map<String, List<String>> createMap() {
    return new LinkedHashMap<>();
  }

  @Override
  protected String parseKey(String key) {
    return key;
  }

  @Override
  protected AbstractListDeserializer<String> getListDeserializer() {
    return LIST_DESERIALIZER;
  }
}
