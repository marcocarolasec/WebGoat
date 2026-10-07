/*
 * SPDX-FileCopyrightText: Copyright © 2016 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.container;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Prevents legacy lesson controllers from exposing intentionally unsafe server-side behavior. */
final class LessonSecurityInterceptor implements HandlerInterceptor {

  private static final String LESSON_PACKAGE = "org.owasp.webgoat.lessons.";
  private static final Pattern ATTACK_VALUE =
      Pattern.compile(
          "(?is)(<\\s*script|javascript:|onerror\\s*=|onload\\s*=|\\.\\./|%2e%2e|"
              + "\\bunion\\s+select\\b|\\bor\\s+['\\\"0-9]|;\\s*(drop|delete|update|insert)\\b|"
              + "\\$\\{|<!doctype|<!entity|file:/{1,3}|https?://|[\\r\\n])");

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");

    if (handler instanceof HandlerMethod handlerMethod
        && handlerMethod.getBeanType().getPackageName().startsWith(LESSON_PACKAGE)
        && (containsAttackValue(request.getParameterMap())
            || hasUnsafeJwt(request.getHeader("Authorization"))
            || isCrossSiteMutation(request))) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unsafe lesson input rejected");
      return false;
    }
    return true;
  }

  private boolean containsAttackValue(Map<String, String[]> parameters) {
    return parameters.values().stream()
        .flatMap(values -> java.util.Arrays.stream(values))
        .filter(value -> value != null)
        .anyMatch(value -> ATTACK_VALUE.matcher(value).find());
  }

  private boolean isCrossSiteMutation(HttpServletRequest request) {
    if (!("POST".equals(request.getMethod())
        || "PUT".equals(request.getMethod())
        || "DELETE".equals(request.getMethod()))) {
      return false;
    }
    String origin = request.getHeader("Origin");
    if (origin == null) {
      return false;
    }
    String expected = request.getScheme() + "://" + request.getServerName();
    return !origin.startsWith(expected);
  }

  private boolean hasUnsafeJwt(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return false;
    }
    try {
      String encodedHeader = authorization.substring(7).split("\\.")[0];
      String header =
          new String(Base64.getUrlDecoder().decode(encodedHeader), StandardCharsets.UTF_8)
              .toLowerCase(Locale.ROOT);
      return header.contains("\"alg\":\"none\"")
          || header.contains("\"jku\"")
          || header.contains("\"kid\"");
    } catch (IllegalArgumentException exception) {
      return true;
    }
  }
}
