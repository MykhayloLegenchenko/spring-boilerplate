package com.example.template.shared.keycloak.service;

import com.example.template.shared.keycloak.config.KeycloakProperties;
import com.example.template.shared.keycloak.model.GetUsersRequest;
import com.example.template.shared.keycloak.model.UserRepresentation;
import com.example.template.shared.model.JsonEntity;
import com.example.template.shared.model.JsonEntityFactory;
import com.example.template.shared.security.SecurityUtilities;
import com.example.template.shared.utils.BeanUtilities;
import java.util.List;
import lombok.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakAdminService {
  private static final ParameterizedTypeReference<List<UserRepresentation>> USER_LIST_TYPE_REF =
      new ParameterizedTypeReference<>() {};

  private final KeycloakProperties properties;
  private final WebClient client;

  public KeycloakAdminService(
      @NonNull KeycloakProperties properties,
      @NonNull WebClient.Builder webClientBuilder,
      @NonNull ApplicationContext ctx) {
    var adminProps = properties.getAdmin();
    assert adminProps != null : "Keycloak admin properties is not set";
    this.properties = properties;
    client =
        webClientBuilder
            .filter(
                SecurityUtilities.oauth2PasswordFilter(
                    ctx, "admin", adminProps.getUsername(), adminProps.getPassword()))
            .baseUrl(properties.getUrl())
            .build();
  }

  public Mono<JsonEntity<Void>> registerUser(UserRepresentation user) {
    return JsonEntityFactory.postJson(client, makeUrl(), user, Void.class);
  }

  public Mono<JsonEntity<List<UserRepresentation>>> getUsers(GetUsersRequest request) {
    var params = BeanUtilities.toParams(request);
    return JsonEntityFactory.get(client, makeUrl(), params, USER_LIST_TYPE_REF);
  }

  private String makeUrl() {
    return "/auth/admin/realms/" + properties.getRealm() + "/users";
  }
}
