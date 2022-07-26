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

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.RoomRepository;

public class CouchDbRoomRepository extends CouchDbCrudRepository<Room> implements RoomRepository {
	public CouchDbRoomRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Room.class, db, "by_id", createIfNotExists);
	}

	@Override
	public Room findByShortId(final String shortId) {
		if (shortId == null) {
			return null;
		}
		final List<Room> roomList = queryView("by_shortid", shortId);

		return !roomList.isEmpty() ? roomList.get(0) : null;
	}

	@Override
	public List<Room> findByOwnerId(final String ownerId, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_pool_ownerid_name")
						.skip(qSkip)
						.limit(qLimit)
						.startKey(ComplexKey.of(false, ownerId))
						.endKey(ComplexKey.of(false, ownerId, ComplexKey.emptyObject()))
						.includeDocs(true),
				Room.class);
	}

	@Override
	public List<String> findIdsByOwnerId(final String ownerId) {
		final ViewResult result = db.queryView(createQuery("by_ownerid")
				.key(ownerId)
				.includeDocs(false));

		return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
	}

	@Override
	public List<String> findIdsByLmsCourseIds(final List<String> lmsCourseIds) {
		final ViewResult result = db.queryView(createQuery("by_lmscourseid")
				.keys(lmsCourseIds)
				.includeDocs(false));

		return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
	}

	@Override
	public List<Room> findStubsByScheduledDeletionAfter(final Date scheduledDeletion) {
		final ViewResult result = db.queryView(createQuery("by_scheduleddeletion")
				.endKey(scheduledDeletion.getTime())
				.includeDocs(false));
		return createEntityStubs(result, (r, k) -> {});
	}
}
