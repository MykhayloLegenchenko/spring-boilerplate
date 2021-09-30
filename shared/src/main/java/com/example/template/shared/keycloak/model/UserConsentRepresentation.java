package com.example.template.shared.keycloak.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class UserConsentRepresentation {
  private String clientId;
  private List<String> grantedClientScopes;
  private Long createdDate;
  private Long lastUpdatedDate;
}
