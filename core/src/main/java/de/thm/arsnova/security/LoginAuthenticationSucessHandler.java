/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.security;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.security.jwt.JwtService;

/**
 * This class gets called when a user successfully logged in.
 */
public class LoginAuthenticationSucessHandler extends
    SimpleUrlAuthenticationSuccessHandler {
  public static final String AUTH_COOKIE_NAME = "auth";
  public static final String URL_ATTRIBUTE = "ars-login-success-url";

  private JwtService jwtService;
  private String targetUrl;
  private String apiPath;

  public LoginAuthenticationSucessHandler(
      final SystemProperties systemProperties,
      final ServletContext servletContext) {
    final String proxyPath = systemProperties.getApi().getProxyPath();
    this.apiPath = proxyPath != null && !proxyPath.isEmpty() ? proxyPath : servletContext.getContextPath();
  }

  @Autowired
  public void setJwtService(final JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected String determineTargetUrl(
      final HttpServletRequest request,
      final HttpServletResponse response) {
    final HttpSession session = request.getSession(false);
    final String url = (String) session.getAttribute(URL_ATTRIBUTE);
    session.removeAttribute(URL_ATTRIBUTE);

    return url;
  }

  @Override
  public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
      final Authentication authentication) throws IOException, ServletException {
    final HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute(URL_ATTRIBUTE) == null) {
      final String token = jwtService.createSignedToken((User) authentication.getPrincipal(), true);
      final Cookie cookie = new Cookie(AUTH_COOKIE_NAME, token);
      cookie.setPath(apiPath);
      cookie.setSecure(request.isSecure());
      cookie.setHttpOnly(true);
      response.addCookie(cookie);
      response.setContentType(MediaType.TEXT_HTML_VALUE);
      response.getWriter().println("<!DOCTYPE html><script>if (window.opener) window.close()</script>");

      return;
    }
    super.onAuthenticationSuccess(request, response, authentication);
  }

  public void setTargetUrl(final String url) {
    targetUrl = url;
  }
}
