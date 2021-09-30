package com.example.template.shared.keycloak.model;

import com.example.template.shared.model.deserializer.StringListMapDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class UserRepresentation {
  private String self; // link
  private String id;
  private String origin;
  private Long createdTimestamp;
  private String username;
  private Boolean enabled;
  private Boolean totp;
  private Boolean emailVerified;
  private String firstName;
  private String lastName;
  private String email;
  private String federationLink;
  private String serviceAccountClientId; // For rep, it points to clientId (not DB ID)

  @JsonDeserialize(using = StringListMapDeserializer.class)
  private Map<String, List<String>> attributes;

  private List<CredentialRepresentation> credentials;
  private Set<String> disableableCredentialTypes;
  private List<String> requiredActions;
  private List<FederatedIdentityRepresentation> federatedIdentities;
  private List<String> realmRoles;
  private Map<String, List<String>> clientRoles;
  private List<UserConsentRepresentation> clientConsents;
  private Integer notBefore;
  private List<String> groups;
  private Map<String, Boolean> access;
}
