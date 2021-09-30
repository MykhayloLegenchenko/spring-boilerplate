package com.example.template.account.microservice.service.ironsession.config;

import com.example.template.shared.model.LinkedStringMap;
import com.example.template.shared.utils.JsonUtilities;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties("iron-session")
@Data
@Validated
public class IronSessionProperties {
  @NotBlank private String cookieName;

  @NotNull
  @Size(min = 1)
  private Map<@NotBlank String, @NotNull @Size(min = 32) String> passwordsMap;

  private boolean httpOnly = true;
  private boolean secure = true;
  @NotBlank private String sameSite = "lax";
  @NotBlank private String path = "/";

  @Min(0)
  private int ttl = 15 * 24 * 3600;

  public Optional<Map.Entry<String, String>> getCurrentPassword() {
    if (passwordsMap == null || passwordsMap.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(passwordsMap.entrySet().iterator().next());
  }

  public void setPasswords(@NonNull String passwords) throws JsonProcessingException {
    if (!passwords.isEmpty() && passwords.charAt(0) == '{') {
      passwordsMap = JsonUtilities.parse(passwords, LinkedStringMap.class);
    } else {
      passwordsMap = new LinkedHashMap<>();
      passwordsMap.put("1", passwords);
    }
  }
}
