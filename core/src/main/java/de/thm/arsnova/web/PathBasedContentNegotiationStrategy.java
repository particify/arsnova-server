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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.controller.AbstractEntityController;

/**
 * This {@link ContentNegotiationStrategy} selects the media type based on request path. It allows to set the correct
 * media type for old clients which do not set the 'Accept' header.
 *
 * @author Daniel Gerhardt
 */
public class PathBasedContentNegotiationStrategy implements ContentNegotiationStrategy {
	private static final Logger logger = LoggerFactory.getLogger(PathBasedContentNegotiationStrategy.class);

	private final String managementPath;

	private MediaType fallback;
	private MediaType empty = MediaType.valueOf(AbstractEntityController.MEDIATYPE_EMPTY);

	public PathBasedContentNegotiationStrategy(final MediaType fallback, final String managementPath) {
		this.fallback = fallback;
		this.managementPath = managementPath + "/";
	}

	@Override
	public List<MediaType> resolveMediaTypes(final NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException {
		final HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		final List<MediaType> mediaTypes = new ArrayList<>();
		if (servletRequest.getServletPath().startsWith(managementPath)) {
			logger.trace("Negotiating content based on path for management API");
			mediaTypes.add(AppConfig.ACTUATOR_MEDIA_TYPE);
			mediaTypes.add(MediaType.TEXT_PLAIN);
		} else if (servletRequest.getServletPath().startsWith("/v2/")) {
			logger.trace("Negotiating content based on path for API v2");
			mediaTypes.add(AppConfig.API_V2_MEDIA_TYPE);
			mediaTypes.add(MediaType.TEXT_PLAIN);
		} else {
			logger.trace("Content negotiation falling back to {}", fallback);
			if (servletRequest.getHeader(HttpHeaders.ACCEPT) == null
					&& Arrays.asList("POST", "PUT", "PATCH").contains(servletRequest.getMethod())) {
				/* This allows AbstractEntityController to send an empty response if no Accept header is set */
				logger.debug("No Accept header present for {} request. Entity will not be sent in response",
						servletRequest.getMethod());
				mediaTypes.add(empty);
			} else {
				mediaTypes.add(fallback);
			}
		}

		return mediaTypes;
	}
}
