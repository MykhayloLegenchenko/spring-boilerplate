package com.example.template.account.microservice.service.ironsession.model;

import java.io.Serial;

public class IronSessionException extends RuntimeException {
  @Serial private static final long serialVersionUID = 2367029606017949425L;

  public IronSessionException(String message) {
    super(message);
  }

  public IronSessionException(String message, Throwable cause) {
    super(message, cause);
  }
}
