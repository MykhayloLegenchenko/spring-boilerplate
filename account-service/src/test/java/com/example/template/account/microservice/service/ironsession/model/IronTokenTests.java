package com.example.template.account.microservice.service.ironsession.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IronTokenTests {

  @Test
  void test() {
    var token = new IronToken();
    token.setMacPrefix("macPrefix");
    token.setPasswordId("passwordId");
    token.setEncryptionSalt("encryptionSalt");
    token.setEncryptionIv("encryptionIv");
    token.setEncryptedB64("encryptedB64");
    token.setExpiration("expiration");
    token.setHmacSalt("hmacSalt");
    token.setHmac("hmac");

    assertEquals(
        "macPrefix*passwordId*encryptionSalt*encryptionIv*encryptedB64*expiration",
        token.getMacBaseString());

    var tokenStr = token.getTokenString();
    assertEquals(
        "macPrefix*passwordId*encryptionSalt*encryptionIv*encryptedB64*expiration*hmacSalt*hmac",
        tokenStr);

    assertEquals(token, IronToken.parse(tokenStr));
  }
}
