package com.example.template.account.microservice;

import com.example.template.shared.test.RandomServerPortEnvInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = RandomServerPortEnvInitializer.class)
class AccountApplicationTests {

  @Test
  void contextLoads() {
    // empty
  }
}
