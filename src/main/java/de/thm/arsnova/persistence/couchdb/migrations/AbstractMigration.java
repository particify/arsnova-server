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

package de.thm.arsnova.persistence.couchdb.migrations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.PagedMangoResponse;

/**
 * Performs common tasks for migrations including querying and updating of
 * entities in bulk, database index creation and storing the migration state.
 *
 * @author Daniel Gerhardt
 */
public abstract class AbstractMigration implements Migration {
	protected static final int LIMIT = 200;
	private static final Logger logger = LoggerFactory.getLogger(AbstractMigration.class);

	protected final MangoCouchDbConnector connector;
	private final String id;
	private final List<InterruptibleConsumer<MigrationState.Migration>> migrationStepHandlers = new ArrayList<>();

	public AbstractMigration(final String id, final MangoCouchDbConnector connector) {
		this.id = id;
		this.connector = connector;
	}

	/**
	 * Returns the migrations ID which is used for ordering and as prefix for
	 * index names.
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * Returns the number of steps which can be executed for this migration.
	 */
	@Override
	public int getStepCount() {
		return migrationStepHandlers.size();
	}

	/**
	 * Performs the next migration step or continues the current one.
	 *
	 * @param state determines which migration step will be performed
	 * @throws MigrationException on interruption
	 */
	@Override
	public void migrate(final MigrationState.Migration state) throws MigrationException {
		try {
			migrationStepHandlers.get(state.getStep()).accept(state);
		} catch (final InterruptedException e) {
			throw new MigrationException("Migration was interrupted.", e);
		}
	}

	protected <E extends MigrationEntity, R extends List<? extends MigrationEntity>> void addEntityMigrationStepHandler(
			final Class<E> type,
			final String indexName,
			final Map<String, Object> querySelector,
			final InterruptibleFunction<E, R> migrationStepHandler) {
		final String fullIndexName = "migration-" + id + '-' + indexName;
		migrationStepHandlers.add(state ->
				performEntityMigrationStep(type, fullIndexName, querySelector, state, migrationStepHandler));
	}

	protected void addCustomMigrationStepHandler(
			final InterruptibleConsumer<MigrationState.Migration> migrationStepHandler) {
		migrationStepHandlers.add(migrationStepHandler);
	}

	private <E extends MigrationEntity, R extends List<? extends MigrationEntity>> void performEntityMigrationStep(
			final Class<E> type,
			final String indexName,
			final Map<String, Object> querySelector,
			final MigrationState.Migration state,
			final InterruptibleFunction<E, R> migrationStepHandler)
			throws InterruptedException {
		connector.createPartialJsonIndex(indexName, Collections.emptyList(), querySelector);
		waitForIndex(indexName);

		final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
		query.setIndexDocument(indexName);
		query.setLimit(LIMIT);
		String bookmark = (String) state.getState();

		// CouchDB bookmarks are used to iterate through all documents. The
		// total number of items is not known upfront. Up to LIMIT documents are
		// retrieved at a time. Termination of the loop is guaranteed under the
		// assumption that the database server handles requests correctly. When
		// there are no more documents, the query using the current bookmark
		// will result in an empty set of documents which will trigger the break
		// condition in the middle of the loop.
		for (int skip = 0;; skip += LIMIT) {
			logger.debug("Migration progress: {}, bookmark: {}", skip, bookmark);
			query.setBookmark(bookmark);
			final PagedMangoResponse<E> response =
					connector.queryForPage(query, type);
			final List<E> entities = response.getEntities();
			bookmark = response.getBookmark();
			if (entities.size() == 0) {
				break;
			}

			final List<MigrationEntity> entitiesForUpdate = new ArrayList<>();
			for (final E entity : entities) {
				// Perform the domain-specific migration logic
				entitiesForUpdate.addAll(migrationStepHandler.apply(entity));
			}

			connector.executeBulk(entitiesForUpdate);
			state.setState(bookmark);
		}
		state.setState(null);
	}

	private void waitForIndex(final String name) throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			if (connector.initializeIndex(name)) {
				return;
			}
			Thread.sleep(10000 * Math.round(1.0 + 0.5 * i));
		}
	}
}
