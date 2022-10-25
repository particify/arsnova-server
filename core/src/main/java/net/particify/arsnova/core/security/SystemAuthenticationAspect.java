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

package net.particify.arsnova.core.security;

import java.util.HashSet;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import net.particify.arsnova.core.service.UserService;

/**
 * This aspect populates the SecurityContextHolder of Spring Security when
 * scheduled methods are executed. It allows the scheduled method to access
 * service methods which are secured by Spring Security annotations.
 *
 * @author Daniel Gerhardt
 */
@Aspect
@Configurable
public class SystemAuthenticationAspect {
  private static final Logger logger = LoggerFactory.getLogger(SystemAuthenticationAspect.class);
  private static final GrantedAuthority SYSTEM_AUTHORITY = new SimpleGrantedAuthority("ROLE_SYSTEM");

  private UserService userService;

  @Around("execution(void net.particify.arsnova.core..*(..))"
      + " && @annotation(org.springframework.scheduling.annotation.Scheduled)")
  public <T> void handleWebsocketAuthentication(final ProceedingJoinPoint pjp) throws Throwable {
    logger.trace("Executing SystemAuthenticationAspect for @Scheduled method {}.",
        pjp.toShortString());
    try {
      populateSecurityContext();
      pjp.proceed();
    } finally {
      clearSecurityContext();
    }
  }

  private void populateSecurityContext() {
    final SecurityContext context = SecurityContextHolder.getContext();
    final Set<GrantedAuthority> authorities = new HashSet<>();
    authorities.add(SYSTEM_AUTHORITY);
    final Authentication auth = new UsernamePasswordAuthenticationToken(
        SYSTEM_AUTHORITY.toString(), SYSTEM_AUTHORITY.toString(), authorities);
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
  }

  private void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }
}
