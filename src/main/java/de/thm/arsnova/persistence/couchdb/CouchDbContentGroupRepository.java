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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.persistence.couchdb;

import java.util.List;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;

import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.persistence.ContentGroupRepository;

public class CouchDbContentGroupRepository extends CouchDbCrudRepository<ContentGroup>
		implements ContentGroupRepository {
	public CouchDbContentGroupRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(ContentGroup.class, db, "by_id", createIfNotExists);
	}

	@Override
	public ContentGroup findByRoomIdAndName(final String roomId, final String name) {
		final List<ContentGroup> contentGroupList = db.queryView(createQuery("by_roomid_name")
						.key(ComplexKey.of(roomId, name))
						.includeDocs(true)
						.reduce(false),
				ContentGroup.class);

		return !contentGroupList.isEmpty() ? contentGroupList.get(0) : null;
	}

	@Override
	public List<ContentGroup> findByRoomId(final String roomId) {
		final List<ContentGroup> contentGroups = db.queryView(createQuery("by_roomid_name")
						.startKey(ComplexKey.of(roomId))
						.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
						.includeDocs(true)
						.reduce(false),
				ContentGroup.class);

		return contentGroups;
	}
}
