package com.example.template.shared.test;

import lombok.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

public class RandomServerPortEnvInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
    TestPropertyValues.of("SERVER_PORT=" + SocketUtils.findAvailableTcpPort())
        .applyTo(applicationContext);
  }
}
