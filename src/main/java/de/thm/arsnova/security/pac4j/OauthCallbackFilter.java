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

package de.thm.arsnova.security.pac4j;

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.client.finder.DefaultCallbackClientFinder;
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

/**
 * Handles callback requests by login redirects from OAuth providers.
 *
 * @author Daniel Gerhardt
 */
public class OauthCallbackFilter extends AbstractAuthenticationProcessingFilter {
	private static final Logger logger = LoggerFactory.getLogger(OauthCallbackFilter.class);
	private final ClientFinder clientFinder = new DefaultCallbackClientFinder();
	private Config config;

	public OauthCallbackFilter(final Config pac4jConfig, final String callbackPath) {
		super(new AntPathRequestMatcher("/**" + callbackPath + "/**"));
		this.config = pac4jConfig;
	}

	@Override
	public Authentication attemptAuthentication(
			final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
			throws AuthenticationException {
		final CommonProfile profile = retrieveProfile(new J2EContext(httpServletRequest, httpServletResponse), null);
		return getAuthenticationManager().authenticate(new OAuthToken(null, profile, Collections.emptyList()));
	}

	private CommonProfile retrieveProfile(final J2EContext context, final String clientName)
			throws AuthenticationServiceException {
		/* Adapted from Pac4j: org.pac4j.core.engine.DefaultCallbackLogic.perform */
		final Clients clients = config.getClients();
		CommonHelper.assertNotNull("clients", clients);
		final List<Client> foundClients = clientFinder.find(clients, context, clientName);
		CommonHelper.assertTrue(foundClients != null && foundClients.size() == 1,
				"unable to find one indirect client for the callback:"
						+ " check the callback URL for a client name parameter or suffix path"
						+ " or ensure that your configuration defaults to one indirect client");
		final Client foundClient = foundClients.get(0);
		logger.debug("client: {}", foundClient);
		CommonHelper.assertNotNull("client", foundClient);
		CommonHelper.assertTrue(foundClient instanceof IndirectClient,
				"only indirect clients are allowed on the callback url");

		try {
			final Credentials credentials = foundClient.getCredentials(context);
			logger.debug("credentials: {}", credentials);
			final CommonProfile profile = foundClient.getUserProfile(credentials, context);
			logger.debug("profile: {}", profile);

			return profile;
		} catch (final HttpAction e) {
			throw new AuthenticationServiceException(e.getMessage());
		}
	}
}
