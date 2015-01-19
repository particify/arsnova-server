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
package de.thm.arsnova.events;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public class NewAnswerEvent extends NovaEvent {

	private static final long serialVersionUID = 1L;

	private final Answer answer;

	private final User user;

	private final Question question;

	private final Session session;

	public NewAnswerEvent(Object source, Answer answer, User user, Question question, Session session) {
		super(source);
		this.answer = answer;
		this.user = user;
		this.question = question;
		this.session = session;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public Answer getAnswer() {
		return answer;
	}

	public User getUser() {
		return user;
	}

	public Question getQuestion() {
		return question;
	}

	public Session getSession() {
		return session;
	}

}
