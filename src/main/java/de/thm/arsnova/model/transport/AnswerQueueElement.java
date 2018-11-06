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
package de.thm.arsnova.model.transport;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;

/**
 * An answer that is about to get saved in the database. Answers are not saved immediately, they are instead stored
 * in a queue that is cleared at specific intervals.
 */
public class AnswerQueueElement {

	private final Room room;

	private final Content content;

	private final Answer answer;

	private final String userId;

	public AnswerQueueElement(Room room, Content content, Answer answer, String userId) {
		this.room = room;
		this.content = content;
		this.answer = answer;
		this.userId = userId;
	}

	public Room getRoom() {
		return room;
	}

	public Content getQuestion() {
		return content;
	}

	public Answer getAnswer() {
		return answer;
	}

	public String getUserId() {
		return userId;
	}
}
