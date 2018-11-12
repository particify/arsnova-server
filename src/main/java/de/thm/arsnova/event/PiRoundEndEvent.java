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

import java.util.HashMap;
import java.util.Map;

/**
 * Fires whenever a peer instruction round has ended.
 */
public class PiRoundEndEvent extends RoomEvent {

	private static final long serialVersionUID = 1L;

	private final String contentId;
	private final String group;

	public PiRoundEndEvent(Object source, String roomId, Content content) {
		super(source, roomId);
		contentId = content.getId();
		/* FIXME: Event does not support content with multiple groups */
		this.group = content.getGroups().toArray(new String[1])[0];
	}

	public String getContentId() {
		return contentId;
	}

	public String getGroup() {
		return group;
	}

	public Map<String, String> getPiRoundEndInformations() {
		Map<String, String> map = new HashMap<>();

		map.put("_id", getContentId());
		map.put("variant", getGroup());

		return map;
	}

}
