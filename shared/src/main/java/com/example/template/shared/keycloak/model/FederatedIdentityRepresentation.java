package com.example.template.shared.keycloak.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class FederatedIdentityRepresentation {
  private String identityProvider;
  private String userId;
  private String userName;
}
