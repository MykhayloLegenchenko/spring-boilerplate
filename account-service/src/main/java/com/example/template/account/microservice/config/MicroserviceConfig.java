package com.example.template.account.microservice.config;

import com.example.template.shared.keycloak.service.KeycloakAdminService;
import com.example.template.shared.keycloak.service.KeycloakClientService;
import com.example.template.shared.microservice.MicroserviceConfigAdapter;
import com.example.template.shared.microservice.MicroserviceConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("com.example.template.account.microservice")
@Import({MicroserviceConfiguration.class, KeycloakAdminService.class, KeycloakClientService.class})
public class MicroserviceConfig implements MicroserviceConfigAdapter {
  @Override
  public void configureRequests(
      ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry
          urlRegistry) {
    urlRegistry
        .antMatchers(HttpMethod.POST, "/account/version", "/account/refresh", "/account/logout")
        .permitAll();
    urlRegistry.antMatchers(HttpMethod.POST, "/account/register", "/account/login").anonymous();
    urlRegistry.antMatchers(HttpMethod.POST, "/account/**").hasRole("USER");
  }
}
