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

package de.thm.arsnova.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Adds caching headers to a HTTP request based on {@link CacheControl} annotation.
 */
@Component
public class CacheControlInterceptorHandler extends HandlerInterceptorAdapter {

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {

		setCacheControlResponseHeader(response, handler);
		return super.preHandle(request, response, handler);
	}

	private void setCacheControlResponseHeader(
			final HttpServletResponse response,
			final Object handler) {

		final CacheControl cacheControl = getCacheControlAnnotation(handler);

		if (cacheControl == null) {
			return;
		}

		final StringBuilder headerValue = new StringBuilder();

		if (cacheControl.policy().length > 0) {
			for (final CacheControl.Policy policy : cacheControl.policy()) {
				if (headerValue.length() > 0) {
					headerValue.append(", ");
				}
				headerValue.append(policy.getPolicyString());
			}
		}

		if (cacheControl.noCache()) {
			if (headerValue.length() > 0) {
				headerValue.append(", ");
			}
			headerValue.append("max-age=0, no-cache");
			response.setHeader("cache-control", headerValue.toString());
		}

		if (cacheControl.maxAge() >= 0) {
			if (headerValue.length() > 0) {
				headerValue.append(", ");
			}
			headerValue.append("max-age=").append(cacheControl.maxAge());
		}

		response.setHeader("cache-control", headerValue.toString());
	}

	private CacheControl getCacheControlAnnotation(final Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return null;
		}

		final HandlerMethod handlerMethod = (HandlerMethod) handler;
		return handlerMethod.getMethodAnnotation(CacheControl.class);
	}
}
