package com.example.template.account.microservice.service.ironsession.model;

import java.util.regex.Pattern;
import lombok.Data;

@Data
public class IronToken {
  private String macPrefix;
  private String passwordId;
  private String encryptionSalt;
  private String encryptionIv;
  private String encryptedB64;
  private String expiration;
  private String hmacSalt;
  private String hmac;

  public static IronToken parse(String token) throws IronSessionException {
    var parts = token.split(Pattern.quote("*"), -1);
    if (parts.length != 8) {
      throw new IronSessionException("Incorrect number of sealed components");
    }

    var result = new IronToken();
    result.setMacPrefix(parts[0]);
    result.setPasswordId(parts[1]);
    result.setEncryptionSalt(parts[2]);
    result.setEncryptionIv(parts[3]);
    result.setEncryptedB64(parts[4]);
    result.setExpiration(parts[5]);
    result.setHmacSalt(parts[6]);
    result.setHmac(parts[7]);

    return result;
  }

  public String getTokenString() {
    return getMacBaseString() + '*' + hmacSalt + '*' + hmac;
  }

  public String getMacBaseString() {
    return macPrefix
        + '*'
        + passwordId
        + '*'
        + encryptionSalt
        + '*'
        + encryptionIv
        + '*'
        + encryptedB64
        + '*'
        + expiration;
  }
}
