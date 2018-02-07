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
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Sets up the SecurityContext OAuth users.
 *
 * @author Daniel Gerhardt
 */
@Component
public class OauthCallbackHandler<R, C extends WebContext> extends DefaultCallbackLogic<R, C> {
	private OauthUserDetailsService oauthUserDetailsService;

	@Override
	protected void saveUserProfile(final C context, final Config config, final CommonProfile profile,
			final boolean multiProfile, final boolean renewSession) {
		User user = oauthUserDetailsService.loadUserDetails(
				new OAuthToken(null, profile, Collections.emptyList()));
		SecurityContextHolder.getContext().setAuthentication(
				new OAuthToken(user, profile, user.getAuthorities()));
	}

	@Override
	protected void renewSession(final C context, final Config config) {
		/* NOOP */
	}

	@Autowired
	public void setOauthUserDetailsService(final OauthUserDetailsService oauthUserDetailsService) {
		this.oauthUserDetailsService = oauthUserDetailsService;
	}
}
