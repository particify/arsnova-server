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
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.transport.ImportExportContainer;
import de.thm.arsnova.entities.transport.ScoreStatistics;

import java.util.List;
import java.util.UUID;

/**
 * The functionality the session service should provide.
 */
public interface RoomService extends EntityService<Room> {
	Room getByShortId(String shortId);

	Room getForAdmin(final String shortId);

	Room getInternal(String shortId, UserAuthentication user);

	Room save(Room session);

	boolean isShortIdAvailable(String shortId);

	String generateShortId();

	List<Room> getUserRooms(String userId);

	List<Room> getUserRoomHistory(String userId);

	List<Room> getMyRooms(int offset, int limit);

	List<Room> getMyRoomHistory(int offset, int limit);

	int countRoomsByCourses(List<Course> courses);

	int activeUsers(String shortId);

	Room setActive(String shortId, Boolean lock);

	Room join(String shortId, UUID socketId);

	Room update(String shortId, Room room);

	Room updateCreator(String shortId, String newCreator);

	Room updateInternal(Room room, UserAuthentication user);

	int[] deleteCascading(Room room);

	ScoreStatistics getLearningProgress(String shortId, String type, String questionVariant);

	ScoreStatistics getMyLearningProgress(String shortId, String type, String questionVariant);

	List<Room> getMyRoomsInfo(int offset, int limit);

	List<Room> getPublicPoolRoomsInfo();

	List<Room> getMyPublicPoolRoomsInfo();

	List<Room> getMyRoomHistoryInfo(int offset, int limit);

	Room importRooms(ImportExportContainer importExportRoom);

	ImportExportContainer exportRoom(String shortId, Boolean withAnswerStatistics, Boolean withFeedbackQuestions);

	Room copyRoomToPublicPool(String shortId, ImportExportContainer.PublicPool pp);

	Room.Settings getFeatures(String shortId);

	Room.Settings updateFeatures(String shortId, Room.Settings settings);

	boolean lockFeedbackInput(String shortId, Boolean lock);

	boolean flipFlashcards(String shortId, Boolean flip);

	void deleteInactiveRooms();
}
