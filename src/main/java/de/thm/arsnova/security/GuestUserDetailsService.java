/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.service.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;

/**
 * Loads UserDetails for a guest user ({@link UserProfile.AuthProvider#ARSNOVA_GUEST}) based on the username (guest
 * token).
 *
 * @author Daniel Gerhardt
 */
@Service
public class GuestUserDetailsService implements UserDetailsService {
	private final UserService userService;
	private final Collection<GrantedAuthority> grantedAuthorities;

	public GuestUserDetailsService(UserService userService) {
		this.userService = userService;
		grantedAuthorities = new HashSet<>();
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_GUEST_USER"));
	}

	@Override
	public UserDetails loadUserByUsername(final String loginId) throws UsernameNotFoundException {
		return loadUserByUsername(loginId, false);
	}

	public UserDetails loadUserByUsername(final String loginId, final boolean autoCreate) {
		return userService.loadUser(UserProfile.AuthProvider.ARSNOVA_GUEST, loginId,
				grantedAuthorities, autoCreate);
	}
}
