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

package de.thm.arsnova.model.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;

import de.thm.arsnova.model.serialization.View;

/**
 * Once a user joins a session, this class is used to identify a returning user.
 */
@ApiModel(value = "LoggedIn", description = "Logged In entity - Contains the Room History (Visited Sessions)")
public class LoggedIn implements Entity {
	private String id;
	private String rev;
	private String user;
	private String sessionId;
	private long timestamp;
	private List<VisitedRoom> visitedSessions = new ArrayList<>();
	private boolean anonymized;

	public LoggedIn() {
		this.updateTimestamp();
	}

	public void addVisitedSession(Room s) {
		if (!isAlreadyVisited(s)) {
			this.visitedSessions.add(new VisitedRoom(s));
		}
	}

	private boolean isAlreadyVisited(Room s) {
		for (VisitedRoom vs : this.visitedSessions) {
			if (vs.getId().equals(s.getId())) {
				return true;
			}
		}
		return false;
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

	public void updateTimestamp() {
		this.timestamp = System.currentTimeMillis();
	}

	@JsonView(View.Persistence.class)
	public String getUser() {
		return user;
	}

	@JsonView(View.Persistence.class)
	public void setUser(String user) {
		this.user = user;
	}

	@JsonView(View.Persistence.class)
	public String getSessionId() {
		return sessionId;
	}

	@JsonView(View.Persistence.class)
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@JsonView(View.Persistence.class)
	public long getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@JsonView(View.Persistence.class)
	public List<VisitedRoom> getVisitedSessions() {
		return visitedSessions;
	}

	@JsonView(View.Persistence.class)
	public void setVisitedSessions(List<VisitedRoom> visitedSessions) {
		this.visitedSessions = visitedSessions;
	}

	@JsonView(View.Persistence.class)
	public boolean isAnonymized() {
		return anonymized;
	}

	@JsonView(View.Persistence.class)
	public void setAnonymized(boolean anonymized) {
		this.anonymized = anonymized;
	}

	@Override
	public String toString() {
		return "LoggedIn [id=" + id + ", rev=" + rev + ", type=" + getType()
				+ ", user=" + user + ", sessionId=" + sessionId
				+ ", timestamp=" + timestamp + ", visitedSessions="
				+ visitedSessions + "]";
	}
}
