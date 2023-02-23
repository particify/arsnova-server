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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import net.particify.arsnova.core.event.BeforeUserProfileAutoCreationEvent;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.service.UserService;

/**
 * Replaces the user ID provided by the authenticating user with the one that is part of LDAP object. This is necessary
 * to get a consistent ID despite case insensitivity.
 */
public class CustomLdapUserDetailsMapper extends LdapUserDetailsMapper implements ApplicationEventPublisherAware {
  public static final GrantedAuthority ROLE_LDAP_USER = new SimpleGrantedAuthority("ROLE_LDAP_USER");

  private static final Logger logger = LoggerFactory.getLogger(CustomLdapUserDetailsMapper.class);

  private String userIdAttr;
  private final Collection<GrantedAuthority> defaultGrantedAuthorities = Set.of(
      User.ROLE_USER,
      ROLE_LDAP_USER
  );
  private ApplicationEventPublisher applicationEventPublisher;

  private UserService userService;

  public CustomLdapUserDetailsMapper(final String ldapUserIdAttr) {
    this.userIdAttr = ldapUserIdAttr;
  }

  @Override
  @Autowired
  public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public UserDetails mapUserFromContext(
      final DirContextOperations ctx,
      final String username,
      final Collection<? extends GrantedAuthority> authorities) {
    final String ctxLdapUsername = ctx.getStringAttribute(userIdAttr);
    final String ldapUsername;
    if (ctxLdapUsername != null) {
      ldapUsername = ctxLdapUsername;
    } else {
      logger.warn("LDAP attribute {} not set. Falling back to lowercased user provided username.", userIdAttr);
      ldapUsername = username.toLowerCase();
    }

    final Collection<GrantedAuthority> grantedAuthorities = new HashSet<>(defaultGrantedAuthorities);
    grantedAuthorities.addAll(authorities);
    if (userService.isAdmin(ldapUsername, UserProfile.AuthProvider.LDAP)) {
      grantedAuthorities.add(User.ROLE_ADMIN);
    }

    final Optional<UserProfile> userProfile =
        Optional.ofNullable(
            userService.getByAuthProviderAndLoginId(UserProfile.AuthProvider.LDAP, ldapUsername));
    return new User(
        userProfile.orElseGet(() -> {
          final UserProfile newUserProfile = new UserProfile(UserProfile.AuthProvider.LDAP, ldapUsername);
          applicationEventPublisher.publishEvent(
              new BeforeUserProfileAutoCreationEvent(this, newUserProfile, Collections.emptyMap()));
          return userService.create(newUserProfile);
        }),
        grantedAuthorities);
  }

  @Autowired
  public void setUserService(final UserService userService) {
    this.userService = userService;
  }
}
