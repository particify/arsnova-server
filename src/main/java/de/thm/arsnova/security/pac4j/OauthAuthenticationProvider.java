/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
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

import de.thm.arsnova.security.User;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Sets up the SecurityContext OAuth users.
 *
 * @author Daniel Gerhardt
 */
@Component
public class OauthAuthenticationProvider implements AuthenticationProvider {
	private OauthUserDetailsService oauthUserDetailsService;

	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		OAuthToken oAuthToken = (OAuthToken) authentication;
		User user = oauthUserDetailsService.loadUserDetails(oAuthToken);

		return new OAuthToken(user, (CommonProfile) oAuthToken.getDetails(), user.getAuthorities());
	}

	@Override
	public boolean supports(final Class<?> aClass) {
		return aClass.isAssignableFrom(OAuthToken.class);
	}

	@Autowired
	public void setOauthUserDetailsService(final OauthUserDetailsService oauthUserDetailsService) {
		this.oauthUserDetailsService = oauthUserDetailsService;
	}
}
