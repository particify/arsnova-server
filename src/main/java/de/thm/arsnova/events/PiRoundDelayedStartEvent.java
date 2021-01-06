/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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
 * Fires whenever a delayed peer instruction round is initiated. The delayed part denotes that this round might not
 * have been started yet.
 */
public class PiRoundDelayedStartEvent extends SessionEvent {

	private static final long serialVersionUID = 1L;
	private final String questionId;
	private final Long startTime;
	private final Long endTime;
	private final String questionVariant;
	private int piRound;

	public PiRoundDelayedStartEvent(Object source, Session session, Question question) {
		super(source, session);
		this.questionId = question.get_id();
		this.startTime = question.getPiRoundStartTime();
		this.endTime = question.getPiRoundEndTime();
		this.questionVariant = question.getQuestionVariant();
		this.piRound = question.getPiRound();
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public String getQuestionId() {
		return questionId;
	}

	public Long getStartTime() {
		return startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public String getQuestionVariant() {
		return questionVariant;
	}

	public Integer getPiRound() {
		return piRound;
	}

	public Map<String, Object> getPiRoundInformations() {
		Map<String, Object> map = new HashMap<>();

		map.put("_id", getQuestionId());
		map.put("endTime", getEndTime());
		map.put("startTime", getStartTime());
		map.put("variant", getQuestionVariant());
		map.put("round", getPiRound());

		return map;
	}
}
