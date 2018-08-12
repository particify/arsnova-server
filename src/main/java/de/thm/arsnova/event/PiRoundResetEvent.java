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
package de.thm.arsnova.event;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;

import java.util.HashMap;
import java.util.Map;

/**
 * Fires whenever a peer instruction round is reset.
 */
public class PiRoundResetEvent extends RoomEvent {

	private static final long serialVersionUID = 1L;

	private final String contentId;
	private final String group;

	public PiRoundResetEvent(Object source, Room room, Content content) {
		super(source, room);
		contentId = content.getId();
		/* FIXME: Event does not support content with multiple groups */
		this.group = content.getGroups().toArray(new String[1])[0];
	}

	@Override
	public void accept(ArsnovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public String getContentId() {
		return contentId;
	}

	public String getGroup() {
		return group;
	}

	public Map<String, String> getPiRoundResetInformations() {
		Map<String, String> map = new HashMap<>();

		map.put("_id", getContentId());
		map.put("variant", getGroup());

		return map;
	}

}
