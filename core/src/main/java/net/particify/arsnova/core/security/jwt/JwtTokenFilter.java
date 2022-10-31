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

package net.particify.arsnova.core.security.jwt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import net.particify.arsnova.core.security.LoginAuthenticationSucessHandler;

@Component
public class JwtTokenFilter extends GenericFilterBean {
  private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("Bearer (.*)", Pattern.CASE_INSENSITIVE);
  private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);
  private JwtAuthenticationProvider jwtAuthenticationProvider;

  @Override
  public void doFilter(final ServletRequest servletRequest,
      final ServletResponse servletResponse,
      final FilterChain filterChain)
      throws IOException, ServletException {
    final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    JwtToken token = null;
    final String jwtHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
    if (jwtHeader != null) {
      final Matcher tokenMatcher = BEARER_TOKEN_PATTERN.matcher(jwtHeader);
      if (tokenMatcher.matches()) {
        token = new JwtToken(tokenMatcher.group(1));
      } else {
        logger.debug("Unsupported authentication scheme.");
      }
    } else {
      logger.debug("No authentication header present.");
      /* Look for auth cookie if Authorization header is not present. */
      if (httpServletRequest.getCookies() != null) {
        final Optional<Cookie> cookie = Arrays.stream(httpServletRequest.getCookies())
            .filter(c -> c.getName().equalsIgnoreCase(LoginAuthenticationSucessHandler.AUTH_COOKIE_NAME))
            .findFirst();
        if (cookie.isPresent()) {
          logger.debug("Trying to use authentication from cookie.");
          token = new JwtToken(cookie.get().getValue());
        }
      }
    }

    if (token != null) {
      try {
        final Authentication authenticatedToken = jwtAuthenticationProvider.authenticate(token);
        if (authenticatedToken != null) {
          logger.debug("Storing JWT to SecurityContext: {}", authenticatedToken);
          SecurityContextHolder.getContext().setAuthentication(authenticatedToken);
        } else {
          logger.debug("Could not authenticate JWT.");
        }
      } catch (final Exception e) {
        logger.debug("JWT authentication failed", e);
      }
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Autowired
  public void setJwtAuthenticationProvider(final JwtAuthenticationProvider jwtAuthenticationProvider) {
    this.jwtAuthenticationProvider = jwtAuthenticationProvider;
  }
}
