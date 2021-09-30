package com.example.template.shared.model.deserializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

public abstract class AbstractMapDeserializer<K, V> extends JsonDeserializer<Map<K, V>> {
  @Override
  @Nullable
  public Map<K, V> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode rootNode = p.readValueAsTree();
    if (rootNode.isNull()) {
      return null;
    }

    if (!rootNode.isObject()) {
      throw new JsonParseException(p, "Node is not object");
    }

    var result = createMap();
    var it = rootNode.fields();
    while (it.hasNext()) {
      var entry = it.next();
      var key = entry.getKey();
      var node = entry.getValue();

      V value = null;
      if (!node.isNull()) {
        value = parseValue(node);
      }

      result.put(parseKey(key), value);
    }

    return result;
  }

  protected Map<K, V> createMap() {
    return new HashMap<>();
  }

  protected abstract K parseKey(String key);

  protected abstract V parseValue(JsonNode node);
}
