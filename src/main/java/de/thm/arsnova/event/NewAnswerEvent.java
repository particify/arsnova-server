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
package de.thm.arsnova.event;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;

/**
 * Fires whenever a new answer is added.
 */
public class NewAnswerEvent extends RoomEvent {

	private static final long serialVersionUID = 1L;

	private final Answer answer;

	private final String userId;

	private final Content content;

	public NewAnswerEvent(Object source, Room room, Answer answer, String userId, Content content) {
		super(source, room);
		this.answer = answer;
		this.userId = userId;
		this.content = content;
	}

	public Answer getAnswer() {
		return answer;
	}

	public String getUserId() {
		return userId;
	}

	public Content getContent() {
		return content;
	}
}
