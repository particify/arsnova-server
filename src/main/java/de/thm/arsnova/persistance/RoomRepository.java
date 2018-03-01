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
package de.thm.arsnova.persistance;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.transport.ImportExportContainer;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RoomRepository extends CrudRepository<Room, String> {
	Room findByShortId(String shortId);
	List<Room> findInactiveGuestRoomsMetadata(long lastActivityBefore);
	List<Room> findByOwner(ClientAuthentication owner, int start, int limit);
	List<Room> findByOwnerId(String ownerId, int start, int limit);
	List<Room> findAllForPublicPool();
	List<Room> findForPublicPoolByOwner(ClientAuthentication owner);
	List<Room> getRoomsWithStatsForOwner(ClientAuthentication owner, int start, int limit);
	List<Room> getRoomHistoryWithStatsForUser(List<Room> rooms, ClientAuthentication owner);
	List<Room> findInfosForPublicPool();
	List<Room> findInfosForPublicPoolByOwner(ClientAuthentication owner);
	List<Room> findRoomsByCourses(List<Course> courses);
	Room importRoom(ClientAuthentication user, ImportExportContainer importRoom);
	ImportExportContainer exportRoom(String id, Boolean withAnswer, Boolean withFeedbackQuestions);
}
