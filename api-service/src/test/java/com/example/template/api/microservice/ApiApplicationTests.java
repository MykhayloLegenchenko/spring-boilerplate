package com.example.template.api.microservice;

import com.example.template.shared.test.RandomServerPortEnvInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = RandomServerPortEnvInitializer.class)
class ApiApplicationTests {

  @Test
  void contextLoads() {
    // empty
  }
}
