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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.service.UserService;

/**
 * Replaces the user ID provided by the authenticating user with the one that is part of LDAP object. This is necessary
 * to get a consistent ID despite case insensitivity.
 */
public class CustomLdapUserDetailsMapper extends LdapUserDetailsMapper {
	public static final GrantedAuthority ROLE_LDAP_USER = new SimpleGrantedAuthority("ROLE_LDAP_USER");

	private static final Logger logger = LoggerFactory.getLogger(CustomLdapUserDetailsMapper.class);

	private String userIdAttr;
	private final Collection<GrantedAuthority> defaultGrantedAuthorities = Set.of(
			User.ROLE_USER,
			ROLE_LDAP_USER
	);

	private UserService userService;

	public CustomLdapUserDetailsMapper(final String ldapUserIdAttr) {
		this.userIdAttr = ldapUserIdAttr;
	}

	public UserDetails mapUserFromContext(
			final DirContextOperations ctx,
			final String username,
			final Collection<? extends GrantedAuthority> authorities) {
		String ldapUsername = ctx.getStringAttribute(userIdAttr);
		if (ldapUsername == null) {
			logger.warn("LDAP attribute {} not set. Falling back to lowercased user provided username.", userIdAttr);
			ldapUsername = username.toLowerCase();
		}

		final Collection<GrantedAuthority> grantedAuthorities = new HashSet<>(defaultGrantedAuthorities);
		grantedAuthorities.addAll(authorities);
		if (userService.isAdmin(ldapUsername, UserProfile.AuthProvider.LDAP)) {
			grantedAuthorities.add(User.ROLE_ADMIN);
		}

		return userService.loadUser(UserProfile.AuthProvider.LDAP, ldapUsername,
				grantedAuthorities, true);
	}

	@Autowired
	public void setUserService(final UserService userService) {
		this.userService = userService;
	}
}
