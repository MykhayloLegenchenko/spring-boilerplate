package com.example.template.shared.security;

import java.util.ArrayList;
import java.util.Map;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

public final class SecurityUtilities {
  private SecurityUtilities() {}

  public static ExchangeFilterFunction oauth2PasswordFilter(
      @NonNull ApplicationContext ctx,
      @NonNull String clientRegistrationId,
      @NonNull String username,
      @NonNull String password) {

    ReactiveClientRegistrationRepository clientRegistrationRepository;
    try {
      clientRegistrationRepository = ctx.getBean(ReactiveClientRegistrationRepository.class);
    } catch (BeansException ex) {
      var properties = ctx.getBean(OAuth2ClientProperties.class);
      var registrations =
          OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values();
      clientRegistrationRepository =
          new InMemoryReactiveClientRegistrationRepository(new ArrayList<>(registrations));
    }

    ReactiveOAuth2AuthorizedClientService authorizedClientService;
    try {
      authorizedClientService = ctx.getBean(ReactiveOAuth2AuthorizedClientService.class);
    } catch (BeansException ex) {
      authorizedClientService =
          new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    var authorizedClientManager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);
    var attributes =
        Mono.just(
            Map.<String, Object>of(
                OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username,
                OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password));
    authorizedClientManager.setContextAttributesMapper(authorizeRequest -> attributes);

    var authorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder().refreshToken().password().build();
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    var filterFunction =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    filterFunction.setDefaultClientRegistrationId(clientRegistrationId);

    return filterFunction;
  }
}
