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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.service.UserService;

/**
 * Loads UserDetails for a registered user ({@link UserProfile.AuthProvider#ARSNOVA}) based on the username (loginId).
 *
 * @author Daniel Gerhardt
 */
@Service
public class RegisteredUserDetailsService implements UserDetailsService {
  public static final GrantedAuthority ROLE_REGISTERED_USER = new SimpleGrantedAuthority("ROLE_REGISTERED_USER");

  private final Collection<GrantedAuthority> defaultGrantedAuthorities = Set.of(
      User.ROLE_USER,
      ROLE_REGISTERED_USER
  );
  private UserService userService;

  @Override
  public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
    final String loginId = username.toLowerCase();
    final Collection<GrantedAuthority> grantedAuthorities = new HashSet<>(defaultGrantedAuthorities);
    if (userService.isAdmin(loginId, UserProfile.AuthProvider.ARSNOVA)) {
      grantedAuthorities.add(User.ROLE_ADMIN);
    }
    return userService.loadUser(UserProfile.AuthProvider.ARSNOVA, loginId,
        grantedAuthorities, false);
  }

  @Autowired
  public void setUserService(final UserService userService) {
    this.userService = userService;
  }
}
