package com.example.template.account.microservice.service.ironsession;

import com.example.template.account.microservice.service.ironsession.config.IronSessionProperties;
import com.example.template.account.microservice.service.ironsession.model.IronSessionException;
import com.example.template.account.microservice.service.ironsession.model.IronToken;
import com.example.template.shared.utils.CryptoUtilities;
import com.example.template.shared.utils.ObjectUtilities;
import com.example.template.shared.utils.StringUtilities;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

/**
 * Stateless session utility using signed and encrypted cookies to store data. Inspired by
 * next-iron-session (https://github.com/vvo/next-iron-session).
 */
@Service
public class IronSessionService {
  static final int TIMESTAMP_SKEW_SEC = 60;

  private static final String MAC_FORMAT_VERSION = "2";
  private static final String MAC_PREFIX = "Fe26." + MAC_FORMAT_VERSION;
  private static final int SALT_LENGTH = 32;
  private static final int IV_LENGTH = 16;

  private final IronSessionProperties properties;
  private final SecretKeyFactory keyFactory;

  public IronSessionService(IronSessionProperties properties) throws NoSuchAlgorithmException {
    this.properties = properties;
    keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
  }

  public <T> T unseal(@NonNull String sealed, Class<T> valueType) throws IronSessionException {
    var bytes = unsealBytes(sealed);

    try {
      var value = ObjectUtilities.fromBytes(bytes);
      if (!valueType.isInstance(value)) {
        throw new IronSessionException("Bad store class: " + value.getClass());
      }

      return valueType.cast(value);
    } catch (ClassNotFoundException | IOException ex) {
      throw new IronSessionException("Cannot deserialize store", ex);
    }
  }

  public String seal(@NonNull Serializable value) throws IronSessionException {
    byte[] bytes;
    try {
      bytes = ObjectUtilities.toBytes(value);
    } catch (IOException ex) {
      throw new IronSessionException("Cannot serialize store", ex);
    }

    return sealBytes(bytes);
  }

  public <T> Optional<T> load(HttpServletRequest request, Class<T> valueType)
      throws IronSessionException {
    var cookie = WebUtils.getCookie(request, properties.getCookieName());
    if (cookie == null) {
      return Optional.empty();
    }
    return Optional.of(unseal(cookie.getValue(), valueType));
  }

  public HttpHeaders save(@NonNull Serializable obj) throws IronSessionException {
    var sealed = seal(obj);
    return createCookieHeaders(sealed, properties.getTtl());
  }

  public HttpHeaders remove() {
    return createCookieHeaders("", 0);
  }

  private String sealBytes(byte[] unsealed) throws IronSessionException {
    // Create token
    var token = new IronToken();
    token.setMacPrefix(MAC_PREFIX);

    // Obtain password
    var passwordEntry =
        properties
            .getCurrentPassword()
            .orElseThrow(() -> new IronSessionException("Invalid password"));

    token.setPasswordId(passwordEntry.getKey());
    var password = passwordEntry.getValue().toCharArray();

    // Generate salt
    var random = CryptoUtilities.getRandom();
    var saltBytes = random.generateSeed(SALT_LENGTH);

    // Generate iv
    var ivBytes = random.generateSeed(IV_LENGTH);

    // Encrypt data
    token.setEncryptedB64(encode64Url(encrypt(createKey(password, saltBytes), ivBytes, unsealed)));

    // Construct MAC base string
    token.setEncryptionSalt(String.valueOf(Hex.encode(saltBytes)));
    token.setEncryptionIv(encode64Url(ivBytes));
    token.setExpiration(String.valueOf(System.currentTimeMillis() + properties.getTtl() * 1000L));
    var macBaseString = token.getMacBaseString();

    // Generate MAC salt
    var macSaltBytes = random.generateSeed(SALT_LENGTH);
    token.setHmacSalt(String.valueOf(Hex.encode(macSaltBytes)));

    // MAC the combined values
    token.setHmac(hmacWithKey(createKey(password, macSaltBytes), macBaseString));

    // Put it all together
    // prefix*[password-id]*encryption-salt*encryption-iv*encrypted*[expiration]*hmac-salt*hmac
    // Allowed URI query name/value characters: *-. \d \w
    return token.getTokenString();
  }

  private byte[] unsealBytes(String sealed) throws IronSessionException {
    // Parse token
    var token = IronToken.parse(sealed);

    // Check prefix
    if (!MAC_PREFIX.equals(token.getMacPrefix())) {
      throw new IronSessionException("Wrong mac prefix");
    }

    // Check expiration
    if (!token.getExpiration().isEmpty()) {
      if (!StringUtilities.isDigits(token.getExpiration())) {
        throw new IronSessionException("Invalid expiration");
      }

      long exp;
      try {
        exp = Long.parseLong(token.getExpiration());
      } catch (NumberFormatException ex) {
        throw new IronSessionException("Invalid expiration", ex);
      }

      if (exp <= (System.currentTimeMillis() - (TIMESTAMP_SKEW_SEC * 1000))) {
        throw new IronSessionException("Expired seal");
      }
    }

    // Check salts
    verifySalt(token.getHmacSalt());
    verifySalt(token.getEncryptionSalt());

    // Deserialize
    var password = properties.getPasswordsMap().get(token.getPasswordId());
    if (password == null) {
      throw new IronSessionException("Cannot find password: " + token.getPasswordId());
    }

    // Check HMAC
    var macBaseString = token.getMacBaseString();
    var mac = hmacWithKey(createKey(password, token.getHmacSalt()), macBaseString);

    if (!token.getHmac().equals(mac)) {
      throw new IronSessionException("Bad hmac value");
    }

    // Decrypt
    return decrypt(
        createKey(password, token.getEncryptionSalt()),
        token.getEncryptionIv(),
        token.getEncryptedB64());
  }

  private HttpHeaders createCookieHeaders(String value, int maxAge) {
    var cookie =
        ResponseCookie.from(properties.getCookieName(), value)
            .httpOnly(properties.isHttpOnly())
            .secure(properties.isSecure())
            .sameSite(properties.getSameSite())
            .path(properties.getPath())
            .maxAge(maxAge)
            .build();

    var headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, cookie.toString());

    return headers;
  }

  private static void verifySalt(String salt) throws IronSessionException {
    if (salt.length() != 2 * SALT_LENGTH || !StringUtilities.isHexLowercase(salt)) {
      throw new IronSessionException("Bad salt");
    }
  }

  private SecretKey createKey(String password, String salt) throws IronSessionException {
    return createKey(password.toCharArray(), Hex.decode(salt));
  }

  private SecretKey createKey(char[] password, byte[] salt) throws IronSessionException {
    var keySpec = new PBEKeySpec(password, salt, 1, 256);
    try {
      return keyFactory.generateSecret(keySpec);
    } catch (InvalidKeySpecException ex) {
      throw new IronSessionException("Cannot generate secret key", ex);
    }
  }

  private static String hmacWithKey(SecretKey key, String data) throws IronSessionException {
    try {
      var mac = Mac.getInstance("HmacSHA256");

      mac.init(key);

      var digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      var base64 = Base64.getEncoder().encodeToString(digest);

      return base64
          .replaceAll(Pattern.quote("+"), "-")
          .replaceAll(Pattern.quote("/"), "_")
          .replaceAll(Pattern.quote("="), "");
    } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
      throw new IronSessionException("Cannot calculate hmac", ex);
    }
  }

  private static String encode64Url(byte[] src) {
    return Base64.getUrlEncoder().encodeToString(src);
  }

  private static byte[] decode64Url(String src) throws IronSessionException {
    try {
      return Base64.getUrlDecoder().decode(src);
    } catch (IllegalArgumentException ex) {
      throw new IronSessionException("Cannot decode Base64Url", ex);
    }
  }

  private static byte[] doCipher(int opmode, SecretKey key, byte[] iv, byte[] data)
      throws IronSessionException {
    if (iv.length != IV_LENGTH) {
      throw new IronSessionException("Bad iv value");
    }

    try {
      var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      var paramSpec = new IvParameterSpec(iv);

      cipher.init(opmode, new SecretKeySpec(key.getEncoded(), "AES"), paramSpec);

      return cipher.doFinal(data);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | IllegalBlockSizeException
        | BadPaddingException ex) {
      throw new IronSessionException("Cannot cypher", ex);
    }
  }

  private static byte[] decrypt(SecretKey key, String iv, String data) throws IronSessionException {
    return doCipher(Cipher.DECRYPT_MODE, key, decode64Url(iv), decode64Url(data));
  }

  private static byte[] encrypt(SecretKey key, byte[] iv, byte[] data) throws IronSessionException {
    return doCipher(Cipher.ENCRYPT_MODE, key, iv, data);
  }
}
