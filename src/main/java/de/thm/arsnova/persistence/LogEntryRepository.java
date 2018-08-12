/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.persistence;

import de.thm.arsnova.model.migration.v2.LogEntry;

import java.util.HashMap;
import java.util.Map;

public interface LogEntryRepository {
	/**
	 * Logs an event to the database. Arbitrary data can be attached as payload. Database logging should only be used
	 * if the logged data is later analyzed by the backend itself. Otherwise use the default logging mechanisms.
	 *
	 * @param event type of the event
	 * @param level severity of the event
	 * @param payload arbitrary logging data
	 */
	void create(String event, LogEntry.LogLevel level, Map<String, Object> payload);

	/**
	 * Logs an event to the database. Arbitrary data can be attached as payload. Database logging should only be used
	 * if the logged data is later analyzed by the backend itself. Otherwise use the default logging mechanisms.
	 *
	 * @param event type of the event
	 * @param payload arbitrary logging data
	 * @param level severity of the event
	 */
	default void log(final String event, final Map<String, Object> payload, final LogEntry.LogLevel level) {
		create(event, level, payload);
	}

	/**
	 * Logs an event of informational severity to the database. Arbitrary data can be attached as payload. Database
	 * logging should only be used if the logged data is later analyzed by the backend itself. Otherwise use the default
	 * logging mechanisms.
	 *
	 * @param event type of the event
	 * @param payload arbitrary logging data
	 */
	default void log(final String event, final Map<String, Object> payload) {
		create(event, LogEntry.LogLevel.INFO, payload);
	}

	/**
	 * Logs an event to the database. Arbitrary data can be attached as payload. Database logging should only be used
	 * if the logged data is later analyzed by the backend itself. Otherwise use the default logging mechanisms.
	 *
	 * @param event type of the event
	 * @param level severity of the event
	 * @param rawPayload key/value pairs of arbitrary logging data
	 */
	default void log(final String event, final LogEntry.LogLevel level, final Object... rawPayload) {
		if (rawPayload.length % 2 != 0) {
			throw new IllegalArgumentException("");
		}
		Map<String, Object> payload = new HashMap<>();
		for (int i = 0; i < rawPayload.length; i += 2) {
			payload.put((String) rawPayload[i], rawPayload[i + 1]);
		}
		create(event, level, payload);
	}

	/**
	 * Logs an event of informational severity to the database. Arbitrary data can be attached as payload. Database
	 * logging should only be used if the logged data is later analyzed by the backend itself. Otherwise use the default
	 * logging mechanisms.
	 *
	 * @param event type of the event
	 * @param rawPayload key/value pairs of arbitrary logging data
	 */
	default void log(final String event, final Object... rawPayload) {
		log(event, LogEntry.LogLevel.INFO, rawPayload);
	}
}
