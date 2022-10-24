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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
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
  private static final Logger logger = LoggerFactory.getLogger(DefaultEntityServiceImpl.class);

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
    objectMapperForPatchTree.registerModule(new JavaTimeModule());
  }

  @Override
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
  public List<T> get(final Iterable<String> ids) {
    final Map<String, Optional<T>> cachedEntities = StreamSupport.stream(ids.spliterator(), false)
        .map(id -> Map.entry(id, Optional.ofNullable(getCachedOrNull(id))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    final List<String> missingIds = cachedEntities.entrySet().stream()
        .filter(entry -> entry.getValue().isEmpty())
        .map(entry -> entry.getKey())
        .collect(Collectors.toList());

    if (!missingIds.isEmpty()) {
      logger.trace("Some entities in list have not yet been cached ({} out of {}).",
          missingIds.size(), cachedEntities.size());
      final List<T> entities = repository.findAllById(missingIds);
      for (final T entity : entities) {
        modifyRetrieved(entity);
        putInCache(entity);
        cachedEntities.put(entity.getId(), Optional.of(entity));
      }
    }

    return StreamSupport.stream(() -> ids.spliterator(), Spliterator.ORDERED, false)
        .map(id -> cachedEntities.get(id))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /** Check the cache without modifying it and return the cached entity or null if the entity is not cached. */
  @Cacheable(cacheNames = "entity", key = "#root.target.getTypeName() + '-' + #id", unless = "true")
  private T getCachedOrNull(final String id) {
    return null;
  }

  @CachePut(cacheNames = "entity", key = "#root.target.getTypeName() + '-' + #entity.id")
  private T putInCache(final T entity) {
    return entity;
  }

  @Override
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

  @Override
  public List<T> create(final List<T> entities) {
    if (entities.stream().anyMatch(e -> e.getId() != null || e.getRevision() != null)) {
      throw new IllegalArgumentException("At least one of the entities is not new.");
    }

    for (final T entity : entities) {
      entity.setCreationTimestamp(new Date());
      prepareCreate(entity);
      eventPublisher.publishEvent(new BeforeCreationEvent<>(this, entity));
      validate(entity);
    }

    repository.saveAll(entities);

    for (final T entity : entities) {
      eventPublisher.publishEvent(new AfterCreationEvent<>(this, entity));
      finalizeCreate(entity);
      modifyRetrieved(entity);
    }

    return entities;
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

  @Override
  public T update(final T entity) {
    return update(entity, View.Persistence.class);
  }

  @Override
  public T update(final T entity, final Class<?> view) {
    return update(repository.findOne(entity.getId()), entity, view);
  }

  @Override
  public T update(final T oldEntity, final T newEntity, final Class<?> view) {
    final T entityForUpdate = cloneEntity(oldEntity);
    final ObjectReader reader = objectMapper.readerForUpdating(entityForUpdate).withView(view);
    final JsonNode tree = objectMapperForPatchTree.valueToTree(newEntity);
    try {
      reader.readValue(tree);
    } catch (final IOException e) {
      throw new RuntimeException("JSON (de-)serialization should never fail here.", e);
    }
    entityForUpdate.setId(oldEntity.getId());
    entityForUpdate.setUpdateTimestamp(new Date());

    prepareUpdate(entityForUpdate);
    eventPublisher.publishEvent(new BeforeFullUpdateEvent<>(this, entityForUpdate, oldEntity));
    validate(entityForUpdate);
    final T updatedEntity = repository.save(entityForUpdate);
    eventPublisher.publishEvent(new AfterFullUpdateEvent<>(
        this, updatedEntity, oldEntity, getChanges(oldEntity, updatedEntity)));
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
    eventPublisher.publishEvent(new AfterPatchEvent<>(
        this, patchedEntity, oldEntity, propertyGetter, getChanges(oldEntity, patchedEntity), changes));
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
          this, e, oldEntities.get(e.getId()), propertyGetter,
          getChanges(oldEntities.get(e.getId()), e), changes));
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
  public void delete(final T entity) {
    prepareDelete(entity);
    eventPublisher.publishEvent(new BeforeDeletionEvent<>(this, entity));
    repository.delete(entity);
    eventPublisher.publishEvent(new AfterDeletionEvent<>(this, entity));
  }

  @Override
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

  private T cloneEntity(final T entity) {
    try {
      return objectMapper.readerFor(entity.getClass()).withView(View.Persistence.class).readValue(
          objectMapper.writerWithView(View.Persistence.class)
          .writeValueAsString(entity));
    } catch (final JsonProcessingException e) {
      throw new RuntimeException("JSON (de-)serialization should never fail here.", e);
    }
  }

  private Map<String, Object> getChanges(final T oldEntity, final T newEntity) {
    final JsonNode oldEntityTree = objectMapper.valueToTree(oldEntity);
    final JsonNode newEntityTree = objectMapper.valueToTree(newEntity);
    final JsonNode changes = getChangesRecursively(oldEntityTree, newEntityTree);
    try {
      return objectMapper.treeToValue(changes, Map.class);
    } catch (final JsonProcessingException e) {
      logger.error("Failed to transform entity changes tree for {}.", type.getName(), e);
      return Collections.emptyMap();
    }
  }

  private ObjectNode getChangesRecursively(final JsonNode oldNode, final JsonNode newNode) {
    final ObjectNode changes = objectMapper.createObjectNode();
    final Iterable<String> oldFieldNames = () -> oldNode.fieldNames();
    final Iterable<String> newFieldNames = () -> newNode.fieldNames();
    final Set<String> fieldNames = Stream.concat(
        StreamSupport.stream(oldFieldNames.spliterator(), false),
        StreamSupport.stream(newFieldNames.spliterator(), false))
        .collect(Collectors.toSet());
    for (final String fieldName : fieldNames) {
      final JsonNode oldInnerNode = oldNode.get(fieldName);
      final JsonNode newInnerNode = newNode.get(fieldName);
      if (oldInnerNode == null || newInnerNode == null || !oldInnerNode.isObject() || !newInnerNode.isObject()) {
        if (!Objects.equals(oldInnerNode, newInnerNode)) {
          changes.set(fieldName, newInnerNode);
        }
      } else {
        final ObjectNode innerChanges = getChangesRecursively(oldInnerNode, newInnerNode);
        if (innerChanges.size() > 0) {
          changes.set(fieldName, innerChanges);
        }
      }
    }

    return changes;
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
