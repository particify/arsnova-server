/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
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

import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.FindQuery;
import de.thm.arsnova.services.EntityService;
import de.thm.arsnova.services.FindQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Base type for Entity controllers which provides basic CRUD operations and supports Entity patching.
 *
 * @param <E> Entity type
 * @author Daniel Gerhardt
 */
public abstract class AbstractEntityController<E extends Entity> {
	private static final Logger logger = LoggerFactory.getLogger(AbstractEntityController.class);
	protected static final String DEFAULT_ROOT_MAPPING = "/";
	protected static final String DEFAULT_ID_MAPPING = "/{id}";
	protected static final String DEFAULT_FIND_MAPPING = "/find";
	protected static final String GET_MAPPING = DEFAULT_ID_MAPPING;
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

	@GetMapping(GET_MAPPING)
	public E get(@PathVariable final String id) {
		return entityService.get(id);
	}

	@PutMapping(PUT_MAPPING)
	public void put(@RequestBody final E entity) {
		entityService.create(entity);
	}

	@PostMapping(POST_MAPPING)
	public void post(@RequestBody final E entity) {
		E oldEntity = entityService.get(entity.getId());
		entityService.update(oldEntity, entity);
	}

	@PatchMapping(PATCH_MAPPING)
	public void patch(@PathVariable final String id, @RequestBody final Map<String, Object> changes)
			throws IOException {
		E entity = entityService.get(id);
		entityService.patch(entity, changes);
	}

	@DeleteMapping(DELETE_MAPPING)
	public void delete(@PathVariable final String id) {
		E entity = entityService.get(id);
		entityService.delete(entity);
	}

	@PostMapping(FIND_MAPPING)
	public Iterable<E> find(@RequestBody final FindQuery<E> findQuery) throws OperationNotSupportedException {
		if (findQueryService != null) {
			logger.debug("Resolving find query: {}", findQuery);
			Set<String> ids = findQueryService.resolveQuery(findQuery);
			logger.debug("Resolved find query to IDs: {}", ids);

			return entityService.get(ids);
		} else {
			throw new OperationNotSupportedException("Find is not supported for this entity type.");
		}
	}

	@Autowired(required = false)
	public void setFindQueryService(final FindQueryService<E> findQueryService) {
		this.findQueryService = findQueryService;
	}
}
