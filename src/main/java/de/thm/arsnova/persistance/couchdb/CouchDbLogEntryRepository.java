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

import de.thm.arsnova.entities.LogEntry;
import de.thm.arsnova.persistance.LogEntryRepository;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CouchDbLogEntryRepository extends CouchDbRepositorySupport<LogEntry> implements LogEntryRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbLogEntryRepository.class);

	public CouchDbLogEntryRepository(Class<LogEntry> type, CouchDbConnector db, boolean createIfNotExists) {
		super(type, db, createIfNotExists);
	}

	@Override
	public void create(String event, LogEntry.LogLevel level, Map<String, Object> payload) {
		LogEntry log = new LogEntry(event, level.ordinal(), payload);
		try {
			db.create(log);
		} catch (final IllegalArgumentException e) {
			logger.error("Logging of '{}' event to database failed.", event, e);
		}
	}
}
