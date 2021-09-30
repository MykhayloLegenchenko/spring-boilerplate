package com.example.template.shared.model.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.Nullable;

public abstract class AbstractListDeserializer<V> extends JsonDeserializer<List<V>> {
  @Override
  @Nullable
  public List<V> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return parseNode(p.readValueAsTree());
  }

  @Nullable
  public List<V> parseNode(JsonNode node) {
    if (node.isNull()) {
      return null;
    }

    List<V> result;
    if (node.isArray()) {
      result = createList(node.size());
      var it = node.elements();
      while (it.hasNext()) {
        var value = it.next();
        result.add(value.isNull() ? null : parseValue(value));
      }
    } else {
      result = createList(1);
      result.add(parseValue(node));
    }

    return result;
  }

  protected abstract List<V> createList(int initialCapacity);

  protected abstract V parseValue(JsonNode node);
}
