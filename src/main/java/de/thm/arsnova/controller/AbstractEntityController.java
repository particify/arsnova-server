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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.naming.OperationNotSupportedException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.FindQuery;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.service.EntityService;
import de.thm.arsnova.service.FindQueryService;
import de.thm.arsnova.web.exceptions.NotFoundException;

/**
 * Base type for Entity controllers which provides basic CRUD operations and supports Entity patching.
 *
 * @param <E> Entity type
 * @author Daniel Gerhardt
 */
public abstract class AbstractEntityController<E extends Entity> {
	public static final String MEDIATYPE_EMPTY = "application/x-empty";
	private static final Logger logger = LoggerFactory.getLogger(AbstractEntityController.class);
	public static final String ENTITY_ID_HEADER = "Arsnova-Entity-Id";
	public static final String ENTITY_REVISION_HEADER = "Arsnova-Entity-Revision";
	protected static final String DEFAULT_ROOT_MAPPING = "/";
	protected static final String DEFAULT_ID_MAPPING = "/{id:[^~].*}";
	protected static final String DEFAULT_ALIAS_MAPPING = "/~{alias}";
	protected static final String DEFAULT_FIND_MAPPING = "/find";
	protected static final String ALIAS_SUBPATH = "/**";
	protected static final String GET_MAPPING = DEFAULT_ID_MAPPING;
	protected static final String GET_MULTIPLE_MAPPING = DEFAULT_ROOT_MAPPING;
	protected static final String PUT_MAPPING = DEFAULT_ID_MAPPING;
	protected static final String POST_MAPPING = DEFAULT_ROOT_MAPPING;
	protected static final String PATCH_MAPPING = DEFAULT_ID_MAPPING;
	protected static final String DELETE_MAPPING = DEFAULT_ID_MAPPING;
	protected static final String FIND_MAPPING = DEFAULT_FIND_MAPPING;
	protected final EntityService<E> entityService;
	protected FindQueryService<E> findQueryService;

	protected AbstractEntityController(final EntityService<E> entityService) {
		this.entityService = entityService;
	}

	protected abstract String getMapping();

	@GetMapping(GET_MAPPING)
	public E get(@PathVariable final String id) {
		return entityService.get(id);
	}

	@GetMapping(GET_MULTIPLE_MAPPING)
	public Iterable<E> getMultiple(@RequestParam final Collection<String> ids) {
		return entityService.get(ids);
	}

	@PutMapping(value = PUT_MAPPING, produces = MEDIATYPE_EMPTY)
	public void putWithoutResponse(@RequestBody final E entity, final HttpServletResponse httpServletResponse) {
		put(entity, httpServletResponse);
	}

	@PutMapping(PUT_MAPPING)
	public E put(@RequestBody final E entity, final HttpServletResponse httpServletResponse) {
		final E oldEntity = entityService.get(entity.getId());
		entityService.update(oldEntity, entity);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, entity.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, entity.getRevision());

		return entity;
	}

	@PostMapping(value = POST_MAPPING, produces = MEDIATYPE_EMPTY)
	@ResponseStatus(HttpStatus.CREATED)
	public void postWithoutResponse(@RequestBody final E entity, final HttpServletResponse httpServletResponse) {
		post(entity, httpServletResponse);
	}

	@PostMapping(POST_MAPPING)
	@ResponseStatus(HttpStatus.CREATED)
	public E post(@RequestBody final E entity, final HttpServletResponse httpServletResponse) {
		entityService.create(entity);
		final String uri = UriComponentsBuilder.fromPath(getMapping()).path(GET_MAPPING)
				.buildAndExpand(entity.getId()).toUriString();
		httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, entity.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, entity.getRevision());

		return entity;
	}

	@PatchMapping(value = PATCH_MAPPING, produces = MEDIATYPE_EMPTY)
	public void patchWithoutResponse(@PathVariable final String id, @RequestBody final Map<String, Object> changes,
			final HttpServletResponse httpServletResponse) throws IOException {
		patch(id, changes, httpServletResponse);
	}

	@PatchMapping(PATCH_MAPPING)
	public E patch(@PathVariable final String id, @RequestBody final Map<String, Object> changes,
			final HttpServletResponse httpServletResponse) throws IOException {
		final E entity = entityService.get(id);
		entityService.patch(entity, changes, View.Public.class);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, entity.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, entity.getRevision());

		return entity;
	}

	@DeleteMapping(DELETE_MAPPING)
	public void delete(@PathVariable final String id) {
		final E entity = entityService.get(id);
		entityService.delete(entity);
	}

	@PostMapping(FIND_MAPPING)
	public Iterable<E> find(@RequestBody final FindQuery<E> findQuery) throws OperationNotSupportedException {
		if (findQueryService != null) {
			logger.debug("Resolving find query: {}", findQuery);
			final Set<String> ids = findQueryService.resolveQuery(findQuery);
			logger.debug("Resolved find query to IDs: {}", ids);

			return entityService.get(ids);
		} else {
			throw new OperationNotSupportedException("Find is not supported for this entity type.");
		}
	}

	@RequestMapping(value = {DEFAULT_ALIAS_MAPPING, DEFAULT_ALIAS_MAPPING + ALIAS_SUBPATH})
	public void forwardAlias(@PathVariable final String alias,
			final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
			throws ServletException, IOException {
		final String subPath = UriUtils.encodePath(
				new AntPathMatcher().extractPathWithinPattern(
				(String) httpServletRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE),
				(String) httpServletRequest.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)),
				StandardCharsets.UTF_8);
		final String targetPath = String.format(
				"%s/%s%s", getMapping(), resolveAlias(alias), subPath != null ? "/" + subPath : "");
		logger.debug("Forwarding alias request to {}", targetPath);
		httpServletRequest.getRequestDispatcher(targetPath)
				.forward(httpServletRequest, httpServletResponse);
	}

	protected String resolveAlias(final String alias) {
		throw new NotFoundException("Aliases not supported for " + getMapping() + ".");
	}

	@Autowired(required = false)
	public void setFindQueryService(final FindQueryService<E> findQueryService) {
		this.findQueryService = findQueryService;
	}
}
