package com.example.template.shared.security.truststore;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("trust-store")
class TrustStoreProperties {
  @NotBlank private String type = "pkcs12";
  @NotBlank private String keyStore;
  @NotBlank private String password;
  private boolean ignoreDomain;
}
