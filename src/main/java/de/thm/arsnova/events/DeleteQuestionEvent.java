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

import de.thm.arsnova.dao.DeletionInfo;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;

/**
 * This event gets fired when a question is deleted.
 *
 * When a question gets deleted, all associated answers are deleted as well. However, the corresponding
 * DeleteAnswerEvents will *not* get fired.
 */
public class DeleteQuestionEvent extends SessionEvent {

	private static final long serialVersionUID = 1L;

	private final Question question;

	private final DeletionInfo deletedAnswers;

	public DeleteQuestionEvent(Object source, Session session, Question question, DeletionInfo answerDeletionInfo) {
		super(source, session);
		this.question = question;
		this.deletedAnswers = answerDeletionInfo;
	}

	public Question getQuestion() {
		return this.question;
	}

	public int countDeletedAnswers() {
		return this.deletedAnswers.count();
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

}
