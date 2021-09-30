package com.example.template.account.microservice.model.entity;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
public class AccountProfile {
  @Id private String userId;

  @Setter(AccessLevel.NONE)
  @NotNull
  private LocalDateTime createdAt;

  @Setter(AccessLevel.NONE)
  @NotNull
  private LocalDateTime updatedAt;

  private boolean subscribe;

  @PrePersist
  protected void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  protected void preUpdate() {
    updatedAt = LocalDateTime.now();
    createdAt = updatedAt;
  }
}
