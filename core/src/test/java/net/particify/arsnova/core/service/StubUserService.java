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

package net.particify.arsnova.core.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.config.properties.AuthenticationProviderProperties;
import net.particify.arsnova.core.config.properties.SecurityProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.persistence.UserRepository;
import net.particify.arsnova.core.security.PasswordUtils;
import net.particify.arsnova.core.security.User;

public class StubUserService extends UserServiceImpl {
  private final Set<GrantedAuthority> grantedAuthorities;
  private User stubUser = null;

  public StubUserService(
      final UserRepository repository,
      final SystemProperties systemProperties,
      final SecurityProperties securityProperties,
      final AuthenticationProviderProperties authenticationProviderProperties,
      final EmailService emailService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator,
      final PasswordUtils passwordUtils) {
    super(repository, systemProperties, securityProperties, authenticationProviderProperties,
        emailService, jackson2HttpMessageConverter, validator, passwordUtils);
    grantedAuthorities = new HashSet<>();
    grantedAuthorities.add(User.ROLE_USER);
  }

  public void setUserAuthenticated(final boolean isAuthenticated) {
    this.setUserAuthenticated(isAuthenticated, "ptsr00");
  }

  public void setUserAuthenticated(final boolean isAuthenticated, final String username) {
    setUserAuthenticated(isAuthenticated, username, "");
  }

  public void setUserAuthenticated(final boolean isAuthenticated, final String username, final String userId) {
    if (isAuthenticated) {
      final UserProfile userProfile = new UserProfile(UserProfile.AuthProvider.ARSNOVA, username);
      if (userId == null || userId.isEmpty()) {
        userProfile.setId(UUID.randomUUID().toString());
      } else {
        userProfile.setId(userId);
      }
      stubUser = new User(userProfile, grantedAuthorities);
    } else {
      stubUser = null;
    }
  }

  @Override
  public User getCurrentUser() {
    return stubUser;
  }
}
