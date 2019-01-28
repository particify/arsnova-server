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
package de.thm.arsnova.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Once a user joins a session, this class is used to identify a returning user.
 */
public class LoggedIn {

	private String _id;
	private String _rev;
	private String type;
	private String user;
	private String sessionId;
	private long timestamp;
	private List<VisitedSession> visitedSessions = new ArrayList<>();
	private List<String> _conflicts;
	private boolean anonymized;

	public LoggedIn() {
		this.type = "logged_in";
		this.updateTimestamp();
	}

	public void addVisitedSession(Session s) {
		if (!isAlreadyVisited(s)) {
			this.visitedSessions.add(new VisitedSession(s));
		}
	}

	private boolean isAlreadyVisited(Session s) {
		for (VisitedSession vs : this.visitedSessions) {
			if (vs.get_id().equals(s.get_id())) {
				return true;
			}
		}
		return false;
	}

	public void updateTimestamp() {
		this.timestamp = System.currentTimeMillis();
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String get_rev() {
		return _rev;
	}

	public void set_rev(String _rev) {
		this._rev = _rev;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public List<VisitedSession> getVisitedSessions() {
		return visitedSessions;
	}

	public void setVisitedSessions(List<VisitedSession> visitedSessions) {
		this.visitedSessions = visitedSessions;
	}

	public List<String> get_conflicts() {
		return _conflicts;
	}

	public void set_conflicts(List<String> _conflicts) {
		this._conflicts = _conflicts;
	}

	public boolean hasConflicts() {
		return !(_conflicts == null || _conflicts.isEmpty());
	}

	public boolean isAnonymized() {
		return anonymized;
	}

	public void setAnonymized(boolean anonymized) {
		this.anonymized = anonymized;
	}

	@Override
	public String toString() {
		return "LoggedIn [_id=" + _id + ", _rev=" + _rev + ", type=" + type
				+ ", user=" + user + ", sessionId=" + sessionId
				+ ", timestamp=" + timestamp + ", visitedSessions="
				+ visitedSessions + "]";
	}
}
