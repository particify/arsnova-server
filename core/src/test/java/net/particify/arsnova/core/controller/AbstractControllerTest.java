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

package net.particify.arsnova.core.controller;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import net.particify.arsnova.core.config.AppConfig;
import net.particify.arsnova.core.config.TestAppConfig;
import net.particify.arsnova.core.config.TestPersistanceConfig;
import net.particify.arsnova.core.config.TestSecurityConfig;
import net.particify.arsnova.core.service.StubUserService;

@SpringJUnitWebConfig({
    AppConfig.class,
    TestAppConfig.class,
    TestPersistanceConfig.class,
    TestSecurityConfig.class})
@ActiveProfiles("test")
public abstract class AbstractControllerTest {

  @Autowired protected StubUserService userService;

  public AbstractControllerTest() {
    super();
  }

  protected void setAuthenticated(final boolean isAuthenticated, final String username) {
    final List<GrantedAuthority> ga = new ArrayList<>();
    if (isAuthenticated) {
      final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "secret", ga);
      SecurityContextHolder.getContext().setAuthentication(token);
      userService.setUserAuthenticated(isAuthenticated, username);
    } else {
      userService.setUserAuthenticated(isAuthenticated);
    }
  }

  @AfterEach
  public void cleanup() {
    SecurityContextHolder.clearContext();
    userService.setUserAuthenticated(false);
  }

}
