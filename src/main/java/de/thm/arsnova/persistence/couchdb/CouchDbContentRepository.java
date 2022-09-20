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

import java.util.List;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.persistence.ContentRepository;

public class CouchDbContentRepository extends CouchDbCrudRepository<Content> implements ContentRepository {
	public CouchDbContentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Content.class, db, "by_id", createIfNotExists);
	}

	@Override
	public List<Content> findByRoomIdForUsers(final String roomId) {
		return findByRoomId(roomId);
	}

	@Override
	public List<Content> findByRoomIdForSpeaker(final String roomId) {
		return findByRoomId(roomId);
	}

	@Override
	public int countByRoomId(final String roomId) {
		final ViewResult result = db.queryView(createQuery("by_roomid")
				.key(roomId)
				.reduce(true));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
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
