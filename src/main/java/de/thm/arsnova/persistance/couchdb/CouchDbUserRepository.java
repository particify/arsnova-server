/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
package de.thm.arsnova.persistance.couchdb;

import com.google.common.collect.Lists;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.persistance.UserRepository;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CouchDbUserRepository extends CouchDbCrudRepository<DbUser> implements UserRepository {
	private static final int BULK_PARTITION_SIZE = 500;

	private static final Logger logger = LoggerFactory.getLogger(CouchDbUserRepository.class);

	public CouchDbUserRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(DbUser.class, db, "by_username", createIfNotExists);
	}

	private void log(Object... strings) {
		/* TODO: method stub */
	}

	@Override
	public DbUser save(final DbUser user) {
		final String id = user.getId();

		if (null != id) {
			db.update(user);
		}

		db.create(user);

		return user;
	}

	@Override
	public DbUser findByUsername(final String username) {
		final List<DbUser> users = queryView("by_username", username);

		return !users.isEmpty() ? users.get(0) : null;
	}

	@Override
	public void delete(final DbUser user) {
		if (db.delete(user) != null) {
			log("delete", "type", "user", "id", user.getId());
		} else {
			logger.error("Could not delete user {}", user.getId());
			throw new DbAccessException("Could not delete document.");
		}
	}

	@Override
	public int deleteInactiveUsers(final long lastActivityBefore) {
		final ViewQuery q = createQuery("by_creation_for_inactive").endKey(lastActivityBefore);
		final List<ViewResult.Row> rows = db.queryView(q).getRows();

		int count = 0;
		final List<List<ViewResult.Row>> partitions = Lists.partition(rows, BULK_PARTITION_SIZE);
		for (final List<ViewResult.Row> partition: partitions) {
			final List<BulkDeleteDocument> newDocs = new ArrayList<>();
			for (final ViewResult.Row oldDoc : partition) {
				final BulkDeleteDocument newDoc = new BulkDeleteDocument(oldDoc.getId(), oldDoc.getValue());
				newDocs.add(newDoc);
				logger.debug("Marked user document {} for deletion.", oldDoc.getId());
			}

			if (newDocs.size() > 0) {
				final List<DocumentOperationResult> results = db.executeBulk(newDocs);
				if (!results.isEmpty()) {
					/* TODO: This condition should be improved so that it checks the operation results. */
					count += newDocs.size();
				}
			}
		}

		if (count > 0) {
			logger.info("Deleted {} inactive users.", count);
			log("cleanup", "type", "user", "count", count);
		}

		return count;
	}
}
