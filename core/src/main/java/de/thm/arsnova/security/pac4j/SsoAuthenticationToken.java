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

package de.thm.arsnova.security.pac4j;

import java.util.Collection;
import org.pac4j.core.profile.UserProfile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import de.thm.arsnova.security.User;

/**
 * Authentication token implementation for Pac4j SSO.
 *
 * @author Daniel Gerhardt
 */
public class SsoAuthenticationToken extends AbstractAuthenticationToken {
  private User principal;

  public SsoAuthenticationToken(final User principal, final UserProfile profile,
      final Collection<? extends GrantedAuthority> grantedAuthorities) {
    super(grantedAuthorities);
    this.principal = principal;
    this.setDetails(profile);
    setAuthenticated(!grantedAuthorities.isEmpty());
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  @Override
  public boolean isAuthenticated() {
    return super.isAuthenticated();
  }
}
