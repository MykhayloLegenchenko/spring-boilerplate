package com.example.template.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class JsonEntityTests {

  @Test
  void defaults() {
    var entity = JsonEntity.create(null, null, HttpStatus.OK);
    assertSame(JsonResult.empty(), entity.result());
    assertNull(entity.getBody());

    var headers = entity.getHeaders();
    assertNotNull(headers);
    assertEquals(0, headers.size());
  }

  @Test
  void getBody() {
    assertEquals("1", JsonEntityFactory.ok("1").getBody());
    assertEquals("2", JsonEntityFactory.error("2", HttpStatus.CONFLICT).getBody());
  }

  @Test
  void gettingValueOnErrorResponse() {
    var msg = "Getting value on error response";
    var entity = JsonEntityFactory.error(HttpStatus.UNAUTHORIZED);

    var ex = assertThrows(AssertionError.class, entity::get);
    assertEquals(msg, ex.getMessage());

    ex = assertThrows(AssertionError.class, entity::value);
    assertEquals(msg, ex.getMessage());
  }

  @Test
  void gettingErrorOnSuccessResponse() {
    var entity = JsonEntityFactory.ok();
    var ex = assertThrows(AssertionError.class, entity::error);
    assertEquals("Getting error on success response", ex.getMessage());
  }

  @Test
  void successStatusWithErrorResult() {
    var ex =
        assertThrows(
            AssertionError.class,
            () -> JsonEntity.create(JsonResult.error("2"), null, HttpStatus.ACCEPTED));
    assertEquals("Success status and error result", ex.getMessage());
  }

  @Test
  void errorStatusWithSuccessResult() {
    var ex =
        assertThrows(
            AssertionError.class,
            () -> JsonEntity.create(JsonResult.ok("1"), null, HttpStatus.BAD_REQUEST));
    assertEquals("Error status and success result", ex.getMessage());
  }
}
