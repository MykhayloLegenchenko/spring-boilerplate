package com.example.template.shared.model;

import static com.example.template.shared.test.TestUtilities.assertEqualsAndSameClass;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.template.shared.utils.JsonUtilities;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import lombok.Data;
import org.junit.jupiter.api.Test;

class LinkedStringMapTests {
  @Test
  void deserialization() throws Exception {
    var parsed =
        JsonUtilities.parse(
            "{\"value\":{\"k1\":\"1\",\"k2\":null,\"k3\":3,\"k4\":\"\"}}", TestBean.class);
    var expected = new LinkedStringMap();
    expected.put("k1", "1");
    expected.put("k2", null);
    expected.put("k3", "3");
    expected.put("k4", "");

    assertEqualsAndSameClass(expected, parsed.getValue());
  }

  @Test
  void nullDeserialization() throws Exception {
    var parsed = JsonUtilities.parse("{\"value\":null}", TestBean.class);
    assertNull(parsed.getValue());
  }

  @Test
  void numberValueDeserialization() throws Exception {
    var parsed = JsonUtilities.parse("{\"value\":{\"k\":1}}", TestBean.class);
    var expected = new LinkedStringMap();
    expected.put("k", "1");

    assertEqualsAndSameClass(expected, parsed.getValue());
  }

  @Test
  void stringDeserialization() {
    assertThrows(
        JsonMappingException.class, () -> JsonUtilities.parse("{\"value\":\"1\"}", TestBean.class));
  }

  @Data
  private static class TestBean {
    @JsonDeserialize(using = LinkedStringMap.Deserializer.class)
    private Map<String, String> value;
  }
}
