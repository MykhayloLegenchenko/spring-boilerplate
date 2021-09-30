package com.example.template.shared.security;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class ServerCookiesExchangeFilterFunction implements ExchangeFilterFunction {
  private static final String SECURITY_CONTEXT_ATTRIBUTES =
      "org.springframework.security.SECURITY_CONTEXT_ATTRIBUTES";

  @Override
  @NonNull
  public Mono<ClientResponse> filter(@NonNull ClientRequest request, ExchangeFunction next) {
    return serverCookies()
        .map(cookies -> addCookies(request, cookies))
        .defaultIfEmpty(request)
        .flatMap(next::exchange);
  }

  @SuppressWarnings("unchecked")
  private static Mono<MultiValueMap<String, String>> serverCookies() {
    return Mono.deferContextual(
        ctx -> {
          if (!ctx.hasKey(SECURITY_CONTEXT_ATTRIBUTES)) {
            return Mono.empty();
          }

          var attrs = (Map<Object, Object>) ctx.get(SECURITY_CONTEXT_ATTRIBUTES);
          if (attrs.containsKey(HttpServletRequest.class)) {
            var cookies = ((HttpServletRequest) attrs.get(HttpServletRequest.class)).getCookies();
            var result = new LinkedMultiValueMap<String, String>(cookies.length);
            for (var cookie : cookies) {
              result.add(cookie.getName(), cookie.getValue());
            }

            return Mono.just(result);
          }

          return Mono.empty();
        });
  }

  private static ClientRequest addCookies(
      ClientRequest request, MultiValueMap<String, String> cookies) {

    return ClientRequest.from(request).cookies(t -> t.addAll(cookies)).build();
  }
}
