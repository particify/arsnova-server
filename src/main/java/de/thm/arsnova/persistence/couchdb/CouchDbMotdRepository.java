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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thm.arsnova.model.Motd;
import de.thm.arsnova.persistence.MotdRepository;

public class CouchDbMotdRepository extends CouchDbCrudRepository<Motd> implements MotdRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbMotdRepository.class);

	public CouchDbMotdRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Motd.class, db, "by_id", createIfNotExists);
	}

	@Override
	public List<Motd> findByRoomId(final String roomId) {
		return find("by_roomid", roomId);
	}

	private List<Motd> find(final String viewName, final String key) {
		return queryView(viewName, key);
	}
}
