package com.example.template.shared.test.keycloak;

import com.example.template.shared.keycloak.config.KeycloakProperties;
import com.example.template.shared.keycloak.model.AccessTokenResponse;
import com.example.template.shared.keycloak.model.CredentialRepresentation;
import com.example.template.shared.keycloak.model.UserRepresentation;
import com.example.template.shared.model.ErrorResponse;
import com.example.template.shared.model.ModelUtilities;
import com.example.template.shared.utils.JsonUtilities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Slf4j
public class MockKeycloakServer {
  private final KeycloakProperties properties;
  private final String adminSub;
  private final RSAKey key;
  private final JWKSet jwkSet;
  private final JWSSigner signer;
  private final JwtDecoder decoder;

  @Getter private final Map<String, UserRepresentation> users;
  @Getter private final Set<String> revokedTokens;
  private final MockWebServer mockWebServer;

  public MockKeycloakServer(@NonNull KeycloakProperties properties) throws JOSEException {
    this.properties = properties;

    adminSub = ModelUtilities.generateKey();

    key = new RSAKeyGenerator(2048).keyID("1").generate();
    jwkSet = new JWKSet(key);
    signer = new RSASSASigner(key);
    decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();

    users = new ConcurrentHashMap<>();
    revokedTokens = ConcurrentHashMap.newKeySet();
    mockWebServer = new MockWebServer();
    mockWebServer.setDispatcher(new KeycloakDispatcher());
  }

  public void start() throws IOException, URISyntaxException {
    mockWebServer.start(new URI(properties.getUrl()).getPort());
  }

  public void shutdown() throws IOException {
    mockWebServer.shutdown();
  }

  public void registerUser(RegisterUserData data) {
    var user = new UserRepresentation();
    user.setId(ModelUtilities.generateKey());
    user.setEnabled(true);
    user.setUsername(data.email());
    user.setEmail(data.email());
    user.setFirstName(data.firstName());
    user.setLastName(data.lastName());

    var credential = new CredentialRepresentation();
    credential.setType("password");
    credential.setValue(data.password());
    credential.setTemporary(false);
    user.setCredentials(List.of(credential));
    users.put(user.getId(), user);
  }

  public Jwt decodeJwt(String token) throws JwtException {
    return decoder.decode(token);
  }

  public AccessTokenResponse userAccessToken(UserRepresentation user) throws JOSEException {
    return accessTokenResponse(
        "/auth/realms/" + properties.getRealm() + "/protocol/openid-connect/token",
        getClientId(),
        user);
  }

  private final class KeycloakDispatcher extends Dispatcher {
    @Override
    public MockResponse dispatch(RecordedRequest request) {
      MultiValueMap<String, String> formBody = null;
      var contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
      if ("application/x-www-form-urlencoded;charset=UTF-8".equals(contentType)) {
        var method = request.getMethod();
        if (!HttpMethod.POST.matches(method)) {
          return badMethod(request);
        }

        formBody = parseFormBody(request);
      }

      var path = request.getRequestUrl().encodedPath();
      if ("/auth/realms/master/protocol/openid-connect/token".equals(path)) {
        return login(request, formBody, true);
      }

      var adminBase = "/auth/admin/realms/" + properties.getRealm();
      if (path.startsWith(adminBase)) {
        var subPath = path.substring(adminBase.length());
        return adminDispatch(request, subPath);
      }

      var clientBase = "/auth/realms/" + properties.getRealm() + "/protocol/openid-connect";
      if (path.startsWith(clientBase)) {
        var subPath = path.substring(clientBase.length());
        return clientDispatch(request, formBody, subPath);
      }

      return notFound(request);
    }

    @SuppressWarnings("UnusedVariable")
    private record Result<T>(T value, MockResponse response) {
      boolean isError() {
        return response != null;
      }

      static <T> Result<T> ok(T value) {
        return new Result<>(value, null);
      }

      static <T> Result<T> error(MockResponse response) {
        return new Result<>(null, response);
      }

      static Result<UserRepresentation> parseBody(RecordedRequest request) {
        var contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (!MediaType.APPLICATION_JSON_VALUE.equals(contentType)) {
          return new Result<>(null, badContentType(request));
        }

        var body = request.getBody().snapshot().utf8();
        try {
          return new Result<>(JsonUtilities.parse(body, UserRepresentation.class), null);
        } catch (JsonProcessingException ex) {
          log.error("parse", ex);

          return error(
              jsonResponse(
                  HttpStatus.BAD_REQUEST, new ErrorResponse("Cannot create JWT tokens " + ex)));
        }
      }
    }

    private MockResponse adminDispatch(RecordedRequest request, String subPath) {
      var response = verifyAdminAuth(request);
      if (response != null) {
        return response;
      }

      if ("/users".equals(subPath)) {
        return adminUsers(request);
      }

      return notFound(request);
    }

    private MockResponse adminUsers(RecordedRequest request) {
      var method = request.getMethod();
      if (HttpMethod.POST.matches(method)) {
        var body = Result.parseBody(request);
        if (body.isError()) {
          return body.response();
        }

        var user = body.value();
        user.setId(ModelUtilities.generateKey());

        users.put(user.getId(), user);
        return ok();
      } else if (HttpMethod.GET.matches(method)) {
        var url = request.getRequestUrl();
        var username = url.queryParameter("username");
        var exact = url.queryParameter("exact");
        if (url.querySize() != 2
            || username == null
            || username.isEmpty()
            || !"true".equals(exact)) {
          return jsonResponse(
              HttpStatus.BAD_REQUEST, new ErrorResponse("Invalid query string: " + url.query()));
        }

        var user = findUser(u -> username.equals(u.getUsername()));
        return ok(user == null ? List.of() : List.of(user));
      }

      return badMethod(request);
    }

    private MockResponse clientDispatch(
        RecordedRequest request, MultiValueMap<String, String> formBody, String subPath) {
      if ("/certs".equals(subPath)) {
        return certs(request);
      }

      assert properties.getClient() != null : "Keycloak client properties is not set";
      if (formBody == null) {
        return badContentType(request);
      }

      var secret = getParam(formBody, "client_secret");
      if (!properties.getClient().getSecret().equals(secret)) {
        return unauthorized("Invalid client secret: " + secret);
      }

      if ("/token".equals(subPath)) {
        return login(request, formBody, false);
      } else if ("/revoke".equals(subPath)) {
        return revoke(formBody);
      }

      return notFound(request);
    }

    private MockResponse certs(RecordedRequest request) {
      var method = request.getMethod();
      if (!HttpMethod.GET.matches(method)) {
        return badMethod(request);
      }

      return ok(jwkSet.toJSONObject());
    }

    private MockResponse login(
        RecordedRequest request, MultiValueMap<String, String> formBody, boolean isAdmin) {
      var clientId = isAdmin ? "admin-cli" : getClientId();
      var response = verifyClientId(request, formBody, clientId);
      if (response != null) {
        return response;
      }

      Result<UserRepresentation> loginResult;
      var grantType = getParam(formBody, "grant_type");
      if ("password".equals(grantType)) {
        loginResult = passwordLogin(formBody, isAdmin);
      } else if ("refresh_token".equals(grantType)) {
        loginResult = refreshLogin(formBody, clientId, isAdmin);
      } else {
        return badParam(formBody, "grant_type");
      }

      if (loginResult.isError()) {
        return loginResult.response();
      }

      return accessToken(request, clientId, loginResult.value());
    }

    private MockResponse revoke(MultiValueMap<String, String> formBody) {
      var tokenTypeHint = getParam(formBody, "token_type_hint");
      if (!"refresh_token".equals(tokenTypeHint)) {
        return badParam(formBody, "token_type_hint");
      }
      var token = getParam(formBody, "token");
      if (token == null) {
        return badParam(formBody, "token");
      }

      var parseResult = parseAndCheckJwt(token, getClientId(), true);
      if (parseResult.isError()) {
        return parseResult.response();
      }

      revokedTokens.add(token);
      return ok();
    }

    private Result<UserRepresentation> passwordLogin(
        MultiValueMap<String, String> formBody, boolean isAdmin) {
      assert properties.getAdmin() != null : "Keycloak admin properties is not set";
      var username = getParam(formBody, "username");
      var password = getParam(formBody, "password");
      if (isAdmin) {
        if (!Objects.equals(properties.getAdmin().getUsername(), username)) {
          return Result.error(badParam(formBody, "username"));
        }

        if (!Objects.equals(properties.getAdmin().getPassword(), password)) {
          return Result.error(badParam(formBody, "password"));
        }

        return Result.ok(null);
      }

      return clientPasswordLogin(formBody, username, password);
    }

    private Result<UserRepresentation> clientPasswordLogin(
        MultiValueMap<String, String> formBody, String username, String password) {
      UserRepresentation user =
          findUser(
              u ->
                  Boolean.TRUE.equals(u.getEnabled()) && Objects.equals(u.getUsername(), username));
      if (user == null) {
        return Result.error(badParam(formBody, "username"));
      }

      boolean success = false;
      if (user.getCredentials() != null) {
        for (var cred : user.getCredentials()) {
          if ("password".equals(cred.getType()) && Objects.equals(cred.getValue(), password)) {
            success = true;
            break;
          }
        }
      }

      return success ? Result.ok(user) : Result.error(badParam(formBody, "password"));
    }

    private Result<UserRepresentation> refreshLogin(
        MultiValueMap<String, String> formBody, String clientId, boolean isAdmin) {

      var refreshToken = getParam(formBody, "refresh_token");
      if (refreshToken == null) {
        return Result.error(badParam(formBody, "refresh_token"));
      }

      var parseResult = parseAndCheckJwt(refreshToken, clientId, true);
      if (parseResult.isError()) {
        return Result.error(parseResult.response());
      }

      var sub = parseResult.value().getSubject();
      UserRepresentation user = null;
      boolean success;
      if (isAdmin) {
        success = adminSub.equals(sub);
      } else {
        user = findUser(u -> Boolean.TRUE.equals(u.getEnabled()) && Objects.equals(u.getId(), sub));
        success = user != null;
      }

      return success
          ? Result.ok(user)
          : Result.error(unauthorized("Invalid token subject: " + sub));
    }

    private UserRepresentation findUser(Predicate<UserRepresentation> predicate) {
      return users.values().stream().filter(predicate).findFirst().orElse(null);
    }

    private static MockResponse verifyClientId(
        RecordedRequest request, MultiValueMap<String, String> formBody, String clientId) {
      var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (authorization == null) {
        if (!clientId.equals(getParam(formBody, "client_id"))) {
          return badParam(formBody, "client_id");
        }
      } else {
        var expected =
            "Basic "
                + Base64.getEncoder()
                    .encodeToString((clientId + ':').getBytes(StandardCharsets.UTF_8));

        if (!expected.equals(authorization)) {
          return jsonResponse(
              HttpStatus.BAD_REQUEST,
              new ErrorResponse("Bad authorization header: " + authorization));
        }
      }

      return null;
    }

    private Result<Jwt> parseAndCheckJwt(String token, String clientId, boolean isRefresh) {
      Jwt jwt;
      try {
        jwt = decodeJwt(token);
      } catch (JwtException ex) {
        return Result.error(unauthorized("Cannot decode token: " + ex));
      }

      if (isRefresh && revokedTokens.contains(token)) {
        unauthorized("Revoked token " + token);
      }

      var typ = jwt.getClaimAsString("typ");
      if (!(isRefresh ? "Refresh" : "Bearer").equals(typ)) {
        return Result.error(badClaim("typ", typ));
      }

      var azp = jwt.getClaimAsString("azp");
      if (!clientId.equals(azp)) {
        return Result.error(badClaim("azp", azp));
      }

      var sub = jwt.getSubject();
      if (sub == null) {
        return Result.error(badClaim("sub", "null"));
      }

      return Result.ok(jwt);
    }

    private MockResponse verifyAdminAuth(RecordedRequest request) {
      var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (authorization == null || !authorization.startsWith("Bearer ")) {
        return unauthorized("Bad authorization header: " + authorization);
      }

      var token = authorization.substring(7);
      var parseResult = parseAndCheckJwt(token, "admin-cli", false);
      if (parseResult.isError()) {
        return parseResult.response();
      }

      var sub = parseResult.value().getSubject();
      if (!adminSub.equals(sub)) {
        return badClaim("sub", sub);
      }

      return null;
    }

    private static String getParam(MultiValueMap<String, String> params, String name) {
      var values = params.get(name);
      return values == null || values.size() != 1 ? null : values.get(0);
    }

    private static MultiValueMap<String, String> parseFormBody(RecordedRequest request) {
      var body = request.getBody().snapshot().utf8();
      String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
      MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
      for (String pair : pairs) {
        int idx = pair.indexOf('=');
        if (idx == -1) {
          result.add(URLDecoder.decode(pair, StandardCharsets.UTF_8), null);
        } else {
          String name = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
          String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
          result.add(name, value);
        }
      }

      return result;
    }

    private static MockResponse ok() {
      return new MockResponse().setStatus("HTTP/1.1 " + HttpStatus.OK);
    }

    private static MockResponse ok(Object body) {
      return jsonResponse(HttpStatus.OK, body);
    }

    private MockResponse accessToken(
        RecordedRequest request, String clientId, UserRepresentation user) {
      try {
        var response = accessTokenResponse(request.getPath(), clientId, user);
        return ok(response);
      } catch (JOSEException ex) {
        log.error("adminLogin", ex);

        return jsonResponse(
            HttpStatus.BAD_REQUEST, new ErrorResponse("Cannot create JWT tokens " + ex));
      }
    }

    private static MockResponse badMethod(RecordedRequest request) {
      var method = request.getMethod();
      return jsonResponse(
          HttpStatus.METHOD_NOT_ALLOWED, new ErrorResponse("Incorrect request method: " + method));
    }

    private static MockResponse badContentType(RecordedRequest request) {
      return jsonResponse(
          HttpStatus.BAD_REQUEST,
          new ErrorResponse(
              "Incorrect content type: " + request.getHeader(HttpHeaders.CONTENT_TYPE)));
    }

    private static MockResponse notFound(RecordedRequest request) {
      return jsonResponse(
          HttpStatus.NOT_FOUND, new ErrorResponse("Bad path: " + request.getPath()));
    }

    private static MockResponse badParam(MultiValueMap<String, String> params, String name) {
      return jsonResponse(
          HttpStatus.BAD_REQUEST,
          new ErrorResponse("Bad param \"" + name + "\": " + params.get(name)));
    }

    private static MockResponse unauthorized(String error) {
      return jsonResponse(HttpStatus.UNAUTHORIZED, new ErrorResponse(error));
    }

    private static MockResponse badClaim(String name, String value) {
      return unauthorized("Invalid claim " + name + ": " + value);
    }

    private static MockResponse jsonResponse(HttpStatus status, Object body) {
      var response = new MockResponse().setStatus("HTTP/1.1 " + status);
      try {
        return response
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(JsonUtilities.toJson(body));
      } catch (JsonProcessingException ex) {
        log.error("jsonResponse", ex);

        return response
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
            .setBody(ex.toString());
      }
    }
  }

  private String getClientId() {
    var client = properties.getClient();
    return client == null ? "test" : client.getId();
  }

  private AccessTokenResponse accessTokenResponse(
      String path, String clientId, UserRepresentation user) throws JOSEException {
    var sub = user == null ? adminSub : user.getId();

    String token;
    var claims = new LinkedHashMap<String, Object>();
    claims.put("iss", path);
    claims.put("sub", sub);
    claims.put("typ", "Bearer");
    claims.put("azp", clientId);
    claims.put("acr", "1");

    var roles = new HashMap<>(1);
    roles.put("roles", Collections.singletonList("USER"));
    claims.put("realm_access", roles);

    claims.put("scope", "email profile");
    token = createSignedJwt(claims, 60);

    claims = new LinkedHashMap<>();
    claims.put("iss", path);
    claims.put("aud", path);
    claims.put("sub", sub);
    claims.put("typ", "Refresh");
    claims.put("azp", clientId);
    claims.put("scope", "email profile");
    var refreshToken = createSignedJwt(claims, 1800);

    var response = new AccessTokenResponse();
    response.setToken(token);
    response.setExpiresIn(60);
    response.setRefreshExpiresIn(1800);
    response.setRefreshToken(refreshToken);
    response.setTokenType("Bearer");
    response.setNotBeforePolicy(0);
    response.setScope("email profile");

    return response;
  }

  private String createSignedJwt(Map<String, Object> claims, int expiresIn) throws JOSEException {
    var now = System.currentTimeMillis() / 1000L + 1;
    var claimsSet = new JWTClaimsSet.Builder();
    claimsSet.claim("exp", now + expiresIn);
    claimsSet.claim("iat", now);
    claimsSet.claim("jti", ModelUtilities.generateKey());
    for (var e : claims.entrySet()) {
      claimsSet.claim(e.getKey(), e.getValue());
    }

    var signedJwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
            claimsSet.build());
    signedJwt.sign(signer);

    return signedJwt.serialize();
  }
}
