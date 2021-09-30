package com.example.template.api.client.service;

import com.example.template.api.contract.ReactiveApiContact;
import com.example.template.shared.microservice.MicroserviceProperties;
import com.example.template.shared.model.JsonEntity;
import com.example.template.shared.model.JsonEntityFactory;
import com.example.template.shared.security.ServerCookiesExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ApiClientService implements ReactiveApiContact {
  private static final String PATH = "/api";
  private final WebClient client;

  public ApiClientService(MicroserviceProperties properties, WebClient.Builder webClientBuilder) {
    client =
        webClientBuilder
            .baseUrl(properties.getDependency("api").getUrl())
            .filter(new ServerCookiesExchangeFilterFunction())
            .build();
  }

  @Override
  public Mono<JsonEntity<String>> version() {
    return JsonEntityFactory.post(client, PATH + "/version", null, String.class);
  }
}
