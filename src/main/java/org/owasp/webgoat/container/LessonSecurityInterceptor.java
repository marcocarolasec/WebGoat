/*
 * SPDX-FileCopyrightText: Copyright © 2016 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.container;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Prevents legacy lesson controllers from exposing intentionally unsafe server-side behavior. */
final class LessonSecurityInterceptor implements HandlerInterceptor {

  private static final String LESSON_PACKAGE = "org.owasp.webgoat.lessons.";

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");

    if (handler instanceof HandlerMethod handlerMethod
        && handlerMethod.getBeanType().getPackageName().startsWith(LESSON_PACKAGE)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Legacy vulnerable endpoint disabled");
      return false;
    }
    return true;
  }
}
