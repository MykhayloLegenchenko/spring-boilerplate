package com.example.template.shared.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.template.shared.model.JsonEntity;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public final class TestUtilities {
  private TestUtilities() {}

  public static void assertEqualsAndSameClass(Object expected, Object actual) {
    assertEquals(expected, actual);

    if (expected != null) {
      assertSame(expected.getClass(), actual.getClass(), "classes");
    }
  }

  public static <T> void assertAllFalse(Iterable<T> values, Predicate<? super T> predicate) {
    values.forEach(
        value ->
            assertFalse(
                predicate.test(value), '"' + (value == null ? "null" : value.toString()) + '"'));
  }

  public static MockHttpServletResponse toMockHttpServletResponse(
      MultiValueMap<String, String> headers) {
    var response = new MockHttpServletResponse();
    headers.forEach((name, values) -> values.forEach((value) -> response.addHeader(name, value)));

    return response;
  }

  public static void assertVersionResponse(Mono<JsonEntity<String>> response) {
    assertNotNull(response);

    var entry = response.block();
    assertNotNull(entry);
    assertTrue(entry.isSuccessful());

    var version = entry.get();
    assertNotNull(entry);

    var pattern = Pattern.compile("^\\d+\\.\\d+\\.\\d+(?:-[a-zA-Z0-9]+)?$");
    var matcher = pattern.matcher(version);
    assertTrue(matcher.matches());
  }
}
