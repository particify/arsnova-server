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
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.LoggedIn;
import de.thm.arsnova.entities.transport.ImportExportSession;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RoomRepository extends CrudRepository<Room, String> {
	Room findByKeyword(String keyword);
	List<Room> findInactiveGuestSessionsMetadata(long lastActivityBefore);
	List<Room> findByUser(UserAuthentication user, int start, int limit);
	List<Room> findByUsername(String username, int start, int limit);
	List<Room> findAllForPublicPool();
	List<Room> findForPublicPoolByUser(UserAuthentication user);
	List<Room> getRoomsWithStatsForUser(UserAuthentication user, int start, int limit);
	List<Room> getVisitedRoomsWithStatsForUser(List<Room> rooms, UserAuthentication user);
	List<Room> findInfosForPublicPool();
	List<Room> findInfosForPublicPoolByUser(UserAuthentication user);
	List<Room> findSessionsByCourses(List<Course> courses);
	Room importSession(UserAuthentication user, ImportExportSession importSession);
	ImportExportSession exportSession(String sessionkey, Boolean withAnswer, Boolean withFeedbackQuestions);
	LoggedIn registerAsOnlineUser(UserAuthentication user, Room room);
}
