package com.example.template.shared.utils;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class WebUtilities {
  private WebUtilities() {}

  public static HttpServletRequest getServletRequest() {
    var attrs = RequestContextHolder.getRequestAttributes();
    if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
      throw new IllegalStateException("Cannot obtain ServletRequestAttributes");
    }

    return servletAttrs.getRequest();
  }
}
