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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;


/**
 * This class gets called when a user failed to login.
 */
public class LoginAuthenticationFailureHandler extends
		SimpleUrlAuthenticationFailureHandler {

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private String failureUrl;

	@Override
	public void onAuthenticationFailure(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final AuthenticationException exception
	) throws IOException, ServletException {
		HttpSession session = request.getSession();
		if (session != null && session.getAttribute("ars-login-failure-url") != null) {
			failureUrl = (String) session.getAttribute("ars-login-failure-url");
		}

		redirectStrategy.sendRedirect(request, response, failureUrl);
	}

	@Override
	public void setDefaultFailureUrl(final String url) {
		failureUrl = url;
	}

}
