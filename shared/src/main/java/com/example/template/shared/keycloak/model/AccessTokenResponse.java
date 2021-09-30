package com.example.template.shared.keycloak.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class AccessTokenResponse {
  @JsonProperty("access_token")
  private String token;

  @JsonProperty("expires_in")
  private long expiresIn;

  @JsonProperty("refresh_expires_in")
  private long refreshExpiresIn;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("id_token")
  private String idToken;

  @JsonProperty("not-before-policy")
  private int notBeforePolicy;

  @JsonProperty("session_state")
  private String sessionState;

  private Map<String, Object> otherClaims;

  // OIDC Financial API Read Only Profile : scope MUST be returned in the response from Token
  // Endpoint
  @JsonProperty("scope")
  private String scope;
}
