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

import java.util.List;
import java.util.Map;

import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;

public interface FeedbackStorageService {
  Feedback getByRoom(Room room);

  Integer getByRoomAndUserId(Room room, String userId);

  void save(Room room, int value, String userId);

  Map<Room, List<String>> cleanVotes(int cleanupFeedbackDelay);

  List<String> cleanVotesByRoom(Room room, int cleanupFeedbackDelayInMins);

  Room findByRoomId(String id);
}
