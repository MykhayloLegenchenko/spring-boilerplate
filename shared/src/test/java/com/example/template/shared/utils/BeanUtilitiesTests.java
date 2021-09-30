package com.example.template.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

class BeanUtilitiesTests {
  @Test
  void trimBean() {
    @Data
    @AllArgsConstructor
    final class TestBean {
      private String value;
      private String nullValue;

      @Setter(AccessLevel.NONE)
      private String ignoreReadOnly;

      private Object ignoreObject;
    }

    var value = "\t string value\t  ";
    var bean = new TestBean(value, null, value, value);
    BeanUtilities.trim(bean);

    assertEquals(value.trim(), bean.getValue());
    assertNull(bean.getNullValue());
    assertSame(value, bean.getIgnoreReadOnly());
    assertSame(value, bean.getIgnoreObject());
  }

  @Test
  void trimBeanWithRecordShouldThrowException() {
    @SuppressWarnings("UnusedVariable")
    record TestRecord(String value) {}

    var ex = assertThrows(AssertionError.class, () -> BeanUtilities.trim(new TestRecord("value")));
    assertEquals("Use BeanUtilities::trimRecord to trim records", ex.getMessage());
  }

  @Test
  void trimRecord() {
    @SuppressWarnings("UnusedVariable")
    record TestRecord(String value, String nullValue, Object ignoreObject) {}

    var value = "\t string value\t  ";
    var record = BeanUtilities.trimRecord(new TestRecord(value, null, value));

    assertEquals(value.trim(), record.value());
    assertNull(record.nullValue());
    assertSame(value, record.ignoreObject());
  }

  @Test
  void beanToParams() {
    @Data
    final class TestBean {
      @Getter(AccessLevel.PRIVATE)
      private String ignorePrivate = "private";

      @Getter(AccessLevel.PACKAGE)
      private String ignorePackage = "package";

      @Getter(AccessLevel.PROTECTED)
      private String ignoreProtected = "protected";

      private boolean booleanValue = true;
      private final int[] intArray = {1, 2, 3};
      private int intValue = 123;
      private Integer integerValue = 345;
      private List<String> stringList = Arrays.asList("one", "two", "three", null, "");
      private String[] stringArray = {"four", "five", "six seven", null, ""};
      private String stringEmpty = "";
      private String stringNull;
      private String stringValue = "string value";
    }

    var expected = new LinkedMultiValueMap<String, String>();
    expected.add("booleanValue", "true");
    expected.addAll("intArray", List.of("1", "2", "3"));
    expected.add("intValue", "123");
    expected.add("integerValue", "345");
    expected.addAll("stringList", List.of("one", "two", "three"));
    expected.addAll("stringArray", List.of("four", "five", "six seven"));
    expected.add("stringValue", "string value");

    var bean = new TestBean();
    var params = BeanUtilities.toParams(bean);
    assertEquals(expected, params);

    @SuppressWarnings("UnusedVariable")
    record TestRecord(
        boolean booleanValue,
        int[] intArray,
        int intValue,
        Integer integerValue,
        List<String> stringList,
        String[] stringArray,
        String stringEmpty,
        String stringNull,
        String stringValue) {}

    var record =
        new TestRecord(
            bean.isBooleanValue(),
            bean.getIntArray(),
            bean.getIntValue(),
            bean.getIntegerValue(),
            bean.getStringList(),
            bean.getStringArray(),
            bean.getStringEmpty(),
            bean.getStringNull(),
            bean.getStringValue());
    params = BeanUtilities.toParams(record);
    assertEquals(expected, params);
  }
}
