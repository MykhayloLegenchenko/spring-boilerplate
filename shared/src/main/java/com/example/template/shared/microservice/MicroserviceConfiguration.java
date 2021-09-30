package com.example.template.shared.microservice;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableDiscoveryClient
@Import({MicroserviceProperties.class, WebSecurityConfigurerAdapterImpl.class})
public class MicroserviceConfiguration {
  // empty
}
