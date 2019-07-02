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

package de.thm.arsnova.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;
import org.springframework.web.util.UriComponentsBuilder;

import de.thm.arsnova.model.serialization.View;

/**
 * This {@link ControllerAdvice} applies a {@link View} based on the
 * <code>view</code> query parameter which is used by
 * {@link com.fasterxml.jackson.annotation.JsonView} for serialization and makes
 * sure that the user is authorized.
 *
 * @author Daniel Gerhardt
 */
@ControllerAdvice
public class JsonViewControllerAdvice extends AbstractMappingJacksonResponseBodyAdvice {
	private static final String VIEW_PARAMETER = "view";

	private static final Logger logger = LoggerFactory.getLogger(JsonViewControllerAdvice.class);

	@Override
	protected void beforeBodyWriteInternal(final MappingJacksonValue bodyContainer,
			final MediaType contentType, final MethodParameter returnType,
			final ServerHttpRequest request, final ServerHttpResponse response) {
		/* TODO: Why does @ControllerAdvice(assignableTypes = AbstractEntityController.class) not work? */
		if (!AbstractEntityController.class.isAssignableFrom(returnType.getContainingClass())) {
			return;
		}

		final List<String> viewList = UriComponentsBuilder.fromUri(request.getURI()).build()
				.getQueryParams().getOrDefault(VIEW_PARAMETER, Collections.emptyList());
		if (viewList.isEmpty()) {
			return;
		}
		final String view = viewList.get(0);
		logger.debug("'{}' parameter found in request URI: {}", VIEW_PARAMETER, view);
		if (bodyContainer.getValue() instanceof Collection) {
			logger.warn("'{}' parameter is currently not supported for listing endpoints.", VIEW_PARAMETER);
		}
		tryAccess(bodyContainer.getValue(), view);
		switch (view) {
			case "owner":
				bodyContainer.setSerializationView(View.Owner.class);
				break;
			case "admin":
				bodyContainer.setSerializationView(View.Admin.class);
				break;
			default:
				return;
		}
	}

	@PreAuthorize("hasPermission(#targetDomainObject, #permission)")
	protected void tryAccess(final Object targetDomainObject, final Object permission) {
		/* Access check is done by aspect. No additional implementation needed. */
	}
}
