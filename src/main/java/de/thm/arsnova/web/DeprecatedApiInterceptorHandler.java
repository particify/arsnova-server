/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Adds a "deprecated" header to any HTTP response that is served by a controller method with the {@link DeprecatedApi}
 * annotation.
 */
@Component
public class DeprecatedApiInterceptorHandler extends HandlerInterceptorAdapter {

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {

		if (getDeprecatedApiAnnotation(handler) != null) {
			response.addHeader("X-Deprecated-Api", "1");
		}

		return super.preHandle(request, response, handler);
	}

	private DeprecatedApi getDeprecatedApiAnnotation(final Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return null;
		}

		final HandlerMethod handlerMethod = (HandlerMethod) handler;
		return handlerMethod.getMethodAnnotation(DeprecatedApi.class);
	}
}
