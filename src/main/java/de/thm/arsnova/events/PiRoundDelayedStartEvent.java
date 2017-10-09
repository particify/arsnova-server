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

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Room;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Fires whenever a delayed peer instruction round is initiated. The delayed part denotes that this round might not
 * have been started yet.
 */
public class PiRoundDelayedStartEvent extends RoomEvent {

	private static final long serialVersionUID = 1L;
	private final String questionId;
	private final Date startTime;
	private final Date endTime;
	private final String group;
	private int piRound;

	public PiRoundDelayedStartEvent(Object source, Room room, Content content) {
		super(source, room);
		this.questionId = content.getId();
		this.group = content.getGroup();
		this.piRound = content.getState().getRound();
		this.endTime = content.getState().getRoundEndTimestamp();
		this.startTime = new Date();
	}

	@Override
	public void accept(ArsnovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public String getQuestionId() {
		return questionId;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getGroup() {
		return group;
	}

	public Integer getPiRound() {
		return piRound;
	}

	public Map<String, Object> getPiRoundInformations() {
		Map<String, Object> map = new HashMap<>();

		map.put("_id", getQuestionId());
		map.put("endTime", getEndTime().getTime());
		map.put("startTime", getStartTime().getTime());
		map.put("variant", getGroup());
		map.put("round", getPiRound());

		return map;
	}
}
