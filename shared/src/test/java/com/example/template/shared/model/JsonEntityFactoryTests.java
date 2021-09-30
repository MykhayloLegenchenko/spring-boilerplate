package com.example.template.shared.model;

import static com.example.template.shared.test.TestUtilities.assertEqualsAndSameClass;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.template.shared.utils.JsonUtilities;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class JsonEntityFactoryTests {
  private static final String CONTENT_TYPE_FORM_URLENCODED =
      MediaType.APPLICATION_FORM_URLENCODED + ";charset=UTF-8";
  private static final ParameterizedTypeReference<TestResponse> TEST_RESPONSE_TYPE_REF =
      new ParameterizedTypeReference<>() {};

  @Test
  void badStatus() {
    var msg = "Bad status";

    var ex =
        assertThrows(
            AssertionError.class,
            () -> JsonEntityFactory.ok(null, null, HttpStatus.INTERNAL_SERVER_ERROR));
    assertEquals(msg, ex.getMessage());

    ex =
        assertThrows(
            AssertionError.class, () -> JsonEntityFactory.error(null, null, HttpStatus.CREATED));
    assertEquals(msg, ex.getMessage());
  }

  @Test
  void successGet() throws Exception {
    var params = new LinkedMultiValueMap<String, String>();
    params.add("p1", "1");
    params.add("p1", "2");
    params.add("p2", "3");

    var responseBody = new TestResponse("success message");
    var responseStatus = HttpStatus.OK;
    var mockResponse = createMockResponse(responseStatus, responseBody);

    assertResponse(
        true,
        HttpMethod.GET,
        "/success?p1=1&p1=2&p2=3",
        null,
        "",
        mockResponse,
        createEntity(responseStatus, mockResponse, responseBody, null),
        webClient ->
            Arrays.asList(
                JsonEntityFactory.get(webClient, "/success", params, TestResponse.class),
                JsonEntityFactory.get(webClient, "/success", params, TEST_RESPONSE_TYPE_REF)));
  }

  @Test
  void errorGet() throws Exception {
    var responseStatus = HttpStatus.BAD_REQUEST;
    var errorBody = new LinkedStringMap();
    errorBody.put("error", "error message");

    var mockResponse = createMockResponse(responseStatus, errorBody);

    assertResponse(
        false,
        HttpMethod.GET,
        "/error",
        null,
        "",
        mockResponse,
        createEntity(responseStatus, mockResponse, null, errorBody),
        webClient ->
            Collections.singletonList(
                JsonEntityFactory.get(webClient, "/error", null, TestResponse.class)));
  }

  @Test
  void errorGetWithEmptyBody() throws Exception {
    var responseStatus = HttpStatus.NOT_FOUND;
    var mockResponse = new MockResponse().setStatus("HTTP/1.1 " + responseStatus);

    assertResponse(
        false,
        HttpMethod.GET,
        "/error",
        null,
        "",
        mockResponse,
        createEntity(responseStatus, mockResponse, null, null),
        webClient ->
            Collections.singletonList(
                JsonEntityFactory.get(webClient, "/error", null, TestResponse.class)));
  }

  @Test
  void successPost() throws Exception {
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("p1", "1");
    formData.add("p1", "2");
    formData.add("p2", "3");

    var responseBody = new TestResponse("success message");
    var responseStatus = HttpStatus.OK;
    var mockResponse = createMockResponse(responseStatus, responseBody);

    assertResponse(
        true,
        HttpMethod.POST,
        "/success",
        CONTENT_TYPE_FORM_URLENCODED,
        "p1=1&p1=2&p2=3",
        mockResponse,
        createEntity(responseStatus, mockResponse, responseBody, null),
        webClient ->
            Arrays.asList(
                JsonEntityFactory.post(webClient, "/success", formData, TestResponse.class),
                JsonEntityFactory.post(webClient, "/success", formData, TEST_RESPONSE_TYPE_REF)));
  }

  @Test
  void errorPost() throws Exception {
    var responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    var errorBody = new LinkedStringMap();
    errorBody.put("error", "error message");

    var mockResponse = createMockResponse(responseStatus, errorBody);

    assertResponse(
        false,
        HttpMethod.POST,
        "/error",
        CONTENT_TYPE_FORM_URLENCODED,
        "",
        mockResponse,
        createEntity(responseStatus, mockResponse, null, errorBody),
        webClient ->
            Collections.singletonList(
                JsonEntityFactory.post(webClient, "/error", null, TestResponse.class)));
  }

  @Test
  void successPostJson() throws Exception {
    var requestBody = new TestResponse("request message");
    var responseBody = new TestResponse("response message");
    var responseStatus = HttpStatus.OK;
    var mockResponse = createMockResponse(responseStatus, responseBody);

    assertResponse(
        true,
        HttpMethod.POST,
        "/success",
        MediaType.APPLICATION_JSON_VALUE,
        "{\"message\":\"request message\"}",
        mockResponse,
        createEntity(responseStatus, mockResponse, responseBody, null),
        webClient ->
            Arrays.asList(
                JsonEntityFactory.postJson(webClient, "/success", requestBody, TestResponse.class),
                JsonEntityFactory.postJson(
                    webClient, "/success", requestBody, TEST_RESPONSE_TYPE_REF)));
  }

  @Test
  void errorPostJson() throws Exception {
    var requestBody = new TestResponse("request message");
    var responseStatus = HttpStatus.UNAUTHORIZED;
    var errorBody = new LinkedStringMap();
    errorBody.put("error", "error message");

    var mockResponse = createMockResponse(responseStatus, errorBody);

    assertResponse(
        false,
        HttpMethod.POST,
        "/error",
        MediaType.APPLICATION_JSON_VALUE,
        "{\"message\":\"request message\"}",
        mockResponse,
        createEntity(responseStatus, mockResponse, null, errorBody),
        webClient ->
            Collections.singletonList(
                JsonEntityFactory.postJson(webClient, "/error", requestBody, TestResponse.class)));
  }

  private static MockResponse createMockResponse(HttpStatus status, Object obj)
      throws JsonProcessingException {
    return new MockResponse()
        .setStatus("HTTP/1.1 " + status)
        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .setBody(JsonUtilities.toJson(obj));
  }

  private static JsonEntity<Object> createEntity(
      HttpStatus status, MockResponse mockResponse, Object body, Map<String, String> errorBody) {

    var mockHeaders = mockResponse.getHeaders();
    var headers = new LinkedMultiValueMap<String, String>(mockHeaders.size());
    for (int i = 0, size = mockHeaders.size(); i < size; i++) {
      var name = mockHeaders.name(i);
      headers.put(name, mockHeaders.values(name));
    }

    return status.is2xxSuccessful()
        ? JsonEntityFactory.ok(body, headers, status)
        : JsonEntityFactory.error(errorBody, headers, status);
  }

  private static void assertResponse(
      boolean shouldSuccess,
      HttpMethod requestMethod,
      String requestPath,
      String requestContentType,
      String requestBody,
      MockResponse mockResponse,
      JsonEntity<?> entity,
      Function<WebClient, List<Mono<? extends JsonEntity<?>>>> responsesProducer)
      throws IOException, InterruptedException {

    @SuppressWarnings("UnusedVariable")
    record Exchange(RecordedRequest request, JsonEntity<?> response) {}

    List<Exchange> exchanges;
    try (var server = new MockWebServer()) {
      server.setDispatcher(
          new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
              return mockResponse;
            }
          });

      var webClient = WebClient.create(server.url("").toString());
      var responses =
          responsesProducer.apply(webClient).stream().map(Mono::block).collect(Collectors.toList());

      int count = responses.size();
      assertEquals(count, server.getRequestCount());
      exchanges = new ArrayList<>(count);
      for (JsonEntity<?> response : responses) {
        exchanges.add(new Exchange(server.takeRequest(), response));
      }
    }

    exchanges.forEach(
        exchange -> {
          var request = exchange.request();
          assertEquals(requestMethod.toString(), request.getMethod());
          assertEquals(requestPath, request.getPath());
          assertEquals(requestContentType, request.getHeader("Content-Type"));
          assertEquals(requestBody, request.getBody().snapshot().utf8());
          assertEqualsAndSameClass(entity, exchange.response());

          if (shouldSuccess) {
            assertTrue(entity.isSuccessful());
            assertFalse(entity.hasError());
            assertTrue(entity.value().isPresent());
          } else {
            assertFalse(entity.isSuccessful());
            assertTrue(entity.hasError());
            assertDoesNotThrow(entity::error);
          }
        });
  }

  @SuppressWarnings("UnusedVariable")
  private record TestResponse(Object message) {}
}
