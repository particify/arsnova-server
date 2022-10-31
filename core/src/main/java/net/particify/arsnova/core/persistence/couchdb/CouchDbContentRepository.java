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

import java.util.List;
import java.util.stream.Collectors;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;

import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.persistence.ContentRepository;

public class CouchDbContentRepository extends CouchDbCrudRepository<Content> implements ContentRepository {
  public CouchDbContentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
    super(Content.class, db, "by_id", createIfNotExists);
  }

  @Override
  public int countByRoomId(final String roomId) {
    final ViewResult result = db.queryView(createQuery("by_roomid")
        .key(roomId)
        .reduce(true));

    return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
  }

  @Override
  public List<String> findIdsByRoomId(final String roomId) {
    final ViewResult result = db.queryView(createQuery("by_roomid").reduce(false).key(roomId));

    return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
  }

  @Override
  public Iterable<Content> findStubsByIds(final List<String> ids) {
    return super.createEntityStubs(db.queryView(createQuery("by_id")
        .keys(ids)
        .reduce(false)), (a, b) -> {});
  }

  @Override
  public Iterable<Content> findStubsByRoomId(final String roomId) {
    return createEntityStubs(db.queryView(createQuery("by_roomid")
        .key(roomId)
        .reduce(false)));
  }

  protected Iterable<Content> createEntityStubs(final ViewResult viewResult) {
    return super.createEntityStubs(viewResult, Content::setRoomId);
  }

  @Override
  public List<Content> findByRoomId(final String roomId) {
    return db.queryView(createQuery("by_roomid")
            .includeDocs(true)
            .reduce(false)
            .key(roomId),
        Content.class);
  }
}
