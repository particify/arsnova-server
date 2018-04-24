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

import de.thm.arsnova.services.UserService;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to load a user based on the results from CAS.
 */
@Service
public class CasUserDetailsService extends AbstractCasAssertionUserDetailsService {
	@Autowired
	private UserService userService;

	@Override
	protected UserDetails loadUserDetails(final Assertion assertion) {
		final List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		final String uid = assertion.getPrincipal().getName();
		if (userService.isAdmin(uid)) {
			grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}

		return new User(
				uid,
				"",
				true,
				true,
				true,
				true,
				grantedAuthorities
				);
	}
}
