package com.example.template.account.client.service;

import com.example.template.account.contract.ReactiveAccountContract;
import com.example.template.account.contract.model.LoginRequest;
import com.example.template.account.contract.model.RegisterUserRequest;
import com.example.template.account.contract.model.TokenResponse;
import com.example.template.shared.microservice.MicroserviceProperties;
import com.example.template.shared.model.JsonEntity;
import com.example.template.shared.model.JsonEntityFactory;
import com.example.template.shared.security.ServerCookiesExchangeFilterFunction;
import javax.validation.Valid;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AccountClientService implements ReactiveAccountContract {
  private static final String PATH = "/account";
  private final WebClient client;

  public AccountClientService(
      MicroserviceProperties properties, WebClient.Builder webClientBuilder) {
    client =
        webClientBuilder
            .baseUrl(properties.getDependency("account").getUrl())
            .filter(new ServerCookiesExchangeFilterFunction())
            .filter(new ServerBearerExchangeFilterFunction())
            .build();
  }

  @Override
  public Mono<JsonEntity<String>> version() {
    return JsonEntityFactory.post(client, PATH + "/version", null, String.class);
  }

  @Override
  public Mono<JsonEntity<TokenResponse>> register(@Valid RegisterUserRequest request) {
    return JsonEntityFactory.postJson(client, PATH + "/register", request, TokenResponse.class);
  }

  @Override
  public Mono<JsonEntity<TokenResponse>> refresh() {
    return JsonEntityFactory.post(client, PATH + "/refresh", null, TokenResponse.class);
  }

  @Override
  public Mono<JsonEntity<TokenResponse>> login(@Valid LoginRequest request) {
    return JsonEntityFactory.postJson(client, PATH + "/login", request, TokenResponse.class);
  }

  @Override
  public Mono<JsonEntity<Void>> logout() {
    return JsonEntityFactory.post(client, PATH + "/logout", null, Void.class);
  }

  @Override
  public Mono<JsonEntity<Void>> update() {
    return JsonEntityFactory.post(client, PATH + "/update", null, Void.class);
  }
}
