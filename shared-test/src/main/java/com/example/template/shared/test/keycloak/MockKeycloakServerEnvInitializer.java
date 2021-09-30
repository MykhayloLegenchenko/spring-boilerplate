package com.example.template.shared.test.keycloak;

import lombok.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

public class MockKeycloakServerEnvInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
    TestPropertyValues.of("KEYCLOAK_URL=http://localhost:" + SocketUtils.findAvailableTcpPort())
        .applyTo(applicationContext);
  }
}
