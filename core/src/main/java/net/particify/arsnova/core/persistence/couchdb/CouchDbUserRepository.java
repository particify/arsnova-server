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

package net.particify.arsnova.core.persistence.couchdb;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.persistence.UserRepository;

public class CouchDbUserRepository extends CouchDbCrudRepository<UserProfile> implements UserRepository {
  private static final int BULK_PARTITION_SIZE = 500;

  private static final Logger logger = LoggerFactory.getLogger(CouchDbUserRepository.class);

  public CouchDbUserRepository(final CouchDbConnector db, final boolean createIfNotExists) {
    super(UserProfile.class, db, "by_id", createIfNotExists);
  }

  private void log(final Object... strings) {
    /* TODO: method stub */
  }

  @Override
  public UserProfile findByAuthProviderAndLoginId(final UserProfile.AuthProvider authProvider, final String loginId) {
    final List<UserProfile> users = queryView("by_authprovider_loginid",
        ComplexKey.of(authProvider.toString(), loginId));

    return !users.isEmpty() ? users.get(0) : null;
  }

  @Override
  public List<UserProfile> findByLoginId(final String loginId) {
    final List<UserProfile> users = queryView("by_loginid", loginId);

    return users;
  }

  @Override
  public void delete(final UserProfile user) {
    if (db.delete(user) != null) {
      log("delete", "type", "user", "id", user.getId());
    } else {
      logger.error("Could not delete user {}", user.getId());
      throw new DbAccessException("Could not delete document.");
    }
  }

  @Override
  public int deleteInactiveUsers(final long lastActivityBefore) {
    final ViewQuery q = createQuery("by_creationtimestamp_for_inactive").endKey(lastActivityBefore);
    final List<ViewResult.Row> rows = db.queryView(q).getRows();

    int count = 0;
    final List<List<ViewResult.Row>> partitions = Lists.partition(rows, BULK_PARTITION_SIZE);
    for (final List<ViewResult.Row> partition : partitions) {
      final List<BulkDeleteDocument> newDocs = new ArrayList<>();
      for (final ViewResult.Row oldDoc : partition) {
        final BulkDeleteDocument newDoc =
            new BulkDeleteDocument(oldDoc.getId(), oldDoc.getValueAsNode().get("_rev").asText());
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
      logger.info("Deleted {} non-activated user accounts.", count);
      log("cleanup", "type", "user", "count", count);
    }

    return count;
  }
}
