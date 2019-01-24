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
package de.thm.arsnova.security.jwt;

import de.thm.arsnova.security.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class JwtToken extends AbstractAuthenticationToken {
	private String token;
	private User principal;

	public JwtToken(final String token, final User principal,
			final Collection<? extends GrantedAuthority> grantedAuthorities) {
		super(grantedAuthorities);
		this.token = token;
		this.principal = principal;
		setAuthenticated(!grantedAuthorities.isEmpty());
	}

	public JwtToken(final String token) {
		this(token, null, Collections.emptyList());
	}

	@Override
	public Object getCredentials() {
		return token;
	}

	@Override
	public Object getPrincipal() {
		return principal;
	}
}
