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
package de.thm.arsnova.model.migration.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;

import java.util.Map;

public class LogEntry implements Entity {
	public enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL
	}

	private String id;
	private String rev;
	private long timestamp = System.currentTimeMillis();
	private String event;
	private int level;
	private Map<String, Object> payload;

	public LogEntry(@JsonProperty String event, @JsonProperty int level, @JsonProperty Map<String, Object> payload) {
		this.event = event;
		this.level = level;
		this.payload = payload;
	}

	@JsonView(View.Persistence.class)
	public String getId() {
		return id;
	}

	@JsonView(View.Persistence.class)
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView(View.Persistence.class)
	public String getRevision() {
		return rev;
	}

	@JsonView(View.Persistence.class)
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView(View.Persistence.class)
	public long getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
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
	public void setLevel(final LogLevel level) {
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
}
