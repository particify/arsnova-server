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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.transport.ImportExportContainer;
import net.particify.arsnova.connector.model.Course;

/**
 * The functionality the session service should provide.
 */
public interface RoomService extends EntityService<Room> {
	String getIdByShortId(String shortId);

	Room getByShortId(String shortId);

	Room getForAdmin(String id);

	boolean isShortIdAvailable(String shortId);

	String generateShortId();

	List<Room> getUserRooms(String userId);

	List<String> getUserRoomIds(String userId);

	List<String> getRoomIdsByModeratorId(String userId);

	List<Room> getUserRoomHistory(String userId);

	List<Room> getMyRooms(int offset, int limit);

	List<Room> getMyRoomHistory(int offset, int limit);

	int countRoomsByCourses(List<Course> courses);

	int activeUsers(String id);

	Room setActive(String id, Boolean lock) throws IOException;

	Room join(String id, UUID socketId);

	Room updateCreator(String id, String newCreator);

	Room transferOwnership(Room room, String newOwnerId);

	Room transferOwnershipThroughToken(Room room, String targetUserToken);

	List<Room> getMyRoomsInfo(int offset, int limit);

	List<Room> getPublicPoolRoomsInfo();

	List<Room> getMyPublicPoolRoomsInfo();

	List<Room> getMyRoomHistoryInfo(int offset, int limit);

	Room importRooms(ImportExportContainer importExportRoom);

	ImportExportContainer exportRoom(String id, Boolean withAnswerStatistics, Boolean withFeedbackQuestions);

	Room copyRoomToPublicPool(String id, ImportExportContainer.PublicPool pp);

	Room.Settings getFeatures(String id);

	Room.Settings updateFeatures(String id, Room.Settings settings);

	boolean lockFeedbackInput(String id, Boolean lock) throws IOException;

	boolean flipFlashcards(String id, Boolean flip);
}
