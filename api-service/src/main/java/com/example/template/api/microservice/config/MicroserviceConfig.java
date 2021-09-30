package com.example.template.api.microservice.config;

import com.example.template.shared.microservice.MicroserviceConfigAdapter;
import com.example.template.shared.microservice.MicroserviceConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

@Configuration
@Import(MicroserviceConfiguration.class)
public class MicroserviceConfig implements MicroserviceConfigAdapter {
  @Override
  public void configureRequests(
      ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry
          urlRegistry) {
    urlRegistry.antMatchers(HttpMethod.POST, "/api/version").permitAll();
    urlRegistry.antMatchers(HttpMethod.POST, "/api/**").hasRole("USER");
  }
}
