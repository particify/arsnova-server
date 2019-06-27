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

package de.thm.arsnova.event;

import java.util.Set;

import de.thm.arsnova.model.Room;

/**
 * Fires whenever the feedback of a specific user has been reset.
 */
public class DeleteFeedbackForRoomsEvent extends ArsnovaEvent {

	private static final long serialVersionUID = 1L;

	private final Set<Room> sessions;

	private final String userId;

	public DeleteFeedbackForRoomsEvent(final Object source, final Set<Room> rooms, final String userId) {
		super(source);
		this.sessions = rooms;
		this.userId = userId;
	}

	public Set<Room> getSessions() {
		return sessions;
	}

	public String getUserId() {
		return userId;
	}

}
