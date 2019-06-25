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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component
public class JwtTokenFilter extends GenericFilterBean {
	private static final String JWT_HEADER_NAME = "Arsnova-Auth-Token";
	private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);
	private JwtAuthenticationProvider jwtAuthenticationProvider;

	@Override
	public void doFilter(final ServletRequest servletRequest,
			final ServletResponse servletResponse,
			final FilterChain filterChain)
			throws IOException, ServletException {
		final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		if (httpServletRequest.getRequestURI().startsWith("/v2/")) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}
		String jwtHeader = httpServletRequest.getHeader(JWT_HEADER_NAME);
		if (jwtHeader != null) {
			JwtToken token = new JwtToken(jwtHeader);
			try {
				Authentication authenticatedToken = jwtAuthenticationProvider.authenticate(token);
				if (authenticatedToken != null) {
					logger.debug("Storing JWT to SecurityContext: {}", authenticatedToken);
					SecurityContextHolder.getContext().setAuthentication(authenticatedToken);
				} else {
					logger.debug("Could not authenticate JWT.");
				}
			} catch (final Exception e) {
				logger.debug("JWT authentication failed", e);
			}
		} else {
			logger.debug("No authentication header present.");
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Autowired
	public void setJwtAuthenticationProvider(final JwtAuthenticationProvider jwtAuthenticationProvider) {
		this.jwtAuthenticationProvider = jwtAuthenticationProvider;
	}
}
