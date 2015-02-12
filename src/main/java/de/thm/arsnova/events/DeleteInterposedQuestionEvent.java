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

import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.Session;

public class DeleteInterposedQuestionEvent extends SessionEvent {

	private static final long serialVersionUID = 1L;

	private final InterposedQuestion question;

	public DeleteInterposedQuestionEvent(Object source, Session session, InterposedQuestion question) {
		super(source, session);
		this.question = question;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public InterposedQuestion getQuestion() {
		return question;
	}

}
