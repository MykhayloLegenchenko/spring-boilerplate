package com.example.template.shared.model;

import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

public final class JsonEntity<T> extends ResponseEntity<Object> {

  private JsonEntity(JsonResult<T> body, MultiValueMap<String, String> headers, HttpStatus status) {
    super(body == null ? JsonResult.empty() : body, headers, status);
  }

  static <T> JsonEntity<T> create(
      JsonResult<T> body, MultiValueMap<String, String> headers, @NonNull HttpStatus status) {
    if (body != null) {
      if (status.isError()) {
        assert body.value() == null : "Error status and success result";
      } else {
        assert body.error() == null : "Success status and error result";
      }
    }

    return new JsonEntity<>(body, headers, status);
  }

  public boolean isSuccessful() {
    return getStatusCode().is2xxSuccessful();
  }

  public boolean hasError() {
    return !getStatusCode().is2xxSuccessful();
  }

  @SuppressWarnings("unchecked")
  public JsonResult<T> result() {
    return (JsonResult<T>) resultObject();
  }

  @SuppressWarnings("unchecked")
  private JsonResult<Object> resultObject() {
    return (JsonResult<Object>) super.getBody();
  }

  public Optional<T> value() {
    assert isSuccessful() : "Getting value on error response";
    return Optional.ofNullable(result().value());
  }

  public T get() {
    assert isSuccessful() : "Getting value on error response";
    return Objects.requireNonNull(result().value());
  }

  public Optional<Object> error() {
    assert hasError() : "Getting error on success response";
    return Optional.ofNullable(result().error());
  }

  @Override
  public Object getBody() {
    var entity = result();
    return entity.value() == null ? entity.error() : entity.value();
  }

  @Override
  public boolean hasBody() {
    return getBody() != null;
  }
}
