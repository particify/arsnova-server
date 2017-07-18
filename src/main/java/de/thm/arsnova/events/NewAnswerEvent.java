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
package de.thm.arsnova.events;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

/**
 * Fires whenever a new answer is added.
 */
public class NewAnswerEvent extends SessionEvent {

	private static final long serialVersionUID = 1L;

	private final Answer answer;

	private final User user;

	private final Content content;

	public NewAnswerEvent(Object source, Session session, Answer answer, User user, Content content) {
		super(source, session);
		this.answer = answer;
		this.user = user;
		this.content = content;
	}

	@Override
	public void accept(ArsnovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public Answer getAnswer() {
		return answer;
	}

	public User getUser() {
		return user;
	}

	public Content getContent() {
		return content;
	}
}
