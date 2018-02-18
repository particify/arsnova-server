package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.Entity;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoRepositoryBean
abstract class CouchDbCrudRepository<T extends Entity> extends CouchDbRepositorySupport<T> implements CrudRepository<T, String> {
	private String countableAllViewName;

	protected CouchDbCrudRepository(
			final Class<T> type,
			final CouchDbConnector db,
			final String designDocName,
			final String countableAllViewName,
			final boolean createIfNotExists) {
		super(type, db, designDocName, createIfNotExists);
		this.countableAllViewName = countableAllViewName;
	}

	protected CouchDbCrudRepository(
			final Class<T> type,
			final CouchDbConnector db,
			final String countableAllViewName,
			final boolean createIfNotExists) {
		super(type, db, createIfNotExists);
		this.countableAllViewName = countableAllViewName;
	}

	protected String getCountableAllViewName() {
		return countableAllViewName;
	}

	@Override
	public <S extends T> S save(final S entity) {
		final String id = entity.getId();
		if (id != null) {
			db.update(entity);
		} else {
			db.create(entity);
		}

		return entity;
	}

	@Override
	public <S extends T> Iterable<S> save(final Iterable<S> entities) {
		if (!(entities instanceof Collection)) {
			throw new IllegalArgumentException("Implementation only supports Collections.");
		}
		db.executeBulk((Collection<S>) entities);

		return entities;
	}

	@Override
	public T findOne(final String id) {
		return get(id);
	}

	@Override
	public boolean exists(final String id) {
		return contains(id);
	}

	@Override
	public Iterable<T> findAll() {
		return db.queryView(createQuery(countableAllViewName).includeDocs(true).reduce(false), type);
	}

	@Override
	public Iterable<T> findAll(final Iterable<String> strings) {
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
	public void delete(final String id) {
		T entity = get(id);
		db.delete(id, entity.getRevision());
	}

	@Override
	public void delete(final T entity) {
		db.delete(entity);
	}

	@Override
	public void delete(final Iterable<? extends T> entities) {
		if (!(entities instanceof Collection)) {
			throw new IllegalArgumentException("Implementation only supports Collections.");
		}

		final List<BulkDeleteDocument> docs = ((Collection<? extends T>) entities).stream()
				.map(entity -> new BulkDeleteDocument(entity.getId(), entity.getRevision()))
				.collect(Collectors.toList());
		db.executeBulk(docs);
	}

	@Override
	public void deleteAll() {
		throw new UnsupportedOperationException("Deletion of all entities is not supported for security reasons.");
	}
}
