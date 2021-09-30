package com.example.template.shared.model;

import static com.example.template.shared.test.TestUtilities.assertEqualsAndSameClass;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.template.shared.utils.JsonUtilities;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.Data;
import org.junit.jupiter.api.Test;

class StringListTests {
  @Test
  void deserialization() throws Exception {
    var parsed = JsonUtilities.parse("{\"value\":[1,null,\"3\",\"\"]}", TestBean.class);
    var expected = new StringList();
    expected.add("1");
    expected.add(null);
    expected.add("3");
    expected.add("");

    assertEqualsAndSameClass(expected, parsed.getValue());
  }

  @Test
  void nullDeserialization() throws Exception {
    var parsed = JsonUtilities.parse("{\"value\":null}", TestBean.class);

    assertNull(parsed.getValue());
  }

  @Test
  void numberDeserialization() throws Exception {
    var parsed = JsonUtilities.parse("{\"value\":1}", TestBean.class);
    var expected = new StringList();
    expected.add("1");

    assertEqualsAndSameClass(expected, parsed.getValue());
  }

  @Data
  private static class TestBean {
    @JsonDeserialize(using = StringList.Deserializer.class)
    private List<String> value;
  }
}
