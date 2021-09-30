package com.example.template.account.microservice.controller;

import static com.example.template.shared.test.TestUtilities.assertVersionResponse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.template.account.client.service.AccountClientService;
import com.example.template.account.contract.model.LoginRequest;
import com.example.template.account.contract.model.RegisterUserRequest;
import com.example.template.account.contract.model.TokenResponse;
import com.example.template.account.microservice.model.SessionData;
import com.example.template.account.microservice.model.repository.AccountProfileRepository;
import com.example.template.account.microservice.service.ironsession.IronSessionService;
import com.example.template.account.microservice.service.ironsession.config.IronSessionProperties;
import com.example.template.shared.keycloak.config.KeycloakProperties;
import com.example.template.shared.keycloak.model.CredentialRepresentation;
import com.example.template.shared.microservice.MicroserviceProperties;
import com.example.template.shared.model.JsonEntity;
import com.example.template.shared.test.RandomServerPortEnvInitializer;
import com.example.template.shared.test.TestUtilities;
import com.example.template.shared.test.keycloak.MockKeycloakServer;
import com.example.template.shared.test.keycloak.MockKeycloakServerEnvInitializer;
import com.example.template.shared.test.keycloak.RegisterUserData;
import java.util.List;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(
    initializers = {RandomServerPortEnvInitializer.class, MockKeycloakServerEnvInitializer.class})
class AccountControllerTests {
  @Autowired private WebClient.Builder webClientBuilder;
  @Autowired private KeycloakProperties keycloakProperties;
  @Autowired private AccountProfileRepository accountProfileRepository;
  @Autowired private MicroserviceProperties microserviceProperties;
  @Autowired private IronSessionProperties ironSessionProperties;
  @Autowired private IronSessionService ironSessionService;

  private MockKeycloakServer mockKeycloakServer;

  // Do not autowire to test controller's validation
  private AccountClientService accountClientService;

  @BeforeEach
  void init() throws Exception {
    mockKeycloakServer = new MockKeycloakServer(keycloakProperties);
    mockKeycloakServer.start();

    accountClientService = new AccountClientService(microserviceProperties, webClientBuilder);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockKeycloakServer.shutdown();
  }

  @Test
  void version() {
    var response = accountClientService.version();
    assertVersionResponse(response);
  }

  @Test
  void register() {
    var request = getTestRegisterRequest();
    var response = accountClientService.register(request);
    var userId = assertLoginResponse(response).userId();
    var profile = accountProfileRepository.findById(userId);
    assertTrue(profile.isPresent());
    profile.ifPresent(
        p -> {
          assertEquals(userId, p.getUserId());
          assertEquals(p.getCreatedAt(), p.getUpdatedAt());
          assertTrue(p.isSubscribe());
        });

    var users = mockKeycloakServer.getUsers();
    assertEquals(1, users.size());

    var user = users.values().stream().findFirst().orElseThrow();
    assertEquals(request.firstName(), user.getFirstName());
    assertEquals(request.lastName(), user.getLastName());
    assertEquals(request.email(), user.getUsername());
    assertEquals(request.email(), user.getEmail());

    var enabled = user.getEnabled();
    assertNotNull(enabled);
    assertTrue(enabled);

    var credential = new CredentialRepresentation();
    credential.setType("password");
    credential.setValue(request.password());
    credential.setTemporary(false);
    assertEquals(List.of(credential), user.getCredentials());
  }

  @Test
  void loginRefreshAndLogout() {
    var registerRequest = getTestRegisterRequest();
    registerUser(registerRequest);

    var loginRequest = new LoginRequest(registerRequest.email(), registerRequest.password(), false);
    var loginResponse = accountClientService.login(loginRequest);
    var loginData = assertLoginResponse(loginResponse);

    var contextRequest = new MockHttpServletRequest();
    contextRequest.setCookies(loginData.cookies());
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(contextRequest));
    var refreshResponse = accountClientService.refresh();
    var refreshData = assertLoginResponse(refreshResponse);
    assertNotEquals(loginData.token(), refreshData.token());
    assertNotEquals(
        loginData.sessionData().getRefreshToken(), refreshData.sessionData().getRefreshToken());

    contextRequest = new MockHttpServletRequest();
    contextRequest.setCookies(refreshData.cookies());
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(contextRequest));
    var logoutResponse = accountClientService.logout();
    assertLogoutResponse(logoutResponse);
    assertTrue(
        mockKeycloakServer
            .getRevokedTokens()
            .contains(refreshData.sessionData().getRefreshToken()));
  }

  private static RegisterUserRequest getTestRegisterRequest() {
    return new RegisterUserRequest("First", "Last", "user@test.com", "password1", true);
  }

  private void registerUser(RegisterUserRequest request) {
    mockKeycloakServer.registerUser(
        new RegisterUserData(
            request.email(), request.password(), request.firstName(), request.lastName()));
  }

  @SuppressWarnings("UnusedVariable")
  private static record LoginData(
      String userId, String token, Jwt jwt, SessionData sessionData, Cookie[] cookies) {}

  private LoginData assertLoginResponse(Mono<JsonEntity<TokenResponse>> responseMono) {
    var response = responseMono.block();
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var value = response.value();
    assertTrue(value.isPresent());

    var body = value.get();
    assertNotNull(body);

    var token = body.token();
    assertNotNull(token);

    var jwt = mockKeycloakServer.decodeJwt(token);
    assertJwt(jwt, false);

    var userId = jwt.getSubject();
    var servletResponse = TestUtilities.toMockHttpServletResponse(response.getHeaders());
    var cookies = servletResponse.getCookies();
    assertEquals(1, cookies.length);

    var cookie = (MockCookie) cookies[0];
    assertEquals(ironSessionProperties.getCookieName(), cookie.getName());
    assertEquals(ironSessionProperties.getTtl(), cookie.getMaxAge());
    assertNotNull(cookie.getValue());

    var servletRequest = new MockHttpServletRequest();
    servletRequest.setCookies(cookie);

    var sessionData =
        assertDoesNotThrow(() -> ironSessionService.load(servletRequest, SessionData.class));
    assertTrue(sessionData.isPresent());
    sessionData.ifPresent(
        data -> {
          var jwt2 = mockKeycloakServer.decodeJwt(data.getRefreshToken());
          assertJwt(jwt2, true);
          assertEquals(userId, jwt2.getSubject());
        });

    return new LoginData(userId, token, jwt, sessionData.get(), cookies);
  }

  private void assertLogoutResponse(Mono<JsonEntity<Void>> responseMono) {
    var response = responseMono.block();
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var value = response.value();
    assertTrue(value.isEmpty());

    var servletResponse = TestUtilities.toMockHttpServletResponse(response.getHeaders());
    var cookies = servletResponse.getCookies();
    assertEquals(1, cookies.length);

    var cookie = (MockCookie) cookies[0];
    assertEquals(ironSessionProperties.getCookieName(), cookie.getName());
    assertEquals(0, cookie.getMaxAge());
    assertNotNull(cookie.getValue());
  }

  private void assertJwt(Jwt jwt, boolean isRefresh) {
    assertEquals(isRefresh ? "Refresh" : "Bearer", jwt.getClaimAsString("typ"));
    assertEquals(keycloakProperties.getClient().getId(), jwt.getClaimAsString("azp"));
    assertNotNull(jwt.getSubject());
  }
}
