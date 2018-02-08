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

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * Handles callback requests by login redirects from OAuth providers.
 *
 * @author Daniel Gerhardt
 */
@Component
public class OauthCallbackFilter extends AbstractAuthenticationProcessingFilter {
	private static final Logger logger = LoggerFactory.getLogger(OauthCallbackFilter.class);
	private Config config;

	public OauthCallbackFilter(Config pac4jConfig) {
		super(new AntPathRequestMatcher("/login/oauth"));
		this.config = pac4jConfig;
	}

	@Override
	public Authentication attemptAuthentication(
			final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
			throws AuthenticationException {
		CommonProfile profile = retrieveProfile(new J2EContext(httpServletRequest, httpServletResponse));
		return getAuthenticationManager().authenticate(new OAuthToken(null, profile, Collections.emptyList()));
	}

	private CommonProfile retrieveProfile(J2EContext context) throws AuthenticationServiceException {
		/* Adapted from Pac4j: org.pac4j.core.engine.DefaultCallbackLogic.perform */
		Clients clients = config.getClients();
		CommonHelper.assertNotNull("clients", clients);
		Client client = clients.findClient(context);
		logger.debug("client: {}", client);
		CommonHelper.assertNotNull("client", client);
		CommonHelper.assertTrue(client instanceof IndirectClient,
				"only indirect clients are allowed on the callback url");

		try {
			Credentials credentials = client.getCredentials(context);
			logger.debug("credentials: {}", credentials);
			CommonProfile profile = client.getUserProfile(credentials, context);
			logger.debug("profile: {}", profile);

			return profile;
		} catch (final HttpAction e) {
			throw new AuthenticationServiceException(e.getMessage());
		}
	}
}
