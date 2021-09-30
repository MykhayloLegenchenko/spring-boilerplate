package com.example.template.shared.model.deserializer;

import static com.example.template.shared.test.TestUtilities.assertEqualsAndSameClass;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.template.shared.utils.JsonUtilities;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.junit.jupiter.api.Test;

class StringListMapDeserializerTests {
  @Test
  void objectDeserialization() throws Exception {
    var parsed =
        JsonUtilities.parse(
            "{\"value\":{\"k1\":[\"1\"],\"k2\":[null],\"k3\":[\"3\",null,\"4\",\"\"],\"k4\":null}}",
            TestBean.class);
    var expected = new LinkedHashMap<String, List<String>>();
    expected.put("k1", Collections.singletonList("1"));
    expected.put("k2", Collections.singletonList(null));
    expected.put("k3", Arrays.asList("3", null, "4", ""));
    expected.put("k4", null);

    assertEqualsAndSameClass(expected, parsed.getValue());
  }

  @Test
  void nullDeserialization() throws Exception {
    var parsed = JsonUtilities.parse("{\"value\":null}", TestBean.class);

    assertNull(parsed.getValue());
  }

  @Test
  void stringDeserialization() {
    assertThrows(
        JsonMappingException.class, () -> JsonUtilities.parse("{\"value\":\"1\"}", TestBean.class));
  }

  @Data
  private static class TestBean {
    @JsonDeserialize(using = StringListMapDeserializer.class)
    private Map<String, List<String>> value;
  }
}
