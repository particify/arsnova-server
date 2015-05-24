/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.entities.transport;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public class AnswerQueueElement {

	private final Session session;

	private final Question question;

	private final Answer answer;

	private final User user;

	public AnswerQueueElement(Session session, Question question, Answer answer, User user) {
		this.session = session;
		this.question = question;
		this.answer = answer;
		this.user = user;
	}

	public Session getSession() {
		return session;
	}

	public Question getQuestion() {
		return question;
	}

	public Answer getAnswer() {
		return answer;
	}

	public User getUser() {
		return user;
	}
}
