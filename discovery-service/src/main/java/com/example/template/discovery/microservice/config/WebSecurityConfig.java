package com.example.template.discovery.microservice.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @SuppressWarnings("unchecked")
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Enable CORS and disable CSRF
    http.cors().and().csrf().disable();

    // Configure stateless session
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    // Disable form login and logout
    http.removeConfigurer(DefaultLoginPageConfigurer.class);
    http.logout().disable();

    // Enable basic authentication
    http.httpBasic();

    // Permit requests to actuator without authentication
    var requests = http.authorizeRequests();
    requests.antMatchers("/actuator/**").permitAll();

    // All other requests should be authenticated
    requests.anyRequest().authenticated();
  }
}
