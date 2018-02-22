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
package de.thm.arsnova.events;

import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * Fires whenever voting on a question is enabled.
 */
public class UnlockVoteEvent extends SessionEvent {

	private static final long serialVersionUID = 1L;

	private final Question question;

	public UnlockVoteEvent(Object source, Session session, Question question) {
		super(source, session);
		this.question = question;
	}

	public String getQuestionId() {
		return this.question.get_id();
	}

	public String getQuestionVariant() {
		return this.question.getQuestionVariant();
	}

	public Boolean getVotingDisabled() {
		return this.question.isVotingDisabled();
	}

	public Map<String, Object> getVotingAdmission() {
		Map<String, Object> map = new HashMap<>();

		map.put("_id", getQuestionId());
		map.put("variant", getQuestionVariant());
		return map;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}
}
