package com.example.template.shared.model;

@SuppressWarnings("UnusedVariable")
public record JsonResult<T>(T value, Object error) {
  private static final JsonResult<Object> EMPTY = new JsonResult<>(null, null);

  public JsonResult {
    assert value == null || error == null : "Either value or error must be null";
  }

  public static <T> JsonResult<T> ok(T value) {
    return value == null ? empty() : new JsonResult<>(value, null);
  }

  public static <T> JsonResult<T> error(Object error) {
    return error == null ? empty() : new JsonResult<>(null, error);
  }

  @SuppressWarnings("unchecked")
  public static <T> JsonResult<T> empty() {
    return (JsonResult<T>) EMPTY;
  }
}
