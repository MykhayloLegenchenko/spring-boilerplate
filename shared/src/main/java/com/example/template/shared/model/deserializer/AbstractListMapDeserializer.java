package com.example.template.shared.model.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public abstract class AbstractListMapDeserializer<K, V>
    extends AbstractMapDeserializer<K, List<V>> {
  @Override
  protected List<V> parseValue(JsonNode node) {
    return getListDeserializer().parseNode(node);
  }

  protected abstract AbstractListDeserializer<V> getListDeserializer();
}
