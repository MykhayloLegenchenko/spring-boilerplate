package com.example.template.account.microservice.service.ironsession;

import static com.example.template.shared.test.TestUtilities.assertEqualsAndSameClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.template.account.microservice.model.SessionData;
import com.example.template.account.microservice.service.ironsession.config.IronSessionProperties;
import com.example.template.account.microservice.service.ironsession.model.IronSessionException;
import com.example.template.account.microservice.service.ironsession.model.IronToken;
import com.example.template.shared.test.TestUtilities;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;

class IronSessionServiceTests {
  private IronSessionProperties properties;
  private IronSessionService service;

  @BeforeEach
  void init() throws Exception {
    properties = new IronSessionProperties();
    properties.setCookieName("template-account-session");
    properties.setPasswords("viPDq0urYq7Fz9h8swv2SNdh3eoXDxOG");

    service = new IronSessionService(properties);
  }

  @Test
  void unsealShouldReturnSameStringThatWasPassedToTheSeal() throws Exception {
    var value = "test data";
    var sealed = service.seal(value);

    var entry = properties.getCurrentPassword().orElse(null);
    assertNotNull(entry);
    properties.setPasswords(
        "{\"2\":\"82HJ2DMLbxJ45xY-MwWO8HgtB31ZPROs\",\""
            + entry.getKey()
            + "\":\""
            + entry.getValue()
            + "\"}");

    assertEquals(value, service.unseal(sealed, String.class));
  }

  @Test
  void unsealWithTokenExpirationForgeryShouldThrowException() {
    var sealed = service.seal("test data");
    var token = IronToken.parse(sealed);
    token.setExpiration(String.valueOf(Long.parseLong(token.getExpiration()) + 1));
    assertThrowsIronSessionException(
        "Bad hmac value", () -> service.unseal(token.getTokenString(), String.class));
  }

  @Test
  void unsealWithTokenEncryptedValueForgeryShouldThrowException() {
    var sealed = service.seal("test data");
    var token = IronToken.parse(sealed);

    var valueBytes = Base64.getUrlDecoder().decode(token.getEncryptedB64());
    valueBytes[0]++;
    token.setEncryptedB64(Base64.getEncoder().encodeToString(valueBytes));

    assertThrowsIronSessionException(
        "Bad hmac value", () -> service.unseal(token.getTokenString(), String.class));
  }

  @Test
  void unsealWithDifferentPasswordShouldThrowException() throws Exception {
    var sealed = service.seal("test data");
    properties.setPasswords("6wPOUxSDgCLUu0lg4E7F3tDpSKmvO8LX");
    assertThrowsIronSessionException("Bad hmac value", () -> service.unseal(sealed, String.class));
  }

  @Test
  void unsealShouldThrowExceptionWhenExpired() {
    properties.setTtl(-1 * IronSessionService.TIMESTAMP_SKEW_SEC);
    var sealed = service.seal("test data");
    assertThrowsIronSessionException("Expired seal", () -> service.unseal(sealed, String.class));
  }

  @Test
  void loadShouldReturnSameObjectThatWasSaved() {
    var data = new SessionData("refreshToken");

    var headers = service.save(data);
    assertNotNull(headers);

    var servletResponse = TestUtilities.toMockHttpServletResponse(headers);
    var cookies = servletResponse.getCookies();
    assertEquals(1, cookies.length);

    var cookie = (MockCookie) cookies[0];
    assertEquals(properties.getCookieName(), cookie.getName());
    assertEquals(properties.isHttpOnly(), cookie.isHttpOnly());
    assertEquals(properties.isSecure(), cookie.getSecure());
    assertEquals(properties.getSameSite(), cookie.getSameSite());
    assertEquals(properties.getPath(), cookie.getPath());
    assertEquals(properties.getTtl(), cookie.getMaxAge());
    assertNotNull(cookie.getValue());

    var servletRequest = new MockHttpServletRequest();
    servletRequest.setCookies(servletResponse.getCookies());
    assertEqualsAndSameClass(data, service.load(servletRequest, SessionData.class).orElse(null));
  }

  @Test
  void loadWithoutCookieShouldReturnEmpty() {
    var servletRequest = new MockHttpServletRequest();
    assertTrue(service.load(servletRequest, SessionData.class).isEmpty());
  }

  @Test
  void remove() {
    var headers = service.remove();
    assertNotNull(headers);

    var mockResponse = TestUtilities.toMockHttpServletResponse(headers);
    assertEquals(1, mockResponse.getCookies().length);

    var cookie = (MockCookie) mockResponse.getCookies()[0];
    assertEquals(properties.getCookieName(), cookie.getName());
    assertEquals(0, cookie.getMaxAge());
  }

  private static void assertThrowsIronSessionException(String message, Executable executable) {
    var ex = assertThrows(IronSessionException.class, executable);
    assertEquals(message, ex.getMessage());
  }
}
