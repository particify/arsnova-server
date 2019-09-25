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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import de.thm.arsnova.security.jwt.JwtService;

/**
 * This class gets called when a user successfully logged in.
 */
public class LoginAuthenticationSucessHandler extends
		SimpleUrlAuthenticationSuccessHandler {
	public static final String URL_ATTRIBUTE = "ars-login-success-url";
	private static final String AUTHENTICATION_ATTRIBUTE = "authentication";

	private JwtService jwtService;
	private String targetUrl;

	@Autowired
	public void setJwtService(final JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected String determineTargetUrl(
			final HttpServletRequest request,
			final HttpServletResponse response) {
		final HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(URL_ATTRIBUTE) == null) {
			final Authentication authentication = (Authentication) request.getAttribute(AUTHENTICATION_ATTRIBUTE);
			final String token = jwtService.createSignedToken((User) authentication.getPrincipal());
			return targetUrl + "?token=" + token;
		}

		final String url = (String) session.getAttribute(URL_ATTRIBUTE);
		session.removeAttribute(URL_ATTRIBUTE);

		return url;
	}

	@Override
	public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
			final Authentication authentication) throws IOException, ServletException {
		request.setAttribute(AUTHENTICATION_ATTRIBUTE, authentication);
		super.onAuthenticationSuccess(request, response, authentication);
	}

	public void setTargetUrl(final String url) {
		targetUrl = url;
	}
}
