package com.example.template.discovery.microservice;

import com.example.template.shared.security.truststore.TrustStoreConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableEurekaServer
@Import(TrustStoreConfig.class)
public class DiscoveryApplication {

  public static void main(String[] args) {
    SpringApplication.run(DiscoveryApplication.class, args);
  }
}
