package com.example.template.shared.keycloak.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class CredentialRepresentation {
  private String id;
  private String type;
  private String userLabel;
  private Long createdDate;
  private String secretData;
  private String credentialData;
  private Integer priority;
  private String value;
  // only used when updating a credential. Might set required action
  private Boolean temporary;
}
