package com.example.template.account.microservice.controller;

import com.example.template.account.contract.AccountContract;
import com.example.template.account.contract.model.LoginRequest;
import com.example.template.account.contract.model.RegisterUserRequest;
import com.example.template.account.contract.model.TokenResponse;
import com.example.template.account.microservice.model.SessionData;
import com.example.template.account.microservice.model.entity.AccountProfile;
import com.example.template.account.microservice.service.ironsession.IronSessionService;
import com.example.template.account.microservice.service.ironsession.model.IronSessionException;
import com.example.template.shared.keycloak.model.AccessTokenResponse;
import com.example.template.shared.keycloak.model.CredentialRepresentation;
import com.example.template.shared.keycloak.model.GetUsersRequest;
import com.example.template.shared.keycloak.model.UserRepresentation;
import com.example.template.shared.keycloak.service.KeycloakAdminService;
import com.example.template.shared.keycloak.service.KeycloakClientService;
import com.example.template.shared.model.JsonEntity;
import com.example.template.shared.model.JsonEntityFactory;
import com.example.template.shared.utils.BeanUtilities;
import com.example.template.shared.utils.WebUtilities;
import java.util.List;
import javax.persistence.EntityManager;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController implements AccountContract {
  private final TransactionTemplate transactionTemplate;
  private final EntityManager em;
  private final KeycloakAdminService keycloakAdminService;
  private final KeycloakClientService keycloakClientService;
  private final IronSessionService ironSessionService;

  public AccountController(
      PlatformTransactionManager transactionManager,
      EntityManager em,
      KeycloakAdminService keycloakAdminService,
      KeycloakClientService keycloakClientService,
      IronSessionService ironSessionService) {

    transactionTemplate = new TransactionTemplate(transactionManager);

    this.em = em;
    this.keycloakAdminService = keycloakAdminService;
    this.keycloakClientService = keycloakClientService;
    this.ironSessionService = ironSessionService;
  }

  @Override
  @PostMapping("/version")
  public JsonEntity<String> version() {
    log.info("version");

    return JsonEntityFactory.ok("0.0.1-SNAPSHOT");
  }

  @Override
  @PostMapping("/register")
  public JsonEntity<TokenResponse> register(@Valid @RequestBody RegisterUserRequest request) {

    log.info("registerUser {}", request);

    request = BeanUtilities.trimRecord(request);
    var user = creatUserRepresentation(request);
    var registerResponse = keycloakAdminService.registerUser(user).block();
    assert registerResponse != null;
    if (registerResponse.hasError()) {
      return JsonEntityFactory.error(registerResponse);
    }

    // Find registered user by username
    var userRequest = new GetUsersRequest();
    userRequest.setUsername(request.email());
    userRequest.setExact(true);
    var userResponse = keycloakAdminService.getUsers(userRequest).block();
    assert userResponse != null;
    if (userResponse.hasError()) {
      return JsonEntityFactory.badGateway();
    }

    var users = userResponse.value().orElse(null);
    if (users == null || users.size() != 1) {
      return JsonEntityFactory.badGateway();
    }

    // Create account profile
    var keycloakUser = users.get(0);
    var profile = new AccountProfile();
    profile.setUserId(keycloakUser.getId());
    profile.setSubscribe(request.subscribe());
    transactionTemplate.executeWithoutResult(status -> em.persist(profile));

    // Log new user in
    var loginRequest = new LoginRequest(request.email(), request.password(), false);
    return login(loginRequest);
  }

  @Override
  @PostMapping("/login")
  public JsonEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("login {}", request);

    request = BeanUtilities.trimRecord(request);
    return makeTokenResponse(
        keycloakClientService.login(request.email(), request.password(), request.rememberMe()));
  }

  @Override
  @PostMapping("/refresh")
  public JsonEntity<TokenResponse> refresh() {
    log.info("refresh");

    var sessionData = ironSessionService.load(WebUtilities.getServletRequest(), SessionData.class);
    if (sessionData.isEmpty()) {
      return JsonEntityFactory.error(HttpStatus.UNAUTHORIZED);
    }

    return makeTokenResponse(keycloakClientService.refresh(sessionData.get().getRefreshToken()));
  }

  @Override
  @PostMapping("/logout")
  public JsonEntity<Void> logout() {
    log.info("logout");

    try {
      var sessionData =
          ironSessionService.load(WebUtilities.getServletRequest(), SessionData.class);
      sessionData.ifPresent(
          data -> keycloakClientService.revoke(data.getRefreshToken()).subscribe());
    } catch (IronSessionException ex) {
      log.warn("logout", ex);
    }

    var headers = ironSessionService.remove();
    return JsonEntityFactory.ok(headers);
  }

  @Override
  @PostMapping("/update")
  public JsonEntity<Void> update() {
    log.info("update");

    return JsonEntityFactory.ok();
  }

  private JsonEntity<TokenResponse> makeTokenResponse(
      Mono<JsonEntity<AccessTokenResponse>> responseMono) {

    var tokenResponse = responseMono.block();
    assert tokenResponse != null;
    if (tokenResponse.hasError()) {
      tokenResponse.getHeaders().addAll(ironSessionService.remove());
      return JsonEntityFactory.error(tokenResponse);
    }

    var tokens = tokenResponse.get();
    var headers = ironSessionService.save(new SessionData(tokens.getRefreshToken()));

    return JsonEntityFactory.ok(new TokenResponse(tokens.getToken()), headers);
  }

  private static UserRepresentation creatUserRepresentation(RegisterUserRequest request) {
    var user = new UserRepresentation();
    user.setEnabled(true);
    user.setUsername(request.email());
    user.setEmail(request.email());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());

    var credential = new CredentialRepresentation();
    credential.setType("password");
    credential.setValue(request.password());
    credential.setTemporary(false);
    user.setCredentials(List.of(credential));

    return user;
  }
}
