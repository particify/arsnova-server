/*
 * Copyright (C) 2012 THM webMedia
 * 
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

public class LoginAuthenticationSucessHandler extends
		SimpleUrlAuthenticationSuccessHandler {

	private String targetUrl;

	@Override
	protected final String determineTargetUrl(
			final HttpServletRequest request,
			final HttpServletResponse response
	) {
		HttpSession session = request.getSession();
		if (session == null || session.getAttribute("ars-referer") == null) {
			return targetUrl;
		}
		String referer = (String) session.getAttribute("ars-referer");
		return referer + targetUrl;
	}

	public final void setTargetUrl(final String targetUrl) {
		this.targetUrl = targetUrl;
	}
}
