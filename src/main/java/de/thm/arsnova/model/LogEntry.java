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

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Map;
import java.util.Objects;

import de.thm.arsnova.model.serialization.View;

public class LogEntry extends Entity {
	public enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL
	}

	private String event;
	private int level;
	private Map<String, Object> payload;

	public LogEntry(@JsonProperty final String event, @JsonProperty final int level,
			@JsonProperty final Map<String, Object> payload) {
		this.event = event;
		this.level = level;
		this.payload = payload;
	}

	@JsonView(View.Persistence.class)
	public String getEvent() {
		return event;
	}

	@JsonView(View.Persistence.class)
	public void setEvent(final String event) {
		this.event = event;
	}

	@JsonView(View.Persistence.class)
	public int getLevel() {
		return level;
	}

	@JsonView(View.Persistence.class)
	public void setLevel(final int level) {
		this.level = level;
	}

	@JsonView(View.Persistence.class)
	public void setLevel(final de.thm.arsnova.model.migration.v2.LogEntry.LogLevel level) {
		this.level = level.ordinal();
	}

	@JsonView(View.Persistence.class)
	public Map<String, Object> getPayload() {
		return payload;
	}

	@JsonView(View.Persistence.class)
	public void setPayload(final Map<String, Object> payload) {
		this.payload = payload;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The following fields of <tt>LogEntry</tt> are excluded from equality checks:
	 * {@link #payload}.
	 * </p>
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final LogEntry logEntry = (LogEntry) o;

		return level == logEntry.level
				&& Objects.equals(id, logEntry.id)
				&& Objects.equals(rev, logEntry.rev)
				&& Objects.equals(creationTimestamp, logEntry.creationTimestamp)
				&& Objects.equals(updateTimestamp, logEntry.updateTimestamp)
				&& Objects.equals(event, logEntry.event);
	}
}
