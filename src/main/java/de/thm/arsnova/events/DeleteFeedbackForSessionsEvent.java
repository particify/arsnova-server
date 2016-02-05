/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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
package de.thm.arsnova.events;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

import java.util.Set;

/**
 * Fires whenever the feedback of a specific user has been reset.
 */
public class DeleteFeedbackForSessionsEvent extends NovaEvent {

	private static final long serialVersionUID = 1L;

	private final Set<Session> sessions;

	private final User user;

	public DeleteFeedbackForSessionsEvent(Object source, Set<Session> sessions, User user) {
		super(source);
		this.sessions = sessions;
		this.user = user;
	}

	public Set<Session> getSessions() {
		return sessions;
	}

	public User getUser() {
		return user;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

}
