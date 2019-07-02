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

package de.thm.arsnova.controller;

import java.util.Set;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.RoomService;

@RestController
@RequestMapping(RoomController.REQUEST_MAPPING)
public class RoomController extends AbstractEntityController<Room> {
	protected static final String REQUEST_MAPPING = "/room";
	private static final String GET_MODERATORS_MAPPING = DEFAULT_ID_MAPPING + "/moderator";
	private static final String MODERATOR_MAPPING = DEFAULT_ID_MAPPING + "/moderator/{userId}";

	private RoomService roomService;

	public RoomController(final RoomService roomService) {
		super(roomService);
		this.roomService = roomService;
	}

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}

	@Override
	protected String resolveAlias(final String shortId) {
		return roomService.getIdByShortId(shortId);
	}

	@GetMapping(GET_MODERATORS_MAPPING)
	public Set<Room.Moderator> getModerators(@PathVariable final String id) {
		return roomService.get(id).getModerators();
	}

	@PutMapping(MODERATOR_MAPPING)
	public void putModerator(@PathVariable final String id, @PathVariable final String userId,
			@RequestBody final Room.Moderator moderator) {
		final Room room = roomService.get(id);
		moderator.setUserId(userId);
		if (moderator.getRoles().isEmpty()) {
			moderator.getRoles().add(Room.Moderator.Role.EXECUTIVE_MODERATOR);
		}
		room.getModerators().removeIf(m -> m.getUserId().equals(userId));
		room.getModerators().add(moderator);
		roomService.update(room);
	}

	@DeleteMapping(MODERATOR_MAPPING)
	public void deleteModerator(@PathVariable final String id, @PathVariable final String userId) {
		final Room room = roomService.get(id);
		room.getModerators().removeIf(m -> m.getUserId().equals(userId));
		roomService.update(room);
	}
}
