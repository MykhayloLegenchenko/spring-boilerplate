package com.example.template.api.microservice.controller;

import static com.example.template.shared.test.TestUtilities.assertVersionResponse;

import com.example.template.api.client.service.ApiClientService;
import com.example.template.shared.microservice.MicroserviceProperties;
import com.example.template.shared.test.RandomServerPortEnvInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = RandomServerPortEnvInitializer.class)
class ApiControllerTests {
  @Autowired private WebClient.Builder webClientBuilder;
  @Autowired private MicroserviceProperties microserviceProperties;

  // Do not autowire to test controller's validation
  private ApiClientService apiClientService;

  @BeforeEach
  void init() {
    apiClientService = new ApiClientService(microserviceProperties, webClientBuilder);
  }

  @Test
  void version() {
    var response = apiClientService.version();
    assertVersionResponse(response);
  }
}
