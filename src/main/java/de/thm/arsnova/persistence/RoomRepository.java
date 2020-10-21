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

package de.thm.arsnova.persistence;

import java.util.List;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.migration.v2.ClientAuthentication;
import de.thm.arsnova.model.transport.ImportExportContainer;
import net.particify.arsnova.connector.model.Course;

public interface RoomRepository extends CrudRepository<Room, String> {
	Room findByShortId(String shortId);

	List<Room> findByOwner(ClientAuthentication owner, int start, int limit);

	List<Room> findByOwnerId(String ownerId, int start, int limit);

	List<String> findIdsByOwnerId(String ownerId);

	List<String> findIdsByModeratorId(String moderatorId);

	List<Room> findAllForPublicPool();

	List<Room> findForPublicPoolByOwnerId(String ownerId);

	List<Room> getRoomsWithStatsForOwnerId(String ownerId, int start, int limit);

	List<Room> getRoomHistoryWithStatsForUser(List<Room> rooms, String ownerId);

	List<Room> findInfosForPublicPool();

	List<Room> findInfosForPublicPoolByOwnerId(String ownerId);

	List<Room> findRoomsByCourses(List<Course> courses);

	Room importRoom(String userId, ImportExportContainer importRoom);

	ImportExportContainer exportRoom(String id, Boolean withAnswer, Boolean withFeedbackQuestions);
}
