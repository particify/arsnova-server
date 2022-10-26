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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import de.thm.arsnova.security.User;

public class JwtAuthenticationProvider implements AuthenticationProvider {
	private JwtService jwtService;

	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		final String token = (String) authentication.getCredentials();
		final User user = jwtService.verifyToken((String) authentication.getCredentials());

		return new JwtToken(token, user, user.getAuthorities());
	}

	@Override
	public boolean supports(final Class<?> authentication) {
		return JwtToken.class.isAssignableFrom(authentication);
	}

	@Autowired
	public void setJwtService(final JwtService jwtService) {
		this.jwtService = jwtService;
	}
}
