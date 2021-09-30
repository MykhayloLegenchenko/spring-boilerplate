package com.example.template.shared.microservice;

import com.example.template.shared.security.truststore.TrustStoreConfig;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Import(TrustStoreConfig.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class WebSecurityConfigurerAdapterImpl extends WebSecurityConfigurerAdapter {
  private final MicroserviceConfigAdapter configurer;

  WebSecurityConfigurerAdapterImpl(MicroserviceConfigAdapter configurer) {
    this.configurer = configurer;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Enable CORS and disable CSRF
    http.cors().and().csrf().disable();

    // Configure stateless session
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    // Disable login and logout
    http.removeConfigurer(DefaultLoginPageConfigurer.class);
    http.logout().disable();

    // Permit requests to actuator without authentication
    var requests = http.authorizeRequests();
    requests.antMatchers("/actuator/**").permitAll();

    // Configure requests authorization
    configurer.configureRequests(requests);

    // Deny any not allowed requests
    requests.anyRequest().denyAll();

    // Configure OAuth 2 resource server with JWT token
    http.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthenticationConverter());
  }

  private static JwtAuthenticationConverter jwtAuthenticationConverter() {
    var jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
    return jwtAuthenticationConverter;
  }

  private static Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
    var delegate = new JwtGrantedAuthoritiesConverter();

    return source -> {
      var grantedAuthorities = delegate.convert(source);

      var realmAccess = (JSONObject) source.getClaim("realm_access");
      if (realmAccess == null) {
        return grantedAuthorities;
      }

      var roles = (JSONArray) realmAccess.get("roles");
      if (roles == null) {
        return grantedAuthorities;
      }

      var keycloakAuthorities =
          roles.stream()
              .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
              .collect(Collectors.toList());
      grantedAuthorities.addAll(keycloakAuthorities);

      return grantedAuthorities;
    };
  }
}
