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
package de.thm.arsnova.events;

import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;

import java.util.List;

/**
 * Fires whenever a set of questions are enabled, i.e., they become visible to students.
 */
public class UnlockQuestionsEvent extends SessionEvent {

	private static final long serialVersionUID = 1L;

	private List<Question> questions;

	public UnlockQuestionsEvent(Object source, Session session, List<Question> questions) {
		super(source, session);
		this.questions = questions;
	}

	public List<Question> getQuestions() {
		return this.questions;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

}
