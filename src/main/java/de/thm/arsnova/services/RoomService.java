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
package de.thm.arsnova.services;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.transport.ImportExportContainer;
import de.thm.arsnova.entities.transport.ScoreStatistics;

import java.util.List;
import java.util.UUID;

/**
 * The functionality the session service should provide.
 */
public interface RoomService extends EntityService<Room> {
	String getIdByShortId(String shortId);

	Room getByShortId(String shortId);

	Room getForAdmin(final String id);

	Room getInternal(String id, ClientAuthentication user);

	Room save(Room session);

	boolean isShortIdAvailable(String shortId);

	String generateShortId();

	List<Room> getUserRooms(String userId);

	List<Room> getUserRoomHistory(String userId);

	List<Room> getMyRooms(int offset, int limit);

	List<Room> getMyRoomHistory(int offset, int limit);

	int countRoomsByCourses(List<Course> courses);

	int activeUsers(String id);

	Room setActive(String id, Boolean lock);

	Room join(String id, UUID socketId);

	Room update(String id, Room room);

	Room updateCreator(String id, String newCreator);

	Room updateInternal(Room room, ClientAuthentication user);

	int[] deleteCascading(Room room);

	ScoreStatistics getLearningProgress(String id, String type, String questionVariant);

	ScoreStatistics getMyLearningProgress(String id, String type, String questionVariant);

	List<Room> getMyRoomsInfo(int offset, int limit);

	List<Room> getPublicPoolRoomsInfo();

	List<Room> getMyPublicPoolRoomsInfo();

	List<Room> getMyRoomHistoryInfo(int offset, int limit);

	Room importRooms(ImportExportContainer importExportRoom);

	ImportExportContainer exportRoom(String id, Boolean withAnswerStatistics, Boolean withFeedbackQuestions);

	Room copyRoomToPublicPool(String id, ImportExportContainer.PublicPool pp);

	Room.Settings getFeatures(String id);

	Room.Settings updateFeatures(String id, Room.Settings settings);

	boolean lockFeedbackInput(String id, Boolean lock);

	boolean flipFlashcards(String id, Boolean flip);

	void deleteInactiveRooms();
}
