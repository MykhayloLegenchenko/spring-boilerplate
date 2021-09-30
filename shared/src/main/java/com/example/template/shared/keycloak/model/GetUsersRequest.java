package com.example.template.shared.keycloak.model;

import lombok.Data;

@Data
public class GetUsersRequest {
  private String search;
  private String lastName;
  private String firstName;
  private String email;
  private String username;
  private Boolean emailVerified;
  private String idpAlias;
  private String idpUserId;
  private Integer first;
  private Integer max;
  private Boolean enabled;
  private Boolean briefRepresentation;
  private Boolean exact;
}
