/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
package de.thm.arsnova;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This class gets called when a user successfully logged in.
 */
public class LoginAuthenticationSucessHandler extends
		SimpleUrlAuthenticationSuccessHandler {

	private String targetUrl;

	@Override
	protected String determineTargetUrl(
			final HttpServletRequest request,
			final HttpServletResponse response
	) {
		HttpSession session = request.getSession();
		if (session == null || session.getAttribute("ars-login-success-url") == null) {
			return targetUrl;
		}

		return (String) session.getAttribute("ars-login-success-url");
	}

	public void setTargetUrl(final String url) {
		targetUrl = url;
	}
}
