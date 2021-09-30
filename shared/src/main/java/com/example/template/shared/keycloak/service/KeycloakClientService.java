package com.example.template.shared.keycloak.service;

import com.example.template.shared.keycloak.config.KeycloakProperties;
import com.example.template.shared.keycloak.model.AccessTokenResponse;
import com.example.template.shared.model.JsonEntity;
import com.example.template.shared.model.JsonEntityFactory;
import lombok.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakClientService {
  private final KeycloakProperties properties;
  private final WebClient client;

  public KeycloakClientService(
      @NonNull KeycloakProperties properties, @NonNull WebClient.Builder webClientBuilder) {
    assert properties.getClient() != null : "Keycloak client properties is not set";
    this.properties = properties;
    client = webClientBuilder.baseUrl(properties.getUrl()).build();
  }

  public Mono<JsonEntity<AccessTokenResponse>> login(
      String username, String password, boolean offlineAccess) {

    var body = createRequestBody();
    body.add("grant_type", "password");
    body.add("username", username);
    body.add("password", password);
    if (offlineAccess) {
      body.add("scope", "offline_access");
    }

    return JsonEntityFactory.post(client, makeUrl("/token"), body, AccessTokenResponse.class);
  }

  public Mono<JsonEntity<AccessTokenResponse>> refresh(String refreshToken) {
    var body = createRequestBody();
    body.add("grant_type", "refresh_token");
    body.add("refresh_token", refreshToken);

    return JsonEntityFactory.post(client, makeUrl("/token"), body, AccessTokenResponse.class);
  }

  public Mono<JsonEntity<Void>> revoke(String refreshToken) {
    var body = createRequestBody();
    body.add("token", refreshToken);
    body.add("token_type_hint", "refresh_token");

    return JsonEntityFactory.post(client, makeUrl("/revoke"), body, Void.class);
  }

  private String makeUrl(String path) {
    return "/auth/realms/" + properties.getRealm() + "/protocol/openid-connect" + path;
  }

  private MultiValueMap<String, String> createRequestBody() {
    var props = properties.getClient();
    var body = new LinkedMultiValueMap<String, String>();
    body.add("client_id", props.getId());
    body.add("client_secret", props.getSecret());

    return body;
  }
}
