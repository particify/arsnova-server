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
package de.thm.arsnova.service;

import de.thm.arsnova.model.FindQuery;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomFindQueryService implements FindQueryService<Room> {
	private RoomService roomService;
	private UserService userService;

	public RoomFindQueryService(final RoomService roomService, final UserService userService) {
		this.roomService = roomService;
		this.userService = userService;
	}

	@Override
	public Set<String> resolveQuery(final FindQuery<Room> findQuery) {
		List<List<String>> ids = new ArrayList<>();
		if (findQuery.getExternalFilters().get("inHistoryOfUserId") instanceof String) {
			UserProfile inHistoryOfUser = userService.get(
					(String) findQuery.getExternalFilters().get("inHistoryOfUserId"));
			ids.add(inHistoryOfUser.getRoomHistory().stream()
					.map(UserProfile.RoomHistoryEntry::getRoomId).collect(Collectors.toList()));
		}
		if (findQuery.getProperties().getOwnerId() != null) {
			ids.add(roomService.getUserRoomIds(findQuery.getProperties().getOwnerId()));
		}

		return ids.stream().flatMap(list -> list.stream()).collect(Collectors.toSet());
	}
}
