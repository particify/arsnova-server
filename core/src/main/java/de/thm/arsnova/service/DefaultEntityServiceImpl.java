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

package de.thm.arsnova.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import de.thm.arsnova.event.AfterCreationEvent;
import de.thm.arsnova.event.AfterDeletionEvent;
import de.thm.arsnova.event.AfterFullUpdateEvent;
import de.thm.arsnova.event.AfterPatchEvent;
import de.thm.arsnova.event.AfterUpdateEvent;
import de.thm.arsnova.event.BeforeCreationEvent;
import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.event.BeforeFullUpdateEvent;
import de.thm.arsnova.event.BeforePatchEvent;
import de.thm.arsnova.event.BulkChangeEvent;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.EntityValidationException;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.CrudRepository;

/**
 * Default implementation of {@link EntityService} which provides CRUD operations for entities independently from the
 * underlying persistence implementation. Authorization for entities is checked before any operation is performed.
 *
 * @param <T> Entity type
 * @author Daniel Gerhardt
 */
public class DefaultEntityServiceImpl<T extends Entity> implements EntityService<T>, ApplicationEventPublisherAware {
	protected Class<T> type;
	protected CrudRepository<T, String> repository;
	protected ApplicationEventPublisher eventPublisher;
	private ObjectMapper objectMapper;
	private ObjectMapper objectMapperForPatchTree;
	private Validator validator;

	public DefaultEntityServiceImpl(
			final Class<T> type,
			final CrudRepository<T, String> repository,
			final ObjectMapper objectMapper,
			final Validator validator) {
		this.type = type;
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.validator = validator;
		objectMapperForPatchTree = new ObjectMapper();
	}

	@Override
	@PreAuthorize("hasPermission(#id, #this.this.getTypeName(), 'read')")
	public T get(final String id) {
		return get(id, false);
	}

	@Override
	@Cacheable(cacheNames = "entity", key = "#root.target.getTypeName() + '-' + #id", condition = "#internal == false")
	public T get(final String id, final boolean internal) {
		final T entity;
		entity = repository.findOne(id);
		if (internal) {
			entity.setInternal(true);
		}
		modifyRetrieved(entity);

		return entity;
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, #this.this.getTypeName(), 'read')", filterTarget = "ids")
	public List<T> get(final Iterable<String> ids) {
		final List<T> entities = repository.findAllById(ids);
		entities.forEach(this::modifyRetrieved);

		return entities;
	}

	@Override
	@PreAuthorize("hasPermission(#entity, 'create')")
	public T create(final T entity) {
		if (entity.getId() != null || entity.getRevision() != null) {
			throw new IllegalArgumentException("Entity is not new.");
		}
		entity.setCreationTimestamp(new Date());

		prepareCreate(entity);
		eventPublisher.publishEvent(new BeforeCreationEvent<>(this, entity));
		validate(entity);
		final T createdEntity = repository.save(entity);
		eventPublisher.publishEvent(new AfterCreationEvent<>(this, createdEntity));
		finalizeCreate(createdEntity);
		modifyRetrieved(entity);

		return createdEntity;
	}

	/**
	 * This method can be overridden by subclasses to modify the entity before creation.
	 *
	 * @param entity The entity to be created
	 */
	protected void prepareCreate(final T entity) {
		/* Implementation provided by subclasses. */
	}

	/**
	 * This method can be overridden by subclasses to modify the entity after creation.
	 *
	 * @param entity The entity which has been created
	 */
	protected void finalizeCreate(final T entity) {
		/* Implementation provided by subclasses. */
	}

	public T update(final T entity) {
		return update(repository.findOne(entity.getId()), entity);
	}

	@Override
	@PreAuthorize("hasPermission(#oldEntity, 'update')")
	public T update(final T oldEntity, final T newEntity) {
		newEntity.setId(oldEntity.getId());
		newEntity.setUpdateTimestamp(new Date());

		prepareUpdate(newEntity);
		eventPublisher.publishEvent(new BeforeFullUpdateEvent<>(this, newEntity, oldEntity));
		validate(newEntity);
		final T updatedEntity = repository.save(newEntity);
		eventPublisher.publishEvent(new AfterFullUpdateEvent<>(this, updatedEntity, oldEntity));
		finalizeUpdate(updatedEntity);
		modifyRetrieved(updatedEntity);

		return updatedEntity;
	}

	/**
	 * This method can be overridden by subclasses to modify the entity before updating.
	 *
	 * @param entity The entity to be updated
	 */
	protected void prepareUpdate(final T entity) {
		/* Implementation provided by subclasses. */
	}

	/**
	 * This method can be overridden by subclasses to modify the entity after updating.
	 *
	 * @param entity The entity which has been updated
	 */
	protected void finalizeUpdate(final T entity) {
		/* Implementation provided by subclasses. */
	}

	@Override
	public T patch(final T entity, final Map<String, Object> changes) throws IOException {
		return patch(entity, changes, Function.identity(), View.Persistence.class);
	}

	@Override
	public T patch(final T entity, final Map<String, Object> changes, final Class<?> view) throws IOException {
		return patch(entity, changes, Function.identity(), view);
	}

	@Override
	public T patch(final T entity, final Map<String, Object> changes,
			final Function<T, ? extends Object> propertyGetter) throws IOException {
		return patch(entity, changes, propertyGetter, View.Persistence.class);
	}

	@Override
	@PreAuthorize("hasPermission(#entity, 'update')")
	public T patch(final T entity, final Map<String, Object> changes,
			final Function<T, ? extends Object> propertyGetter, final Class<?> view) throws IOException {
		final T oldEntity = cloneEntity(entity);
		final Object obj = propertyGetter.apply(entity);
		final ObjectReader reader = objectMapper.readerForUpdating(obj).withView(view);
		final JsonNode tree = objectMapperForPatchTree.valueToTree(changes);
		reader.readValue(tree);
		entity.setUpdateTimestamp(new Date());
		preparePatch(entity);
		eventPublisher.publishEvent(new BeforePatchEvent<>(this, entity, oldEntity, propertyGetter, changes));
		validate(entity);
		final T patchedEntity = repository.save(entity);
		eventPublisher.publishEvent(new AfterPatchEvent<>(this, patchedEntity, oldEntity, propertyGetter, changes));
		modifyRetrieved(patchedEntity);

		return patchedEntity;
	}

	@Override
	public List<T> patch(final Iterable<T> entities, final Map<String, Object> changes) throws IOException {
		return patch(entities, changes, Function.identity(), View.Persistence.class);
	}

	@Override
	public List<T> patch(final Iterable<T> entities, final Map<String, Object> changes, final Class<?> view)
			throws IOException {
		return patch(entities, changes, Function.identity(), view);
	}

	@Override
	public List<T> patch(final Iterable<T> entities, final Map<String, Object> changes,
			final Function<T, ? extends Object> propertyGetter) throws IOException {
		return patch(entities, changes, propertyGetter, View.Persistence.class);
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, 'update')", filterTarget = "entities")
	public List<T> patch(final Iterable<T> entities, final Map<String, Object> changes,
			final Function<T, ? extends Object> propertyGetter, final Class<?> view) throws IOException {
		final JsonNode tree = objectMapperForPatchTree.valueToTree(changes);
		final Map<String, T> oldEntities = new HashMap<>();
		for (final T entity : entities) {
			final T oldEntity = cloneEntity(entity);
			oldEntities.put(entity.getId(), oldEntity);
			final Object obj = propertyGetter.apply(entity);
			final ObjectReader reader = objectMapper.readerForUpdating(obj).withView(view);
			reader.readValue(tree);
			entity.setUpdateTimestamp(new Date());
			preparePatch(entity);
			eventPublisher.publishEvent(new BeforePatchEvent<>(this, entity, oldEntity, propertyGetter, changes));
			validate(entity);
		}

		final List<T> patchedEntities = repository.saveAll(entities);
		patchedEntities.forEach((e) -> {
			eventPublisher.publishEvent(new AfterPatchEvent<>(
					this, e, oldEntities.get(e.getId()), propertyGetter, changes));
			modifyRetrieved(e);
		});
		eventPublisher.publishEvent(new BulkChangeEvent<>(this, this.type, entities));

		return patchedEntities;
	}

	/**
	 * This method can be overridden by subclasses to modify the entity before patching. By default, the implementation
	 * of {@link #prepareUpdate} is used.
	 *
	 * @param entity The entity to be patched
	 */
	protected void preparePatch(final T entity) {
		prepareUpdate(entity);
	}

	@Override
	@PreAuthorize("hasPermission(#entity, 'delete')")
	public void delete(final T entity) {
		prepareDelete(entity);
		eventPublisher.publishEvent(new BeforeDeletionEvent<>(this, entity));
		repository.delete(entity);
		eventPublisher.publishEvent(new AfterDeletionEvent<>(this, entity));
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, 'delete')", filterTarget = "entities")
	public void delete(final Iterable<T> entities) {
		for (final T entity : entities) {
			prepareDelete(entity);
			eventPublisher.publishEvent(new BeforeDeletionEvent<>(this, entity));
		}
		repository.deleteAll(entities);
		for (final T entity : entities) {
			eventPublisher.publishEvent(new AfterDeletionEvent<>(this, entity));
		}
		eventPublisher.publishEvent(new BulkChangeEvent<>(this, this.type, entities));
	}

	/**
	 * This method can be overridden by subclasses to do additional entity related actions before deletion.
	 *
	 * @param entity The entity to be deleted
	 */
	protected void prepareDelete(final T entity) {
		/* Implementation provided by subclasses. */
	}

	/**
	 * This method can be overridden by subclasses to modify a retrieved entity before it is returned by service
	 * methods.
	 *
	 * @param entity The entity to be modified
	 */
	protected void modifyRetrieved(final T entity) {
		/* Implementation provided by subclasses. */
	}

	protected void validate(final T entity) {
		final Errors errors = new BeanPropertyBindingResult(entity, type.getName());
		validator.validate(entity, errors);
		if (errors.hasErrors()) {
			throw new EntityValidationException(errors, entity);
		}
	}

	private T cloneEntity(final T entity) throws JsonProcessingException {
		return objectMapper.readerFor(entity.getClass()).withView(View.Persistence.class).readValue(
				objectMapper.writerWithView(View.Persistence.class)
				.writeValueAsString(entity));
	}

	public String getTypeName() {
		return type.getSimpleName().toLowerCase();
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	@Component
	public static class EntityCacheHandler {
		@CachePut(
				cacheNames = "entity",
				key = "#event.entity.supertype.simpleName.toLowerCase() + '-' + #event.entity.id",
				condition = "#event.entity.internal == false")
		@EventListener
		public Entity handleCreate(final AfterCreationEvent event) {
			return event.getEntity();
		}

		@CachePut(
				cacheNames = "entity",
				key = "#event.entity.supertype.simpleName.toLowerCase() + '-' + #event.entity.id",
				condition = "#event.entity.internal == false")
		@CacheEvict(
				cacheNames = "entity",
				key = "#event.entity.supertype.simpleName.toLowerCase() + '-' + #event.entity.id",
				condition = "#event.entity.internal == true")
		@EventListener
		public Entity handleUpdate(final AfterUpdateEvent event) {
			return event.getEntity();
		}

		@CacheEvict(
				cacheNames = "entity",
				key = "#event.entity.supertype.simpleName.toLowerCase() + '-' + #event.entity.id")
		@EventListener
		public void handleDelete(final AfterDeletionEvent event) {

		}
	}
}
