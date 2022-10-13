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

package de.thm.arsnova.persistence.couchdb;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.repository.NoRepositoryBean;

import de.thm.arsnova.event.WriteRepositoryEvent;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.persistence.CrudRepository;

@NoRepositoryBean
abstract class CouchDbCrudRepository<T extends Entity>
    extends CouchDbRepositorySupport<T> implements CrudRepository<T, String>, ApplicationEventPublisherAware {
  protected ApplicationEventPublisher applicationEventPublisher;

  private final Class<T> type;
  private String countableAllViewName;

  protected CouchDbCrudRepository(
      final Class<T> type,
      final CouchDbConnector db,
      final String designDocName,
      final String countableAllViewName,
      final boolean createIfNotExists) {
    super(type, db, designDocName, createIfNotExists);
    this.type = type;
    this.countableAllViewName = countableAllViewName;
  }

  protected CouchDbCrudRepository(
      final Class<T> type,
      final CouchDbConnector db,
      final String countableAllViewName,
      final boolean createIfNotExists) {
    super(type, db, createIfNotExists);
    this.type = type;
    this.countableAllViewName = countableAllViewName;
  }

  protected String getCountableAllViewName() {
    return countableAllViewName;
  }

  @Override
  public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public <S extends T> S save(final S entity) {
    publishWriteEvent(false);
    final String id = entity.getId();
    if (id != null) {
      db.update(entity);
    } else {
      db.create(entity);
    }

    return entity;
  }

  @Override
  public <S extends T> List<S> saveAll(final Iterable<S> entities) {
    if (!(entities instanceof List)) {
      throw new IllegalArgumentException("Implementation only supports Lists.");
    }
    publishWriteEvent(true);
    final List<S> entityList = (List<S>) entities;
    db.executeBulk(entityList);

    return entityList;
  }

  @Override
  public Optional<T> findById(final String id) {
    try {
      return Optional.of(get(id));
    } catch (final DocumentNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public T findOne(final String id) {
    return get(id);
  }

  @Override
  public boolean existsById(final String id) {
    return contains(id);
  }

  @Override
  public List<T> findAll() {
    return db.queryView(createQuery(countableAllViewName).includeDocs(true).reduce(false), type);
  }

  @Override
  public List<T> findAllById(final Iterable<String> strings) {
    if (!(strings instanceof Collection)) {
      throw new IllegalArgumentException("Implementation only supports Collections.");
    }
    if (((Collection) strings).isEmpty()) {
      return Collections.emptyList();
    }

    return db.queryView(createQuery(countableAllViewName)
            .keys((Collection<String>) strings)
            .includeDocs(true).reduce(false),
        type);
  }

  @Override
  public long count() {
    return db.queryView(createQuery(countableAllViewName).reduce(true)).getRows().get(0).getValueAsInt();
  }

  @Override
  public void deleteById(final String id) {
    publishWriteEvent(false);
    final T entity = get(id);
    db.delete(id, entity.getRevision());
  }

  @Override
  public void delete(final T entity) {
    publishWriteEvent(false);
    db.delete(entity);
  }

  @Override
  public void deleteAll(final Iterable<? extends T> entities) {
    if (!(entities instanceof Collection)) {
      throw new IllegalArgumentException("Implementation only supports Collections.");
    }

    publishWriteEvent(true);
    final List<BulkDeleteDocument> docs = ((Collection<? extends T>) entities).stream()
        .map(entity -> new BulkDeleteDocument(entity.getId(), entity.getRevision()))
        .collect(Collectors.toList());
    db.executeBulk(docs);
  }

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException("Deletion of all entities is not supported for security reasons.");
  }

  @Override
  public void deleteAllById(final Iterable<? extends String> iterable) {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  /**
   * Creates stub entities from a ViewResult. Stub entities only have meta data (id, revision, reference id) set.
   *
   * @param viewResult A CouchDB ViewResult. The first part of its keys is expected to be the id of another entity.
   * @param keyPropertySetter A setter method of the Entity class which is called to store the first element of the
   *     key.
   * @return Entity stubs
   */
  protected List<T> createEntityStubs(final ViewResult viewResult, final BiConsumer<T, String> keyPropertySetter) {
    return viewResult.getRows().stream().map(row -> {
      final T stub;
      try {
        stub = type.newInstance();
        stub.setId(row.getId());
        stub.setRevision(row.getValueAsNode().get("_rev").asText());
        final String key = row.getKeyAsNode().isContainerNode()
            ? row.getKeyAsNode().get(0).asText() : row.getKey();
        keyPropertySetter.accept(stub, key);

        return stub;
      } catch (InstantiationException | IllegalAccessException e) {
        return null;
      }
    }).collect(Collectors.toList());
  }

  protected void publishWriteEvent(final boolean multiple) {
    this.applicationEventPublisher.publishEvent(
        new WriteRepositoryEvent(this, type.getSimpleName(), multiple));
  }
}
