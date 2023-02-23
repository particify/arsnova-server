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

package net.particify.arsnova.core.security.pac4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.saml.profile.SAML2Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.config.properties.AuthenticationProviderProperties;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.security.AbstractUserDetailsService;
import net.particify.arsnova.core.security.User;
import net.particify.arsnova.core.service.UserService;

/**
 * Loads UserDetails for an Pac4j SSO user (e.g. {@link UserProfile.AuthProvider#OIDC}) based on an unique identifier
 * extracted from the Pac4j profile.
 *
 * @author Daniel Gerhardt
 */
@Service
public class SsoUserDetailsService extends AbstractUserDetailsService
    implements AuthenticationUserDetailsService<SsoAuthenticationToken> {
  public static final GrantedAuthority ROLE_OAUTH_USER = new SimpleGrantedAuthority("ROLE_OAUTH_USER");

  protected final Collection<GrantedAuthority> defaultGrantedAuthorities = Set.of(
      User.ROLE_USER,
      ROLE_OAUTH_USER
  );
  private final AuthenticationProviderProperties.Saml samlProperties;

  public SsoUserDetailsService(final UserService userService,
      final AuthenticationProviderProperties authenticationProviderProperties) {
    super(UserProfile.AuthProvider.NONE, userService);
    this.samlProperties = authenticationProviderProperties.getSaml();
  }

  public User loadUserDetails(final SsoAuthenticationToken token)
      throws UsernameNotFoundException {
    if (token.getDetails() instanceof OidcProfile oidcProfile) {
      return loadOidcUserDetails(oidcProfile);
    } else if (token.getDetails() instanceof SAML2Profile saml2Profile) {
      return loadSamlUserDetails(saml2Profile);
    } else {
      throw new IllegalArgumentException("AuthenticationToken not supported");
    }
  }

  private User loadOidcUserDetails(final OidcProfile profile) {
    final Set<GrantedAuthority> grantedAuthorities = new HashSet<>(defaultGrantedAuthorities);
    if (userService.isAdmin(profile.getId(), UserProfile.AuthProvider.OIDC)) {
      grantedAuthorities.add(User.ROLE_ADMIN);
    }

    return getOrCreate(profile.getId(), UserProfile.AuthProvider.OIDC, grantedAuthorities, profile.getAttributes());
  }

  private User loadSamlUserDetails(final SAML2Profile profile) {
    final Set<GrantedAuthority> grantedAuthorities = new HashSet<>(defaultGrantedAuthorities);
    final String uidAttr = samlProperties.getUserIdAttribute();
    final String uid;

    if (uidAttr == null || "".equals(uidAttr)) {
      uid = profile.getId();
    } else {
      uid = profile.getAttribute(uidAttr, List.class).get(0).toString();
    }
    if (userService.isAdmin(uid, UserProfile.AuthProvider.SAML)) {
      grantedAuthorities.add(User.ROLE_ADMIN);
    }

    return getOrCreate(uid, UserProfile.AuthProvider.SAML, grantedAuthorities, profile.getAttributes());
  }
}
