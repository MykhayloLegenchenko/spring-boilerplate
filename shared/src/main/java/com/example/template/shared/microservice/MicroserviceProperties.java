package com.example.template.shared.microservice;

import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties("microservice")
@Data
@Validated
public class MicroserviceProperties {
  private Map<@NotBlank String, @Valid DependencyDefinition> dependencies;

  public DependencyDefinition getDependency(String name) {
    var dependency = dependencies == null ? null : dependencies.get(name);
    assert dependency != null : "Dependency \"" + name + "\" not found";

    return dependency;
  }

  @Data
  public static class DependencyDefinition {
    private @NotNull @URL String url;
    private Map<@NotBlank String, @NotBlank String> attributes;
  }
}
