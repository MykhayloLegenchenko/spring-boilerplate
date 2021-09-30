package com.example.template.shared.security.truststore;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import javax.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ResourceUtils;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty("trust-store.key-store")
@EnableConfigurationProperties(TrustStoreProperties.class)
public class TrustStoreConfig {

  public TrustStoreConfig(@NotNull TrustStoreProperties properties)
      throws GeneralSecurityException, IOException {
    var trustStore = KeyStore.getInstance(properties.getType());
    try (var is = ResourceUtils.getURL(properties.getKeyStore()).openStream()) {
      trustStore.load(is, properties.getPassword().toCharArray());
    }

    Security.insertProviderAt(new TrustManagerProvider(trustStore, properties.isIgnoreDomain()), 1);
  }
}
