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

import java.util.Date;
import java.util.HashMap;

import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;

public class PiRoundDelayedStartEvent extends SessionEvent {

	private static final long serialVersionUID = 1L;

	private final String questionId;

	private final Long startTime;

	private final Long endTime;

	public PiRoundDelayedStartEvent(Object source, Session session, Question question) {
		super(source, session);
		this.questionId = question.get_id();
		this.startTime = question.getPiRoundStartTime();
		this.endTime = question.getPiRoundEndTime();
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

	public HashMap<String, String> getPiRoundInformations() {
		HashMap<String, String> map = new HashMap<String, String>();

		map.put("id", getQuestionId());
		map.put("endTime", getEndTime().toString());
		map.put("startTime", getStartTime().toString());
		map.put("actualTime", String.valueOf(new Date().getTime()));

		return map;
	}

}
