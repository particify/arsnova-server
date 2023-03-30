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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apereo.cas.client.validation.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import net.particify.arsnova.core.event.BeforeUserProfileAutoCreationEvent;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.service.UserService;

/**
 * Class to load a user based on the results from CAS.
 */
public class CasUserDetailsService
    extends AbstractCasAssertionUserDetailsService
    implements ApplicationEventPublisherAware {
  public static final GrantedAuthority ROLE_CAS_USER = new SimpleGrantedAuthority("ROLE_CAS_USER");

  private final Collection<GrantedAuthority> defaultGrantedAuthorities = Set.of(
      User.ROLE_USER,
      ROLE_CAS_USER
  );

  private UserService userService;
  private ApplicationEventPublisher applicationEventPublisher;

  @Override
  @Autowired
  public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Autowired
  public void setUserService(final UserService userService) {
    this.userService = userService;
  }

  @Override
  protected UserDetails loadUserDetails(final Assertion assertion) {
    final String uid = assertion.getPrincipal().getName();
    final Set<GrantedAuthority> grantedAuthorities = new HashSet<>(defaultGrantedAuthorities);
    if (userService.isAdmin(uid, UserProfile.AuthProvider.CAS)) {
      grantedAuthorities.add(User.ROLE_ADMIN);
    }

    final Optional<UserProfile> userProfile =
        Optional.ofNullable(
            userService.getByAuthProviderAndLoginId(UserProfile.AuthProvider.CAS, assertion.getPrincipal().getName()));
    return new User(
        userProfile.orElseGet(() -> {
          final UserProfile newUserProfile =
              new UserProfile(UserProfile.AuthProvider.CAS, assertion.getPrincipal().getName());
          applicationEventPublisher.publishEvent(
              new BeforeUserProfileAutoCreationEvent(this, newUserProfile, Collections.emptyMap()));
          return userService.create(newUserProfile);
        }),
        grantedAuthorities);
  }
}
