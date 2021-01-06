/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import java.util.Collection;

/**
 * Replaces the user ID provided by the authenticating user with the one that is part of LDAP object. This is necessary
 * to get a consistent ID despite case insensitivity.
 */
public class CustomLdapUserDetailsMapper extends LdapUserDetailsMapper {
	private static final Logger logger = LoggerFactory.getLogger(CustomLdapUserDetailsMapper.class);

	private String userIdAttr;

	public CustomLdapUserDetailsMapper(String ldapUserIdAttr) {
		this.userIdAttr = ldapUserIdAttr;
	}

	public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
										  Collection<? extends GrantedAuthority> authorities) {
		String ldapUsername = ctx.getStringAttribute(userIdAttr);
		if (ldapUsername == null) {
			logger.warn("LDAP attribute {} not set. Falling back to lowercased user provided username.", userIdAttr);
			ldapUsername = username.toLowerCase();
		}

		 return super.mapUserFromContext(ctx, ldapUsername, authorities);
	}
}
