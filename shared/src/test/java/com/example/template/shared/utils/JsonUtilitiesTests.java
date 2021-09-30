package com.example.template.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.Data;
import org.junit.jupiter.api.Test;

class JsonUtilitiesTests {
  private static final String JSON_STR =
      "{\"stringValue\":\"string value\",\"stringNullValue\":null,\"integerValue\":2,\"invValue\":1}";

  @Test
  void testToJson() throws Exception {
    var json = JsonUtilities.toJson(TestBean.getBean());
    assertEquals(JSON_STR, json);
  }

  @Test
  void parse() throws Exception {
    var parsed = JsonUtilities.parse(JSON_STR, TestBean.class);
    var expected = TestBean.getBean();
    assertEquals(expected, parsed);
  }

  @Test
  void toJsonForLogResultShouldEqualsToJsonResult() {
    var json = JsonUtilities.toJsonForLog(TestBean.getBean());
    var jsonForLog = JsonUtilities.toJsonForLog(TestBean.getBean());
    assertEquals(json, jsonForLog);
  }

  @Data
  private static final class TestBean {
    private String stringValue;
    private String stringNullValue;
    private int integerValue;
    private int invValue;

    static TestBean getBean() {
      var bean = new TestBean();
      bean.setStringValue("string value");
      bean.setIntegerValue(2);
      bean.setInvValue(1);

      return bean;
    }
  }
}
