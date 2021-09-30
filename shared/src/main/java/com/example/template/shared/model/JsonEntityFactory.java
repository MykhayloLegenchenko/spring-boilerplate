package com.example.template.shared.model;

import com.example.template.shared.utils.JsonUtilities;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public final class JsonEntityFactory {
  private static final MultiValueMap<String, String> EMPTY_PARAMS = new LinkedMultiValueMap<>();
  private static final JsonEntity<Void> OK_RESULT = ok(null, null, null);
  private static final JsonEntity<Object> BAD_GATEWAY_RESULT = error(HttpStatus.BAD_GATEWAY);

  private JsonEntityFactory() {}

  public static JsonEntity<Void> ok() {
    return OK_RESULT;
  }

  public static JsonEntity<Void> ok(HttpHeaders headers) {
    return ok(null, headers, null);
  }

  public static <T> JsonEntity<T> ok(T value) {
    return ok(value, null, null);
  }

  public static <T> JsonEntity<T> ok(T value, HttpHeaders headers) {
    return ok(value, headers, null);
  }

  public static <T> JsonEntity<T> ok(
      T value, MultiValueMap<String, String> headers, HttpStatus status) {
    assert status == null || status.is2xxSuccessful() : "Bad status";
    return JsonEntity.create(
        JsonResult.ok(value), headers, status == null ? HttpStatus.OK : status);
  }

  public static <T> JsonEntity<T> error(@NonNull HttpStatus status) {
    return error(null, null, status);
  }

  public static <T> JsonEntity<T> error(@NonNull JsonEntity<?> entity) {
    return error(entity.result().error(), entity.getHeaders(), entity.getStatusCode());
  }

  public static <T> JsonEntity<T> error(Object error, @NonNull HttpStatus status) {
    return error(error, null, status);
  }

  public static <T> JsonEntity<T> error(
      Object error, MultiValueMap<String, String> headers, @NonNull HttpStatus status) {
    assert !status.is2xxSuccessful() : "Bad status";
    return JsonEntity.create(JsonResult.error(error), headers, status);
  }

  public static <T> JsonEntity<T> exception(Throwable t) {
    return error(new ErrorResponse(t.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @SuppressWarnings("unchecked")
  public static <T> JsonEntity<T> badGateway() {
    return (JsonEntity<T>) BAD_GATEWAY_RESULT;
  }

  public static <T> Mono<JsonEntity<T>> get(
      @NonNull WebClient client,
      @NonNull String uri,
      MultiValueMap<String, String> params,
      @NonNull Class<T> resultType) {
    return get(client, uri, params, resultType, null);
  }

  public static <T> Mono<JsonEntity<T>> get(
      @NonNull WebClient client,
      @NonNull String uri,
      MultiValueMap<String, String> params,
      @NonNull ParameterizedTypeReference<T> resultRef) {
    return get(client, uri, params, null, resultRef);
  }

  private static <T> Mono<JsonEntity<T>> get(
      WebClient client,
      String uri,
      MultiValueMap<String, String> params,
      Class<T> resultType,
      ParameterizedTypeReference<T> resultRef) {
    return client
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.path(uri).queryParams(params == null ? EMPTY_PARAMS : params).build())
        .exchangeToMono(response -> fromResponse(response, resultType, resultRef))
        .doOnNext(
            response -> {
              if (response.hasError()) {
                log.error("Request error GET {} {} {}", uri, params, response);
              }
            });
  }

  public static <T> Mono<JsonEntity<T>> post(
      @NonNull WebClient client,
      @NonNull String uri,
      MultiValueMap<String, String> formData,
      @NonNull Class<T> resultType) {
    return post(client, uri, formData, resultType, null);
  }

  public static <T> Mono<JsonEntity<T>> post(
      @NonNull WebClient client,
      @NonNull String uri,
      MultiValueMap<String, String> formData,
      @NonNull ParameterizedTypeReference<T> resultRef) {
    return post(client, uri, formData, null, resultRef);
  }

  private static <T> Mono<JsonEntity<T>> post(
      WebClient client,
      String uri,
      MultiValueMap<String, String> formData,
      Class<T> resultType,
      ParameterizedTypeReference<T> resultRef) {
    return client
        .post()
        .uri(uri)
        .body(BodyInserters.fromFormData(formData == null ? EMPTY_PARAMS : formData))
        .exchangeToMono(response -> fromResponse(response, resultType, resultRef))
        .doOnNext(
            response -> {
              if (response.hasError()) {
                log.error("Request error POST {} {} {}", uri, formData, response);
              }
            });
  }

  public static <T> Mono<JsonEntity<T>> postJson(
      @NonNull WebClient client,
      @NonNull String uri,
      @NonNull Object body,
      @NonNull Class<T> resultType) {
    return postJson(client, uri, body, resultType, null);
  }

  public static <T> Mono<JsonEntity<T>> postJson(
      @NonNull WebClient client,
      @NonNull String uri,
      @NonNull Object body,
      @NonNull ParameterizedTypeReference<T> resultRef) {
    return postJson(client, uri, body, null, resultRef);
  }

  private static <T> Mono<JsonEntity<T>> postJson(
      WebClient client,
      String uri,
      Object body,
      Class<T> resultType,
      ParameterizedTypeReference<T> resultRef) {
    return client
        .post()
        .uri(uri)
        .bodyValue(body)
        .exchangeToMono(response -> fromResponse(response, resultType, resultRef))
        .doOnNext(
            response -> {
              if (response.hasError() && log.isErrorEnabled()) {
                log.error(
                    "Request error POST {} {} {}", uri, JsonUtilities.toJsonForLog(body), response);
              }
            });
  }

  private static <T> Mono<JsonEntity<T>> fromResponse(
      ClientResponse response, Class<T> resultType, ParameterizedTypeReference<T> resultRef) {
    var status = response.statusCode();
    var headers = response.headers().asHttpHeaders();

    Mono<JsonResult<T>> result;
    if (status.is2xxSuccessful()) {
      Mono<T> bodyMono =
          resultType == null ? response.bodyToMono(resultRef) : response.bodyToMono(resultType);
      result = bodyMono.map(JsonResult::ok);
    } else {
      result = response.bodyToMono(LinkedStringMap.class).map(JsonResult::error);
    }

    return result
        .defaultIfEmpty(JsonResult.empty())
        .map(body -> JsonEntity.create(body, headers, status));
  }
}
