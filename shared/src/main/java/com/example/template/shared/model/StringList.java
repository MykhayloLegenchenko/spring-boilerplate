package com.example.template.shared.model;

import com.example.template.shared.model.StringList.Deserializer;
import com.example.template.shared.model.deserializer.AbstractListDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonDeserialize(using = Deserializer.class)
public class StringList extends ArrayList<String> {
  @Serial private static final long serialVersionUID = -3701197917362653570L;

  public StringList(int initialCapacity) {
    super(initialCapacity);
  }

  public StringList() {}

  public StringList(Collection<String> collection) {
    super(collection);
  }

  public static class Deserializer extends AbstractListDeserializer<String> {
    @Override
    protected List<String> createList(int initialCapacity) {
      return new StringList(initialCapacity);
    }

    @Override
    protected String parseValue(JsonNode node) {
      return node.asText();
    }
  }
}
