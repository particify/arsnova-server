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

import org.pac4j.core.config.Config;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.http.J2ENopHttpActionAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles callback requests by login redirects from OAuth providers.
 *
 * @author Daniel Gerhardt
 */
@Component
public class OauthCallbackFilter extends OncePerRequestFilter {
	private OauthCallbackHandler<Object, J2EContext> oauthCallbackHandler;
	private Config config;
	private String defaultUrl;
	private String suffix;

	public OauthCallbackFilter(OauthCallbackHandler<Object, J2EContext> oauthCallbackHandler, Config pac4jConfig) {
		this.oauthCallbackHandler = oauthCallbackHandler;
		this.config = pac4jConfig;
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest httpServletRequest,
			final HttpServletResponse httpServletResponse, final FilterChain filterChain)
			throws ServletException, IOException {
		if (httpServletRequest.getServletPath().endsWith(suffix)) {
			final J2EContext context = new J2EContext(httpServletRequest, httpServletResponse, config.getSessionStore());
			oauthCallbackHandler.perform(context, config, J2ENopHttpActionAdapter.INSTANCE, defaultUrl,
					false, false);
		} else {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		}
	}

	public void setDefaultUrl(final String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}

	public void setSuffix(final String suffix) {
		this.suffix = suffix;
	}
}
