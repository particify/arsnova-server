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

import de.thm.arsnova.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * This {@link javax.servlet.Filter} overrides the Content-Type for JSON data sent by the client with the ARSnova API v2
 * JSON Content-Type.
 *
 * @author Daniel Gerhardt
 */
@Component
public class V2ContentTypeOverrideFilter extends OncePerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(V2ContentTypeOverrideFilter.class);
	private final List<String> contentTypeHeaders;

	{
		contentTypeHeaders = new ArrayList<>();
		contentTypeHeaders.add(AppConfig.API_V2_MEDIA_TYPE_VALUE);
	}

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		return !request.getServletPath().startsWith("/v2/");
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest requestWrapper = new HttpServletRequestWrapper(request) {
			@Override
			public String getHeader(final String name) {
				String header = super.getHeader(name);
				if (header != null && HttpHeaders.CONTENT_TYPE.equals(name)
						&& MediaType.APPLICATION_JSON.includes(MediaType.valueOf(header))) {
					logger.debug("Overriding {} header: {}", HttpHeaders.CONTENT_TYPE, AppConfig.API_V2_MEDIA_TYPE_VALUE);
					return AppConfig.API_V2_MEDIA_TYPE_VALUE;
				}

				return header;
			}

			@Override
			public String getContentType() {
				return getHeader(HttpHeaders.CONTENT_TYPE);
			}

			@Override
			public Enumeration<String> getHeaders(final String name) {
				String firstHeader = super.getHeaders(name).nextElement();
				if (firstHeader != null && HttpHeaders.CONTENT_TYPE.equals(name)
						&& MediaType.APPLICATION_JSON.includes(MediaType.valueOf(firstHeader))) {
					logger.debug("Overriding {} header: {}", HttpHeaders.CONTENT_TYPE, AppConfig.API_V2_MEDIA_TYPE_VALUE);
					return Collections.enumeration(contentTypeHeaders);
				}

				return super.getHeaders(name);
			}
		};
		filterChain.doFilter(requestWrapper, response);
	}
}
