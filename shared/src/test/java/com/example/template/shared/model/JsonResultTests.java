package com.example.template.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JsonResultTests {
  @Test
  void bothValueAndErrorNotNullShouldThrowException() {
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    var ex = assertThrows(AssertionError.class, () -> new JsonResult<>("1", "2"));
    assertEquals("Either value or error must be null", ex.getMessage());
  }
}
