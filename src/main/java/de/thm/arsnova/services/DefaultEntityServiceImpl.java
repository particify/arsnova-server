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
package de.thm.arsnova.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.serialization.View;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Default implementation of {@link EntityService} which provides CRUD operations for entities independently from the
 * underlying persistence implementation. Authorization for entities is checked before any operation is performed.
 *
 * @param <T> Entity type
 * @author Daniel Gerhardt
 */
public class DefaultEntityServiceImpl<T extends Entity> implements EntityService<T> {
	protected Class<T> type;
	protected CrudRepository<T, String> repository;
	private ObjectMapper objectMapper;

	public DefaultEntityServiceImpl(Class<T> type, CrudRepository<T, String> repository, ObjectMapper objectMapper) {
		this.type = type;
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@Override
	@PreAuthorize("hasPermission(#id, #this.this.getTypeName(), 'read')")
	public T get(final String id) {
		return repository.findOne(id);
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, #this.this.getTypeName(), 'read')", filterTarget = "ids")
	public Iterable<T> get(final Collection<String> ids) {
		return repository.findAll(ids);
	}

	@Override
	@PreAuthorize("hasPermission(#entity, 'create')")
	public T create(final T entity) {
		if (entity.getId() != null || entity.getRevision() != null) {
			throw new IllegalArgumentException("Entity is not new.");
		}
		entity.setCreationTimestamp(new Date());

		return repository.save(entity);
	}

	@Override
	@PreAuthorize("hasPermission(#oldEntity, 'update')")
	public T update(final T oldEntity, final T newEntity) {
		newEntity.setId(oldEntity.getId());
		newEntity.setUpdateTimestamp(new Date());

		return repository.save(newEntity);
	}

	@Override
	public T patch(final T entity, final Map<String, Object> changes) throws IOException {
		return patch(entity, changes, Function.identity());
	}

	@Override
	@PreAuthorize("hasPermission(#entity, 'update')")
	public T patch(final T entity, final Map<String, Object> changes,
			final Function<T, ? extends Object> propertyGetter) throws IOException {
		Object obj = propertyGetter.apply(entity);
		ObjectReader reader = objectMapper.readerForUpdating(obj).withView(View.Public.class);
		JsonNode tree = objectMapper.valueToTree(changes);
		reader.readValue(tree);
		entity.setUpdateTimestamp(new Date());

		return repository.save(entity);
	}

	@Override
	public Iterable<T> patch(final Collection<T> entities, final Map<String, Object> changes) throws IOException {
		return patch(entities, changes, Function.identity());
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, 'update')", filterTarget = "entities")
	public Iterable<T> patch(final Collection<T> entities, final Map<String, Object> changes,
			final Function<T, ? extends Object> propertyGetter) throws IOException {
		final JsonNode tree = objectMapper.valueToTree(changes);
		for (T entity : entities) {
			Object obj = propertyGetter.apply(entity);
			ObjectReader reader = objectMapper.readerForUpdating(obj).withView(View.Public.class);
			reader.readValue(tree);
			entity.setUpdateTimestamp(new Date());
		}

		return repository.save(entities);
	}

	@Override
	@PreAuthorize("hasPermission(#entity, 'delete')")
	public void delete(final T entity) {
		repository.delete(entity);
	}

	public String getTypeName() {
		return type.getSimpleName().toLowerCase();
	}
}
