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

package de.thm.arsnova.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.FindQuery;
import de.thm.arsnova.model.Room;

@Service
public class RoomFindQueryService implements FindQueryService<Room> {
  private RoomService roomService;

  public RoomFindQueryService(final RoomService roomService) {
    this.roomService = roomService;
  }

  @Override
  public Set<String> resolveQuery(final FindQuery<Room> findQuery) {
    final List<List<String>> ids = new ArrayList<>();
    if (findQuery.getProperties().getOwnerId() != null) {
      ids.add(roomService.getUserRoomIds(findQuery.getProperties().getOwnerId()));
    }

    return ids.stream().flatMap(list -> list.stream()).collect(Collectors.toSet());
  }
}
