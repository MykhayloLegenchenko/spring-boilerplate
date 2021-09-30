package com.example.template.shared.keycloak.config;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties("keycloak")
@Data
@Validated
public class KeycloakProperties {
  @NotNull @URL private String url;
  @NotBlank private String realm;
  @Valid private ClientProperties client;
  @Valid private AdminProperties admin;

  @Data
  public static class ClientProperties {
    @NotBlank private String id;
    @NotBlank private String secret;
  }

  @Data
  public static class AdminProperties {
    @NotBlank private String username;
    @NotBlank private String password;
  }
}
